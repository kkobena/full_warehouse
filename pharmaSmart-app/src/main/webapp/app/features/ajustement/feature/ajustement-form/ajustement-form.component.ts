import { Component, DestroyRef, effect, inject, OnInit, signal, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';

import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { NgbConfirmDialogService } from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Select } from 'primeng/select';
import { InputNumber } from 'primeng/inputnumber';
import { TooltipModule } from 'primeng/tooltip';
import { Toast } from 'primeng/toast';
import { TagModule } from 'primeng/tag';
import { InputGroup } from 'primeng/inputgroup';
import { InputGroupAddon } from 'primeng/inputgroupaddon';

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
  providers: [MessageService],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    Select,
    InputNumber,
    TooltipModule,
    Toast,
    TagModule,
    InputGroup,
    InputGroupAddon,
    AjustementLinesTableComponent,
    CommandeProductSearchComponent,
  ],
})
export class AjustementFormComponent implements OnInit {
  readonly facade = inject(AjustementFacade);
  private readonly router = inject(Router);
  private readonly modal = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly productSearch = viewChild<CommandeProductSearchComponent>('productSearch');
  private readonly motifSelect = viewChild<Select>('motifSelect');
  private readonly qtyBox = viewChild<InputNumber>('qtyBox');

  /** Produit sélectionné (pour affichage meta). */
  protected readonly produitSearch = signal<ProduitSearch | null>(null);

  /** Valeur saisie : positive = entrée, négative = sortie. */
  protected qty = 0;

  private lastEvent$ = toObservable(this.facade.lastEvent).pipe(
    filter((e): e is AjustEvent => e !== null),
  );

  constructor() {
    effect(() => {
      const err = this.facade.error();
      if (err) {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err, life: 5000 });
      }
    });
  }

  ngOnInit(): void {
    this.facade.resetForm();
    this.facade.init();
    this.lastEvent$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      switch (event.type) {
        case 'AJUST_FINALIZED':
          this.messageService.add({
            severity: 'success',
            summary: 'Clôturé',
            detail: 'Ajustement enregistré et appliqué au stock.',
            life: 3000,
          });
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

  // ── Storage ───────────────────────────────────────────────────────────────

  protected get selectedStorage(): IStorage | null {
    return this.facade.selectedStorage();
  }

  protected set selectedStorage(s: IStorage | null) {
    if (s) {
      this.facade.setStorage(s);
      setTimeout(() => this.motifSelect()?.el?.nativeElement?.querySelector('input')?.focus(), 50);
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
    // Force p-inputnumber display to blank
    const inputEl = this.qtyBox()?.input()?.nativeElement;
    if (inputEl) inputEl.value = '';
    setTimeout(() => this.productSearch()?.getFocus(), 50);
  }

  protected onProduitSelected(p: ProduitSearch | null): void {
    this.produitSearch.set(p);
    this.facade.setProduit(p ? this.adaptProduit(p) : null);
    this.qty = p ? 1 : 0;
    if (p) setTimeout(() => { this.qtyBox()?.input()?.nativeElement?.focus(); this.qtyBox()?.input()?.nativeElement?.select(); }, 50);
  }

  protected onQtyFocus(): void {
    this.qtyBox()?.input()?.nativeElement?.select();
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
