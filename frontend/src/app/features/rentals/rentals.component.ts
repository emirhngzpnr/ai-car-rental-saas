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
import { forkJoin } from 'rxjs';
import { formatTryAmount } from '../../core/format/currency.util';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LoadingStateComponent } from '../../shared/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { ReservationResponse } from '../reservations/reservation.models';
import { ReservationService } from '../reservations/reservation.service';
import { CompleteRentalRequest, RentalResponse, StartRentalRequest } from './rental.models';
import { RentalService } from './rental.service';
import { CompleteRentalDialogComponent } from './complete-rental-dialog.component';
import { StartRentalDialogComponent } from './start-rental-dialog.component';

@Component({
  selector: 'acr-rentals',
  imports: [DatePipe, MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule, MatPaginatorModule, MatTableModule, MatTooltipModule, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, StatusBadgeComponent],
  templateUrl: './rentals.component.html',
  styleUrl: './rentals.component.scss'
})
export class RentalsComponent implements OnInit {
  private readonly rentalService = inject(RentalService);
  private readonly reservationService = inject(ReservationService);
  private readonly dialog = inject(MatDialog);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly confirmedReservations = signal<ReservationResponse[]>([]);
  readonly dataSource = new MatTableDataSource<RentalResponse>([]);
  readonly displayedColumns = ['rental', 'vehicle', 'period', 'mileage', 'financials', 'status', 'actions'];
  private paginator: MatPaginator | undefined;

  @ViewChild(MatPaginator)
  set tablePaginator(paginator: MatPaginator | undefined) {
    this.paginator = paginator;
    this.dataSource.paginator = paginator ?? null;
  }

  ngOnInit(): void {
    this.dataSource.filterPredicate = (item, filter) =>
      `${item.id} ${item.vehiclePlateNumber} ${item.vehicleBrand} ${item.status} ${item.tenantName}`.toLowerCase().includes(filter);
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      rentals: this.rentalService.getRentals(),
      reservations: this.reservationService.getReservations()
    }).subscribe({
      next: ({ rentals, reservations }) => {
        this.dataSource.data = rentals;
        this.confirmedReservations.set(reservations.filter((reservation) => reservation.status === 'CONFIRMED'));
        this.paginator?.firstPage();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Rental data could not be loaded.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  applyFilter(value: string): void {
    this.dataSource.filter = value.trim().toLowerCase();
    this.dataSource.paginator?.firstPage();
  }

  openStartDialog(): void {
    const dialogRef = this.dialog.open(StartRentalDialogComponent, {
      data: { reservations: this.confirmedReservations() },
      width: '640px',
      maxWidth: '94vw'
    });
    dialogRef.afterClosed().subscribe((result?: StartRentalRequest) => {
      if (result) this.startRental(result);
    });
  }

  openCompleteDialog(rental: RentalResponse): void {
    const dialogRef = this.dialog.open(CompleteRentalDialogComponent, {
      data: { rental },
      width: '640px',
      maxWidth: '94vw'
    });
    dialogRef.afterClosed().subscribe((result?: CompleteRentalRequest) => {
      if (result) this.completeRental(rental.id, result);
    });
  }

  canComplete(rental: RentalResponse): boolean {
    return rental.status === 'ACTIVE';
  }

  money(value: number | string | null | undefined): string {
    return formatTryAmount(value);
  }

  private startRental(request: StartRentalRequest): void {
    this.saving.set(true);
    this.rentalService.startRental(request).subscribe({
      next: () => this.loadAll(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Rental could not be started.');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }

  private completeRental(id: number, request: CompleteRentalRequest): void {
    this.saving.set(true);
    this.rentalService.completeRental(id, request).subscribe({
      next: () => this.loadAll(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Rental could not be completed.');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }
}
