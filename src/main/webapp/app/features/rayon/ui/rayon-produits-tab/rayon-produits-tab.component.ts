import { Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { AutoCompleteModule, AutoCompleteCompleteEvent, AutoCompleteSelectEvent } from 'primeng/autocomplete';
import { IRayon } from '../../models/rayon.model';
import { IProduit } from '../../../../shared/model/produit.model';
import { RayonProduitApiService } from '../../data-access/services/rayon-produit-api.service';
import { NgbConfirmDialogService } from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';

interface ProduitInRayon extends IProduit {
  rayonProduitId?: number;
}

@Component({
  selector: 'app-rayon-produits-tab',
  templateUrl: './rayon-produits-tab.component.html',
  styleUrl: './rayon-produits-tab.component.scss',
  imports: [FormsModule, TableModule, ButtonModule, TooltipModule, AutoCompleteModule],
})
export class RayonProduitsTabComponent {
  readonly rayon = input.required<IRayon>();

  protected produits = signal<ProduitInRayon[]>([]);
  protected totalItems = signal(0);
  protected loading = signal(false);
  protected rows = 20;
  protected page = 0;

  protected selectedProduit: IProduit | null = null;
  protected searchSuggestions = signal<IProduit[]>([]);

  private readonly api = inject(RayonProduitApiService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private currentRayonId: number | null = null;

  constructor() {
    effect(() => {
      const rayon = this.rayon();
      if (!rayon?.id || rayon.id === this.currentRayonId) return;
      this.currentRayonId = rayon.id;
      this.page = 0;
      this.loadProduits(rayon, 0, this.rows);
    });
  }

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    this.page = Math.floor((event.first ?? 0) / (event.rows ?? this.rows));
    this.rows = event.rows ?? this.rows;
    this.loadProduits(this.rayon(), this.page, this.rows);
  }

  protected onComplete(event: AutoCompleteCompleteEvent): void {
    const query = event.query?.trim();
    if (!query) {
      this.searchSuggestions.set([]);
      return;
    }
    this.api.searchProduits(query).subscribe({
      next: res => this.searchSuggestions.set(res.body ?? []),
      error: () => this.searchSuggestions.set([]),
    });
  }

  protected onProduitSelected(event: AutoCompleteSelectEvent): void {
    const produit = event.value as IProduit;
    if (!produit?.id || !this.rayon()?.id) return;
    this.api.create({ rayonId: this.rayon().id!, produitId: produit.id }).subscribe({
      next: () => {
        this.selectedProduit = null;
        this.searchSuggestions.set([]);
        this.page = 0;
        this.loadProduits(this.rayon(), 0, this.rows);
        this.notificationService.success(`"${produit.libelle}" affecté au rayon`);
      },
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err)),
    });
  }

  protected onRetirer(produit: ProduitInRayon): void {
    if (!produit.rayonProduitId) return;
    this.confirmDialog.onConfirm(
      () => this.retirerProduit(produit),
      'Retirer du rayon',
      `Retirer "${produit.libelle}" de ce rayon ?`
    );
  }

  private loadProduits(rayon: IRayon, page: number, size: number): void {
    this.loading.set(true);
    this.api.queryByRayon({ rayonId: rayon.id!, page, size }).subscribe({
      next: res => {
        this.totalItems.set(Number(res.headers.get('X-Total-Count') ?? 0));
        const data: ProduitInRayon[] = (res.body ?? []).map(p => ({
          ...p,
          rayonProduitId: p.rayonProduits?.find(rp => rp.rayonId === rayon.id)?.id,
        }));
        this.produits.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private retirerProduit(produit: ProduitInRayon): void {
    this.api.delete(produit.rayonProduitId!).subscribe({
      next: () => {
        this.produits.update(list => list.filter(p => p.id !== produit.id));
        this.totalItems.update(n => n - 1);
        this.notificationService.success(`"${produit.libelle}" retiré du rayon`);
      },
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err)),
    });
  }
}
