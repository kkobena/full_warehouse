import { Component, inject, input, OnInit, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonComponent, DataTableComponent, InputNumberComponent, SelectComponent } from 'app/shared/ui';
import { forkJoin } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { IDelivery } from 'app/shared/model/delevery.model';
import { AbstractOrderItem } from 'app/shared/model/abstract-order-item.model';
import { IMotifRetourProduit } from 'app/shared/model/motif-retour-produit.model';
import { IAvoirFournisseur, IBonLigneItem } from 'app/shared/model/avoir-fournisseur.model';
import { AvoirFournisseurService } from 'app/entities/commande/retour_fournisseur/avoir-fournisseur.service';
import { DeliveryService } from 'app/entities/commande/delevery/delivery.service';
import { ModifRetourProduitService } from 'app/entities/motif-retour-produit/motif-retour-produit.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { RetourCommentaireModalComponent } from './retour-commentaire-modal.component';

interface RetourLine {
  orderLine: AbstractOrderItem;
  selected: boolean;
  qtyRetour: number;
  motifRetourId: number | null;
  prixAchat: number;
  maxQty: number;
}

@Component({
  selector: 'app-retour-workspace',
  imports: [CommonModule, FormsModule, ButtonComponent, DataTableComponent, InputNumberComponent, SelectComponent],
  templateUrl: './retour-workspace.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './retour-workspace.component.scss',
})
export class RetourWorkspaceComponent implements OnInit {
  private readonly avoirService = inject(AvoirFournisseurService);
  private readonly entityService = inject(DeliveryService);
  private readonly motifService = inject(ModifRetourProduitService);
  private readonly notificationService = inject(NotificationService);
  private readonly modalService = inject(NgbModal);

  readonly bon = input.required<IDelivery>();

  readonly avoirCreated = output<IAvoirFournisseur>();
  readonly cancelled = output<void>();

  protected lines = signal<RetourLine[]>([]);
  protected motifs = signal<IMotifRetourProduit[]>([]);
  protected loading = signal(true);
  protected isSaving = signal(false);
  protected loadedBon: IDelivery | null = null;

  ngOnInit(): void {
    forkJoin({
      commande: this.entityService.find(this.bon().commandeId),
      motifs: this.motifService.query({ size: 999 }),
    }).subscribe({
      next: ({ commande, motifs }) => {
        this.loadedBon = commande.body;
        this.motifs.set(motifs.body ?? []);
        const orderLines: AbstractOrderItem[] = ((commande.body as any)?.orderLines ?? []) as AbstractOrderItem[];
        this.lines.set(
          orderLines
            .filter(ol => (ol.quantityReceived ?? 0) > 0)
            .map(ol => ({
              orderLine: ol,
              selected: false,
              qtyRetour: ol.quantityReceived ?? 1,
              motifRetourId: null as number | null,
              prixAchat: ol.orderCostAmount ?? 0,
              maxQty: ol.quantityReceived ?? 1,
            })),
        );
        this.loading.set(false);
      },
      error: () => {
        this.notificationService.error('Erreur lors du chargement du bon');
        this.loading.set(false);
      },
    });
  }

  protected get selectedLines(): RetourLine[] {
    return this.lines().filter(l => l.selected);
  }

  protected get canSave(): boolean {
    const sel = this.selectedLines;
    if (sel.length === 0 || this.isSaving()) return false;
    return sel.every(l => l.qtyRetour > 0 && l.motifRetourId != null);
  }

  protected getTotalMontant(): number {
    return this.selectedLines.reduce((s, l) => s + l.qtyRetour * l.prixAchat, 0);
  }

  protected onQtyChange(line: RetourLine): void {
    if (line.qtyRetour > line.maxQty) line.qtyRetour = line.maxQty;
    if (line.qtyRetour < 1) line.qtyRetour = 1;
  }

  protected onSaveClick(): void {
    if (!this.canSave) return;
    const ref = this.modalService.open(RetourCommentaireModalComponent, { centered: true, size: 'lg' });
    ref.result.then(
      (commentaire: string) => this.save(commentaire || undefined),
      () => {},
    );
  }

  private save(commentaire?: string): void {
    const bon = this.loadedBon ?? this.bon();
    if (!bon.commandeId?.id || !bon.commandeId?.orderDate) return;

    this.isSaving.set(true);

    const lignes: IBonLigneItem[] = this.selectedLines.map(l => ({
      orderLineId: l.orderLine.orderLineId!.id,
      orderLineOrderDate: l.orderLine.orderLineId!.orderDate,
      produitId: l.orderLine.produitId!,
      produitCip: l.orderLine.produitCip,
      qtyRetour: l.qtyRetour,
      motifRetourId: l.motifRetourId!,
      prixAchat: l.prixAchat,
    }));

    this.avoirService
      .createFromBonLignes({
        commandeId: bon.commandeId.id,
        commandeOrderDate: bon.commandeId.orderDate,
        commentaire,
        lignes,
      })
      .subscribe({
        next: avoir => {
          this.isSaving.set(false);
          this.notificationService.success('Avoir créé — réf. ' + (avoir.reference ?? ''));
          this.avoirCreated.emit(avoir);
        },
        error: () => {
          this.isSaving.set(false);
          this.notificationService.error("Erreur lors de la création de l'avoir");
        },
      });
  }

  protected cancel(): void {
    this.cancelled.emit();
  }
}
