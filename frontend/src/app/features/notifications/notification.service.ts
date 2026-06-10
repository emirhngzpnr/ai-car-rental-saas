import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../../core/http/page-response';
import { NotificationChannel, NotificationResponse, NotificationStatus, NotificationType } from './notification.models';

export interface NotificationQuery {
  type?: NotificationType | '';
  channel?: NotificationChannel | '';
  status?: NotificationStatus | '';
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/notifications`;

  getNotifications(query: NotificationQuery): Observable<PageResponse<NotificationResponse>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);

    if (query.type) params = params.set('type', query.type);
    if (query.channel) params = params.set('channel', query.channel);
    if (query.status) params = params.set('status', query.status);

    return this.http.get<PageResponse<NotificationResponse>>(this.baseUrl, { params });
  }
}
