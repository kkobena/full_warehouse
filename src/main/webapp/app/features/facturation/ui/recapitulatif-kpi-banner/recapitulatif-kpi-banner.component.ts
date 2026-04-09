import { Component, input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { IRecapitulatifKpi } from "../../data-access/models";
import { Tooltip } from "primeng/tooltip";

@Component({
  selector: "app-recapitulatif-kpi-banner",
  imports: [CommonModule, Tooltip],
  templateUrl: "./recapitulatif-kpi-banner.component.html",
  styleUrl: "./recapitulatif-kpi-banner.component.scss"
})
export class RecapitulatifKpiBannerComponent {
  readonly kpi = input<IRecapitulatifKpi | null>(null);
  readonly loading = input(false);
}
