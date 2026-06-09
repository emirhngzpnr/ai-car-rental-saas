import { Component, Input } from '@angular/core';

type StatusTone = 'pending' | 'success' | 'danger' | 'neutral';

@Component({
  selector: 'acr-status-badge',
  template: '<span class="acr-status" [class]="toneClass">{{ status }}</span>'
})
export class StatusBadgeComponent {
  @Input({ required: true }) status = '';

  get toneClass(): string {
    return `acr-status--${this.resolveTone(this.status)}`;
  }

  private resolveTone(status: string): StatusTone {
    const normalized = status.toUpperCase();

    if (['PENDING', 'PENDING_PAYMENT'].includes(normalized)) {
      return 'pending';
    }

    if (['APPROVED', 'SENT', 'CONFIRMED', 'SUCCESS', 'DEPOSIT_PAID'].includes(normalized)) {
      return 'success';
    }

    if (['REJECTED', 'FAILED', 'CANCELLED', 'EXPIRED'].includes(normalized)) {
      return 'danger';
    }

    return 'neutral';
  }
}
