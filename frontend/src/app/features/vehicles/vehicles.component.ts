import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LoadingStateComponent } from '../../shared/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';
import { formatTryAmount } from '../../core/format/currency.util';
import { CreateVehicleRequest, UpdateVehicleRequest, VehicleResponse } from './vehicle.models';
import { VehicleService } from './vehicle.service';
import { VehicleFormDialogComponent, VehicleFormDialogResult } from './vehicle-form-dialog.component';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'acr-vehicles',
  imports: [
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatTableModule,
    MatTooltipModule,
    EmptyStateComponent,
    ErrorStateComponent,
    LoadingStateComponent,
    PageHeaderComponent,
    StatusBadgeComponent
  ],
  templateUrl: './vehicles.component.html',
  styleUrl: './vehicles.component.scss'
})
export class VehiclesComponent implements OnInit {
  private readonly vehicleService = inject(VehicleService);
  private readonly dialog = inject(MatDialog);
  private readonly authService = inject(AuthService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly canManage = signal(this.authService.hasAnyRole(['SUPER_ADMIN', 'TENANT_ADMIN']));
  readonly dataSource = new MatTableDataSource<VehicleResponse>([]);
  readonly displayedColumns = ['vehicle', 'plateNumber', 'status', 'pricing', 'mileage', 'tenant', 'active', 'actions'];

  private paginator: MatPaginator | undefined;

  @ViewChild(MatPaginator)
  set tablePaginator(paginator: MatPaginator | undefined) {
    this.paginator = paginator;
    this.dataSource.paginator = paginator ?? null;
  }

  ngOnInit(): void {
    this.dataSource.filterPredicate = (vehicle, filter) => {
      const value = `${vehicle.brand} ${vehicle.model} ${vehicle.plateNumber} ${vehicle.status} ${vehicle.tenantName ?? ''}`.toLowerCase();
      return value.includes(filter);
    };
    this.loadVehicles();
  }

  loadVehicles(): void {
    this.loading.set(true);
    this.error.set('');

    this.vehicleService.getVehicles().subscribe({
      next: (vehicles) => {
        this.dataSource.data = vehicles;
        if (this.paginator) {
          this.paginator.firstPage();
        }
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Vehicles could not be loaded.');
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
    if (!this.canManage()) return;

    const dialogRef = this.dialog.open(VehicleFormDialogComponent, {
      data: { vehicle: null },
      width: '780px',
      maxWidth: '94vw'
    });

    dialogRef.afterClosed().subscribe((result?: VehicleFormDialogResult) => {
      if (result) {
        this.createVehicle(result as CreateVehicleRequest);
      }
    });
  }

  openEditDialog(vehicle: VehicleResponse): void {
    if (!this.canManage()) return;

    const dialogRef = this.dialog.open(VehicleFormDialogComponent, {
      data: { vehicle },
      width: '780px',
      maxWidth: '94vw'
    });

    dialogRef.afterClosed().subscribe((result?: VehicleFormDialogResult) => {
      if (result) {
        this.updateVehicle(vehicle.id, result as UpdateVehicleRequest);
      }
    });
  }

  deleteVehicle(vehicle: VehicleResponse): void {
    if (!this.canManage()) return;

    this.confirmDialog.confirm({
      title: 'Deactivate vehicle',
      message: `Deactivate vehicle ${vehicle.plateNumber}? Existing historical records will remain available.`,
      confirmLabel: 'Deactivate',
      tone: 'danger'
    }).subscribe((confirmed) => {
      if (!confirmed) return;
      this.saving.set(true);
      this.vehicleService.deleteVehicle(vehicle.id).subscribe({
        next: () => this.loadVehicles(),
        error: (error: HttpErrorResponse) => {
          this.error.set(error.error?.message || 'Vehicle could not be deactivated.');
          this.saving.set(false);
        },
        complete: () => this.saving.set(false)
      });
    });
  }

  money(value: number | string | null | undefined): string {
    return formatTryAmount(value);
  }

  private createVehicle(request: CreateVehicleRequest): void {
    this.saving.set(true);
    this.vehicleService.createVehicle(request).subscribe({
      next: () => this.loadVehicles(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Vehicle could not be created.');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }

  private updateVehicle(id: number, request: UpdateVehicleRequest): void {
    this.saving.set(true);
    this.vehicleService.updateVehicle(id, request).subscribe({
      next: () => this.loadVehicles(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Vehicle could not be updated.');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }
}
