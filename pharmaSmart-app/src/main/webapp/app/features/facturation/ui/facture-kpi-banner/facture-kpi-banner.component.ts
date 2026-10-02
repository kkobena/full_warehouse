import { Component, input, ChangeDetectionStrategy } from "@angular/core";
import { IFacturationKpi } from "../../data-access/models";
import { DecimalPipe } from "@angular/common";
import { KpiItemComponent, KpiStripComponent } from "app/shared/ui";

@Component({
  selector: "app-facture-kpi-banner",
  imports: [DecimalPipe, KpiStripComponent, KpiItemComponent],
  templateUrl: "./facture-kpi-banner.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FactureKpiBannerComponent {
  readonly kpi = input<IFacturationKpi | null>(null);
  readonly loading = input(false);
}
