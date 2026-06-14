import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LoadingStateComponent } from '../../shared/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';
import { CreateTenantRequest, TenantResponse, UpdateTenantRequest } from './tenant.models';
import { TenantService } from './tenant.service';
import { TenantFormDialogComponent, TenantFormDialogResult } from './tenant-form-dialog.component';

@Component({
  selector: 'acr-tenants',
  imports: [MatButtonModule, MatDialogModule, MatIconModule, MatTableModule, MatTooltipModule, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, StatusBadgeComponent],
  templateUrl: './tenants.component.html',
  styleUrl: './tenants.component.scss'
})
export class TenantsComponent implements OnInit {
  private readonly tenantService = inject(TenantService);
  private readonly dialog = inject(MatDialog);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly tenants = signal<TenantResponse[]>([]);
  readonly columns = ['tenant', 'contact', 'subDomain', 'slug', 'status', 'actions'];

  ngOnInit(): void {
    this.loadTenants();
  }

  loadTenants(): void {
    this.loading.set(true);
    this.error.set('');
    this.tenantService.getTenants().subscribe({
      next: (tenants) => this.tenants.set(tenants),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Tenants could not be loaded.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(TenantFormDialogComponent, { data: { tenant: null }, width: '760px', maxWidth: '94vw' });
    dialogRef.afterClosed().subscribe((result?: TenantFormDialogResult) => {
      if (result) this.createTenant(result as CreateTenantRequest);
    });
  }

  openEditDialog(tenant: TenantResponse): void {
    const dialogRef = this.dialog.open(TenantFormDialogComponent, { data: { tenant }, width: '760px', maxWidth: '94vw' });
    dialogRef.afterClosed().subscribe((result?: TenantFormDialogResult) => {
      if (result) this.updateTenant(tenant.id, result as UpdateTenantRequest);
    });
  }

  deactivateTenant(tenant: TenantResponse): void {
    this.confirmDialog.confirm({
      title: 'Deactivate tenant',
      message: `Deactivate tenant ${tenant.companyName}? Tenant history remains available, but active operations should be reviewed first.`,
      confirmLabel: 'Deactivate',
      tone: 'danger'
    }).subscribe((confirmed) => {
      if (!confirmed) return;
      this.saving.set(true);
      this.tenantService.deleteTenant(tenant.id).subscribe({
        next: () => this.loadTenants(),
        error: (error: HttpErrorResponse) => {
          this.error.set(error.error?.message || 'Tenant could not be deactivated.');
          this.saving.set(false);
        },
        complete: () => this.saving.set(false)
      });
    });
  }

  private createTenant(request: CreateTenantRequest): void {
    this.saving.set(true);
    this.tenantService.createTenant(request).subscribe({
      next: () => this.loadTenants(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Tenant could not be created.');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }

  private updateTenant(id: number, request: UpdateTenantRequest): void {
    this.saving.set(true);
    this.tenantService.updateTenant(id, request).subscribe({
      next: () => this.loadTenants(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Tenant could not be updated.');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }
}
