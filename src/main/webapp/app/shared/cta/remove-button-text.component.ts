import {Component, input} from '@angular/core';
import {CtaComponent, Severity} from "./cta.component";
import {ButtonModule} from "primeng/button";
import {TooltipModule} from "primeng/tooltip";

@Component({
  selector: 'jhi-remove-tex-button',
  imports: [ButtonModule,TooltipModule],
  templateUrl:'./cta.component.html',

})
export class RemoveButtonTextComponent extends CtaComponent {
  rounded = input<boolean>(true);
  isText = input<boolean>(true);
  raised = input<boolean>(false);
  icon = input<string>('pi pi-trash');
  severity = input<Severity>('danger');
  tooltip = input<string>('warehouseApp.buttons.delete');
}
