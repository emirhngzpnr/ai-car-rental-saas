import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  LucideCar,
  LucideCircleAlert,
  LucideClock,
  LucideSearch,
  LucideSparkles,
  LucideX
} from '@lucide/angular';
import { formatTryAmount } from '../../core/format/currency.util';
import {
  MarketplaceVehicle,
  SemanticVehicleSearchCriteria,
  VehicleSearchCriteria
} from './marketplace.models';
import { MarketplaceService } from './marketplace.service';

@Component({
  selector: 'acr-marketplace-search',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    LucideCar,
    LucideCircleAlert,
    LucideClock,
    LucideSearch,
    LucideSparkles,
    LucideX
  ],
  templateUrl: './marketplace-search.component.html',
  styleUrl: './marketplace-search.component.scss'
})
export class MarketplaceSearchComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(MarketplaceService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly categories = ['ECONOMY', 'COMPACT', 'SEDAN', 'SUV', 'LUXURY', 'VAN'];
  readonly fuels = ['GASOLINE', 'DIESEL', 'HYBRID', 'ELECTRIC', 'LPG'];
  readonly timeOptions = Array.from(
    { length: 96 },
    (_, index) => `${String(Math.floor(index / 4)).padStart(2, '0')}:${String((index % 4) * 15).padStart(2, '0')}`
  );
  readonly today = this.startOfDay(new Date());

  readonly vehicles = signal<MarketplaceVehicle[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly page = signal(0);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly aiLoading = signal(false);
  readonly aiError = signal('');
  readonly aiSummary = signal('');
  readonly aiInferences = signal<string[]>([]);
  readonly aiWarnings = signal<string[]>([]);
  readonly appliedAiFilters = signal<string[]>([]);

  readonly form = this.fb.group({
    aiQuery: ['', [Validators.maxLength(500)]],
    pickupDate: [null as Date | null, Validators.required],
    pickupTime: ['10:00', Validators.required],
    returnDate: [null as Date | null, Validators.required],
    returnTime: ['10:00', Validators.required],
    location: [''],
    minDailyPrice: [null as number | null],
    maxDailyPrice: [null as number | null],
    minDailyKmLimit: [null as number | null],
    brand: [''],
    categories: [[] as string[]],
    transmission: [''],
    fuelType: [''],
    minSeats: [null as number | null],
    sort: ['recommended']
  });

  ngOnInit(): void {
    const query = this.route.snapshot.queryParamMap;
    const pickup = this.parseDateTime(query.get('pickupDateTime'), 1);
    const returned = this.parseDateTime(query.get('returnDateTime'), 3);
    const categories = query.getAll('categories').flatMap((item) => item.split(',')).filter(Boolean);

    this.form.patchValue({
      pickupDate: pickup.date,
      pickupTime: pickup.time,
      returnDate: returned.date,
      returnTime: returned.time,
      location: query.get('location') || '',
      minDailyPrice: this.num(query.get('minDailyPrice')),
      maxDailyPrice: this.num(query.get('maxDailyPrice')),
      minDailyKmLimit: this.num(query.get('minDailyKmLimit')),
      brand: query.get('brand') || '',
      categories,
      transmission: query.get('transmission') || '',
      fuelType: query.get('fuelType') || '',
      minSeats: this.num(query.get('minSeats')),
      sort: query.get('sort') || 'recommended'
    });
    this.search(Number(query.get('page') || 0));
  }

  applyAiFilters(): void {
    const value = this.form.getRawValue();
    const query = value.aiQuery?.trim();
    if (!query) {
      this.aiError.set('Describe the vehicle you need before applying AI filters.');
      return;
    }
    if (!value.pickupDate || !value.returnDate || !value.pickupTime || !value.returnTime) {
      this.form.markAllAsTouched();
      this.aiError.set('Select pickup and return dates before applying AI filters.');
      return;
    }

    const pickupDateTime = this.combine(value.pickupDate, value.pickupTime);
    const returnDateTime = this.combine(value.returnDate, value.returnTime);
    if (new Date(returnDateTime) <= new Date(pickupDateTime)) {
      this.aiError.set('Return date and time must be after pickup.');
      return;
    }

    this.aiLoading.set(true);
    this.aiError.set('');
    this.aiSummary.set('');
    this.aiInferences.set([]);
    this.aiWarnings.set([]);
    this.appliedAiFilters.set([]);
    this.service.interpretSearch({
      query,
      pickupDateTime,
      returnDateTime,
      location: value.location || undefined
    }).subscribe({
      next: (response) => {
        this.applyCriteria(response.criteria);
        this.aiSummary.set(response.summary);
        this.aiInferences.set(response.inferences);
        this.aiWarnings.set(response.warnings);
        this.appliedAiFilters.set(this.describeCriteria(response.criteria));
        this.search(0);
      },
      error: (apiError) => {
        this.aiSummary.set('');
        this.aiInferences.set([]);
        this.aiWarnings.set([]);
        this.appliedAiFilters.set([]);
        this.aiError.set(
          apiError.error?.message
          || 'AI search is temporarily unavailable. You can continue with the manual filters.'
        );
        this.aiLoading.set(false);
      },
      complete: () => this.aiLoading.set(false)
    });
  }

  clearAiInterpretation(): void {
    this.form.controls.aiQuery.setValue('');
    this.aiError.set('');
    this.aiSummary.set('');
    this.aiInferences.set([]);
    this.aiWarnings.set([]);
    this.appliedAiFilters.set([]);
  }

  search(page: number): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const criteria = this.buildCriteria(page);
    if (new Date(criteria.returnDateTime) <= new Date(criteria.pickupDateTime)) {
      this.error.set('Return date and time must be after pickup.');
      return;
    }

    this.loading.set(true);
    this.error.set('');
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: criteria,
      replaceUrl: true
    });
    this.service.search(criteria).subscribe({
      next: (response) => {
        this.vehicles.set(response.content);
        this.page.set(response.page);
        this.total.set(response.totalElements);
        this.totalPages.set(response.totalPages);
      },
      error: (apiError) => {
        this.error.set(this.searchErrorMessage(apiError));
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  detailParams(): object {
    return this.buildCriteria(this.page());
  }

  money(value: number): string {
    return formatTryAmount(value);
  }

  private applyCriteria(criteria: SemanticVehicleSearchCriteria): void {
    this.form.patchValue({
      minDailyPrice: criteria.minDailyPrice,
      maxDailyPrice: criteria.maxDailyPrice,
      minDailyKmLimit: criteria.minDailyKmLimit,
      brand: criteria.brand || '',
      categories: criteria.categories || [],
      transmission: criteria.transmission || '',
      fuelType: criteria.fuelType || '',
      minSeats: criteria.minSeats,
      location: criteria.location || this.form.controls.location.value || '',
      sort: criteria.sort || 'recommended'
    });
  }

  private describeCriteria(criteria: SemanticVehicleSearchCriteria): string[] {
    const chips: string[] = [];
    if (criteria.minDailyPrice != null) chips.push(`Min ${formatTryAmount(criteria.minDailyPrice)}`);
    if (criteria.maxDailyPrice != null) chips.push(`Max ${formatTryAmount(criteria.maxDailyPrice)}`);
    if (criteria.minDailyKmLimit != null) chips.push(`${criteria.minDailyKmLimit}+ km/day`);
    if (criteria.categories?.length) chips.push(...criteria.categories.map((item) => this.label(item)));
    if (criteria.transmission) chips.push(this.label(criteria.transmission));
    if (criteria.fuelType) chips.push(this.label(criteria.fuelType));
    if (criteria.minSeats != null) chips.push(`${criteria.minSeats}+ seats`);
    if (criteria.location) chips.push(criteria.location);
    return chips;
  }

  private buildCriteria(page: number): VehicleSearchCriteria {
    const value = this.form.getRawValue();
    return {
      pickupDateTime: this.combine(value.pickupDate!, value.pickupTime!),
      returnDateTime: this.combine(value.returnDate!, value.returnTime!),
      location: value.location || '',
      minDailyPrice: value.minDailyPrice,
      maxDailyPrice: value.maxDailyPrice,
      minDailyKmLimit: value.minDailyKmLimit,
      brand: value.brand || '',
      categories: value.categories || [],
      transmission: value.transmission || '',
      fuelType: value.fuelType || '',
      minSeats: value.minSeats,
      sort: value.sort || 'recommended',
      page: Math.max(0, page),
      size: 12
    };
  }

  private combine(date: Date, time: string): string {
    const [hours, minutes] = time.split(':').map(Number);
    const value = new Date(date);
    value.setHours(hours, minutes, 0, 0);
    const local = new Date(value.getTime() - value.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 19);
  }

  private parseDateTime(value: string | null, days: number): { date: Date; time: string } {
    const date = value ? new Date(value) : this.defaultDate(days);
    return {
      date: this.startOfDay(date),
      time: `${String(date.getHours()).padStart(2, '0')}:${String(Math.floor(date.getMinutes() / 15) * 15).padStart(2, '0')}`
    };
  }

  private defaultDate(days: number): Date {
    const date = new Date();
    date.setDate(date.getDate() + days);
    date.setHours(10, 0, 0, 0);
    return date;
  }

  private startOfDay(date: Date): Date {
    const value = new Date(date);
    value.setHours(0, 0, 0, 0);
    return value;
  }

  private num(value: string | null): number | null {
    return value ? Number(value) : null;
  }

  private label(value: string): string {
    return value.toLowerCase().replace('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
  }

  private searchErrorMessage(apiError: { status?: number; error?: { message?: string } }): string {
    if (apiError.status === 0) {
      return 'The rental service is currently unreachable. Make sure the backend is running and try again.';
    }
    return apiError.error?.message || 'Please verify the dates and filters.';
  }
}
