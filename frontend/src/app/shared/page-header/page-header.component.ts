import { Component, Input } from '@angular/core';

@Component({
  selector: 'acr-page-header',
  templateUrl: './page-header.component.html',
  styleUrl: './page-header.component.scss'
})
export class PageHeaderComponent {
  @Input({ required: true }) title = '';
  @Input() description = '';
}
