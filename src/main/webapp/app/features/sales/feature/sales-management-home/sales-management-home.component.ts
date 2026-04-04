import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgbNav, NgbNavChangeEvent, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavOutlet } from '@ng-bootstrap/ng-bootstrap';
import { SaleToolbarService, SalesManagementTab } from '../../data-access/services/sale-toolbar.service';
import { SalesJournalComponent } from '../sales-journal/sales-journal.component';
import { SalesEnCoursComponent } from '../sales-en-cours/sales-en-cours.component';
import { PresaleListComponent } from '../presale-list/presale-list.component';
import { DevisListComponent } from '../devis-list/devis-list.component';
import { BreadcrumbService } from '../../../../shared/components/breadcrumb/breadcrumb.service';

const TAB_LABELS: Record<SalesManagementTab, string> = {
  'journal':   'Journal des ventes',
  'en-cours':  'Ventes en cours',
  'presales':  'Pré-ventes',
  'devis':     'Proformas',
};

@Component({
  selector: 'app-sales-management-home',
  templateUrl: './sales-management-home.component.html',
  styleUrl: './sales-management-home.component.scss',
  imports: [
    NgbNav,
    NgbNavItem,
    NgbNavLink,
    NgbNavContent,
    NgbNavOutlet,
    SalesJournalComponent,
    SalesEnCoursComponent,
    PresaleListComponent,
    DevisListComponent,
  ],
})
export class SalesManagementHomeComponent implements OnInit {
  private readonly toolbarService = inject(SaleToolbarService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  protected active = signal<SalesManagementTab>('journal');

  constructor() {
    inject(DestroyRef).onDestroy(() => this.breadcrumbService.clearTabCrumb());
  }

  ngOnInit(): void {
    const tab = this.toolbarService.params().activeTab;
    this.active.set(tab);
    this.breadcrumbService.setTabCrumb(TAB_LABELS[tab]);
  }

  protected onNavChange(evt: NgbNavChangeEvent): void {
    const tab = evt.nextId as SalesManagementTab;
    this.active.set(tab);
    this.toolbarService.update({ activeTab: tab });
    this.breadcrumbService.setTabCrumb(TAB_LABELS[tab] ?? tab);
  }
}
