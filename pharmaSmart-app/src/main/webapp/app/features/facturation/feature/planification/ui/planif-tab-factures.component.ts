import { Component, computed, inject, input, signal } from "@angular/core";
import { DatePipe } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { TableModule } from "primeng/table";
import { ButtonModule } from "primeng/button";
import { ToggleSwitch } from "primeng/toggleswitch";
import { Tooltip } from "primeng/tooltip";

import { IPlanification } from "../../../data-access/models";
import { PlanificationStateService } from "../planification-state.service";
import { PlanifDetailPanelComponent } from "./planif-detail-panel.component";
import { ButtonGroup } from "primeng/buttongroup";

@Component({
  selector: "app-planif-tab-factures",
  imports: [DatePipe, FormsModule, TableModule, ButtonModule, ToggleSwitch, Tooltip, PlanifDetailPanelComponent, ButtonGroup],
  templateUrl: "./planif-tab-factures.component.html",
  styleUrl: "./planif-tab-factures.component.scss"
})
export class PlanifTabFacturesComponent {
  protected readonly showHint = signal<boolean>(localStorage.getItem("planif-tab-factures-hint-dismissed") !== "1");
  readonly mode = input.required<"def" | "prov">();
  protected readonly state = inject(PlanificationStateService);

  protected readonly planifications = computed(() =>
    this.mode() === "def" ? this.state.planificationsDefinitives() : this.state.planificationsProvisoires()
  );

  protected readonly selectedPlan = computed(() =>
    this.mode() === "def" ? this.state.selectedPlanDef() : this.state.selectedPlanProv()
  );

  protected readonly periodiciteBadgeClass = computed(() =>
    this.mode() === "def" ? "pharma-badge pharma-badge-info" : "pharma-badge pharma-badge-warning"
  );

  dismissHint(): void {
    localStorage.setItem("planif-tab-factures-hint-dismissed", "1");
    this.showHint.set(false);
  }

  protected onSelectPlan(plan: IPlanification | null): void {
    if (this.mode() === "def") this.state.onSelectPlanDef(plan);
    else this.state.onSelectPlanProv(plan);
  }
}

