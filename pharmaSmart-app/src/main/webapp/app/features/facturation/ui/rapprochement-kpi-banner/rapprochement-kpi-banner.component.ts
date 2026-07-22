import { Component, input, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { KpiItemComponent, KpiStripComponent } from "app/shared/ui";
import { IRapprochementKpi } from "../../data-access/models";

@Component({
  selector: "app-rapprochement-kpi-banner",
  imports: [CommonModule, KpiStripComponent, KpiItemComponent],
  templateUrl: "./rapprochement-kpi-banner.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./rapprochement-kpi-banner.component.scss"
})
export class RapprochementKpiBannerComponent {
  readonly kpi = input<IRapprochementKpi | null>(null);
  readonly loading = input(false);
}
