import { Component, computed, DestroyRef, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, finalize } from 'rxjs';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, DataTableComponent, SelectSearchComponent } from 'app/shared/ui';
import { IBed, IBedLigne } from '../../data-access/bed.model';
import { BedService } from '../../data-access/bed.service';
import { ProduitSearch } from 'app/shared/model/produit.model';
import { ProduitService } from 'app/entities/produit/produit.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { NgbConfirmDialogService } from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';

@Component({
  selector: 'app-bed-lignes',
  templateUrl: './bed-lignes.component.html',
  styleUrls: ['./bed-lignes.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    NgbTooltip,
    SelectSearchComponent,
  ],
})
export class BedLignesComponent {
  readonly bed = input.required<IBed>();
  readonly isBrouillon = input(false);

  readonly bedUpdated = output<IBed>();
  readonly validated = output<void>();
  readonly retour = output<void>();

  readonly selectedProduit = signal<ProduitSearch | null>(null);
  protected ligneQuantite = 1;
  protected lignePrixAchat = 0;
  readonly produitSuggestions = signal<ProduitSearch[]>([]);
  readonly addingLigne = signal(false);

  readonly totalLignes = computed(() =>
    (this.bed()?.lignes ?? []).reduce((s, l) => s + (l.quantite ?? 0) * (l.prixAchat ?? 0), 0),
  );
  readonly ligneCount = computed(() => this.bed()?.lignes?.length ?? 0);

  private readonly bedService = inject(BedService);
  private readonly produitService = inject(ProduitService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Passé à `[typeahead]` de `app-select-search` dans le seul but d'être « observé » :
   * ng-select désactive alors son propre filtrage client (par `bindLabel`), qui sinon
   * masquait les résultats retournés par le backend sur un code CIP (le texte tapé ne
   * matche pas `libelle`). Voir `commande-product-search.component.ts` pour le même correctif.
   */
  protected readonly typeaheadSink$ = new Subject<string>();

  constructor() {
    this.typeaheadSink$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
  }

  onSearchProduit(term: string): void {
    this.produitService
      .search({ search: term, page: 0, size: 15 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => this.produitSuggestions.set(res.body ?? []));
  }

  onProduitSelect(produit: ProduitSearch | null): void {
    this.selectedProduit.set(produit);
    this.lignePrixAchat = produit?.fournisseurProduit?.prixAchat ?? 0;
  }

  onAjouterLigne(): void {
    const bed = this.bed();
    const produit = this.selectedProduit();
    if (!bed?.id || !produit || this.ligneQuantite <= 0) {
      this.notificationService.error('Sélectionnez un produit et une quantité valide', 'Validation');
      return;
    }
    const fp = produit.fournisseurProduit;
    if (!fp) {
      this.notificationService.error("Ce produit n'a pas de fournisseur principal configuré", 'Erreur');
      return;
    }
    this.addingLigne.set(true);
    const ligne: IBedLigne = {
      fournisseurProduitId: fp.id,
      quantite: this.ligneQuantite,
      prixAchat: this.lignePrixAchat,
      prixVente: fp.prixUni,
    };
    this.bedService
      .addLigne(bed.id!, bed.orderDate!, ligne)
      .pipe(
        finalize(() => this.addingLigne.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: updated => {
          this.bedUpdated.emit(updated);
          this.selectedProduit.set(null);
          this.ligneQuantite = 1;
          this.lignePrixAchat = 0;
        },
        error: () => this.notificationService.error("Erreur lors de l'ajout de la ligne", 'Erreur'),
      });
  }

  onSupprimerLigne(ligne: IBedLigne): void {
    const bed = this.bed();
    if (!bed?.id) return;
    this.bedService
      .removeLigne(bed.id!, bed.orderDate!, ligne.id!, ligne.orderDate!)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          const updated: IBed = { ...bed, lignes: (bed.lignes ?? []).filter(l => l.id !== ligne.id) };
          this.bedUpdated.emit(updated);
        },
        error: () => this.notificationService.error('Erreur lors de la suppression de la ligne', 'Erreur'),
      });
  }

  onValider(): void {
    const bed = this.bed();
    this.confirmDialog.onConfirm(
      () => this.validated.emit(),
      'Valider le BED',
      `Valider définitivement le ${bed.receiptReference} ? Cette action créditera le stock et est irréversible.`,
    );
  }
}
