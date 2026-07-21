import {Component, computed, input, output} from '@angular/core';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle
} from '@ng-bootstrap/ng-bootstrap';

import {AppButtonSeverity, AppButtonSize} from '../button/button.component';


export interface AppSplitButtonItem {
  label: string;
  icon?: string;
  disabled?: boolean;
  /** Trace une séparation au-dessus de l'entrée. */
  separatorBefore?: boolean;
  command?: () => void;
}

/**

 * Une action principale, plus un menu déroulant (`NgbDropdown` dans un `.btn-group`).
 *
 * @example
 * <app-split-button
 *   label="Enregistrer"
 *   icon="pi pi-save"
 *   [items]="[
 *     { label: 'Enregistrer et fermer', icon: 'pi pi-check', command: saveAndClose },
 *     { label: 'Supprimer', icon: 'pi pi-trash', separatorBefore: true, command: remove },
 *   ]"
 *   (clicked)="save()"
 * />
 */
@Component({
  selector: 'app-split-button',
  imports: [NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem, NgbDropdownButtonItem],
  template: `
    <div [class]="groupClasses()" ngbDropdown [placement]="placement()">
      <button type="button" [class]="mainButtonClasses()" [disabled]="disabled()"
              (click)="clicked.emit()">
        @if (icon()) {
          <i [class]="icon()" aria-hidden="true"></i>
        }
        @if (label()) {
          <span>{{ label() }}</span>
        }
      </button>

      <button
        type="button"
        [class]="toggleClasses()"
        ngbDropdownToggle
        [disabled]="disabled()"
        [attr.aria-label]="menuAriaLabel()"
      ></button>

      <div ngbDropdownMenu>
        @for (item of items(); track item.label) {
          @if (item.separatorBefore) {
            <div class="dropdown-divider"></div>
          }
          <button type="button" ngbDropdownItem [disabled]="item.disabled ?? false"
                  (click)="runCommand(item)">
            @if (item.icon) {
              <i [class]="item.icon" aria-hidden="true"></i>
            }
            {{ item.label }}
          </button>
        }
      </div>
    </div>
  `,
  styles: `
    // Le thème Bootswatch "yeti" colore explicitement le menu déroulant d'après la
    // severity du bouton (_bootswatch.scss : « .btn-group .dropdown-toggle.btn-info
    // ~ .dropdown-menu { background-color: #0ea5e9; ... .dropdown-item { color: #fff } } »,
    // une règle par variante). Sa spécificité (4 classes) bat les custom properties
    // Bootstrap normales : il faut une sélecteur au moins aussi précis, pas juste
    // réassigner --bs-dropdown-*. On neutralise donc explicitement pour les 8 variantes
    // (y compris help/contrast, propres à ce Design System), quelle que soit la severity.
    .btn-group .dropdown-toggle ~ .dropdown-menu {
      background-color: var(--bs-body-bg) !important;
      border-color: var(--bs-border-color) !important;

      .dropdown-item {
        color: var(--bs-body-color) !important;
      }

      .dropdown-item:hover,
      .dropdown-item:focus {
        background-color: var(--bs-tertiary-bg) !important;
        color: var(--bs-body-color) !important;
      }
    }

    [ngbDropdownItem] {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    // Voir ButtonComponent : Aura laisse le bouton hériter du line-height ambiant,
    // Bootstrap impose 1.5 et gonfle la boîte de texte.
    .btn {
      --bs-btn-line-height: inherit;
    }


    //
    // Portée par le .btn-group et non par chaque bouton : deux ombres adjacentes se
    // superposeraient à la jointure entre l'action principale et le chevron, laissant
    // une couture visible.
    .app-btn-raised {
      box-shadow: 0 3px 1px -2px rgba(0, 0, 0, 0.2),
      0 2px 2px 0 rgba(0, 0, 0, 0.14),
      0 1px 5px 0 rgba(0, 0, 0, 0.12);
    }


    // aient exactement la même largeur côte à côte dans une barre d'actions.
    .app-btn-normal {
      font-size: inherit;
      --bs-btn-padding-x: 0.625rem;
    }
  `,
})
export class SplitButtonComponent {
  private static readonly BOOTSTRAP_VARIANT: Record<AppButtonSeverity, string> = {
    primary: 'primary',
    secondary: 'secondary',
    success: 'success',
    info: 'info',
    warn: 'warning',
    danger: 'danger',
    help: 'help',
    contrast: 'contrast',
  };
  readonly label = input<string>('');
  readonly icon = input<string>('');
  readonly items = input.required<readonly AppSplitButtonItem[]>();
  readonly severity = input<AppButtonSeverity>('primary');
  readonly size = input<AppButtonSize>('normal');
  readonly outlined = input<boolean>(false);
  readonly disabled = input<boolean>(false);
  readonly placement = input<string>('bottom-end');
  readonly menuAriaLabel = input<string>('Autres actions');
  /** Ombre portée, calquée sur l'élévation de `p-splitbutton`. */
  readonly raised = input<boolean>(false);
  /** Clic sur l'action principale. Les entrées du menu passent par leur `command`. */
  readonly clicked = output<void>();
  protected readonly groupClasses = computed(() => ['btn-group', this.raised() ? 'app-btn-raised' : ''].filter(Boolean).join(' '));

  // `app-btn-normal` : voir ButtonComponent. Aura ne fixe pas de taille de police
  private readonly variantClass = computed(() => {
    const variant = SplitButtonComponent.BOOTSTRAP_VARIANT[this.severity()];
    return this.outlined() ? `btn-outline-${variant}` : `btn-${variant}`;
  });
  // sur le bouton par défaut, Bootstrap impose 1rem — d'où des boutons trop grands.
  private readonly sizeClass = computed(() => {
    if (this.size() === 'small') {
      return 'btn-sm';
    }
    if (this.size() === 'large') {
      return 'btn-lg';
    }
    return 'app-btn-normal';
  });
  protected readonly mainButtonClasses = computed(() =>
    ['btn', this.variantClass(), this.sizeClass(), 'd-inline-flex', 'align-items-center', 'gap-2'].filter(Boolean).join(' '),
  );


  protected readonly toggleClasses = computed(() =>
    ['btn', this.variantClass(), this.sizeClass(), 'dropdown-toggle-split'].filter(Boolean).join(' '),
  );

  protected runCommand(item: AppSplitButtonItem): void {
    item.command?.();
  }
}
