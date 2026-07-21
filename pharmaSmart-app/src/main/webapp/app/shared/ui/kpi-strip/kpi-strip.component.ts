import {ChangeDetectionStrategy, Component, input} from '@angular/core';

import {SkeletonComponent} from '../skeleton/skeleton.component';

/**
 * Accent coloré porté par la barre verticale à gauche de l'item.
 *
 * ⚠ Passer la valeur en liaison — `[accent]="'primary'"` — et non en attribut statique.
 * IntelliJ ne résout pas l'attribut statique vers cette entrée et signale
 * « Attribute accent is not allowed here ». Angular accepte les deux formes.
 * Même précaution que pour `variant` d'`app-card` et `appKeyFilter`.
 */
export type AppKpiAccent = 'primary' | 'success' | 'danger' | 'warning' | 'info' | 'secondary' | 'none';

/**
 * Bandeau d'indicateurs horizontal — remplace le markup `.kpi-strip` recopié sur 32 écrans.
 *
 * Les items sont projetés (`<app-kpi-item>`) plutôt que passés en tableau : leur
 * sous-texte contient souvent du contenu riche — flèche d'évolution conditionnelle,
 * pourcentage formaté, `@if` — qu'une simple chaîne ne couvrirait pas.
 *
 * Les séparateurs ne sont plus des éléments : `app-kpi-item + app-kpi-item` les trace en
 * CSS. Cela supprime les 115 `<div class="kpi-strip-divider">` manuels du code, et avec
 * eux les oublis et les doublons.
 *
 * @example
 * <app-kpi-strip [loading]="isLoading()" [skeletonCount]="7">
 *   <app-kpi-item [accent]="'primary'" icon="pi pi-shopping-cart" iconClass="text-primary"
 *                 label="CA Net" [value]="venteRecord?.netAmount | number" valueClass="text-primary">
 *     <ng-container ngProjectAs="[kpiSub]">{{ venteRecord?.saleCount | number }} ventes</ng-container>
 *   </app-kpi-item>
 * </app-kpi-strip>
 */
@Component({
  selector: 'app-kpi-strip',
  imports: [SkeletonComponent],
  template: `
    <div [class]="stripClasses()">
      @if (loading()) {
        <!--
          Dimensions reprises telles quelles du markup d'origine, pour que la bascule
          chargement → données ne provoque aucun saut de mise en page.
        -->
        @for (i of skeletonSlots(); track i) {
          <div class="kpi-strip-item">
            <app-skeleton [shape]="'circle'" size="1.5rem" />
            <div class="kpi-strip-body">
              <app-skeleton class="mb-1" height="0.6rem" width="5rem" />
              <app-skeleton class="mb-1" height="1.1rem" width="7rem" />
              <app-skeleton height="0.6rem" width="4rem" />
            </div>
          </div>
        }
      } @else {
        <ng-content />
      }
    </div>
  `,
  styleUrl: './kpi-strip.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KpiStripComponent {
  /** Affiche un squelette de chargement à la place des items projetés. */
  readonly loading = input<boolean>(false);

  /** Nombre d'items simulés pendant le chargement. */
  readonly skeletonCount = input<number>(4);

  /** Classes additionnelles, ex. `mb-2`. */
  readonly stripClass = input<string>('');

  protected stripClasses(): string {
    return ['kpi-strip', this.stripClass()].filter(Boolean).join(' ');
  }

  protected skeletonSlots(): number[] {
    return Array.from({length: this.skeletonCount()}, (_, i) => i);
  }
}
