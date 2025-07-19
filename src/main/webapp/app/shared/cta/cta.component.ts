import {Component, inject, input, output} from '@angular/core';
import {ButtonModule} from "primeng/button";
import {TranslateService} from "@ngx-translate/core";
import {TooltipModule} from "primeng/tooltip";

export  type Severity = 'primary' | 'secondary' | 'success' | 'info' | 'warn' | 'danger' | 'help';

@Component({
  selector: 'jhi-cta',
  imports: [ButtonModule, TooltipModule],
  templateUrl:'./cta.component.html',
})
export class CtaComponent {
  click = output<void>();
  disabled = input<boolean>(false);
  rounded = input<boolean>(false);
  isText = input<boolean>(false);
  raised = input<boolean>(true);
  icon = input<string>();
  label = input<string>();
  tooltip = input<string>();
  severity = input<Severity>('primary');
  class = input<string>();
  private readonly translateService = inject(TranslateService);

  onClick(): void {
    this.click.emit();
  }

  protected get labelValue(): string {
    return this.label() ? this.translateService.instant(this.label()) : '';
  }
  protected get tooltipValue(): string {
    return this.tooltip() ? this.translateService.instant(this.tooltip()) : '';
  }
}
