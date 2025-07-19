import {Component, input} from '@angular/core';
import {CtaComponent, Severity} from "./cta.component";
import {ButtonModule} from "primeng/button";
import {TooltipModule} from "primeng/tooltip";

@Component({
  selector: 'jhi-primary-button',
  imports: [ButtonModule,TooltipModule],
  templateUrl:'./cta.component.html',

})
export class PrimaryButtonComponent extends CtaComponent {
  icon = input<string>('pi pi-check');
  severity = input<Severity>('primary');
  label = input<string>('warehouseApp.buttons.save');

}
