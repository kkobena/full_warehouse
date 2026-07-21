import { Component, computed, input, output, ChangeDetectionStrategy } from '@angular/core';
import {
  NgbDropdown,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
  NgbTooltip,
} from '@ng-bootstrap/ng-bootstrap';
import { IDelivery } from '../../../../shared/model/delevery.model';

export type BonAction = 'voirDetail' | 'receive' | 'cancel' | 'exportPdf' | 'printEtiquette' | 'retourComplet' | 'retourParLigne' | 'reconcilierFacture';

interface MenuEntry {
  label?: string;
  icon?: string;
  action?: BonAction;
  separator?: boolean;
}

@Component({
  selector: 'app-list-bons-actions',
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
        @for (item of menuItems(); track $index) {
          @if (item.separator) {
            <div class="dropdown-divider"></div>
          } @else {
            <button type="button" ngbDropdownItem (click)="menuAction.emit(item.action!)">
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
export class ListBonsActionsComponent {
  readonly delivery = input.required<IDelivery>();

  readonly menuAction = output<BonAction>();

  protected readonly menuItems = computed<MenuEntry[]>(() => {
    const d = this.delivery();
    const received = d.orderStatus === 'RECEIVED' || (d as any).statut === 'RECEIVED';
    const items: MenuEntry[] = [];

    if (received) {
      items.push({ label: 'Saisir la réception', icon: 'pi pi-inbox', action: 'receive' });
      items.push({ separator: true });
      items.push({ label: 'Annuler ce bon', icon: 'pi pi-times', action: 'cancel' });
      items.push({ separator: true });
    }

    if (!received) {
      items.push({ label: 'Voir le détail', icon: 'pi pi-eye', action: 'voirDetail' });
      items.push({ separator: true });
    }

    items.push({ label: 'Imprimer BL', icon: 'pi pi-file-pdf', action: 'exportPdf' });

    if (!received) {
      items.push({ label: 'Étiquettes', icon: 'pi pi-print', action: 'printEtiquette' });
      items.push({ separator: true });
      items.push({ label: 'Retour complet', icon: 'pi pi-replay', action: 'retourComplet' });
      items.push({ label: 'Retour par ligne', icon: 'pi pi-list-check', action: 'retourParLigne' });
      items.push({ separator: true });
      items.push({
        label: d.reconciliationStatut === 'RECONCILIEE' ? 'Modifier la réconciliation' : 'Rapprocher la facture',
        icon: d.reconciliationStatut === 'RECONCILIEE' ? 'pi pi-pencil' : 'pi pi-file-check',
        action: 'reconcilierFacture',
      });
    }

    return items;
  });
}
