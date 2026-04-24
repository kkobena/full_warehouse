import { Component, computed, input, output } from '@angular/core';
import { Button } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { MenuModule } from 'primeng/menu';
import { MenuItem } from 'primeng/api';
import { FournisseurSuggestionSummary } from '../../data-access/suggestion-enrichie.model';

export type SuggestionFournisseurAction = 'valider' | 'commander' | 'exportPdf' | 'exportCsv' | 'supprimer';

@Component({
  selector: 'app-suggestion-fournisseur-actions',
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
export class SuggestionFournisseurActionsComponent {
  readonly fournisseur = input.required<FournisseurSuggestionSummary>();

  readonly menuAction = output<SuggestionFournisseurAction>();

  protected readonly menuItems = computed<MenuItem[]>(() => {
    const statut = this.fournisseur().statut;
    const items: MenuItem[] = [];

    if (statut !== 'VALIDEE') {
      items.push({
        label: 'Valider',
        icon: 'pi pi-check',
        command: () => this.menuAction.emit('valider')
      });
    }

    items.push({
      label: 'Commander',
      icon: 'pi pi-shopping-cart',
      command: () => this.menuAction.emit('commander')
    });
    items.push({ separator: true });
    items.push({
      label: 'Export PDF',
      icon: 'pi pi-file-pdf',
      command: () => this.menuAction.emit('exportPdf')
    });
    items.push({
      label: 'Export CSV',
      icon: 'pi pi-file-excel',
      command: () => this.menuAction.emit('exportCsv')
    });
    items.push({ separator: true });
    items.push({
      label: 'Supprimer',
      icon: 'pi pi-trash',
      command: () => this.menuAction.emit('supprimer')
    });

    return items;
  });

  protected openContextMenu(event: Event, menu: any): void {
    event.stopPropagation();
    menu.toggle(event);
  }
}
