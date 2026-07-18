import { Component, computed, input, output, ChangeDetectionStrategy } from '@angular/core';
import { Button } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { MenuModule } from 'primeng/menu';
import { MenuItem } from 'primeng/api';
import { ICommande } from 'app/shared/model/commande.model';

export type CommandeRequestedAction = 'editer' | 'receptionner' | 'exportCsv' | 'exportPdf' | 'supprimer';

@Component({
  selector: 'app-commande-requested-actions',
  imports: [Button, TooltipModule, MenuModule],
  changeDetection: ChangeDetectionStrategy.Eager,
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
export class CommandeRequestedActionsComponent {
  readonly commande = input.required<ICommande>();

  readonly menuAction = output<CommandeRequestedAction>();

  protected readonly menuItems = computed<MenuItem[]>(() => [
    {
      label: 'Éditer',
      icon: 'pi pi-pencil',
      command: () => this.menuAction.emit('editer')
    },
    {
      label: 'Réceptionner',
      icon: 'pi pi-inbox',
      command: () => this.menuAction.emit('receptionner')
    },
    { separator: true },
    {
      label: 'Export CSV',
      icon: 'pi pi-file-excel',
      command: () => this.menuAction.emit('exportCsv')
    },
    {
      label: 'Imprimer PDF',
      icon: 'pi pi-print',
      command: () => this.menuAction.emit('exportPdf')
    },
    { separator: true },
    {
      label: 'Supprimer',
      icon: 'pi pi-trash',
      command: () => this.menuAction.emit('supprimer')
    },
  ]);

  protected openContextMenu(event: Event, menu: any): void {
    event.stopPropagation();
    menu.toggle(event);
  }
}
