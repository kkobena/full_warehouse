import {ChangeDetectionStrategy, Component, computed, input, output} from '@angular/core';
import {
  NgbDropdown,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
  NgbTooltip,
} from '@ng-bootstrap/ng-bootstrap';
import {ICommande} from 'app/shared/model/commande.model';
import {COMPARAISON_DISPONIBILITE_ACTIVE} from '../pharmaml/pharmaml.constants';

export type CommandeRequestedAction =
  | 'editer'
  | 'receptionner'
  | 'envoyerPharmaml'
  | 'comparerGrossistes'
  | 'exportCsv'
  | 'exportPdf'
  | 'supprimer';

interface MenuEntry {
  label: string;
  icon: string;
  action: CommandeRequestedAction;
  separatorBefore?: boolean;
  /** Entrée retirée du menu pour cette commande — ex. un envoi déjà transmis. */
  hidden?: (commande: ICommande) => boolean;
}

@Component({
  selector: 'app-commande-requested-actions',
  imports: [NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem, NgbTooltip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div ngbDropdown container="body" placement="bottom-end">
      <button
        type="button"
        class="btn btn-sm btn-link text-secondary app-actions-toggle"
        ngbDropdownToggle
        (click)="$event.stopPropagation()"
        ngbTooltip="Actions"
        placement="left"
        aria-label="Actions"
      >
        <i class="pi pi-ellipsis-v" aria-hidden="true"></i>
      </button>
      <div ngbDropdownMenu>
        @for (item of visibleItems(); track item.action) {
          @if (item.separatorBefore) {
            <div class="dropdown-divider"></div>
          }
          <button type="button" ngbDropdownItem (click)="menuAction.emit(item.action)">
            <i [class]="item.icon" aria-hidden="true"></i> {{ item.label }}
          </button>
        }
      </div>
    </div>
  `,
  styles: `
    // L'icône ellipsis suffit à signaler un menu — pas besoin du chevron que
    // \`ngbDropdownToggle\` ajoute via \`.dropdown-toggle::after\`.
    .app-actions-toggle::after {
      display: none;
    }
  `,
})
export class CommandeRequestedActionsComponent {
  readonly commande = input.required<ICommande>();

  readonly menuAction = output<CommandeRequestedAction>();

  private readonly menuItems: MenuEntry[] = [
    {label: 'Éditer', icon: 'pi pi-pencil', action: 'editer'},
    {label: 'Réceptionner', icon: 'pi pi-inbox', action: 'receptionner'},

    {
      label: 'Envoyer via PharmaML',
      icon: 'pi pi-send',
      action: 'envoyerPharmaml',
      separatorBefore: true,
      hidden: c => !!c.hasBeenSubmittedToPharmaML,
    },

    {
      label: 'Comparer multi-grossistes',
      icon: 'pi pi-chart-bar',
      action: 'comparerGrossistes',
      hidden: () => !COMPARAISON_DISPONIBILITE_ACTIVE,
    },
    {label: 'Export CSV', icon: 'pi pi-file-excel', action: 'exportCsv', separatorBefore: true},
    {label: 'Imprimer PDF', icon: 'pi pi-print', action: 'exportPdf'},
    {label: 'Supprimer', icon: 'pi pi-trash', action: 'supprimer', separatorBefore: true},
  ];

  protected readonly visibleItems = computed(() =>
    this.menuItems.filter(item => !item.hidden?.(this.commande())),
  );
}
