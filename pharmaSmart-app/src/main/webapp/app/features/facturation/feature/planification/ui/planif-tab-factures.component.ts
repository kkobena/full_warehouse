import { Component, computed, inject, input, signal, ChangeDetectionStrategy } from "@angular/core";
import { DatePipe } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import {
  ButtonComponent,
  DataTableComponent,
  SelectableRowDirective,
  SwitchComponent
} from "../../../../../shared/ui";

import { IPlanification } from "../../../data-access/models";
import { PlanificationStateService } from "../planification-state.service";
import { PlanifDetailPanelComponent } from "./planif-detail-panel.component";

@Component({
  selector: "app-planif-tab-factures",
  imports: [
    DatePipe,
    FormsModule,
    NgbTooltip,
    ButtonComponent,
    DataTableComponent,
    SelectableRowDirective,
    SwitchComponent,
    PlanifDetailPanelComponent
  ],
  templateUrl: "./planif-tab-factures.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
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

  /**
   * `app-data-table` est générique : `selectionChange` est typé `T | T[] | null` même en
   * mode `selectionMode="single"` (le type ne peut pas être affiné selon cette valeur de
   * template). On ramène ici à une valeur unique avant de déléguer à `onSelectPlan`.
   */
  protected onSelectionChange(value: IPlanification | IPlanification[] | null): void {
    this.onSelectPlan(Array.isArray(value) ? (value[0] ?? null) : value);
  }

  /**
   * `app-switch` bascule visuellement dès le clic ; si l'utilisateur annule la confirmation
   * (ou si l'appel API échoue), `plan.actif` ne change jamais, donc Angular ne redéclenche
   * jamais `writeValue()` sur `[ngModel]` (valeur liée identique) et le switch reste
   * visuellement faussé. On force ici le réaffichage correct via la référence du composant.
   */
  protected revertSwitch(switchRef: SwitchComponent, plan: IPlanification): () => void {
    return () => switchRef.writeValue(plan.actif);
  }
}

