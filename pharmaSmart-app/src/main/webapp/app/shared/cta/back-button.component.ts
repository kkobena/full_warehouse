import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent } from '../ui';
import { CtaComponent, Severity } from './cta.component';

@Component({
  selector: 'jhi-back-button',
  imports: [ButtonComponent, NgbTooltip],
  changeDetection: ChangeDetectionStrategy.Eager,
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
