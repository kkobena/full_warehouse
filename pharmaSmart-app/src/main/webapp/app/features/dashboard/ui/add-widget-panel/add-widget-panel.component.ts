import { ChangeDetectionStrategy, Component, computed, input, model, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BadgeComponent, ButtonComponent, InputComponent, OffcanvasComponent } from 'app/shared/ui';
import { WIDGET_CATEGORY_LABELS, WidgetCategory, WidgetDefinition } from '../../models/dashboard.model';

export interface CatalogueEntry {
  definition: WidgetDefinition;
  licensed: boolean;
  alreadyAdded: boolean;
}

/**
 * Panneau latéral « Ajouter un widget ». Ne reçoit que les widgets autorisés au rôle : un widget
 * interdit n'y figure pas. Un widget hors licence y figure, grisé, pour signaler qu'il existe.
 */
@Component({
  selector: 'app-add-widget-panel',
  imports: [FormsModule, OffcanvasComponent, InputComponent, ButtonComponent, BadgeComponent],
  template: `
    <app-offcanvas [visible]="visible()" (visibleChange)="visible.set($event)" width="420px">
      <div appOffcanvasHeader class="d-flex align-items-center gap-2">
        <i class="pi pi-plus-circle text-primary" aria-hidden="true"></i>
        <span class="fw-bold fs-5">Ajouter un widget</span>
      </div>

      <app-input
        class="d-block mb-3"
        type="search"
        placeholder="Rechercher un widget…"
        ariaLabel="Rechercher un widget"
        [ngModel]="search()"
        (ngModelChange)="search.set($event ?? '')"
      />

      @for (group of groups(); track group.category) {
        <h3 class="catalogue-category">{{ group.label }}</h3>
        <ul class="list-unstyled mb-3">
          @for (entry of group.entries; track entry.definition.key) {
            <li class="catalogue-entry" [class.is-locked]="!entry.licensed">
              <i [class]="entry.definition.icon + ' catalogue-icon'" aria-hidden="true"></i>
              <div class="flex-fill">
                <div class="fw-semibold">
                  {{ entry.definition.label }}
                  @if (entry.alreadyAdded) {
                    <app-badge class="ms-1" label="déjà ajouté" severity="secondary" />
                  }
                </div>
                <div class="small text-body-secondary">
                  {{ entry.licensed ? entry.definition.description : 'Non inclus dans votre abonnement' }}
                </div>
              </div>
              @if (entry.licensed) {
                <app-button label="Ajouter" icon="pi pi-plus" size="small" [outlined]="true" (clicked)="add.emit(entry.definition)" />
              } @else {
                <i class="pi pi-lock text-body-secondary" aria-label="Non inclus dans votre abonnement"></i>
              }
            </li>
          }
        </ul>
      } @empty {
        <p class="text-body-secondary text-center py-4">
          {{ entries().length === 0 ? 'Aucun widget ne vous est autorisé.' : 'Aucun widget ne correspond à la recherche.' }}
        </p>
      }
    </app-offcanvas>
  `,
  styles: `
    .catalogue-category {
      font-size: 0.8rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: var(--bs-secondary-color);
      margin-bottom: 0.5rem;
    }
    .catalogue-entry {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.6rem 0.5rem;
      border-bottom: 1px solid var(--bs-border-color-translucent);
    }
    .catalogue-entry.is-locked {
      opacity: 0.6;
    }
    .catalogue-icon {
      font-size: 1.25rem;
      width: 1.5rem;
      text-align: center;
      color: var(--bs-primary);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AddWidgetPanelComponent {
  readonly visible = model.required<boolean>();
  readonly entries = input.required<CatalogueEntry[]>();
  readonly add = output<WidgetDefinition>();

  protected readonly search = signal('');

  protected readonly groups = computed(() => {
    const term = normalize(this.search());
    const matching = this.entries().filter(
      e => !term || normalize(e.definition.label).includes(term) || normalize(e.definition.description).includes(term),
    );
    const categories = Object.keys(WIDGET_CATEGORY_LABELS) as WidgetCategory[];
    return categories
      .map(category => ({
        category,
        label: WIDGET_CATEGORY_LABELS[category],
        entries: matching.filter(e => e.definition.category === category),
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
