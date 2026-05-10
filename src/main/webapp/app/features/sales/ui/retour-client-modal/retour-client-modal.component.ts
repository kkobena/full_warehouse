import { Component, computed, DestroyRef, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { Button } from "primeng/button";
import { Select } from "primeng/select";
import {
  ISaleForRetour,
  ISaleLineForRetour,
  ModeReglementRetour,
  MotifRetourClient,
  RetourClientApiService,
  RetourClientRequest
} from "../../data-access/services/retour-client-api.service";
import { InputNumber } from "primeng/inputnumber";
import { Textarea } from "primeng/textarea";
import { ISales } from "../../../../shared/model";
import { SalesApiService } from "../../data-access/services/sales-api.service";

@Component({
  selector: "app-retour-client-modal",
  templateUrl: "./retour-client-modal.component.html",
  styleUrl: "./retour-client-modal.component.scss",
  imports: [CommonModule, FormsModule, Button, Select, InputNumber, Textarea]
})
export class RetourClientModalComponent {
  readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(RetourClientApiService);
  private readonly salesApi = inject(SalesApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly saleLoading = signal(false);
  protected readonly saleError = signal<string | null>(null);
  protected readonly currentSale = signal<ISaleForRetour | null>(null);
  protected readonly selectedLines = signal<ISaleLineForRetour[]>([]);

  protected readonly motif = signal<MotifRetourClient | null>(null);
  protected readonly modeReglement = signal<ModeReglementRetour | null>(null);
  protected readonly commentaire = signal("");

  protected readonly submitLoading = signal(false);
  protected readonly submitError = signal<string | null>(null);

  protected readonly motifOptions: { label: string; value: MotifRetourClient }[] = [
    { label: "Erreur de dispensation", value: "ERREUR_DISPENSATION" },
    { label: "Produit défectueux", value: "PRODUIT_DEFECTUEUX" },
    { label: "Erreur de quantité", value: "ERREUR_QUANTITE" },
    { label: "Insatisfaction client", value: "INSATISFACTION" },
    { label: "Autre", value: "AUTRE" }
  ];

  protected readonly modeReglementOptions: { label: string; value: ModeReglementRetour; icon: string }[] = [
    { label: "Remboursement espèces", value: "REMBOURSEMENT_ESPECES", icon: "pi pi-money-bill" },
    { label: "Remboursement CB", value: "REMBOURSEMENT_CB", icon: "pi pi-credit-card" },
    { label: "Avoir client", value: "AVOIR_CLIENT", icon: "pi pi-ticket" }
  ];

  protected readonly montantTotal = computed(() =>
    this.selectedLines().reduce((s, l) => s + (l.quantiteRetour ?? 0) * (l.netUnitPrice ?? 0), 0)
  );

  protected readonly canSubmit = computed(() =>
    !!this.currentSale() &&
    !!this.motif() &&
    !!this.modeReglement() &&
    this.selectedLines().some(l => (l.quantiteRetour ?? 0) > 0)
  );

  set sale(sale: ISales) {
    if (!sale.saleId) return;
    this.saleLoading.set(true);
    this.saleError.set(null);
    this.currentSale.set(null);
    this.selectedLines.set([]);
    this.salesApi.findSale(sale.saleId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: fullSale => {
          this.saleLoading.set(false);
          const forRetour = this.mapToForRetour(fullSale);
          this.currentSale.set(forRetour);
          this.selectedLines.set(forRetour.lines?.map(l => ({ ...l, quantiteRetour: 0 })) ?? []);
        },
        error: () => {
          this.saleLoading.set(false);
          this.saleError.set("Impossible de charger les détails de la vente.");
        }
      });
  }

  private mapToForRetour(fullSale: ISales): ISaleForRetour {
    const c = fullSale.customer;
    const customerName = c?.fullName
      ?? (c ? `${c.firstName ?? ""} ${c.lastName ?? ""}`.trim() || undefined : undefined);
    return {
      saleId: fullSale.saleId?.id ?? fullSale.id,
      saleDate: fullSale.saleId?.saleDate,
      numberTransaction: fullSale.numberTransaction,
      customerName,
      lines: (fullSale.salesLines ?? []).map(l => ({
        salesLineId: l.id,
        salesLineDate: l.saleLineId?.saleDate ?? fullSale.saleId?.saleDate,
        produitLibelle: l.produitLibelle,
        codeCip: l.code,
        quantitySold: l.quantitySold,
        netUnitPrice: l.netUnitPrice,
        quantiteRetour: 0
      }))
    };
  }

  protected updateQty(salesLineId: number | undefined, qty: number): void {
    this.selectedLines.update(lines =>
      lines.map(l => l.salesLineId === salesLineId
        ? { ...l, quantiteRetour: Math.min(Number(qty), l.quantitySold ?? 0) }
        : l)
    );
  }

  protected confirmerRetour(): void {
    const sale = this.currentSale();
    const motif = this.motif();
    const modeReglement = this.modeReglement();
    if (!sale || !motif || !modeReglement) return;

    const lines = this.selectedLines()
      .filter(l => (l.quantiteRetour ?? 0) > 0)
      .map(l => ({ salesLineId: l.salesLineId!, salesLineDate: l.salesLineDate!, quantite: l.quantiteRetour! }));

    if (lines.length === 0) return;

    const request: RetourClientRequest = {
      saleId: sale.saleId!,
      saleDate: sale.saleDate!,
      motif,
      modeReglement,
      commentaire: this.commentaire() || undefined,
      lines
    };

    this.submitLoading.set(true);
    this.submitError.set(null);
    this.api.validerRetour(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.submitLoading.set(false);
          this.activeModal.close(result);
        },
        error: () => {
          this.submitLoading.set(false);
          this.submitError.set("Une erreur est survenue. Veuillez réessayer.");
        }
      });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
