import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { KnowledgeDocument, KnowledgeDocumentRequest } from './knowledge-base.models';

@Injectable({ providedIn: 'root' })
export class KnowledgeBaseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/knowledge-base/documents`;

  list(): Observable<KnowledgeDocument[]> {
    return this.http.get<KnowledgeDocument[]>(this.baseUrl);
  }

  create(request: KnowledgeDocumentRequest): Observable<KnowledgeDocument> {
    return this.http.post<KnowledgeDocument>(this.baseUrl, request);
  }

  update(id: number, request: KnowledgeDocumentRequest): Observable<KnowledgeDocument> {
    return this.http.put<KnowledgeDocument>(`${this.baseUrl}/${id}`, request);
  }

  reembed(id: number): Observable<KnowledgeDocument> {
    return this.http.post<KnowledgeDocument>(`${this.baseUrl}/${id}/reembed`, {});
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
