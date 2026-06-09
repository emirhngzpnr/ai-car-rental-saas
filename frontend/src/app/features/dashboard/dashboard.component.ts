import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { LoadingStateComponent } from '../../shared/loading-state/loading-state.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { DashboardSummaryResponse } from './dashboard.models';
import { DashboardService } from './dashboard.service';
import { formatTryAmount } from '../../core/format/currency.util';

@Component({
  selector: 'acr-dashboard',
  imports: [
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    EmptyStateComponent,
    LoadingStateComponent,
    ErrorStateComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly summary = signal<DashboardSummaryResponse | null>(null);

  ngOnInit(): void {
    this.loadSummary();
  }

  loadSummary(): void {
    this.loading.set(true);
    this.error.set('');

    this.dashboardService.getSummary().subscribe({
      next: (summary) => this.summary.set(summary),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Dashboard summary could not be loaded.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  money(value: number | string | null | undefined): string {
    return formatTryAmount(value);
  }
}
