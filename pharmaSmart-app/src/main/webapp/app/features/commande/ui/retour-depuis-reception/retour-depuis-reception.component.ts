import { Component, ElementRef, inject, OnInit, Renderer2, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { InputNumberModule } from "primeng/inputnumber";
import { IOrderLine } from "../../../../shared/model/order-line.model";
import { ICommande } from "../../../../shared/model/commande.model";
import { IAvoirFournisseur, IAvoirFromBonLignesCommand } from "../../../../shared/model/avoir-fournisseur.model";
import { AvoirFournisseurService } from "../../../../entities/commande/retour_fournisseur/avoir-fournisseur.service";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";
import { TableModule } from "primeng/table";
import { SelectModule } from "primeng/select";
import { IMotifRetourProduit } from "../../../../shared/model/motif-retour-produit.model";
import { ModifRetourProduitService } from "../../../../entities/motif-retour-produit/motif-retour-produit.service";

interface RetourLine {
  orderLine: IOrderLine;
  qtyRetour: number;
  maxQty: number;
  motifRetourId: number | null;
}

@Component({
  selector: "app-retour-depuis-reception",
  templateUrl: "./retour-depuis-reception.component.html",
  styleUrls: ["./retour-depuis-reception.component.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, FormsModule, ButtonModule, InputNumberModule, TableModule, SelectModule]
})
export class RetourDepuisReceptionComponent implements OnInit {
  commande!: ICommande;
  orderLines: IOrderLine[] = [];
  header = "Retour fournisseur";

  protected retourLines: RetourLine[] = [];
  protected commentaire = "";
  protected saving = false;

  protected motifs: IMotifRetourProduit[] = [];

  private readonly activeModal = inject(NgbActiveModal);
  private readonly avoirService = inject(AvoirFournisseurService);
  private readonly motifService = inject(ModifRetourProduitService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly renderer = inject(Renderer2);
  private readonly elementRef = inject(ElementRef);
  ngOnInit(): void {
    this.motifService.query({ size: 999 }).subscribe({ next: res => (this.motifs = res.body ?? []) });
    this.retourLines = this.orderLines
      .filter(l => (l.quantityReceived ?? 0) > 0)  //On ne peut retourner une quantité qu'on a pas reçu
      .map(l => ({
        orderLine: l,
        qtyRetour: 0,
        maxQty: l.quantityReceived ?? 0,
        motifRetourId: null as number | null
      }));
  }

  protected get hasSelection(): boolean {
    return this.retourLines.some(r => r.qtyRetour > 0);
  }

  protected get canSave(): boolean {
    const sel = this.retourLines.filter(r => r.qtyRetour > 0);
    return sel.length > 0 && sel.every(r => r.motifRetourId != null);
  }

  protected get isRetourComplet(): boolean {
    return this.retourLines.length > 2 && this.retourLines.every(r => r.qtyRetour === r.maxQty);
  }

  protected get totalLignes(): number {
    return this.retourLines.filter(r => r.qtyRetour > 0).length;
  }

  protected get totalUnites(): number {
    return this.retourLines.reduce((s, r) => s + r.qtyRetour, 0);
  }
  protected onDropdownShow(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector(".modal-body");
    if (modalBody) {
      this.renderer.addClass(modalBody, "overflow-visible");
    }
  }

  protected onDropdownHide(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector(".modal-body");
    if (modalBody) {
      this.renderer.removeClass(modalBody, "overflow-visible");
    }
  }
  protected onSelectAll(): void {
    this.retourLines.forEach(r => (r.qtyRetour = r.maxQty));
  }

  protected onReset(): void {
    this.retourLines.forEach(r => {
      r.qtyRetour = 0;
      r.motifRetourId = null;
    });
  }

  protected onSubmit(): void {
    const selected = this.retourLines.filter(r => r.qtyRetour > 0);
    if (selected.length === 0) return;

    const command: IAvoirFromBonLignesCommand = {
      commandeId: this.commande.commandeId!.id,
      commandeOrderDate: this.commande.commandeId!.orderDate,
      commentaire: this.isRetourComplet ? (this.commentaire || undefined) : undefined,
      lignes: selected.map(r => ({
        orderLineId: r.orderLine.orderLineId!.id,
        orderLineOrderDate: r.orderLine.orderLineId!.orderDate,
        produitId: r.orderLine.produitId!,
        produitCip: r.orderLine.produitCip,
        qtyRetour: r.qtyRetour,
        motifRetourId: r.motifRetourId!,
        prixAchat: r.orderLine.costAmount ?? r.orderLine.orderCostAmount
      }))
    };

    this.saving = true;
    this.avoirService.createFromReception(command).subscribe({
      next: (avoir: IAvoirFournisseur) => {
        this.saving = false;
        this.notificationService.success(
          `Avoir ${avoir.reference ?? ''} créé — ${selected.length} ligne(s), ${this.totalUnites} unité(s)`,
          "Retour fournisseur"
        );
        this.activeModal.close(avoir);
      },
      error: err => {
        this.saving = false;
        this.notificationService.error(this.errorService.getErrorMessage(err), "Retour fournisseur");
      }
    });
  }

  protected onCancel(): void {
    this.activeModal.dismiss();
  }
}
