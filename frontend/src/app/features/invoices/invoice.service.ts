import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../../core/http/page-response';
import { InvoiceQuery, InvoiceResponse } from './invoice.models';

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/invoices`;

  getInvoices(query: InvoiceQuery): Observable<PageResponse<InvoiceResponse>> {
    let params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size)
      .set('sort', 'issuedAt,desc');

    if (query.status) params = params.set('status', query.status);
    if (query.type) params = params.set('type', query.type);

    return this.http.get<PageResponse<InvoiceResponse>>(this.baseUrl, { params });
  }

  downloadInvoicePdf(invoiceId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${invoiceId}/pdf`, {
      responseType: 'blob'
    });
  }
}
