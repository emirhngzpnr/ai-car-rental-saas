import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { formatTryAmount } from '../../core/format/currency.util';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LoadingStateComponent } from '../../shared/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';
import { ReservationResponse } from '../reservations/reservation.models';
import { ReservationService } from '../reservations/reservation.service';
import { PaymentService } from './payment.service';
import { PaymentTransactionResponse } from './payment.models';

@Component({
  selector: 'acr-payments',
  imports: [
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    EmptyStateComponent,
    ErrorStateComponent,
    LoadingStateComponent,
    PageHeaderComponent,
    StatusBadgeComponent
  ],
  templateUrl: './payments.component.html',
  styleUrl: './payments.component.scss'
})
export class PaymentsComponent implements OnInit {
  private readonly reservationService = inject(ReservationService);
  private readonly paymentService = inject(PaymentService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly loading = signal(false);
  readonly processing = signal(false);
  readonly error = signal('');
  readonly lastPayment = signal<PaymentTransactionResponse | null>(null);
  readonly reservations = signal<ReservationResponse[]>([]);
  readonly reservationColumns = ['reservation', 'vehicle', 'amount', 'status', 'actions'];

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set('');

    this.reservationService.getReservations().subscribe({
      next: (reservations) => {
        this.reservations.set(reservations.filter((item) => item.status === 'PENDING_PAYMENT'));
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Payment operations could not be loaded.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  createDepositPayment(reservation: ReservationResponse): void {
    this.confirmDialog.confirm({
      title: 'Create deposit payment',
      message: `Create deposit payment for reservation #${reservation.id}? The payment request will use a new idempotency key.`,
      confirmLabel: 'Create payment'
    }).subscribe((confirmed) => {
      if (!confirmed) return;
      this.processing.set(true);
      this.error.set('');
      this.paymentService.createPayment({
        tenantId: reservation.tenantId,
        reservationId: reservation.id,
        rentalId: null,
        paymentType: 'DEPOSIT_PAYMENT',
        amount: reservation.depositAmount,
        idempotencyKey: this.createIdempotencyKey('deposit', reservation.id)
      }).subscribe({
        next: (payment) => {
          this.lastPayment.set(payment);
          this.loadData();
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(error.error?.message || 'Deposit payment could not be created.');
          this.processing.set(false);
        },
        complete: () => this.processing.set(false)
      });
    });
  }

  money(value: number | string | null | undefined): string {
    return formatTryAmount(value);
  }

  private createIdempotencyKey(prefix: string, id: number): string {
    const randomPart = typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`;

    return `${prefix}-${id}-${randomPart}`;
  }
}
