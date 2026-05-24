import { Component, input } from '@angular/core';
import { CtaComponent, Severity } from './cta.component';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

@Component({
  selector: 'jhi-back-button',
  imports: [ButtonModule, TooltipModule],
  templateUrl: './cta.component.html',
})
export class BackButtonComponent extends CtaComponent {
  icon = input<string>('pi pi-arrow-left');
  severity = input<Severity>('secondary');
  label = input<string>('warehouseApp.buttons.back');

  onClick(): void {
    window.history.back();
  }
}
