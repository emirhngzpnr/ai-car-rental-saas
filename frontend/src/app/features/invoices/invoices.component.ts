import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { formatTryAmount } from '../../core/format/currency.util';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LoadingStateComponent } from '../../shared/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { InvoiceResponse, InvoiceStatus, InvoiceType } from './invoice.models';
import { InvoiceService } from './invoice.service';

@Component({
  selector: 'acr-invoices',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatPaginatorModule,
    MatSelectModule,
    MatTableModule,
    EmptyStateComponent,
    ErrorStateComponent,
    LoadingStateComponent,
    PageHeaderComponent,
    StatusBadgeComponent
  ],
  templateUrl: './invoices.component.html',
  styleUrl: './invoices.component.scss'
})
export class InvoicesComponent implements OnInit {
  private readonly invoiceService = inject(InvoiceService);
  private readonly formBuilder = inject(FormBuilder);

  readonly loading = signal(false);
  readonly downloadingId = signal<number | null>(null);
  readonly error = signal('');
  readonly invoices = signal<InvoiceResponse[]>([]);
  readonly totalElements = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly displayedColumns = ['invoice', 'customer', 'vehicle', 'amount', 'status', 'issuedAt', 'actions'];
  readonly statuses: (InvoiceStatus | '')[] = ['', 'ISSUED', 'CANCELLED'];
  readonly types: (InvoiceType | '')[] = ['', 'RENTAL_COMPLETION', 'REFUND'];

  readonly filterForm = this.formBuilder.nonNullable.group({
    status: ['' as InvoiceStatus | ''],
    type: ['' as InvoiceType | '']
  });

  ngOnInit(): void {
    this.loadInvoices();
  }

  loadInvoices(): void {
    this.loading.set(true);
    this.error.set('');

    this.invoiceService.getInvoices({
      ...this.filterForm.getRawValue(),
      page: this.pageIndex(),
      size: this.pageSize()
    }).subscribe({
      next: (page) => {
        this.invoices.set(page.content);
        this.totalElements.set(page.totalElements);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Invoices could not be loaded.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  applyFilters(): void {
    this.pageIndex.set(0);
    this.loadInvoices();
  }

  pageChanged(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadInvoices();
  }

  downloadInvoice(invoice: InvoiceResponse): void {
    this.downloadingId.set(invoice.id);
    this.error.set('');

    this.invoiceService.downloadInvoicePdf(invoice.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `${invoice.invoiceNumber}.pdf`;
        anchor.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Invoice PDF could not be downloaded.');
        this.downloadingId.set(null);
      },
      complete: () => this.downloadingId.set(null)
    });
  }

  money(value: number | string | null | undefined): string {
    return formatTryAmount(value);
  }
}
