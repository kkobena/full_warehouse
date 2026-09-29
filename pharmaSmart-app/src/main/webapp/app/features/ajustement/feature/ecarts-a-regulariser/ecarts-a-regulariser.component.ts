import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {DecimalPipe} from '@angular/common';
import {Router} from '@angular/router';
import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {AjustementApiService} from '../../data-access/services/ajustement-api.service';
import {IEcartStock} from '../../models';
import {ButtonComponent, DataTableComponent, HintComponent, ToolbarComponent} from '../../../../shared/ui';

/** Lot 4 de PLAN-VENTE-SUR-STOCK-ERRONE : la liste de travail du pharmacien avant ajustement ou inventaire. */
@Component({
  selector: 'app-ecarts-a-regulariser',
  template: `
    <div class="pharma-smart-content p-1">
      <app-toolbar icon="pi pi-exclamation-circle" title="Écarts à régulariser" [subtitle]="sousTitre()">
        <ng-container ngProjectAs="[toolbarActions]">
          <div class="pharma-toolbar-actions">
            <div class="pharma-button-group">
              <app-button (clicked)="charger()" [loading]="loading()" icon="pi pi-refresh" label="Actualiser" severity="secondary" [outlined]="true" />
              <app-button (clicked)="retour()" icon="pi pi-arrow-left" label="Ajustements" severity="secondary" [outlined]="true" />
              <app-button (clicked)="nouvelAjustement()" [raised]="true" icon="pi pi-plus" label="Nouvel ajustement" severity="success" />
            </div>
          </div>
        </ng-container>
      </app-toolbar>

      <app-hint storageKey="ecarts-a-regulariser-hint">
        Un stock négatif est normal tant qu'il correspond aux avoirs ouverts : c'est ce que l'officine doit à ses clients.
        Au-delà, des boîtes sont sorties sans être connues de la machine — à corriger par un ajustement ou à l'inventaire.
      </app-hint>

      <app-data-table [loading]="loading()" [value]="ecarts()" dataKey="produitId">
        <ng-template #header>
          <tr class="pharma-table-head">
            <th style="width: 14%">Code CIP</th>
            <th>Produit</th>
            <th class="text-right" style="width: 13%" ngbTooltip="Stock du magasin, réserve et UG comprises">Stock machine</th>
            <th class="text-right" style="width: 13%" ngbTooltip="Quantité encore due aux clients">Dû aux avoirs</th>
            <th class="text-right" style="width: 13%" ngbTooltip="Unités sorties sans explication">Écart</th>
          </tr>
        </ng-template>
        <ng-template #body let-ecart>
          <tr class="pharma-row-hover" [attr.data-produit]="ecart.produitId">
            <td class="text-muted">{{ ecart.codeCip ?? '–' }}</td>
            <td><span class="pharma-entity-name">{{ ecart.libelle }}</span></td>
            <td class="text-right">{{ ecart.stock | number }}</td>
            <td class="text-right">{{ ecart.quantiteDue | number }}</td>
            <td class="text-right fw-semibold text-danger">{{ ecart.ecart | number }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td class="text-center text-muted py-4" colspan="5">
              <i class="pi pi-check-circle text-success me-2"></i>Aucun écart : chaque stock négatif est couvert par un avoir ouvert.
            </td>
          </tr>
        </ng-template>
      </app-data-table>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, NgbTooltip, ButtonComponent, DataTableComponent, HintComponent, ToolbarComponent],
})
export class EcartsARegulariserComponent implements OnInit {
  protected readonly ecarts = signal<IEcartStock[]>([]);
  protected readonly loading = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly sousTitre = computed(() => {
    if (this.erreur()) return this.erreur()!;
    const liste = this.ecarts();
    const unites = liste.reduce((total, e) => total + e.ecart, 0);
    return liste.length ? `${liste.length} produit(s), ${unites} unité(s) non expliquée(s)` : '';
  });

  private readonly api = inject(AjustementApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.charger();
  }

  protected charger(): void {
    this.loading.set(true);
    this.erreur.set(null);
    this.api
      .listEcartsARegulariser()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ecarts => {
          this.ecarts.set(ecarts);
          this.loading.set(false);
        },
        error: () => {
          this.erreur.set('Impossible de charger les écarts');
          this.loading.set(false);
        },
      });
  }

  protected retour(): void {
    this.router.navigate(['/features-ajustement']);
  }

  protected nouvelAjustement(): void {
    this.router.navigate(['/features-ajustement/new']);
  }
}
