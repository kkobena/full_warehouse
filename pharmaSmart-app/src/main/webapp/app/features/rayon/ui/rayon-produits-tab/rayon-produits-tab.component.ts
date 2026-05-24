import { Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { AutoCompleteModule, AutoCompleteCompleteEvent, AutoCompleteSelectEvent } from 'primeng/autocomplete';
import { InputTextModule } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { IRayon } from '../../models/rayon.model';
import { IProduit } from '../../../../shared/model';
import { RayonProduitApiService } from '../../data-access/services/rayon-produit-api.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';
import { RayonAssignFormComponent, RayonAssignResult } from '../rayon-assign-form/rayon-assign-form.component';

interface ProduitInRayon extends IProduit {
  rayonProduitId?: number;
  otherStorageLabels?: string[];
}

@Component({
  selector: 'app-rayon-produits-tab',
  templateUrl: './rayon-produits-tab.component.html',
  styleUrl: './rayon-produits-tab.component.scss',
  imports: [FormsModule, TableModule, ButtonModule, TooltipModule, AutoCompleteModule, InputTextModule, IconField, InputIcon],
})
export class RayonProduitsTabComponent {
  readonly rayon = input.required<IRayon>();

  protected produits = signal<ProduitInRayon[]>([]);
  protected totalItems = signal(0);
  protected loading = signal(false);
  protected rows = 10;
  protected page = 0;

  protected selectedProduit: IProduit | null = null;
  protected searchSuggestions = signal<IProduit[]>([]);
  protected filterText = signal('');
  protected selectedProduits: ProduitInRayon[] = [];

  private readonly api = inject(RayonProduitApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly modalService = inject(NgbModal);
  private currentRayonId: number | null = null;

  constructor() {
    effect(() => {
      const rayon = this.rayon();
      if (!rayon?.id || rayon.id === this.currentRayonId) return;
      this.currentRayonId = rayon.id;
      this.page = 0;
      this.filterText.set('');
      this.selectedProduits = [];
      this.loadProduits(rayon, 0, this.rows, '');
    });
  }

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    this.page = Math.floor((event.first ?? 0) / (event.rows ?? this.rows));
    this.rows = event.rows ?? this.rows;
    this.loadProduits(this.rayon(), this.page, this.rows, this.filterText());
  }

  protected onFilterChange(value: string): void {
    this.filterText.set(value);
    this.page = 0;
    this.loadProduits(this.rayon(), 0, this.rows, value);
  }

  protected onComplete(event: AutoCompleteCompleteEvent): void {
    const query = event.query?.trim();
    if (!query) { this.searchSuggestions.set([]); return; }
    this.api.searchProduits(query).subscribe({
      next: res => this.searchSuggestions.set(res.body ?? []),
      error: () => this.searchSuggestions.set([]),
    });
  }

  protected onProduitSelected(event: AutoCompleteSelectEvent): void {
    const produit = event.value as IProduit;
    if (!produit?.id || !this.rayon()?.id) return;
    this.api.assign({ rayonId: this.rayon().id!, produitId: produit.id }).subscribe({
      next: () => this.afterAssign(produit),
      error: err => {
        const errorKey = err?.error?.errorKey;
        if (errorKey === 'duplicateProduitStockage') {
          const fromLibelle: string = err?.error?.payload?.rayonLibelle ?? 'un autre rayon';
          this.openMoveModal(
            [produit as ProduitInRayon],
            this.rayon().storageId!,
            this.rayon().id!,
            `"${produit.libelle}" est dans le rayon "${fromLibelle}". Choisir un nouvel emplacement.`,
            false,
          );
        } else {
          this.notificationService.error(this.errorService.getErrorMessage(err));
        }
      },
    });
  }

  protected onMove(produit: ProduitInRayon): void {
    this.openMoveModal(
      [produit],
      this.rayon().storageId!,
      this.rayon().id!,
      `Déplacer "${produit.libelle}" vers un autre emplacement`,
      this.rayon().code === 'SANS',
    );
  }

  protected onClone(produit: ProduitInRayon): void {
    const occupiedStorageIds = (produit.rayonProduits ?? [])
      .map(rp => rp.storageId)
      .filter((id): id is number => id != null);

    const ref = this.modalService.open(RayonAssignFormComponent, { size: 'lg', centered: true, backdrop: 'static' });
    const inst = ref.componentInstance as RayonAssignFormComponent;
    inst.produit = produit;
    inst.mode = 'add-storage';
    inst.title = `Affecter "${produit.libelle}" à un autre stockage`;
    inst.occupiedRealStorageIds = occupiedStorageIds;

    ref.closed.subscribe((result: RayonAssignResult) => {
      this.api.assign({ produitId: result.produitId, rayonId: result.rayonId }).subscribe({
        next: () => {
          this.notificationService.success(`"${produit.libelle}" affecté au nouveau stockage`);
        },
        error: err => this.notificationService.error(this.errorService.getErrorMessage(err)),
      });
    });
  }

  protected onMoveSelected(): void {
    if (!this.selectedProduits.length) return;
    const title = this.selectedProduits.length === 1
      ? `Déplacer "${this.selectedProduits[0].libelle}"`
      : `Déplacer ${this.selectedProduits.length} produits`;
    this.openMoveModal(
      [...this.selectedProduits],
      this.rayon().storageId!,
      this.rayon().id!,
      title,
      this.rayon().code === 'SANS',
    );
  }

  protected onRetirer(produit: ProduitInRayon): void {
    if (!produit.rayonProduitId) return;
    this.openMoveModal(
      [produit],
      this.rayon().storageId!,
      this.rayon().id!,
      `Retirer "${produit.libelle}" - choisir un emplacement de remplacement ou placer en "Sans emplacement"`,
      this.rayon().code === 'SANS',
    );
  }

  private openMoveModal(
    produits: ProduitInRayon[],
    storageId: number,
    rayonId: number,
    title: string,
    isSansSource: boolean,
  ): void {
    const ref = this.modalService.open(RayonAssignFormComponent, { size: 'lg', centered: true, backdrop: 'static' });
    const inst = ref.componentInstance as RayonAssignFormComponent;
    inst.produit = produits[0];
    inst.mode = 'move';
    inst.title = title;
    inst.currentStorageId = storageId;
    inst.currentRayonId = rayonId;
    inst.currentRayonIsSans = isSansSource;

    ref.closed.subscribe((result: RayonAssignResult) => {
      const produitIds = produits.map(p => p.id!);
      this.api.moveBatch(produitIds, result.rayonId).subscribe({
        next: () => {
          this.selectedProduits = [];
          this.page = 0;
          this.loadProduits(this.rayon(), 0, this.rows, this.filterText());
          const msg = produits.length === 1
            ? `"${produits[0].libelle}" déplacé avec succès`
            : `${produits.length} produits déplacés avec succès`;
          this.notificationService.success(msg);
        },
        error: err => this.notificationService.error(this.errorService.getErrorMessage(err)),
      });
    });
  }

  private afterAssign(produit: IProduit, message?: string): void {
    this.selectedProduit = null;
    this.searchSuggestions.set([]);
    this.page = 0;
    this.loadProduits(this.rayon(), 0, this.rows, this.filterText());
    this.notificationService.success(message ?? `"${produit.libelle}" affecté au rayon`);
  }

  private loadProduits(rayon: IRayon, page: number, size: number, search: string): void {
    this.loading.set(true);
    this.api.queryByRayon({ rayonId: rayon.id!, page, size, search: search || undefined }).subscribe({
      next: res => {
        this.totalItems.set(Number(res.headers.get('X-Total-Count') ?? 0));
        const data: ProduitInRayon[] = (res.body ?? []).map(p => ({
          ...p,
          rayonProduitId: p.rayonProduits?.find(rp => rp.rayonId === rayon.id)?.id,
          otherStorageLabels: [...new Set(
            (p.rayonProduits ?? [])
              .filter(rp => rp.storageId !== rayon.storageId && rp.codeRayon !== 'SANS')
              .map(rp => rp.libelleStorage ?? `Stockage ${rp.storageId}`)
          )],
        }));
        this.produits.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
