import { Component, input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { IRapprochementKpi } from "../../data-access/models";

@Component({
  selector: "app-rapprochement-kpi-banner",
  imports: [CommonModule],
  templateUrl: "./rapprochement-kpi-banner.component.html",
  styleUrl: "./rapprochement-kpi-banner.component.scss"
})
export class RapprochementKpiBannerComponent {
  readonly kpi = input<IRapprochementKpi | null>(null);
  readonly loading = input(false);
}
