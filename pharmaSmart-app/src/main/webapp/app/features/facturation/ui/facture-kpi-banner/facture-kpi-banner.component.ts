import { Component, input, ChangeDetectionStrategy } from "@angular/core";
import { IFacturationKpi } from "../../data-access/models";
import { CommonModule } from "@angular/common";

@Component({
  selector: "app-facture-kpi-banner",
  imports: [CommonModule],
  templateUrl: "./facture-kpi-banner.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./facture-kpi-banner.component.scss"
})
export class FactureKpiBannerComponent {
  readonly kpi = input<IFacturationKpi | null>(null);
  readonly loading = input(false);
}
