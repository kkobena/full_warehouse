import { AfterViewInit, Component, computed, DestroyRef, effect, ElementRef, inject, OnInit, signal, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';

import { NgbModal, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { NgbConfirmDialogService } from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ButtonComponent, InputNumberComponent, SelectComponent, SelectSearchComponent } from '../../../../shared/ui';

import { AjustementFacade } from '../../data-access/facades/ajustement.facade';
import { AjustEvent, ILotItem } from '../../models';
import { IAjustement } from '../../../../shared/model/ajustement.model';
import { IMotifAjustement } from '../../../../shared/model/motif-ajustement.model';
import { IStorage } from '../../../../shared/model/magasin.model';
import { IProduit } from '../../../../shared/model';
import { ProduitSearch } from '../../../../shared/model';
import { AjustementFinalyseModalComponent } from '../../ui/ajustement-finalyse-modal/ajustement-finalyse-modal.component';
import { AjustementLinesTableComponent } from '../../ui/ajustement-lines-table/ajustement-lines-table.component';
import { CommandeProductSearchComponent } from '../../../commande/ui/commande-product-search/commande-product-search.component';

@Component({
  selector: 'app-ajustement-form',
  templateUrl: './ajustement-form.component.html',
  styleUrl: './ajustement-form.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    SelectComponent,
    SelectSearchComponent,
    InputNumberComponent,
    NgbTooltip,
    AjustementLinesTableComponent,
    CommandeProductSearchComponent,
  ],
})
export class AjustementFormComponent implements OnInit, AfterViewInit {
  readonly facade = inject(AjustementFacade);
  private readonly router = inject(Router);
  private readonly modal = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly productSearch = viewChild<CommandeProductSearchComponent>('productSearch');
  private readonly motifSelect = viewChild('motifSelect', { read: ElementRef<HTMLElement> });
  private readonly qtyBox = viewChild('qtyBox', { read: ElementRef<HTMLElement> });

  /** Produit sélectionné (pour affichage meta). */
  protected readonly produitSearch = signal<ProduitSearch | null>(null);

  /** Lots disponibles enrichis d'un libellé complet — affiché tel quel dans le select fermé. */
  protected readonly lotOptions = computed(() =>
    this.facade.availableLots().map(lot => ({ ...lot, displayLabel: this.lotOptionLabel(lot) })),
  );

  /** Valeur saisie : positive = entrée, négative = sortie. */
  protected qty = 0;

  private lastEvent$ = toObservable(this.facade.lastEvent).pipe(
    filter((e): e is AjustEvent => e !== null),
  );

  constructor() {
    effect(() => {
      const err = this.facade.error();
      if (err) {
        this.notificationService.error(err, 'Erreur');
      }
    });
  }

  ngOnInit(): void {
    this.facade.resetForm();
    this.facade.init();
    this.lastEvent$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      switch (event.type) {
        case 'AJUST_FINALIZED':
          this.notificationService.success('Ajustement enregistré et appliqué au stock.', 'Clôturé');
          setTimeout(() => this.router.navigate(['/features-ajustement']), 1600);
          break;
        case 'AJUST_CREATED':
        case 'LINE_ADDED':
          this.resetEntry();
          break;
        default:
          break;
      }
    });
  }

  ngAfterViewInit(): void {
    this.qtyBox()?.nativeElement.querySelector('input')?.addEventListener('focus', () => this.onQtyFocus());
  }

  // ── Storage ───────────────────────────────────────────────────────────────

  protected get selectedStorage(): IStorage | null {
    return this.facade.selectedStorage();
  }

  protected set selectedStorage(s: IStorage | null) {
    if (s) {
      this.facade.setStorage(s);
      setTimeout(() => this.motifSelect()?.nativeElement.querySelector('input')?.focus(), 50);
    }
  }

  // ── Motif ─────────────────────────────────────────────────────────────────

  protected get selectedMotif(): IMotifAjustement | null {
    return this.facade.selectedMotif();
  }

  protected set selectedMotif(m: IMotifAjustement | null) {
    this.facade.setMotif(m);
    if (m) setTimeout(() => this.productSearch()?.getFocus(), 50);
  }

  // ── Lot ───────────────────────────────────────────────────────────────────

  protected get selectedLot(): ILotItem | null {
    return this.facade.selectedLot();
  }

  protected set selectedLot(lot: ILotItem | null) {
    this.facade.setLot(lot);
  }

  // ── Product ───────────────────────────────────────────────────────────────

  private resetEntry(): void {
    this.qty = 0;
    this.produitSearch.set(null);
    this.facade.setProduit(null);
    this.productSearch()?.reset();
    // Force app-input-number display to blank
    const inputEl = this.qtyBox()?.nativeElement.querySelector('input');
    if (inputEl) inputEl.value = '';
    setTimeout(() => this.productSearch()?.getFocus(), 50);
  }

  protected onProduitSelected(p: ProduitSearch | null): void {
    this.produitSearch.set(p);
    this.facade.setProduit(p ? this.adaptProduit(p) : null);
    this.qty = p ? 1 : 0;
    if (p) {
      setTimeout(() => {
        const input = this.qtyBox()?.nativeElement.querySelector('input');
        input?.focus();
        input?.select();
      }, 50);
    }
  }

  protected onQtyFocus(): void {
    this.qtyBox()?.nativeElement.querySelector('input')?.select();
  }

  // ── Ligne ─────────────────────────────────────────────────────────────────

  protected onAddLine(): void {
    if (!this.canAdd) return;
    this.facade.addLine(this.qty);
  }

  protected onDeleteLine(line: IAjustement): void {
    if (!line.id) return;
    this.confirmDialog.onConfirm(
      () => this.facade.removeLine(line.id!),
      'Supprimer la ligne',
      'Confirmer la suppression de cette ligne d\'ajustement ?',
      'pi pi-trash',
    );
  }

  protected onDeleteSelection(lines: IAjustement[]): void {
    const ids = lines.map(l => l.id!).filter(Boolean);
    if (!ids.length) return;
    this.confirmDialog.onConfirm(
      () => this.facade.removeLines(ids),
      'Supprimer la sélection',
      `Supprimer les ${ids.length} ligne(s) sélectionnée(s) ?`,
      'pi pi-trash',
    );
  }

  protected onUpdateQty(event: { line: IAjustement; absQty: number }): void {
    this.facade.updateLineQty(event.line, event.absQty);
  }

  protected onSearchLines(search: string): void {
    this.facade.searchLines(search);
  }

  // ── Finalisation ─────────────────────────────────────────────────────────

  protected openFinaliseModal(): void {
    const ref = this.modal.open(AjustementFinalyseModalComponent, {
      size: 'md',
      backdrop: 'static',
    });
    ref.result.then(
      (commentaire: string) => this.facade.finalise(commentaire),
      () => {},
    );
  }

  // ── Navigation ────────────────────────────────────────────────────────────

  protected goBack(): void {
    this.router.navigate(['/features-ajustement']);
  }

  // ── Computed ─────────────────────────────────────────────────────────────

  protected get lotVisible(): boolean {
    return this.facade.gestionLot() && !!this.facade.selectedProduit() && this.qty > 0;
  }

  protected get canAdd(): boolean {
    return !!this.facade.selectedProduit() && !!this.facade.selectedMotif() && this.qty !== 0;
  }

  protected get stockAfter(): number {
    return this.facade.stockActuel() + this.qty;
  }

  protected lotOptionLabel(lot: ILotItem): string {
    const exp = lot.expiryDate ? ` — exp. ${lot.expiryDate}` : '';
    return `${lot.numLot ?? 'N°' + lot.id}${exp}  (${lot.currentQuantity} u.)`;
  }

  // ── Adapter ──────────────────────────────────────────────────────────────

  private adaptProduit(p: ProduitSearch): IProduit {
    return {
      id: p.id,
      totalQuantity: p.totalQuantity,
      qtyReserve: p.reserveQuantity,
    } as IProduit;
  }
}
