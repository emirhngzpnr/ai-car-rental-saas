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
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LoadingStateComponent } from '../../shared/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { NotificationChannel, NotificationResponse, NotificationStatus, NotificationType } from './notification.models';
import { NotificationService } from './notification.service';

@Component({
  selector: 'acr-notifications',
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatPaginatorModule, MatSelectModule, MatTableModule, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, StatusBadgeComponent],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss'
})
export class NotificationsComponent implements OnInit {
  private readonly notificationService = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly notifications = signal<NotificationResponse[]>([]);
  readonly totalElements = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly displayedColumns = ['notification', 'recipient', 'channel', 'status', 'sentAt'];
  readonly types: NotificationType[] = ['PAYMENT_COMPLETED', 'RESERVATION_CREATED', 'RESERVATION_CONFIRMED', 'RESERVATION_EXPIRED', 'RENTAL_STARTED', 'RENTAL_COMPLETED', 'REFUND_PROCESSED', 'REFUND_COMPLETED', 'AI_PRICING_APPROVED'];
  readonly channels: NotificationChannel[] = ['EMAIL', 'SMS', 'PUSH'];
  readonly statuses: NotificationStatus[] = ['PENDING', 'SENT', 'FAILED'];

  readonly filterForm = this.formBuilder.nonNullable.group({
    type: ['' as NotificationType | ''],
    channel: ['' as NotificationChannel | ''],
    status: ['' as NotificationStatus | '']
  });

  ngOnInit(): void {
    this.loadNotifications();
  }

  loadNotifications(): void {
    this.loading.set(true);
    this.error.set('');
    this.notificationService.getNotifications({
      ...this.filterForm.getRawValue(),
      page: this.pageIndex(),
      size: this.pageSize()
    }).subscribe({
      next: (page) => {
        this.notifications.set(page.content);
        this.totalElements.set(page.totalElements);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Notifications could not be loaded.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  applyFilters(): void {
    this.pageIndex.set(0);
    this.loadNotifications();
  }

  pageChanged(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadNotifications();
  }
}
