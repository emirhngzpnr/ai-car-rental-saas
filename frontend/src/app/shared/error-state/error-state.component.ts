import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'acr-error-state',
  imports: [MatIconModule],
  templateUrl: './error-state.component.html',
  styleUrl: './error-state.component.scss'
})
export class ErrorStateComponent {
  @Input() title = 'Unable to load data';
  @Input() message = 'Please try again or contact support if the issue continues.';
}
