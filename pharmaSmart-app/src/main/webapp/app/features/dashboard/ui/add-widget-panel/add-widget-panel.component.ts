import { ChangeDetectionStrategy, Component, computed, effect, input, model, output, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonComponent, CheckboxComponent, InputComponent, OffcanvasComponent } from 'app/shared/ui';
import { WIDGET_CATEGORY_LABELS, WidgetCategory, WidgetDefinition } from '../../models/dashboard.model';

export interface CatalogueEntry {
  definition: WidgetDefinition;
  licensed: boolean;
  alreadyAdded: boolean;
}

/**
 * Panneau latéral « Ajouter une tuile ». Par défaut, seules les tuiles absentes du tableau de bord
 * sont listées. La case « Tout afficher » fait apparaître aussi les tuiles déjà présentes, avec une
 * distinction visuelle et le bouton « Ajouter en double ».
 */
@Component({
  selector: 'app-add-widget-panel',
  imports: [FormsModule, OffcanvasComponent, InputComponent, ButtonComponent, CheckboxComponent],
  template: `
    <app-offcanvas [visible]="visible()" (visibleChange)="visible.set($event)" width="420px">
      <div appOffcanvasHeader class="d-flex align-items-center gap-2">
        <i class="pi pi-plus-circle text-primary" aria-hidden="true"></i>
        <span class="fw-bold fs-5">Ajouter une tuile</span>
      </div>

      <div class="catalogue-toolbar">
        <app-input
          type="search"
          placeholder="Rechercher une tuile…"
          ariaLabel="Rechercher une tuile"
          [ngModel]="search()"
          (ngModelChange)="search.set($event ?? '')"
        />
        <app-checkbox
          label="Tout afficher (y compris les tuiles déjà présentes)"
          [ngModel]="toutAfficher()"
          (ngModelChange)="toutAfficher.set($event ?? false)"
        />
      </div>

      @for (group of groups(); track group.category) {
        <h3 class="catalogue-category">{{ group.label }}</h3>
        <ul class="list-unstyled mb-3">
          @for (entry of group.entries; track entry.definition.key) {
            <li
              class="catalogue-entry"
              [class.is-locked]="!entry.licensed"
              [class.is-added]="entry.alreadyAdded"
              [class.is-pending]="isPending(entry.definition.key)"
            >
              <i [class]="entry.definition.icon + ' catalogue-icon'" aria-hidden="true"></i>
              <div class="flex-fill">
                <div class="catalogue-entry-label">
                  {{ entry.definition.label }}
                  @if (entry.alreadyAdded) {
                    <i class="pi pi-check-circle catalogue-added-mark" aria-label="Déjà sur le tableau de bord"></i>
                  }
                </div>
                <div class="small text-body-secondary">
                  {{ entry.licensed ? entry.definition.description : 'Non inclus dans votre abonnement' }}
                </div>
              </div>
              @if (entry.licensed) {
                @if (entry.alreadyAdded) {
                  <app-button label="Ajouter en double" icon="pi pi-clone" size="small" severity="secondary" [outlined]="true" (clicked)="addDuplicate(entry.definition)" />
                } @else {
                  <input
                    type="checkbox"
                    class="form-check-input catalogue-check"
                    [checked]="isPending(entry.definition.key)"
                    (change)="togglePending(entry.definition.key)"
                    [attr.aria-label]="'Sélectionner ' + entry.definition.label"
                  />
                }
              } @else {
                <i class="pi pi-lock text-body-secondary" aria-label="Non inclus dans votre abonnement"></i>
              }
            </li>
          }
        </ul>
      } @empty {
        <p class="text-body-secondary text-center py-4">
          @if (entries().length === 0) {
            Aucune tuile ne vous est autorisée.
          } @else if (emptyReason() === 'all-added') {
            Toutes les tuiles disponibles sont déjà sur votre tableau de bord.
            <br />
            <button class="btn btn-link btn-sm p-0 mt-1" (click)="toutAfficher.set(true)">Tout afficher</button>
          } @else {
            Aucune tuile ne correspond à la recherche.
          }
        </p>
      }

      @if (pendingCount() > 0) {
        <div class="catalogue-batch-bar">
          <span class="catalogue-batch-count">{{ pendingCount() }} tuile{{ pendingCount() > 1 ? 's' : '' }} sélectionnée{{ pendingCount() > 1 ? 's' : '' }}</span>
          <div class="d-flex gap-2">
            <app-button label="Annuler" severity="secondary" [text]="true" size="small" (clicked)="clearPending()" />
            <app-button
              [label]="'Ajouter ' + pendingCount()"
              icon="pi pi-check"
              size="small"
              (clicked)="confirmBatch()"
            />
          </div>
        </div>
      }
    </app-offcanvas>
  `,
  styleUrl: './add-widget-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AddWidgetPanelComponent {
  readonly visible = model.required<boolean>();
  readonly entries = input.required<CatalogueEntry[]>();
  /** Émet le tableau des tuiles à ajouter (1 élément pour un ajout en double direct). */
  readonly add = output<WidgetDefinition[]>();

  protected readonly search = signal('');
  protected readonly toutAfficher = signal(false);
  protected readonly pendingKeys = signal<string[]>([]);

  private readonly pendingSet = computed(() => new Set(this.pendingKeys()));
  protected readonly pendingCount = computed(() => this.pendingKeys().length);

  constructor() {
    effect(() => {
      if (!this.visible()) untracked(() => this.pendingKeys.set([]));
    });
  }

  protected togglePending(key: string): void {
    const keys = this.pendingKeys();
    this.pendingKeys.set(keys.includes(key) ? keys.filter(k => k !== key) : [...keys, key]);
  }

  protected isPending(key: string): boolean {
    return this.pendingSet().has(key);
  }

  protected confirmBatch(): void {
    const defs = this.entries()
      .filter(e => this.pendingSet().has(e.definition.key))
      .map(e => e.definition);
    if (defs.length > 0) {
      this.add.emit(defs);
      this.pendingKeys.set([]);
    }
  }

  protected addDuplicate(definition: WidgetDefinition): void {
    this.add.emit([definition]);
  }

  protected clearPending(): void {
    this.pendingKeys.set([]);
  }

  private readonly filtrees = computed(() => {
    const term = normalize(this.search());
    return this.entries().filter(
      e => !term || normalize(e.definition.label).includes(term) || normalize(e.definition.description).includes(term),
    );
  });

  protected readonly emptyReason = computed<'no-entries' | 'all-added' | 'no-match'>(() => {
    if (this.entries().length === 0) return 'no-entries';
    const f = this.filtrees();
    if (f.length === 0) return 'no-match';
    if (!this.toutAfficher() && f.every(e => e.alreadyAdded)) return 'all-added';
    return 'no-match';
  });

  protected readonly groups = computed(() => {
    const candidates = this.toutAfficher() ? this.filtrees() : this.filtrees().filter(e => !e.alreadyAdded);
    const categories = Object.keys(WIDGET_CATEGORY_LABELS) as WidgetCategory[];
    return categories
      .map(category => ({
        category,
        label: WIDGET_CATEGORY_LABELS[category],
        entries: candidates.filter(e => e.definition.category === category),
      }))
      .filter(group => group.entries.length > 0);
  });
}

function normalize(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim();
}
