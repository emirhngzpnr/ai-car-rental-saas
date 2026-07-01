import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { LucideCircleAlert, LucideMessageCircle, LucideSend, LucideSparkles, LucideX } from '@lucide/angular';
import { MarketplaceAssistantCitation } from '../../features/marketplace/marketplace.models';
import { MarketplaceService } from '../../features/marketplace/marketplace.service';

@Component({
  selector: 'acr-customer-assistant',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    LucideCircleAlert,
    LucideMessageCircle,
    LucideSend,
    LucideSparkles,
    LucideX
  ],
  template: `
    <button
      class="assistant-launcher"
      type="button"
      (click)="toggle()"
      [attr.aria-expanded]="open()"
      aria-label="Open rental rules assistant"
    >
      <svg lucideMessageCircle [size]="24"></svg>
    </button>

    @if (open()) {
      <section class="assistant-panel" aria-label="Rental rules assistant">
        <header>
          <div>
            <span class="assistant-icon"><svg lucideSparkles [size]="18"></svg></span>
            <div>
              <strong>Rental rules assistant</strong>
              <small>Ask about deposits, insurance, cancellation or delivery rules.</small>
            </div>
          </div>
          <button type="button" class="icon-button" (click)="toggle()" aria-label="Close assistant">
            <svg lucideX [size]="17"></svg>
          </button>
        </header>

        <div class="assistant-body">
          @if (lastQuestion()) {
            <div class="message message--user">
              <span>{{ lastQuestion() }}</span>
            </div>
          }

          @if (answer()) {
            <div class="message message--assistant">
              <p>{{ answer() }}</p>
              @if (citations().length) {
                <div class="citations" aria-label="Sources">
                  @for (citation of citations(); track citation.title + citation.tenantName) {
                    <span>{{ citation.tenantName }} - {{ citation.title }}</span>
                  }
                </div>
              }
            </div>
          } @else {
            <div class="empty-state">
              <span>Company policy answers are based on tenant Knowledge Base documents.</span>
            </div>
          }

          @if (warnings().length) {
            <div class="warning">
              <svg lucideCircleAlert [size]="17"></svg>
              <div>
                @for (warning of warnings(); track warning) {
                  <span>{{ warning }}</span>
                }
              </div>
            </div>
          }

          @if (error()) {
            <div class="error">
              <svg lucideCircleAlert [size]="17"></svg>
              <span>{{ error() }}</span>
            </div>
          }
        </div>

        <form [formGroup]="form" (ngSubmit)="ask()">
          <mat-form-field appearance="outline">
            <mat-label>Policy question</mat-label>
            <textarea
              matInput
              formControlName="query"
              rows="2"
              maxlength="500"
              placeholder="When is the deposit refunded?"
            ></textarea>
          </mat-form-field>
          <button mat-flat-button color="primary" type="submit" [disabled]="loading() || form.invalid">
            <svg lucideSend [size]="16"></svg>
            {{ loading() ? 'Asking...' : 'Ask' }}
          </button>
        </form>
      </section>
    }
  `,
  styles: [`
    .assistant-launcher{position:fixed;right:24px;bottom:24px;z-index:60;width:54px;height:54px;border:0;border-radius:50%;background:#1261b5;color:#fff;display:grid;place-items:center;box-shadow:0 10px 24px rgba(18,97,181,.28);cursor:pointer}
    .assistant-panel{position:fixed;right:24px;bottom:90px;z-index:61;width:min(430px,calc(100vw - 32px));background:#fff;border:1px solid #dce2e9;border-radius:10px;box-shadow:0 18px 42px rgba(15,23,42,.18);overflow:hidden;color:#162033}
    header{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;padding:16px 18px;border-bottom:1px solid #e7edf3;background:#fbfcfe}
    header>div{display:flex;gap:10px;min-width:0}.assistant-icon{width:36px;height:36px;border-radius:8px;background:#eaf2ff;color:#1261b5;display:grid;place-items:center;flex:0 0 auto}header strong{display:block;font-size:15px}header small{display:block;color:#657287;font-size:12px;line-height:1.35;margin-top:2px}
    .icon-button{width:32px;height:32px;border:1px solid #dce2e9;border-radius:6px;background:#fff;color:#526176;display:grid;place-items:center;cursor:pointer;flex:0 0 auto}
    .assistant-body{max-height:340px;overflow:auto;padding:16px 18px;display:flex;flex-direction:column;gap:12px;background:#fff}
    .empty-state{border:1px dashed #cbd6e2;border-radius:8px;background:#f8fafc;color:#657287;font-size:13px;line-height:1.45;padding:14px}
    .message{max-width:94%;border-radius:10px;font-size:13px;line-height:1.5}.message--user{align-self:flex-end;background:#1261b5;color:#fff;padding:10px 12px}.message--assistant{align-self:flex-start;background:#f7fafc;border:1px solid #dce2e9;padding:12px 13px}.message p{margin:0;color:#35445b;white-space:pre-line}
    .citations{display:flex;flex-wrap:wrap;gap:6px;margin-top:10px;padding-top:10px;border-top:1px solid #e7edf3}.citations span{background:#eef3f8;border:1px solid #dce2e9;border-radius:999px;color:#40516a;font-size:11px;padding:5px 8px}
    .warning,.error{display:flex;gap:8px;align-items:flex-start;border-radius:8px;padding:9px 10px;font-size:12px;line-height:1.4}.warning{background:#fff7e8;border:1px solid #f3d59a;color:#8a5700}.error{background:#fff1f1;border:1px solid #f0b3b3;color:#a11c1c}.warning div{display:flex;flex-direction:column;gap:3px}
    form{border-top:1px solid #e7edf3;padding:12px 14px 14px;display:grid;grid-template-columns:1fr auto;gap:10px;align-items:start;background:#fbfcfe}mat-form-field{min-width:0}textarea{resize:none}form button{height:56px;min-width:88px;gap:6px}
    @media(max-width:640px){.assistant-launcher{right:16px;bottom:16px}.assistant-panel{right:16px;bottom:78px}form{grid-template-columns:1fr}form button{width:100%}}
  `]
})
export class CustomerAssistantComponent {
  private readonly fb = inject(FormBuilder);
  private readonly marketplaceService = inject(MarketplaceService);

  readonly open = signal(false);
  readonly loading = signal(false);
  readonly answer = signal('');
  readonly lastQuestion = signal('');
  readonly citations = signal<MarketplaceAssistantCitation[]>([]);
  readonly warnings = signal<string[]>([]);
  readonly error = signal('');

  readonly form = this.fb.group({
    query: ['', [Validators.required, Validators.maxLength(500)]]
  });

  toggle(): void {
    this.open.update((value) => !value);
  }

  ask(): void {
    const query = this.form.controls.query.value?.trim();
    if (!query) {
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.answer.set('');
    this.lastQuestion.set(query);
    this.citations.set([]);
    this.warnings.set([]);

    this.marketplaceService.askAssistant({ query }).subscribe({
      next: (response) => {
        this.answer.set(this.formatAnswer(response.answer));
        this.citations.set(response.citations);
        this.warnings.set(this.policyWarnings(response.warnings));
        if (response.intent === 'VEHICLE_SEARCH') {
          this.warnings.set([
            ...this.policyWarnings(response.warnings),
            'This assistant answers rental policy questions. Use Find a car for vehicle searches.'
          ]);
        }
      },
      error: (apiError) => {
        this.error.set(apiError.error?.message || 'Assistant is temporarily unavailable.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  private formatAnswer(value: string): string {
    const trimmed = value.trim();
    if (!trimmed.startsWith('{')) {
      return trimmed;
    }
    try {
      const parsed = JSON.parse(trimmed) as { response?: unknown; answer?: unknown };
      const response = parsed.response ?? parsed.answer;
      return typeof response === 'string' && response.trim() ? response.trim() : trimmed;
    } catch {
      return trimmed;
    }
  }

  private policyWarnings(values: string[]): string[] {
    return values.filter((warning) => {
      const normalized = warning.toLowerCase();
      return !normalized.includes('supported search terms')
        && !normalized.includes('pickup')
        && !normalized.includes('return dates')
        && !normalized.includes('matching vehicles');
    });
  }
}
