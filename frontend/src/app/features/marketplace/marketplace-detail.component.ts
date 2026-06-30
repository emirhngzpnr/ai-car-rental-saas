import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { formatTryAmount } from '../../core/format/currency.util';
import { MarketplaceVehicleDetail, VehicleReview } from './marketplace.models';
import { MarketplaceService } from './marketplace.service';

@Component({
  selector: 'acr-marketplace-detail',
  imports: [MatButtonModule, MatIconModule, RouterLink],
  template: `
    <a class="back" routerLink="/rent" [queryParams]="queryParams"><mat-icon>arrow_back</mat-icon>Back to results</a>
    @if (loading()) {
      <div class="state">Loading vehicle details...</div>
    } @else if (error()) {
      <div class="state error">{{ error() }}</div>
    } @else {
      @if (vehicle(); as v) {
        <section class="detail">
          <div class="media">
            @if (v.imageUrl) {
              <img [src]="v.imageUrl" [alt]="v.brand + ' ' + v.model">
            } @else {
              <mat-icon>directions_car</mat-icon>
            }
          </div>
          <div class="summary">
            <span class="tenant">{{ v.tenantName }}</span>
            <h1>{{ v.brand }} {{ v.model }}</h1>
            <p>{{ v.productionYear }} · {{ v.location || 'Pickup location confirmed after booking' }}</p>
            <div class="rating-summary" [class.rating-summary--empty]="!v.reviewCount">
              @if (v.reviewCount) {
                <span>★</span>
                <strong>{{ v.averageRating.toFixed(1) }}</strong>
                <small>{{ v.reviewCount }} review{{ v.reviewCount === 1 ? '' : 's' }}</small>
              } @else {
                <small>No reviews yet</small>
              }
            </div>
            <div class="specs">
              <div><mat-icon>speed</mat-icon><strong>{{ v.dailyKmLimit }} km</strong><span>Daily limit</span></div>
              <div><mat-icon>settings</mat-icon><strong>{{ v.transmission || 'N/A' }}</strong><span>Transmission</span></div>
              <div><mat-icon>local_gas_station</mat-icon><strong>{{ v.fuelType || 'N/A' }}</strong><span>Fuel</span></div>
              <div><mat-icon>group</mat-icon><strong>{{ v.seatCount || 'N/A' }}</strong><span>Seats</span></div>
            </div>
            <div class="price">
              <div><strong>{{ money(v.dailyPrice) }}</strong><span>per day · {{ money(v.extraKmPricePerKm) }} per extra km</span></div>
              <a mat-flat-button color="primary" [routerLink]="['/rent/checkout', v.vehicleId]" [queryParams]="queryParams">Reserve this car</a>
            </div>
          </div>
        </section>
        <section class="insurance">
          <h2>Coverage options</h2>
          @if (v.insurancePackages.length === 0) {
            <p>No optional insurance packages are currently available.</p>
          } @else {
            <div class="package-grid">
              @for (p of v.insurancePackages; track p.id) {
                <article><strong>{{ p.name }}</strong><span>{{ p.type }}</span><p>{{ p.coverageDescription }}</p><b>{{ money(p.dailyPrice) }} / day</b></article>
              }
            </div>
          }
        </section>
        <section class="reviews">
          <div class="section-head">
            <div>
              <h2>Customer reviews</h2>
              <p>Only customers who completed a rental can review this vehicle.</p>
            </div>
            @if (reviewsTotal() > 0) {
              <strong>{{ reviewsTotal() }} total</strong>
            }
          </div>
          @if (reviewsLoading()) {
            <div class="review-state">Loading reviews...</div>
          } @else if (reviewsError()) {
            <div class="review-state error">{{ reviewsError() }}</div>
          } @else if (reviews().length === 0) {
            <div class="review-state">No customer reviews yet.</div>
          } @else {
            <div class="review-list">
              @for (review of reviews(); track review.id) {
                <article>
                  <div class="review-meta">
                    <strong>{{ review.customerDisplayName }}</strong>
                    <span>{{ stars(review.rating) }}</span>
                  </div>
                  @if (review.title) {
                    <h3>{{ review.title }}</h3>
                  }
                  <p>{{ review.comment }}</p>
                  <small>{{ date(review.createdAt) }}</small>
                </article>
              }
            </div>
            @if (reviewsPage() + 1 < reviewsTotalPages()) {
              <button mat-stroked-button type="button" (click)="loadReviews(reviewsPage() + 1)">Load more reviews</button>
            }
          }
        </section>
      }
    }
  `,
  styles: [`
    .back{display:inline-flex;align-items:center;gap:6px;color:#315b86;margin-bottom:16px}.detail{display:grid;grid-template-columns:1.15fr 1fr;background:#fff;border:1px solid #dce2e9;border-radius:8px;overflow:hidden}.media{min-height:380px;background:#e9eef3;display:grid;place-items:center}.media img{width:100%;height:100%;object-fit:cover}.media mat-icon{font-size:76px;width:76px;height:76px;color:#8491a2}.summary{padding:34px}.tenant{color:#1261b5;font-size:12px;font-weight:700;text-transform:uppercase}.summary h1{font-size:32px;margin:8px 0}.summary>p{color:#68758a}.rating-summary{display:flex;align-items:center;gap:6px;color:#40516a;margin-top:10px}.rating-summary span{color:#b7791f}.rating-summary small{color:#68758a}.rating-summary--empty{color:#68758a}.specs{display:grid;grid-template-columns:repeat(2,1fr);gap:10px;margin:26px 0}.specs div{border:1px solid #e0e5eb;border-radius:6px;padding:13px;display:grid;grid-template-columns:auto 1fr;column-gap:9px}.specs mat-icon{grid-row:1/3;color:#51647b}.specs span{font-size:12px;color:#788497}.price{border-top:1px solid #e0e5eb;padding-top:20px;display:flex;justify-content:space-between;align-items:center}.price div{display:flex;flex-direction:column}.price strong{font-size:25px}.price span{font-size:12px;color:#68758a}.insurance,.reviews{margin-top:18px;background:#fff;border:1px solid #dce2e9;border-radius:8px;padding:22px}.insurance h2,.reviews h2{margin-top:0}.package-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.package-grid article{border:1px solid #dce2e9;border-radius:6px;padding:15px;display:flex;flex-direction:column;gap:5px}.package-grid span{font-size:11px;color:#1261b5}.package-grid p{color:#647186;font-size:13px;min-height:38px}.section-head{display:flex;justify-content:space-between;gap:16px}.section-head p{margin:0;color:#68758a}.review-state{border:1px solid #e0e5eb;border-radius:6px;color:#68758a;padding:24px;text-align:center}.review-list{display:grid;gap:10px}.review-list article{border:1px solid #e0e5eb;border-radius:6px;padding:14px}.review-meta{display:flex;justify-content:space-between;gap:12px}.review-meta span{color:#b7791f;letter-spacing:1px}.review-list h3{font-size:16px;margin:10px 0 4px}.review-list p{color:#40516a;margin:0 0 10px}.review-list small{color:#788497}.reviews button{margin-top:14px}.state{background:#fff;border:1px solid #dce2e9;padding:40px;text-align:center;border-radius:8px}.error{color:#a11c1c}@media(max-width:800px){.detail{grid-template-columns:1fr}.media{min-height:240px}.summary{padding:22px}.package-grid{grid-template-columns:1fr}}@media(max-width:480px){.specs{grid-template-columns:1fr}.price,.section-head{align-items:stretch;flex-direction:column;gap:14px}}
  `]
})
export class MarketplaceDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(MarketplaceService);
  readonly vehicle = signal<MarketplaceVehicleDetail | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly reviews = signal<VehicleReview[]>([]);
  readonly reviewsLoading = signal(false);
  readonly reviewsError = signal('');
  readonly reviewsPage = signal(0);
  readonly reviewsTotal = signal(0);
  readonly reviewsTotalPages = signal(0);
  queryParams: Record<string, string> = {};
  private vehicleId = 0;

  ngOnInit(): void {
    this.queryParams = { ...this.route.snapshot.queryParams };
    this.vehicleId = Number(this.route.snapshot.paramMap.get('vehicleId'));
    this.service.getVehicle(this.vehicleId).subscribe({
      next: (v) => this.vehicle.set(v),
      error: (e) => {
        this.error.set(e.error?.message || 'Vehicle could not be loaded.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
    this.loadReviews(0);
  }

  loadReviews(page: number): void {
    this.reviewsLoading.set(true);
    this.service.getVehicleReviews(this.vehicleId, page, 5).subscribe({
      next: (result) => {
        this.reviews.set(page === 0 ? result.content : [...this.reviews(), ...result.content]);
        this.reviewsPage.set(result.page);
        this.reviewsTotal.set(result.totalElements);
        this.reviewsTotalPages.set(result.totalPages);
        this.reviewsError.set('');
      },
      error: (e) => this.reviewsError.set(e.error?.message || 'Reviews could not be loaded.'),
      complete: () => this.reviewsLoading.set(false)
    });
  }

  money(value: number): string { return formatTryAmount(value); }
  stars(value: number): string { return '★'.repeat(value) + '☆'.repeat(5 - value); }
  date(value: string): string { return new Date(value).toLocaleDateString('en-GB', { dateStyle: 'medium' }); }
}
