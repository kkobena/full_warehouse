import { Component, input, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { IRecapitulatifKpi } from "../../data-access/models";
import { KpiItemComponent, KpiStripComponent } from "../../../../shared/ui";

@Component({
  selector: "app-recapitulatif-kpi-banner",
  imports: [CommonModule, KpiItemComponent, KpiStripComponent],
  templateUrl: "./recapitulatif-kpi-banner.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: "./recapitulatif-kpi-banner.component.scss"
})
export class RecapitulatifKpiBannerComponent {
  readonly kpi = input<IRecapitulatifKpi | null>(null);
  readonly loading = input(false);
}
