import { Component, input } from '@angular/core';
import { CtaComponent, Severity } from './cta.component';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

@Component({
  selector: 'jhi-remove-button',
  imports: [ButtonModule, TooltipModule],
  templateUrl: './cta.component.html',
})
export class RemoveButtonTextComponent extends CtaComponent {
  icon = input<string>('pi pi-trash');
  severity = input<Severity>('danger');
  label = input<string>('warehouseApp.buttons.delete');
}
