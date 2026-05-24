import { Component, computed, DestroyRef, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { Button } from "primeng/button";
import { Select } from "primeng/select";
import { Checkbox } from "primeng/checkbox";
import { InputNumber } from "primeng/inputnumber";
import {
  IRetourClientResult,
  ISaleForRetour,
  ISaleLineForRetour,
  ModeReglementRetour,
  MotifRetourClient,
  RetourClientApiService,
  RetourClientRequest,
} from "../../data-access/services/retour-client-api.service";
import { Textarea } from "primeng/textarea";
import { ISales } from "../../../../shared/model";

type RetourLine = ISaleLineForRetour & {
  emballageIntact: boolean;
  numLotLisible: boolean;
  datePeremptionValide: boolean;
};

@Component({
  selector: "app-retour-client-modal",
  templateUrl: "./retour-client-modal.component.html",
  styleUrl: "./retour-client-modal.component.scss",
  imports: [CommonModule, FormsModule, Button, Select, InputNumber, Textarea, Checkbox],
})
export class RetourClientModalComponent {
  readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(RetourClientApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly saleLoading = signal(false);
  protected readonly saleError = signal<string | null>(null);
  protected readonly currentSale = signal<ISaleForRetour | null>(null);
  protected readonly selectedLines = signal<RetourLine[]>([]);

  protected readonly motif = signal<MotifRetourClient | null>(null);
  protected readonly modeReglement = signal<ModeReglementRetour | null>(null);
  protected readonly commentaire = signal("");
  protected readonly avecEchange = signal(false);

  protected readonly submitLoading = signal(false);
  protected readonly submitError = signal<string | null>(null);
  protected readonly retourResult = signal<IRetourClientResult | null>(null);

  protected readonly motifOptions: { label: string; value: MotifRetourClient }[] = [
    { label: "Erreur de dispensation", value: "ERREUR_DISPENSATION" },
    { label: "Produit défectueux", value: "PRODUIT_DEFECTUEUX" },
    { label: "Erreur de quantité", value: "ERREUR_QUANTITE" },
    { label: "Insatisfaction client", value: "INSATISFACTION" },
    { label: "Autre", value: "AUTRE" },
  ];

  protected readonly modeReglementOptions: { label: string; value: ModeReglementRetour; icon: string }[] = [
    { label: "Remboursement espèces", value: "REMBOURSEMENT_ESPECES", icon: "pi pi-money-bill" },
    { label: "Remboursement CB", value: "REMBOURSEMENT_CB", icon: "pi pi-credit-card" },
    { label: "Avoir client", value: "AVOIR_CLIENT", icon: "pi pi-ticket" },
  ];

  protected readonly montantTotal = computed(() =>
    this.selectedLines().reduce((s, l) => {
      const price = l.montantRemboursableClient ?? l.netUnitPrice ?? 0;
      return s + (l.quantiteRetour ?? 0) * price;
    }, 0)
  );

  protected readonly canSubmit = computed(() => {
    if (!this.currentSale() || !this.motif()) return false;
    if (!this.avecEchange() && !this.modeReglement()) return false;
    return this.selectedLines().some(l => !l.retourInterdit && (l.quantiteRetour ?? 0) > 0);
  });

  /** Reçoit l'ISales sélectionnée depuis le contexte appelant. */
  set sale(sale: ISales) {
    const id = sale.saleId?.id;
    const saleDate = sale.saleId?.saleDate;
    if (!id || !saleDate) {
      this.saleError.set("Vente invalide : identifiant ou date manquant.");
      return;
    }
    this.saleLoading.set(true);
    this.saleError.set(null);
    this.currentSale.set(null);
    this.selectedLines.set([]);
    this.api
      .findSaleById(id, saleDate)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: enrichedSale => {
          this.saleLoading.set(false);
          this.currentSale.set(enrichedSale);
          this.selectedLines.set(
            enrichedSale.lines?.map(l => ({
              ...l,
              quantiteRetour: 0,
              emballageIntact: true,
              numLotLisible: true,
              datePeremptionValide: true,
            })) ?? []
          );
        },
        error: () => {
          this.saleLoading.set(false);
          this.saleError.set(`Impossible de charger la vente (id=${id}).`);
        },
      });
  }

  protected updateQty(salesLineId: number | undefined, qty: number): void {
    this.selectedLines.update(lines =>
      lines.map(l =>
        l.salesLineId === salesLineId
          ? { ...l, quantiteRetour: Math.min(Number(qty), l.quantitySold ?? 0) }
          : l
      )
    );
  }

  protected updateLineState(
    salesLineId: number | undefined,
    field: "emballageIntact" | "numLotLisible" | "datePeremptionValide",
    value: boolean
  ): void {
    this.selectedLines.update(lines =>
      lines.map(l => (l.salesLineId === salesLineId ? { ...l, [field]: value } : l))
    );
  }

  protected onAvecEchangeChange(val: boolean): void {
    this.avecEchange.set(val);
    if (val) this.modeReglement.set(null);
  }

  protected confirmerRetour(): void {
    const sale = this.currentSale();
    const motif = this.motif();
    const modeReglement: ModeReglementRetour = this.avecEchange() ? "AVOIR_CLIENT" : this.modeReglement()!;
    if (!sale || !motif || !modeReglement) return;

    const lines = this.selectedLines()
      .filter(l => !l.retourInterdit && (l.quantiteRetour ?? 0) > 0)
      .map(l => ({
        salesLineId: l.salesLineId!,
        salesLineDate: l.salesLineDate!,
        quantite: l.quantiteRetour!,
        emballageIntact: l.emballageIntact,
        numLotLisible: l.numLotLisible,
        datePeremptionValide: l.datePeremptionValide,
      }));

    if (lines.length === 0) return;

    const request: RetourClientRequest = {
      saleId: sale.saleId!,
      saleDate: sale.saleDate!,
      motif,
      modeReglement,
      avecEchange: this.avecEchange() || undefined,
      commentaire: this.commentaire() || undefined,
      lines,
    };

    this.submitLoading.set(true);
    this.submitError.set(null);
    this.api
      .validerRetour(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.submitLoading.set(false);
          if (result.partiel) {
            this.retourResult.set(result);
          } else {
            this.activeModal.close(result);
          }
        },
        error: () => {
          this.submitLoading.set(false);
          this.submitError.set("Une erreur est survenue. Veuillez réessayer.");
        },
      });
  }

  protected fermerApresResultat(): void {
    this.activeModal.close(this.retourResult());
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
