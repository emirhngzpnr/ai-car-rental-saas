import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CustomerProfile } from '../../core/customer-auth/customer-auth.models';
import { CustomerReservation, MarketplaceSearchResponse, MarketplaceVehicleDetail, ReservationResponse, SemanticVehicleSearchRequest, SemanticVehicleSearchResponse, TrackingResponse, VehicleSearchCriteria } from './marketplace.models';

@Injectable({ providedIn: 'root' })
export class MarketplaceService {
  private readonly http = inject(HttpClient);

  search(criteria: VehicleSearchCriteria): Observable<MarketplaceSearchResponse> {
    let params = new HttpParams();
    Object.entries(criteria).forEach(([key, value]) => {
      if (value === null || value === undefined || value === '') return;
      if (Array.isArray(value)) {
        value.forEach((item) => params = params.append(key, String(item)));
      } else {
        params = params.set(key, String(value));
      }
    });
    return this.http.get<MarketplaceSearchResponse>(`${environment.apiUrl}/api/public/marketplace/vehicles`, { params });
  }
  interpretSearch(request: SemanticVehicleSearchRequest): Observable<SemanticVehicleSearchResponse> {
    return this.http.post<SemanticVehicleSearchResponse>(
      `${environment.apiUrl}/api/public/marketplace/vehicle-search/interpret`,
      request
    );
  }
  getVehicle(id: number): Observable<MarketplaceVehicleDetail> {
    return this.http.get<MarketplaceVehicleDetail>(`${environment.apiUrl}/api/public/marketplace/vehicles/${id}`);
  }
  createGuestReservation(tenantSlug: string, request: object): Observable<ReservationResponse> {
    return this.http.post<ReservationResponse>(`${environment.apiUrl}/api/public/tenants/${tenantSlug}/reservations`, request);
  }
  createCustomerReservation(request: object): Observable<ReservationResponse> {
    return this.http.post<ReservationResponse>(`${environment.apiUrl}/api/customer/reservations`, request);
  }
  payGuestDeposit(tenantSlug: string, code: string, email: string, idempotencyKey: string): Observable<object> {
    return this.http.post(`${environment.apiUrl}/api/public/tenants/${tenantSlug}/reservations/${code}/deposit-payment`, { email, idempotencyKey });
  }
  payCustomerDeposit(code: string, idempotencyKey: string): Observable<object> {
    return this.http.post(`${environment.apiUrl}/api/customer/reservations/${code}/deposit-payment`, { idempotencyKey });
  }
  track(code: string, email: string): Observable<TrackingResponse> {
    return this.http.get<TrackingResponse>(`${environment.apiUrl}/api/public/reservations/track`, { params: { reservationCode: code, email } });
  }
  profile(): Observable<CustomerProfile> { return this.http.get<CustomerProfile>(`${environment.apiUrl}/api/customer/me`); }
  updateProfile(request: object): Observable<CustomerProfile> { return this.http.put<CustomerProfile>(`${environment.apiUrl}/api/customer/me`, request); }
  customerReservations(): Observable<CustomerReservation[]> { return this.http.get<CustomerReservation[]>(`${environment.apiUrl}/api/customer/reservations`); }
  customerReservation(code: string): Observable<CustomerReservation> { return this.http.get<CustomerReservation>(`${environment.apiUrl}/api/customer/reservations/${code}`); }
}
