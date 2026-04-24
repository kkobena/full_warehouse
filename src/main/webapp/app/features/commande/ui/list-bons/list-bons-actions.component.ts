import { Component, computed, input, output } from '@angular/core';
import { Button } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { MenuModule } from 'primeng/menu';
import { MenuItem } from 'primeng/api';
import { IDelivery } from '../../../../shared/model/delevery.model';

export type BonAction = 'voirDetail' | 'receive' | 'cancel' | 'exportPdf' | 'printEtiquette' | 'retourComplet' | 'retourParLigne';

@Component({
  selector: 'app-list-bons-actions',
  imports: [Button, TooltipModule, MenuModule],
  template: `
    <p-menu #rowMenu [popup]="true" [model]="menuItems()" appendTo="body" />
    <p-button
      icon="pi pi-ellipsis-v"
      [text]="true"
      size="small"
      severity="secondary"
      pTooltip="Actions"
      tooltipPosition="left"
      (onClick)="openContextMenu($event, rowMenu)"
    />
  `,
})
export class ListBonsActionsComponent {
  readonly delivery = input.required<IDelivery>();

  readonly menuAction = output<BonAction>();

  protected readonly menuItems = computed<MenuItem[]>(() => {
    const d = this.delivery();
    const received = d.orderStatus === 'RECEIVED' || (d as any).statut === 'RECEIVED';
    const items: MenuItem[] = [];

    if (received) {
      items.push({
        label: 'Saisir la réception',
        icon: 'pi pi-inbox',
        command: () => this.menuAction.emit('receive')
      });
      items.push({ separator: true });
      items.push({
        label: 'Annuler ce bon',
        icon: 'pi pi-times',
        command: () => this.menuAction.emit('cancel')
      });
      items.push({ separator: true });
    }

    if (!received) {
      items.push({
        label: 'Voir le détail',
        icon: 'pi pi-eye',
        command: () => this.menuAction.emit('voirDetail')
      });
      items.push({ separator: true });
    }

    items.push({
      label: 'Imprimer BL',
      icon: 'pi pi-file-pdf',
      command: () => this.menuAction.emit('exportPdf')
    });

    if (!received) {
      items.push({
        label: 'Étiquettes',
        icon: 'pi pi-print',
        command: () => this.menuAction.emit('printEtiquette')
      });
      items.push({ separator: true });
      items.push({
        label: 'Retour complet',
        icon: 'pi pi-replay',
        command: () => this.menuAction.emit('retourComplet')
      });
      items.push({
        label: 'Retour par ligne',
        icon: 'pi pi-list-check',
        command: () => this.menuAction.emit('retourParLigne')
      });
    }

    return items;
  });

  protected openContextMenu(event: Event, menu: any): void {
    event.stopPropagation();
    menu.toggle(event);
  }
}
