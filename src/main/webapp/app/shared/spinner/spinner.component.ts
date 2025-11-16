import { Component, inject, input } from '@angular/core';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';

@Component({
  selector: 'jhi-spinner',
  imports: [NgxSpinnerModule],
  template: `
    <ngx-spinner [fullScreen]="fullScreen()" bdColor="rgba(255,255,255,0.5)" color="#f13151" size="medium" type="timer"></ngx-spinner>
  `,
  styleUrl: './spinner.component.scss',
})
export class SpinnerComponent {
  private readonly spinner = inject(NgxSpinnerService);
  fullScreen = input<boolean>(false);
  show(): void {
    this.spinner.show();
  }
  hide(): void {
    this.spinner.hide();
  }
}
