import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateInsurancePackageRequest, InsurancePackageResponse, UpdateInsurancePackageRequest } from './insurance-package.models';

@Injectable({ providedIn: 'root' })
export class InsurancePackageService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/insurance-packages`;

  getInsurancePackages(tenantId: number): Observable<InsurancePackageResponse[]> {
    return this.http.get<InsurancePackageResponse[]>(this.baseUrl, {
      params: new HttpParams().set('tenantId', tenantId)
    });
  }

  createInsurancePackage(request: CreateInsurancePackageRequest): Observable<InsurancePackageResponse> {
    return this.http.post<InsurancePackageResponse>(this.baseUrl, request);
  }

  updateInsurancePackage(id: number, request: UpdateInsurancePackageRequest): Observable<InsurancePackageResponse> {
    return this.http.put<InsurancePackageResponse>(`${this.baseUrl}/${id}`, request);
  }

  deleteInsurancePackage(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
