import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LoadingStateComponent } from '../../shared/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { TenantSettingResponse } from './tenant-setting.models';
import { TenantSettingService } from './tenant-setting.service';

@Component({
  selector: 'acr-tenant-settings',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule, MatTableModule, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, StatusBadgeComponent],
  templateUrl: './tenant-settings.component.html',
  styleUrl: './tenant-settings.component.scss'
})
export class TenantSettingsComponent implements OnInit {
  private readonly tenantSettingService = inject(TenantSettingService);
  private readonly authService = inject(AuthService);

  readonly loading = signal(false);
  readonly savingKey = signal('');
  readonly error = signal('');
  readonly settings = signal<TenantSettingResponse[]>([]);
  readonly editValues = signal<Record<string, string>>({});
  readonly displayedColumns = ['setting', 'value', 'type', 'status', 'actions'];
  readonly canManage = signal(this.authService.hasAnyRole(['TENANT_ADMIN']));

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.loading.set(true);
    this.error.set('');

    this.tenantSettingService.getCurrentTenantSettings().subscribe({
      next: (settings) => {
        this.settings.set(settings);
        this.editValues.set(Object.fromEntries(settings.map((setting) => [setting.settingKey, setting.settingValue])));
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Tenant settings could not be loaded.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  updateValue(setting: TenantSettingResponse, value: string): void {
    this.editValues.update((values) => ({ ...values, [setting.settingKey]: value }));
  }

  save(setting: TenantSettingResponse): void {
    if (!this.canManage() || !this.isValid(setting)) return;

    this.savingKey.set(setting.settingKey);
    this.error.set('');
    this.tenantSettingService.updateCurrentTenantSetting(setting.settingKey, {
      settingValue: this.editValues()[setting.settingKey]
    }).subscribe({
      next: (updated) => {
        this.settings.update((settings) => settings.map((item) => item.id === updated.id ? updated : item));
        this.updateValue(updated, updated.settingValue);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Tenant setting could not be updated.');
        this.savingKey.set('');
      },
      complete: () => this.savingKey.set('')
    });
  }

  isDirty(setting: TenantSettingResponse): boolean {
    return this.editValues()[setting.settingKey] !== setting.settingValue;
  }

  isValid(setting: TenantSettingResponse): boolean {
    const value = this.editValues()[setting.settingKey];
    if (value === undefined || value === '') return false;
    if (setting.dataType === 'INTEGER') return Number.isInteger(Number(value));
    if (setting.dataType === 'DECIMAL') return !Number.isNaN(Number(value));
    if (setting.dataType === 'BOOLEAN') return value === 'true' || value === 'false';
    return value.trim().length > 0;
  }
}
