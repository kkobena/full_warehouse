import { Component, input } from '@angular/core';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent } from '../ui';
import { CtaComponent, Severity } from './cta.component';

@Component({
  selector: 'jhi-primary-button',
  imports: [ButtonComponent, NgbTooltip],
  templateUrl: './cta.component.html',
})
export class PrimaryButtonComponent extends CtaComponent {
  icon = input<string>('pi pi-check');
  severity = input<Severity>('primary');
  label = input<string>('warehouseApp.buttons.save');
}
