export type KnowledgeDocumentCategory =
  | 'RENTAL_POLICY'
  | 'DEPOSIT_POLICY'
  | 'INSURANCE_POLICY'
  | 'CANCELLATION_POLICY'
  | 'FUEL_POLICY'
  | 'DELIVERY_POLICY'
  | 'GENERAL';

export interface KnowledgeDocument {
  id: number;
  tenantId: number;
  title: string;
  category: KnowledgeDocumentCategory;
  content: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeDocumentRequest {
  title: string;
  category: KnowledgeDocumentCategory;
  content: string;
}
