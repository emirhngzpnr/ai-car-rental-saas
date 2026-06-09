import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateVehicleRequest, UpdateVehicleRequest, VehicleResponse } from './vehicle.models';

@Injectable({ providedIn: 'root' })
export class VehicleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/vehicle`;

  getVehicles(): Observable<VehicleResponse[]> {
    return this.http.get<VehicleResponse[]>(this.baseUrl);
  }

  createVehicle(request: CreateVehicleRequest): Observable<VehicleResponse> {
    return this.http.post<VehicleResponse>(this.baseUrl, request);
  }

  updateVehicle(id: number, request: UpdateVehicleRequest): Observable<VehicleResponse> {
    return this.http.put<VehicleResponse>(`${this.baseUrl}/${id}`, request);
  }

  deleteVehicle(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
