import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { formatTryAmount } from '../../core/format/currency.util';
import { CustomerReservation, CustomerVehicleReview } from '../marketplace/marketplace.models';
import { MarketplaceService } from '../marketplace/marketplace.service';

@Component({
  selector: 'acr-customer-reservation-detail',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    RouterLink
  ],
  template: `
    <a class="back" routerLink="/customer/account/reservations">
      <mat-icon>arrow_back</mat-icon>
      My bookings
    </a>

    @if (error()) {
      <div class="state error">{{ error() }}</div>
    } @else {
      @if (item(); as r) {
        <section class="detail">
          <header>
            <div>
              <span>{{ r.tenantName }}</span>
              <h1>{{ r.vehicleBrand }} {{ r.vehicleModel }}</h1>
              <p>{{ r.reservationCode }}</p>
            </div>
            <strong>{{ r.status }}</strong>
          </header>

          <dl>
            <div><dt>Pickup</dt><dd>{{ date(r.pickupDateTime) }}</dd></div>
            <div><dt>Return</dt><dd>{{ date(r.returnDateTime) }}</dd></div>
            <div><dt>Rental estimate</dt><dd>{{ money(r.estimatedRentalPrice) }}</dd></div>
            <div><dt>Insurance</dt><dd>{{ money(r.insuranceTotalPrice || 0) }}</dd></div>
            <div><dt>Deposit</dt><dd>{{ money(r.depositAmount) }}</dd></div>
            <div class="total"><dt>Total estimate</dt><dd>{{ money(r.totalEstimatedPrice) }}</dd></div>
            <div><dt>Payment</dt><dd>{{ r.paymentStatusSummary }}</dd></div>
          </dl>

          @if (r.status === 'PENDING_PAYMENT') {
            <p class="demo-note">Demo payment provider. No real card will be charged.</p>
            <button mat-flat-button color="primary" (click)="pay()" [disabled]="paying()">
              {{ paying() ? 'Processing...' : 'Pay demo deposit' }}
            </button>
          }
          @if (message()) {
            <p class="message">{{ message() }}</p>
          }
        </section>

        @if (r.status === 'COMPLETED') {
          <section class="review-panel">
            <div class="review-head">
              <div>
                <h2>{{ review() ? 'Edit your review' : 'Write a review' }}</h2>
                <p>Share feedback only after the rental has been completed.</p>
              </div>
              @if (review()) {
                <button mat-stroked-button color="warn" type="button" (click)="deleteReview()" [disabled]="reviewSaving()">
                  Delete
                </button>
              }
            </div>

            <form [formGroup]="reviewForm" (ngSubmit)="submitReview()">
              <mat-form-field appearance="outline">
                <mat-label>Rating</mat-label>
                <mat-select formControlName="rating">
                  @for (rating of ratings; track rating) {
                    <mat-option [value]="rating">{{ rating }} star{{ rating === 1 ? '' : 's' }}</mat-option>
                  }
                </mat-select>
                @if (reviewForm.controls.rating.hasError('required')) {
                  <mat-error>Rating is required</mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Title</mat-label>
                <input matInput formControlName="title" maxlength="100">
                @if (reviewForm.controls.title.hasError('maxlength')) {
                  <mat-error>Title can be at most 100 characters</mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline" class="full">
                <mat-label>Comment</mat-label>
                <textarea matInput formControlName="comment" rows="5" maxlength="1000"></textarea>
                @if (reviewForm.controls.comment.hasError('required')) {
                  <mat-error>Comment is required</mat-error>
                }
                @if (reviewForm.controls.comment.hasError('maxlength')) {
                  <mat-error>Comment can be at most 1000 characters</mat-error>
                }
              </mat-form-field>

              @if (reviewError()) {
                <p class="review-error">{{ reviewError() }}</p>
              }

              <button mat-flat-button color="primary" type="submit" [disabled]="reviewForm.invalid || reviewSaving()">
                {{ reviewSaving() ? 'Saving...' : (review() ? 'Update review' : 'Submit review') }}
              </button>
            </form>
          </section>
        } @else {
          <section class="review-panel muted">Only customers who completed a rental can review this vehicle.</section>
        }
      }
    }
  `,
  styles: [`
    .back{display:inline-flex;align-items:center;gap:6px;color:#315b86;margin-bottom:16px}
    .detail,.review-panel{max-width:760px;margin:auto;background:#fff;border:1px solid #dce2e9;border-radius:8px;padding:24px}
    .review-panel{margin-top:16px}
    .detail header,.review-head{display:flex;justify-content:space-between;gap:16px;border-bottom:1px solid #e5e9ef;padding-bottom:18px}
    .detail header span{font-size:12px;color:#1261b5;font-weight:700}
    .detail h1,.review-panel h2{margin:5px 0}
    .detail p,.review-head p{margin:0;color:#68758a}
    .detail header>strong{align-self:start;background:#eef3f8;padding:7px;border-radius:4px;font-size:12px}
    dl div{display:flex;justify-content:space-between;padding:11px 0;border-bottom:1px solid #edf0f4}
    dt{color:#68758a}
    dd{margin:0;font-weight:650}
    .total{font-size:17px}
    .demo-note{background:#f6f8fb;border:1px solid #dce2e9;border-radius:6px;color:#52627a;margin-top:16px!important;padding:10px 12px}
    .detail button,.review-panel form button[type=submit]{width:100%;height:46px;margin-top:12px}
    .message{color:#267b3f!important;text-align:center;margin-top:12px!important}
    .state{padding:30px;background:#fff}
    .error,.review-error{color:#a11c1c}
    .review-panel form{display:grid;grid-template-columns:180px 1fr;gap:12px;margin-top:18px}
    .review-panel .full,.review-panel form button,.review-error{grid-column:1/-1}
    .muted{color:#68758a;text-align:center}
    @media(max-width:650px){.detail header,.review-head{flex-direction:column}.review-panel form{grid-template-columns:1fr}}
  `]
})
export class CustomerReservationDetailComponent implements OnInit {
  private readonly service = inject(MarketplaceService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  readonly item = signal<CustomerReservation | null>(null);
  readonly review = signal<CustomerVehicleReview | null>(null);
  readonly error = signal('');
  readonly paying = signal(false);
  readonly message = signal('');
  readonly reviewError = signal('');
  readonly reviewSaving = signal(false);
  readonly ratings = [5, 4, 3, 2, 1];
  private readonly code = this.route.snapshot.paramMap.get('reservationCode') || '';

  readonly reviewForm = this.fb.group({
    rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    title: ['', [Validators.maxLength(100)]],
    comment: ['', [Validators.required, Validators.maxLength(1000)]]
  });

  ngOnInit(): void {
    this.load();
  }

  pay(): void {
    this.paying.set(true);
    this.service.payCustomerDeposit(this.code, globalThis.crypto.randomUUID()).subscribe({
      next: () => {
        this.message.set('Deposit payment completed.');
        this.load();
      },
      error: (e) => {
        this.error.set(e.error?.message || 'Payment failed.');
        this.paying.set(false);
      }
    });
  }

  submitReview(): void {
    if (this.reviewForm.invalid) {
      this.reviewForm.markAllAsTouched();
      return;
    }

    this.reviewSaving.set(true);
    this.reviewError.set('');
    const request = {
      rating: Number(this.reviewForm.controls.rating.value),
      title: this.reviewForm.controls.title.value || null,
      comment: this.reviewForm.controls.comment.value || ''
    };
    const call = this.review()
      ? this.service.updateCustomerReview(this.code, request)
      : this.service.createCustomerReview(this.code, request);

    call.subscribe({
      next: (review) => {
        this.review.set(review);
        this.reviewForm.patchValue({
          rating: review.rating,
          title: review.title || '',
          comment: review.comment
        });
        this.message.set('Review saved.');
      },
      error: (e) => this.reviewError.set(e.error?.message || 'Review could not be saved.'),
      complete: () => this.reviewSaving.set(false)
    });
  }

  deleteReview(): void {
    this.reviewSaving.set(true);
    this.service.deleteCustomerReview(this.code).subscribe({
      next: () => {
        this.review.set(null);
        this.reviewForm.reset({ rating: 5, title: '', comment: '' });
        this.message.set('Review deleted.');
      },
      error: (e) => this.reviewError.set(e.error?.message || 'Review could not be deleted.'),
      complete: () => this.reviewSaving.set(false)
    });
  }

  private load(): void {
    this.service.customerReservation(this.code).subscribe({
      next: (reservation) => {
        this.item.set(reservation);
        if (reservation.status === 'COMPLETED') {
          this.loadReview();
        }
      },
      error: (e) => this.error.set(e.error?.message || 'Booking could not be loaded.'),
      complete: () => this.paying.set(false)
    });
  }

  private loadReview(): void {
    this.service.getCustomerReview(this.code).subscribe({
      next: (review) => {
        this.review.set(review);
        this.reviewForm.patchValue({
          rating: review.rating,
          title: review.title || '',
          comment: review.comment
        });
      },
      error: () => this.review.set(null)
    });
  }

  money(value: number): string {
    return formatTryAmount(value);
  }

  date(value: string): string {
    return new Date(value).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' });
  }
}
