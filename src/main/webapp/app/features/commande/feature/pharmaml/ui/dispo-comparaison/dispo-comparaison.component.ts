import { Component, computed, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { TagModule } from "primeng/tag";
import { ButtonModule } from "primeng/button";
import { TooltipModule } from "primeng/tooltip";
import { IDispoGrossisteResult, IInfoProduit } from "../../../../../../shared/model/pharmaml.model";
import { IFournisseur } from "../../../../../../shared/model/fournisseur.model";
import { CommandeId } from "../../../../../../shared/model/abstract-commande.model";
import { PharmamlApiService } from "../../../../data-access/pharmaml-api.service";
import { NotificationService } from "../../../../../../shared/services/notification.service";
import { ErrorService } from "../../../../../../shared/error.service";
import { FournisseurSelectComponent } from "../../../../../partners/ui/fournisseur-select/fournisseur-select.component";

export interface ComparaisonRow {
  codeProduit: string;
  designation: string | null;
  resultats: Map<number, IInfoProduit>;
}

@Component({
  selector: "app-dispo-comparaison",
  templateUrl: "./dispo-comparaison.component.html",
  styleUrls: ["./dispo-comparaison.component.scss"],
  imports: [CommonModule, FormsModule, TagModule, ButtonModule, TooltipModule, FournisseurSelectComponent]
})
export class DispoComparaisonComponent {
  commandeId!: CommandeId;
  suggestionId: number | null = null;
  header!: string;

  readonly selectedFournisseurs = signal<IFournisseur[]>([]);
  readonly rows = signal<ComparaisonRow[]>([]);
  readonly loading = signal(false);

  readonly hasResults = computed(() => this.rows().length > 0);
  readonly colonnes = computed(() => this.selectedFournisseurs().filter(f => f.id != null));

  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(PharmamlApiService);
  private readonly errorService = inject(ErrorService);
  private readonly notificationService = inject(NotificationService);

  comparer(): void {
    const grossistes = this.colonnes();
    if (grossistes.length === 0) return;

    this.loading.set(true);
    this.rows.set([]);

    const grossisteIds = grossistes.map(f => f.id!);

    const call$ = this.suggestionId != null
      ? this.api.disponibiliteMultiSuggestion(this.suggestionId, grossisteIds)
      : this.api.disponibiliteMulti(this.commandeId.id, this.commandeId.orderDate, grossisteIds);

    call$.subscribe({
      next: res => {
        const results: IDispoGrossisteResult[] = res.body ?? [];
        const produits = new Map<string, ComparaisonRow>();

        results.forEach(result => {
          result.produits.forEach(info => {
            if (!produits.has(info.codeProduit)) {
              produits.set(info.codeProduit, {
                codeProduit: info.codeProduit,
                designation: info.designation,
                resultats: new Map()
              });
            }
            produits.get(info.codeProduit)!.resultats.set(result.grossisteId, info);
          });
        });

        this.rows.set(
          [...produits.values()].sort((a, b) =>
            (a.designation ?? a.codeProduit).localeCompare(b.designation ?? b.codeProduit)
          )
        );
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false),
          this.notificationService.error(this.errorService.getErrorMessage(error), "Erreur");
      }
    });
  }

  onFournisseurChange(value: IFournisseur[]): void {
    this.selectedFournisseurs.set(value);
    this.rows.set([]);
  }

  infoFor(row: ComparaisonRow, fournisseurId: number): IInfoProduit | undefined {
    return row.resultats.get(fournisseurId);
  }

  isRowRupture(row: any): boolean {
    return this.colonnes().every(f => !this.infoFor(row, f.id!)?.disponible);
  }

  close(): void {
    this.activeModal.dismiss();
  }
}
