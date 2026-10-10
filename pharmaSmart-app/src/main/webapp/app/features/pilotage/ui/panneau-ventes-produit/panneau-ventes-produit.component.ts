import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, model } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map, of } from 'rxjs';

import { ProduitStatService } from 'app/entities/produit/stat/produit-stat.service';
import { DataTableComponent, KpiItemComponent, KpiStripComponent, OffcanvasComponent } from 'app/shared/ui';
import { formatCurrency, formatDateFR, formatNumber } from 'app/shared/utils/format-utils';

/** Nombre de ventes montrées : le panneau répond à « qui a acheté ce produit », pas à un export. */
const VENTES_MAX = 100;

/** Dernier niveau de la descente : les ventes d'un produit sur la période (historique produit existant), résumées en tête. */
@Component({
  selector: 'app-panneau-ventes-produit',
  imports: [OffcanvasComponent, DataTableComponent, KpiStripComponent, KpiItemComponent, DatePipe],
  template: `
    <app-offcanvas (visibleChange)="visible.set($event)" [visible]="visible()" width="760px">
      <div appOffcanvasHeader class="panneau-ventes-entete">
        <i aria-hidden="true" class="pi pi-list"></i>
        <div>
          <h2 class="panneau-ventes-titre">{{ produit()?.libelle }}</h2>
          <p class="panneau-ventes-sous-titre">Ventes du {{ formaterDate(du()) }} au {{ formaterDate(au()) }}</p>
        </div>
      </div>

      <div class="panneau-ventes-corps">
        @if (ventes.error()) {
          <p class="text-danger" role="alert">Les ventes n'ont pas pu être chargées.</p>
        }

        <app-kpi-strip [loading]="ventes.isLoading()" [skeletonCount]="4">
          <app-kpi-item [value]="resume().nombre" label="Ventes" />
          <app-kpi-item [value]="formaterNombre(resume().quantite)" label="Quantité" />
          <app-kpi-item [value]="formaterMontant(resume().montant)" label="Montant TTC" />
          <app-kpi-item [value]="formaterMontant(resume().remise)" label="Remises" />
        </app-kpi-strip>

        @if (tronque()) {
          <p class="panneau-ventes-note" role="status">
            <i aria-hidden="true" class="pi pi-info-circle"></i>
            Seules les {{ ventesMax }} ventes les plus récentes sont listées et résumées ; l'historique complet est dans la fiche du produit.
          </p>
        }

        <app-data-table [scrollable]="true" scrollHeight="calc(100vh - 16rem)" [loading]="ventes.isLoading()" [stripedRows]="true" [value]="ventes.value() ?? []" emptyMessage="Aucune vente sur la période" size="small">
          <ng-template #header>
            <tr class="pharma-table-head">
              <th scope="col">Date</th>
              <th scope="col">Vente</th>
              <th class="text-end" scope="col">Qté</th>
              <th class="text-end" scope="col">Remise</th>
              <th class="text-end" scope="col">Montant TTC</th>
              <th scope="col">Vendeur</th>
            </tr>
          </ng-template>
          <ng-template #body let-vente>
            <tr>
              <td class="text-nowrap">{{ vente.mvtDate | date: 'dd/MM/yyyy HH:mm' }}</td>
              <td class="text-nowrap">{{ vente.reference }}</td>
              <td class="text-end">{{ vente.quantite }}</td>
              <td class="text-end">{{ vente.montantRemise ? formaterMontant(vente.montantRemise) : '—' }}</td>
              <td class="text-end fw-semibold">{{ formaterMontant(vente.montantTtc) }}</td>
              <td>{{ vente.user }}</td>
            </tr>
          </ng-template>
        </app-data-table>
      </div>
    </app-offcanvas>
  `,
  styles: `
    .panneau-ventes-entete {
      display: flex;
      align-items: flex-start;
      gap: 0.6rem;
      min-width: 0;

      > .pi {
        margin-top: 0.3rem;
        color: var(--pharma-chrome-tab);
        font-size: 1.1rem;
      }
    }

    .panneau-ventes-titre {
      margin: 0;
      font-size: 1.15rem;
      font-weight: 700;
    }

    .panneau-ventes-sous-titre {
      margin: 0;
      font-size: 0.8rem;
      color: var(--pharma-text-muted);
    }

    .panneau-ventes-corps {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .panneau-ventes-note {
      display: flex;
      gap: 0.35rem;
      margin: 0;
      font-size: 0.8rem;
      color: var(--pharma-text-muted);

      .pi {
        margin-top: 0.15rem;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PanneauVentesProduitComponent {
  readonly visible = model.required<boolean>();
  readonly produit = input<{ cle: string; libelle: string } | null>(null);
  readonly du = input.required<string>();
  readonly au = input.required<string>();

  private readonly produitStatService = inject(ProduitStatService);

  protected readonly ventesMax = VENTES_MAX;

  protected readonly ventes = rxResource({
    params: () => ({ produit: this.produit(), du: this.du(), au: this.au() }),
    stream: ({ params }) =>
      params.produit
        ? this.produitStatService
            .getProduitHistoriqueVente({ produitId: params.produit.cle, fromDate: params.du, toDate: params.au, page: 0, size: VENTES_MAX })
            .pipe(map(reponse => reponse.body ?? []))
        : of([]),
  });

  protected readonly resume = computed(() =>
    (this.ventes.value() ?? []).reduce(
      (total, vente) => ({
        nombre: total.nombre + 1,
        quantite: total.quantite + (vente.quantite ?? 0),
        montant: total.montant + (vente.montantTtc ?? 0),
        remise: total.remise + (vente.montantRemise ?? 0),
      }),
      { nombre: 0, quantite: 0, montant: 0, remise: 0 },
    ),
  );

  protected readonly tronque = computed(() => (this.ventes.value()?.length ?? 0) >= VENTES_MAX);

  protected formaterMontant(valeur: number): string {
    return formatCurrency(valeur);
  }

  protected formaterNombre(valeur: number): string {
    return formatNumber(valeur);
  }

  protected formaterDate(date: string): string {
    return formatDateFR(date);
  }
}
