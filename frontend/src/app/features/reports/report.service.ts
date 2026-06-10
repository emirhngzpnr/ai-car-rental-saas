import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MonthlyRevenueResponse, MonthlySummaryResponse, TopVehicleResponse } from './report.models';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/reports`;

  getMonthlyRevenue(): Observable<MonthlyRevenueResponse[]> {
    return this.http.get<MonthlyRevenueResponse[]>(`${this.baseUrl}/monthly-revenue`);
  }

  getMonthlySummary(year: number, month: number): Observable<MonthlySummaryResponse> {
    const params = new HttpParams().set('year', year).set('month', month);
    return this.http.get<MonthlySummaryResponse>(`${this.baseUrl}/monthly-summary`, { params });
  }

  getTopVehicles(limit = 5): Observable<TopVehicleResponse[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<TopVehicleResponse[]>(`${this.baseUrl}/top-vehicles`, { params });
  }
}
