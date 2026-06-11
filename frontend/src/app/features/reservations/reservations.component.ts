import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { formatTryAmount } from '../../core/format/currency.util';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LoadingStateComponent } from '../../shared/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';
import { VehicleService } from '../vehicles/vehicle.service';
import { VehicleResponse } from '../vehicles/vehicle.models';
import { CreateReservationRequest, ReservationResponse } from './reservation.models';
import { ReservationService } from './reservation.service';
import { ReservationFormDialogComponent } from './reservation-form-dialog.component';

@Component({
  selector: 'acr-reservations',
  imports: [DatePipe, MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule, MatPaginatorModule, MatTableModule, MatTooltipModule, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, StatusBadgeComponent],
  templateUrl: './reservations.component.html',
  styleUrl: './reservations.component.scss'
})
export class ReservationsComponent implements OnInit {
  private readonly reservationService = inject(ReservationService);
  private readonly vehicleService = inject(VehicleService);
  private readonly dialog = inject(MatDialog);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly vehicles = signal<VehicleResponse[]>([]);
  readonly dataSource = new MatTableDataSource<ReservationResponse>([]);
  readonly displayedColumns = ['reservation', 'vehicle', 'period', 'status', 'total', 'actions'];
  private paginator: MatPaginator | undefined;

  @ViewChild(MatPaginator)
  set tablePaginator(paginator: MatPaginator | undefined) {
    this.paginator = paginator;
    this.dataSource.paginator = paginator ?? null;
  }

  ngOnInit(): void {
    this.dataSource.filterPredicate = (item, filter) =>
      `${item.customerFullName} ${item.customerEmail} ${item.vehiclePlateNumber} ${item.status}`.toLowerCase().includes(filter);
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    this.error.set('');
    this.vehicleService.getVehicles().subscribe({
      next: (vehicles) => this.vehicles.set(vehicles.filter((vehicle) => vehicle.active)),
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message || 'Vehicles could not be loaded.')
    });
    this.reservationService.getReservations().subscribe({
      next: (reservations) => {
        this.dataSource.data = reservations;
        this.paginator?.firstPage();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Reservations could not be loaded.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  applyFilter(value: string): void {
    this.dataSource.filter = value.trim().toLowerCase();
    this.dataSource.paginator?.firstPage();
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(ReservationFormDialogComponent, {
      data: { vehicles: this.vehicles() },
      width: '820px',
      maxWidth: '94vw'
    });
    dialogRef.afterClosed().subscribe((result?: CreateReservationRequest) => {
      if (result) this.createReservation(result);
    });
  }

  cancelReservation(reservation: ReservationResponse): void {
    this.confirmDialog.confirm({
      title: 'Cancel reservation',
      message: `Cancel reservation #${reservation.id}? This action will release the vehicle for future operations.`,
      confirmLabel: 'Cancel reservation',
      tone: 'danger'
    }).subscribe((confirmed) => {
      if (!confirmed) return;
      this.saving.set(true);
      this.reservationService.cancelReservation(reservation.id).subscribe({
        next: () => this.loadAll(),
        error: (error: HttpErrorResponse) => {
          this.error.set(error.error?.message || 'Reservation could not be cancelled.');
          this.saving.set(false);
        },
        complete: () => this.saving.set(false)
      });
    });
  }

  confirmReservation(reservation: ReservationResponse): void {
    this.saving.set(true);
    this.reservationService.confirmReservation(reservation.id).subscribe({
      next: () => this.loadAll(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Reservation could not be confirmed.');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }

  canConfirm(reservation: ReservationResponse): boolean {
    return reservation.status === 'DEPOSIT_PAID';
  }

  canCancel(reservation: ReservationResponse): boolean {
    return !['CANCELLED', 'CONVERTED_TO_RENTAL'].includes(reservation.status);
  }

  money(value: number | string | null | undefined): string {
    return formatTryAmount(value);
  }

  private createReservation(request: CreateReservationRequest): void {
    this.saving.set(true);
    this.reservationService.createReservation(request).subscribe({
      next: () => this.loadAll(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Reservation could not be created.');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }
}
