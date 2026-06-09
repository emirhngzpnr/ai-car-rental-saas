import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreatePaymentRequest, PaymentTransactionResponse } from './payment.models';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/payments`;

  createPayment(request: CreatePaymentRequest): Observable<PaymentTransactionResponse> {
    return this.http.post<PaymentTransactionResponse>(this.baseUrl, request);
  }
}
