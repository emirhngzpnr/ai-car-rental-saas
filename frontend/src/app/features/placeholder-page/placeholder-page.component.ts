import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';

@Component({
  selector: 'acr-placeholder-page',
  imports: [MatButtonModule, MatIconModule, PageHeaderComponent, EmptyStateComponent],
  templateUrl: './placeholder-page.component.html',
  styleUrl: './placeholder-page.component.scss'
})
export class PlaceholderPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly routeData = toSignal(this.route.data, {
    initialValue: this.route.snapshot.data
  });

  readonly title = computed(() => this.routeData()['title'] as string);
  readonly description = computed(() => this.routeData()['description'] as string);
}
