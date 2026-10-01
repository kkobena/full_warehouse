import { ChangeDetectionStrategy, Component, computed, effect, input, model } from '@angular/core';

import { SkeletonComponent } from '../skeleton/skeleton.component';
import { AppSurfaceAccent, AppSurfaceVariant } from '../surface.types';

/**
 * Largeur dans la {@link DetailGridComponent} : `auto` partage la rangée, `wide` la prend
 * entière avec des colonnes larges (libellés longs), `full` la prend entière sans grille de
 * champs (tableau, contenu libre).
 *
 * ⚠ Passer la valeur en liaison — `[width]="'full'"` — et non en attribut statique.
 */
export type AppDetailSectionWidth = 'auto' | 'wide' | 'full';

/** `fields` : grille de `app-detail-field`. `block` : contenu libre (jauge, tableau…). */
export type AppDetailSectionLayout = 'fields' | 'block';

const STORAGE_PREFIX = 'detail-section:';

/**
 * Carte repliable d'un panneau de détail, à ranger dans une `app-detail-grid`.
 *
 * <p>Avec `storageKey`, l'état replié est mémorisé : une section repliée sur un produit l'est
 * encore sur le suivant, ce qui est tout l'intérêt dans un master/detail qu'on parcourt.
 *
 * <p>Habillage tiré de `app/shared/scss/_surface.scss` : `accent` teinte le liseré, l'en-tête et
 * l'icône ; `variant="muted"` met à plat l'information de référence.
 *
 * @example
 * <app-detail-section header="Tarifs assurance" icon="pi pi-percentage" [width]="'full'"
 *                     [count]="tarifs().length" storageKey="produit.tarifs">
 *   <app-button ngProjectAs="[sectionActions]" label="Ajouter" … />
 *   <table>…</table>
 * </app-detail-section>
 */
@Component({
  selector: 'app-detail-section',
  imports: [SkeletonComponent],
  host: {
    '[class.detail-section--wide]': "width() === 'wide'",
    '[class.detail-section--full]': "width() === 'full'",
    '[class.detail-section--muted]': "variant() === 'muted'",
    '[class.detail-section--accent]': "accent() !== 'none'",
    '[class]': 'accentClass()',
  },
  template: `
    <div class="detail-section-header">
      <button
        type="button"
        class="detail-section-toggle"
        [disabled]="!collapsible()"
        [attr.aria-expanded]="!collapsed()"
        (click)="toggle()"
      >
        @if (icon()) {
          <i [class]="icon()" aria-hidden="true"></i>
        }
        <span class="detail-section-title">{{ header() }}</span>
        @if (count() != null) {
          <span class="detail-section-count">{{ count() }}</span>
        }
        @if (collapsible()) {
          <i class="pi pi-chevron-down detail-section-chevron" aria-hidden="true"></i>
        }
      </button>
      <ng-content select="[sectionActions]" />
    </div>

    @if (!collapsed()) {
      <div [class]="bodyClasses()">
        @if (loading()) {
          @for (i of skeletonSlots(); track i) {
            <app-skeleton height="1.1rem" />
          }
        } @else {
          <ng-content />
        }
      </div>
    }
  `,
  styleUrl: './detail-section.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetailSectionComponent {
  readonly header = input.required<string>();

  /** Classe d'icône précédant le titre, ex. `pi pi-tag`. */
  readonly icon = input<string>('');

  /** Nombre d'éléments, affiché en pastille : il reste lisible section repliée. */
  readonly count = input<number | null>(null);

  readonly width = input<AppDetailSectionWidth>('auto');

  readonly accent = input<AppSurfaceAccent>('none');

  /** Disposition du corps. Par défaut `block` en largeur `full`, `fields` sinon. */
  readonly layout = input<AppDetailSectionLayout | null>(null);

  readonly variant = input<AppSurfaceVariant>('raised');

  readonly collapsible = input<boolean>(true);

  /** Lier en deux temps : `[collapsed]="x()" (collapsedChange)="x.set($event)"`. */
  readonly collapsed = model<boolean>(false);

  /** Clé de mémorisation de l'état replié dans le stockage local. */
  readonly storageKey = input<string>('');

  /** Squelette à la place du contenu, le temps d'un chargement propre à la section. */
  readonly loading = input<boolean>(false);

  readonly skeletonRows = input<number>(3);

  protected readonly accentClass = computed(() => (this.accent() === 'none' ? '' : `pharma-accent-${this.accent()}`));

  protected readonly bodyClasses = computed(() => {
    const layout = this.layout() ?? (this.width() === 'full' ? 'block' : 'fields');
    return layout === 'block' ? 'detail-section-body' : 'detail-section-body detail-section-body--fields';
  });

  protected readonly skeletonSlots = computed(() => Array.from({ length: this.skeletonRows() }, (_, i) => i));

  private restoredKey = '';

  constructor() {
    // Un seul effet, pour ne pas dépendre de l'ordre entre restauration et persistance.
    effect(() => {
      const key = this.storageKey();
      const collapsed = this.collapsed();
      if (!key) {
        return;
      }
      if (this.restoredKey !== key) {
        this.restoredKey = key;
        const saved = readStored(key);
        if (saved != null && saved !== collapsed) {
          this.collapsed.set(saved);
        }
        return;
      }
      writeStored(key, collapsed);
    });
  }

  protected toggle(): void {
    if (this.collapsible()) {
      this.collapsed.set(!this.collapsed());
    }
  }
}

function readStored(key: string): boolean | null {
  try {
    const v = localStorage.getItem(STORAGE_PREFIX + key);
    return v == null ? null : v === 'true';
  } catch {
    return null;
  }
}

function writeStored(key: string, collapsed: boolean): void {
  try {
    localStorage.setItem(STORAGE_PREFIX + key, String(collapsed));
  } catch {
    // Stockage indisponible (navigation privée) : l'état reste valable pour la session.
  }
}
