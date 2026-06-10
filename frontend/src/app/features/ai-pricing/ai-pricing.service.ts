import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../../core/http/page-response';
import { AiPricingRecommendationManagementResponse, AiPricingRecommendationResponse, AiPricingStatus } from './ai-pricing.models';

@Injectable({ providedIn: 'root' })
export class AiPricingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/ai/pricing`;

  recommendPrice(vehicleId: number): Observable<AiPricingRecommendationResponse> {
    return this.http.get<AiPricingRecommendationResponse>(`${this.baseUrl}/recommendation/${vehicleId}`);
  }

  getRecommendations(status: AiPricingStatus | '' = '', page = 0, size = 10): Observable<PageResponse<AiPricingRecommendationManagementResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<PageResponse<AiPricingRecommendationManagementResponse>>(`${this.baseUrl}/recommendations`, { params });
  }

  approveRecommendation(id: number): Observable<AiPricingRecommendationManagementResponse> {
    return this.http.post<AiPricingRecommendationManagementResponse>(`${this.baseUrl}/recommendations/${id}/approve`, {});
  }

  rejectRecommendation(id: number): Observable<AiPricingRecommendationManagementResponse> {
    return this.http.post<AiPricingRecommendationManagementResponse>(`${this.baseUrl}/recommendations/${id}/reject`, {});
  }
}
