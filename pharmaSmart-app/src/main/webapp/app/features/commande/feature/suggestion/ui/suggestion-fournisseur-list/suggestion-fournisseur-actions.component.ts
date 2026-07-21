import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import {
  NgbDropdown,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
  NgbTooltip,
} from '@ng-bootstrap/ng-bootstrap';
import { FournisseurSuggestionSummary } from '../../data-access/suggestion-enrichie.model';

export type SuggestionFournisseurAction = 'editer' | 'valider' | 'commander' | 'exportPdf' | 'exportCsv' | 'supprimer';

interface MenuEntry {
  label: string;
  icon: string;
  action: SuggestionFournisseurAction;
  separatorBefore?: boolean | ((fournisseur: FournisseurSuggestionSummary) => boolean);
  hidden?: (fournisseur: FournisseurSuggestionSummary) => boolean;
}

@Component({
  selector: 'app-suggestion-fournisseur-actions',
  imports: [NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem, NgbTooltip],
  changeDetection: ChangeDetectionStrategy.Eager,
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
        @for (item of menuItems; track item.action) {
          @if (!item.hidden || !item.hidden(fournisseur())) {
            @if (item.separatorBefore === true || (item.separatorBefore && item.separatorBefore(fournisseur()))) {
              <div class="dropdown-divider"></div>
            }
            <button type="button" ngbDropdownItem (click)="menuAction.emit(item.action)">
              <i [class]="item.icon" aria-hidden="true"></i> {{ item.label }}
            </button>
          }
        }
      </div>
    </div>
  `,
  styles: `
    .app-actions-toggle::after {
      display: none;
    }
  `,
})
export class SuggestionFournisseurActionsComponent {
  readonly fournisseur = input.required<FournisseurSuggestionSummary>();

  readonly menuAction = output<SuggestionFournisseurAction>();

  protected readonly menuItems: MenuEntry[] = [
    { label: 'Éditer', icon: 'pi pi-pencil', action: 'editer' },
    { label: 'Valider', icon: 'pi pi-check', action: 'valider', separatorBefore: true, hidden: f => f.statut === 'VALIDEE' },
    { label: 'Commander', icon: 'pi pi-shopping-cart', action: 'commander', separatorBefore: f => f.statut === 'VALIDEE' },
    { label: 'Export PDF', icon: 'pi pi-file-pdf', action: 'exportPdf', separatorBefore: true },
    { label: 'Export CSV', icon: 'pi pi-file-excel', action: 'exportCsv' },
    { label: 'Supprimer', icon: 'pi pi-trash', action: 'supprimer', separatorBefore: true },
  ];
}
