import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { KnowledgeBaseService } from './knowledge-base.service';
import { KnowledgeDocument, KnowledgeDocumentCategory, KnowledgeDocumentRequest } from './knowledge-base.models';

@Component({
  selector: 'acr-knowledge-base',
  standalone: true,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
    PageHeaderComponent
  ],
  templateUrl: './knowledge-base.component.html',
  styleUrl: './knowledge-base.component.scss'
})
export class KnowledgeBaseComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(KnowledgeBaseService);

  readonly categories: KnowledgeDocumentCategory[] = [
    'RENTAL_POLICY',
    'DEPOSIT_POLICY',
    'INSURANCE_POLICY',
    'CANCELLATION_POLICY',
    'FUEL_POLICY',
    'DELIVERY_POLICY',
    'GENERAL'
  ];
  readonly documents = signal<KnowledgeDocument[]>([]);
  readonly selected = signal<KnowledgeDocument | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(180)]],
    category: ['GENERAL' as KnowledgeDocumentCategory, Validators.required],
    content: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(20000)]]
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.service.list().subscribe({
      next: (documents) => this.documents.set(documents),
      error: (error) => this.error.set(error.error?.message || 'Knowledge documents could not be loaded.'),
      complete: () => this.loading.set(false)
    });
  }

  edit(document: KnowledgeDocument): void {
    this.selected.set(document);
    this.success.set('');
    this.form.patchValue({
      title: document.title,
      category: document.category,
      content: document.content
    });
  }

  newDocument(): void {
    this.selected.set(null);
    this.success.set('');
    this.form.reset({
      title: '',
      category: 'GENERAL',
      content: ''
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const request: KnowledgeDocumentRequest = this.form.getRawValue() as KnowledgeDocumentRequest;
    const current = this.selected();
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    const call = current ? this.service.update(current.id, request) : this.service.create(request);
    call.subscribe({
      next: (document) => {
        this.success.set('Document saved and embedded for assistant search.');
        this.selected.set(document);
        this.load();
      },
      error: (error) => this.error.set(error.error?.message || 'Knowledge document could not be saved.'),
      complete: () => this.saving.set(false)
    });
  }

  reembed(document: KnowledgeDocument): void {
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    this.service.reembed(document.id).subscribe({
      next: () => this.success.set('Document embeddings were refreshed.'),
      error: (error) => this.error.set(error.error?.message || 'Document could not be re-embedded.'),
      complete: () => this.saving.set(false)
    });
  }

  deactivate(document: KnowledgeDocument): void {
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    this.service.deactivate(document.id).subscribe({
      next: () => {
        if (this.selected()?.id === document.id) {
          this.newDocument();
        }
        this.success.set('Document deactivated.');
        this.load();
      },
      error: (error) => this.error.set(error.error?.message || 'Document could not be deactivated.'),
      complete: () => this.saving.set(false)
    });
  }

  label(value: string): string {
    return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
  }
}
