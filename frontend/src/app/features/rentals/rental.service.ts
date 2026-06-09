import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CompleteRentalRequest, RentalResponse, StartRentalRequest } from './rental.models';

@Injectable({ providedIn: 'root' })
export class RentalService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/rentals`;

  getRentals(): Observable<RentalResponse[]> {
    return this.http.get<RentalResponse[]>(this.baseUrl);
  }

  startRental(request: StartRentalRequest): Observable<RentalResponse> {
    return this.http.post<RentalResponse>(`${this.baseUrl}/start`, request);
  }

  completeRental(id: number, request: CompleteRentalRequest): Observable<RentalResponse> {
    return this.http.put<RentalResponse>(`${this.baseUrl}/${id}/complete`, request);
  }
}
