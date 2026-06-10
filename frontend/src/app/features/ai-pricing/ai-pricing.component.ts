import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { formatTryAmount } from '../../core/format/currency.util';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LoadingStateComponent } from '../../shared/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { VehicleResponse } from '../vehicles/vehicle.models';
import { VehicleService } from '../vehicles/vehicle.service';
import { AiPricingRecommendationManagementResponse, AiPricingRecommendationResponse, AiPricingStatus } from './ai-pricing.models';
import { AiPricingService } from './ai-pricing.service';

@Component({
  selector: 'acr-ai-pricing',
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatPaginatorModule, MatSelectModule, MatTableModule, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, StatusBadgeComponent],
  templateUrl: './ai-pricing.component.html',
  styleUrl: './ai-pricing.component.scss'
})
export class AiPricingComponent implements OnInit {
  private readonly aiPricingService = inject(AiPricingService);
  private readonly vehicleService = inject(VehicleService);
  private readonly formBuilder = inject(FormBuilder);

  readonly loading = signal(false);
  readonly processing = signal(false);
  readonly error = signal('');
  readonly vehicles = signal<VehicleResponse[]>([]);
  readonly recommendation = signal<AiPricingRecommendationResponse | null>(null);
  readonly recommendations = signal<AiPricingRecommendationManagementResponse[]>([]);
  readonly totalElements = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly columns = ['vehicle', 'prices', 'confidence', 'status', 'createdAt', 'actions'];
  readonly statuses: (AiPricingStatus | '')[] = ['', 'PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'];

  readonly recommendationForm = this.formBuilder.nonNullable.group({
    vehicleId: [0, [Validators.required, Validators.min(1)]]
  });

  readonly filterForm = this.formBuilder.nonNullable.group({
    status: ['PENDING' as AiPricingStatus | '']
  });

  ngOnInit(): void {
    this.loadInitialData();
  }

  loadInitialData(): void {
    this.loading.set(true);
    this.error.set('');
    this.vehicleService.getVehicles().subscribe({
      next: (vehicles) => this.vehicles.set(vehicles.filter((vehicle) => vehicle.active)),
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message || 'Vehicles could not be loaded.')
    });
    this.loadRecommendations();
  }

  loadRecommendations(): void {
    this.loading.set(true);
    this.aiPricingService.getRecommendations(this.filterForm.controls.status.value, this.pageIndex(), this.pageSize()).subscribe({
      next: (page) => {
        this.recommendations.set(page.content);
        this.totalElements.set(page.totalElements);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'AI pricing recommendations could not be loaded.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  requestRecommendation(): void {
    if (this.recommendationForm.invalid) {
      this.recommendationForm.markAllAsTouched();
      return;
    }

    this.processing.set(true);
    this.error.set('');
    this.aiPricingService.recommendPrice(this.recommendationForm.controls.vehicleId.value).subscribe({
      next: (recommendation) => {
        this.recommendation.set(recommendation);
        this.pageIndex.set(0);
        this.loadRecommendations();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Recommendation could not be generated.');
        this.processing.set(false);
      },
      complete: () => this.processing.set(false)
    });
  }

  approve(item: AiPricingRecommendationManagementResponse): void {
    this.processing.set(true);
    this.aiPricingService.approveRecommendation(item.id).subscribe({
      next: () => this.loadRecommendations(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Recommendation could not be approved.');
        this.processing.set(false);
      },
      complete: () => this.processing.set(false)
    });
  }

  reject(item: AiPricingRecommendationManagementResponse): void {
    this.processing.set(true);
    this.aiPricingService.rejectRecommendation(item.id).subscribe({
      next: () => this.loadRecommendations(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Recommendation could not be rejected.');
        this.processing.set(false);
      },
      complete: () => this.processing.set(false)
    });
  }

  applyFilters(): void {
    this.pageIndex.set(0);
    this.loadRecommendations();
  }

  pageChanged(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadRecommendations();
  }

  canAct(item: AiPricingRecommendationManagementResponse): boolean {
    return item.status === 'PENDING';
  }

  money(value: number | string | null | undefined): string {
    return formatTryAmount(value);
  }
}
