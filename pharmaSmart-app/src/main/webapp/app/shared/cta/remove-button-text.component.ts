import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent } from '../ui';
import { CtaComponent, Severity } from './cta.component';

@Component({
  selector: 'jhi-remove-tex-button',
  imports: [ButtonComponent, NgbTooltip],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './cta.component.html',
})
export class RemoveButtonTextComponent extends CtaComponent {
  rounded = input<boolean>(true);
  isText = input<boolean>(true);
  raised = input<boolean>(false);
  icon = input<string>('pi pi-trash');
  severity = input<Severity>('danger');
  tooltip = input<string>('warehouseApp.buttons.delete');
}
