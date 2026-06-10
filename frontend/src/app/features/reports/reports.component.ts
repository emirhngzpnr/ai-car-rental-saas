import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { forkJoin } from 'rxjs';
import { formatTryAmount } from '../../core/format/currency.util';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LoadingStateComponent } from '../../shared/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { MonthlyRevenueResponse, MonthlySummaryResponse, TopVehicleResponse } from './report.models';
import { ReportService } from './report.service';

@Component({
  selector: 'acr-reports',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatTableModule, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.scss'
})
export class ReportsComponent implements OnInit {
  private readonly reportService = inject(ReportService);
  private readonly formBuilder = inject(FormBuilder);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly monthlyRevenue = signal<MonthlyRevenueResponse[]>([]);
  readonly topVehicles = signal<TopVehicleResponse[]>([]);
  readonly monthlySummary = signal<MonthlySummaryResponse | null>(null);
  readonly revenueColumns = ['month', 'totalRevenue'];
  readonly topVehicleColumns = ['vehicle', 'plateNumber', 'rentalCount', 'totalRevenue'];

  readonly summaryForm = this.formBuilder.nonNullable.group({
    year: [new Date().getFullYear(), [Validators.required, Validators.min(2026)]],
    month: [new Date().getMonth() + 1, [Validators.required, Validators.min(1), Validators.max(12)]]
  });

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.loading.set(true);
    this.error.set('');
    const { year, month } = this.summaryForm.getRawValue();

    forkJoin({
      monthlyRevenue: this.reportService.getMonthlyRevenue(),
      topVehicles: this.reportService.getTopVehicles(5),
      monthlySummary: this.reportService.getMonthlySummary(year, month)
    }).subscribe({
      next: ({ monthlyRevenue, topVehicles, monthlySummary }) => {
        this.monthlyRevenue.set(monthlyRevenue);
        this.topVehicles.set(topVehicles);
        this.monthlySummary.set(monthlySummary);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Reports could not be loaded.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  money(value: number | string | null | undefined): string {
    return formatTryAmount(value);
  }
}
