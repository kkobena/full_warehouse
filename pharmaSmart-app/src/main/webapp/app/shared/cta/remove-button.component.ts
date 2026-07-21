import { Component, input } from '@angular/core';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent } from '../ui';
import { CtaComponent, Severity } from './cta.component';

@Component({
  selector: 'jhi-remove-button',
  imports: [ButtonComponent, NgbTooltip],
  templateUrl: './cta.component.html',
})
export class RemoveButtonTextComponent extends CtaComponent {
  icon = input<string>('pi pi-trash');
  severity = input<Severity>('danger');
  label = input<string>('warehouseApp.buttons.delete');
}
