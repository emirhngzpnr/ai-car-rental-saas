import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../core/auth/auth.service';
import { formatTryAmount } from '../../core/format/currency.util';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LoadingStateComponent } from '../../shared/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';
import { CreateInsurancePackageRequest, InsurancePackageResponse, UpdateInsurancePackageRequest } from './insurance-package.models';
import { InsurancePackageService } from './insurance-package.service';
import { InsurancePackageFormDialogComponent, InsurancePackageFormDialogResult } from './insurance-package-form-dialog.component';

@Component({
  selector: 'acr-insurance-packages',
  imports: [MatButtonModule, MatDialogModule, MatIconModule, MatTableModule, MatTooltipModule, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, StatusBadgeComponent],
  templateUrl: './insurance-packages.component.html',
  styleUrl: './insurance-packages.component.scss'
})
export class InsurancePackagesComponent implements OnInit {
  private readonly insurancePackageService = inject(InsurancePackageService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly packages = signal<InsurancePackageResponse[]>([]);
  readonly tenantId = signal(this.authService.session()?.tenantId ?? null);
  readonly canManage = signal(this.authService.hasAnyRole(['SUPER_ADMIN', 'TENANT_ADMIN']));
  readonly columns = ['package', 'coverage', 'price', 'status', 'actions'];

  ngOnInit(): void {
    this.loadPackages();
  }

  loadPackages(): void {
    const tenantId = this.tenantId();
    if (!tenantId) {
      this.packages.set([]);
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.insurancePackageService.getInsurancePackages(tenantId).subscribe({
      next: (packages) => this.packages.set(packages),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Insurance packages could not be loaded.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  openCreateDialog(): void {
    const tenantId = this.tenantId();
    if (!tenantId || !this.canManage()) return;

    const dialogRef = this.dialog.open(InsurancePackageFormDialogComponent, {
      data: { tenantId, insurancePackage: null },
      width: '620px',
      maxWidth: '94vw'
    });

    dialogRef.afterClosed().subscribe((result?: InsurancePackageFormDialogResult) => {
      if (result) this.createPackage(result as CreateInsurancePackageRequest);
    });
  }

  openEditDialog(insurancePackage: InsurancePackageResponse): void {
    const tenantId = this.tenantId();
    if (!tenantId || !this.canManage()) return;

    const dialogRef = this.dialog.open(InsurancePackageFormDialogComponent, {
      data: { tenantId, insurancePackage },
      width: '620px',
      maxWidth: '94vw'
    });

    dialogRef.afterClosed().subscribe((result?: InsurancePackageFormDialogResult) => {
      if (result) this.updatePackage(insurancePackage.id, result as UpdateInsurancePackageRequest);
    });
  }

  deactivatePackage(insurancePackage: InsurancePackageResponse): void {
    if (!this.canManage()) return;
    this.confirmDialog.confirm({
      title: 'Deactivate insurance package',
      message: `Deactivate insurance package ${insurancePackage.name}?`,
      confirmLabel: 'Deactivate',
      tone: 'danger'
    }).subscribe((confirmed) => {
      if (!confirmed) return;
      this.saving.set(true);
      this.insurancePackageService.deleteInsurancePackage(insurancePackage.id).subscribe({
        next: () => this.loadPackages(),
        error: (error: HttpErrorResponse) => {
          this.error.set(error.error?.message || 'Insurance package could not be deactivated.');
          this.saving.set(false);
        },
        complete: () => this.saving.set(false)
      });
    });
  }

  money(value: number | string | null | undefined): string {
    return formatTryAmount(value);
  }

  private createPackage(request: CreateInsurancePackageRequest): void {
    this.saving.set(true);
    this.insurancePackageService.createInsurancePackage(request).subscribe({
      next: () => this.loadPackages(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Insurance package could not be created.');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }

  private updatePackage(id: number, request: UpdateInsurancePackageRequest): void {
    this.saving.set(true);
    this.insurancePackageService.updateInsurancePackage(id, request).subscribe({
      next: () => this.loadPackages(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Insurance package could not be updated.');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }
}
