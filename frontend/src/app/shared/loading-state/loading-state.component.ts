import { Component, Input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'acr-loading-state',
  imports: [MatProgressSpinnerModule],
  templateUrl: './loading-state.component.html',
  styleUrl: './loading-state.component.scss'
})
export class LoadingStateComponent {
  @Input() label = 'Loading';
}
