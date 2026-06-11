import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TenantSettingResponse, UpdateTenantSettingRequest } from './tenant-setting.models';

@Injectable({ providedIn: 'root' })
export class TenantSettingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/tenant-settings`;

  getCurrentTenantSettings(): Observable<TenantSettingResponse[]> {
    return this.http.get<TenantSettingResponse[]>(`${this.baseUrl}/me`);
  }

  updateCurrentTenantSetting(settingKey: string, request: UpdateTenantSettingRequest): Observable<TenantSettingResponse> {
    return this.http.put<TenantSettingResponse>(`${this.baseUrl}/me/${settingKey}`, request);
  }
}
