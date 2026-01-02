import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';

// PrimeNG imports
import { TableModule } from 'primeng/table';
import { ChartModule } from 'primeng/chart';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { BadgeModule } from 'primeng/badge';
import { TooltipModule } from 'primeng/tooltip';
import { CheckboxModule } from 'primeng/checkbox';
import { ProgressBarModule } from 'primeng/progressbar';
import { TagModule } from 'primeng/tag';
import { SplitButtonModule } from 'primeng/splitbutton';
import { ToastModule } from 'primeng/toast';
import { MenuItem, MessageService } from 'primeng/api';

// Font Awesome
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faExclamationTriangle,
  faBoxes,
  faClock,
  faSync,
  faFileExport,
  faChartBar,
  faClipboardList,
  faWarehouse,
  faBell,
  faTruck,
  faStar,
} from '@fortawesome/free-solid-svg-icons';

import { ResponsableCommandeDashboardService } from './responsable-commande-dashboard.service';
import {
  IResponsableCommandeDashboard,
  ISuggestionReappro,
  ICommandeAReceptionner,
  IAnalyseABC,
  IPerformanceFournisseur,
  IAlertNotification,
  IStockAlerts,
  ICommandesEnCours,
  IPeremptions,
  IRotationStock,
} from './responsable-commande-dashboard.model';

@Component({
  selector: 'jhi-responsable-commande-dashboard',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ChartModule,
    ButtonModule,
    CardModule,
    BadgeModule,
    TooltipModule,
    CheckboxModule,
    ProgressBarModule,
    TagModule,
    SplitButtonModule,
    FontAwesomeModule,
    ToastModule,
  ],
  providers: [MessageService],
  templateUrl: './responsable-commande-dashboard.component.html',
  styleUrls: ['./responsable-commande-dashboard.component.scss'],
})
export class ResponsableCommandeDashboardComponent implements OnInit {
  // Services
  private dashboardService = inject(ResponsableCommandeDashboardService);
  private messageService = inject(MessageService);
  private router = inject(Router);

  // Font Awesome icons
  faExclamationTriangle = faExclamationTriangle;
  faBoxes = faBoxes;
  faClock = faClock;
  faSync = faSync;
  faFileExport = faFileExport;
  faChartBar = faChartBar;
  faClipboardList = faClipboardList;
  faWarehouse = faWarehouse;
  faBell = faBell;
  faTruck = faTruck;
  faStar = faStar;

  // Signals pour les données
  protected stockAlerts = signal<IStockAlerts | null>(null);
  protected commandesEnCours = signal<ICommandesEnCours | null>(null);
  protected peremptions = signal<IPeremptions | null>(null);
  protected rotationStock = signal<IRotationStock | null>(null);
  protected suggestions = signal<ISuggestionReappro[]>([]);
  protected commandesAReceptionner = signal<ICommandeAReceptionner[]>([]);
  protected analyseABC = signal<IAnalyseABC | null>(null);
  protected performanceFournisseurs = signal<IPerformanceFournisseur[]>([]);
  protected notifications = signal<IAlertNotification[]>([]);
  protected isLoading = signal<boolean>(false);
  protected lastRefresh = signal<Date | null>(null);

  // Computed signals
  protected totalAlertes = computed(() => {
    const alerts = this.stockAlerts();
    if (!alerts) return 0;
    return alerts.rupture + alerts.stockCritique + alerts.bientotEnRupture + alerts.reassortStockRayon;
  });

  protected selectedSuggestions = computed(() => {
    return this.suggestions().filter(s => s.selected);
  });

  protected totalMontantSuggestions = computed(() => {
    return this.selectedSuggestions().reduce((sum, s) => sum + (s.prixUnitaire || 0) * s.quantiteSuggeree, 0);
  });

  // Quick Actions Menu Items
  protected quickActions = signal<MenuItem[]>([
    {
      label: 'Nouvelle Commande',
      icon: 'pi pi-file-edit',
      command: () => this.nouvelleCommande(),
    },
    {
      label: 'Analyser Stock',
      icon: 'pi pi-search',
      command: () => this.analyserStock(),
    },
    {
      label: 'Inventaire',
      icon: 'pi pi-clipboard',
      command: () => this.lancerInventaire(),
    },
    {
      label: 'Gérer Alertes',
      icon: 'pi pi-exclamation-triangle',
      command: () => this.gererAlertes(),
    },
  ]);

  ngOnInit(): void {
    this.loadDashboardData();
  }

  protected loadDashboardData(): void {
    this.isLoading.set(true);

    this.dashboardService.getDashboardData().subscribe({
      next: (res: HttpResponse<IResponsableCommandeDashboard>) => {
        const data = res.body;
        if (data) {
          this.stockAlerts.set(data.stockAlerts);
          this.commandesEnCours.set(data.commandesEnCours);
          this.peremptions.set(data.peremptions);
          this.rotationStock.set(data.rotationStock);
          if (data.suggestions) this.suggestions.set(data.suggestions);
          if (data.commandesAReceptionner) this.commandesAReceptionner.set(data.commandesAReceptionner);
          if (data.analyseABC) this.analyseABC.set(data.analyseABC);
          if (data.performanceFournisseurs) this.performanceFournisseurs.set(data.performanceFournisseurs);
          if (data.notifications) this.notifications.set(data.notifications);
        }
        this.lastRefresh.set(new Date());
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors du chargement des données du tableau de bord',
        });
      },
    });
  }

  protected refreshDashboard(): void {
    this.loadDashboardData();
  }

  protected toggleSuggestion(suggestion: ISuggestionReappro): void {
    const updatedSuggestions = this.suggestions().map(s => (s.produitId === suggestion.produitId ? { ...s, selected: !s.selected } : s));
    this.suggestions.set(updatedSuggestions);
  }

  protected toggleAllSuggestions(): void {
    const allSelected = this.suggestions().every(s => s.selected);
    const updatedSuggestions = this.suggestions().map(s => ({ ...s, selected: !allSelected }));
    this.suggestions.set(updatedSuggestions);
  }

  protected genererCommande(): void {
    const selected = this.selectedSuggestions();
    if (selected.length === 0) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'Veuillez sélectionner au moins un produit',
      });
      return;
    }

    this.dashboardService.genererCommandeFromSuggestions(selected).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Commande générée avec succès',
        });
        this.loadDashboardData();
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors de la génération de la commande',
        });
      },
    });
  }

  protected getRotationColor(taux: number): string {
    if (taux >= 4) return 'success';
    if (taux >= 2) return 'warning';
    return 'danger';
  }

  protected getStarArray(note: number): number[] {
    return Array(5)
      .fill(0)
      .map((_, i) => (i < note ? 1 : 0));
  }

  protected getSeverityByType(type: string): 'success' | 'info' | 'warn' | 'danger' {
    switch (type) {
      case 'OK':
        return 'success';
      case 'INFO':
        return 'info';
      case 'ATTENTION':
        return 'warn';
      case 'URGENT':
        return 'danger';
      default:
        return 'info';
    }
  }

  // Quick Actions Methods
  protected nouvelleCommande(): void {
    this.router.navigate(['/commande/new']);
  }

  protected analyserStock(): void {
    this.router.navigate(['/stock/analyse']);
  }

  protected lancerInventaire(): void {
    this.router.navigate(['/inventaire']);
  }

  protected gererAlertes(): void {
    this.router.navigate(['/alertes']);
  }

  protected exporterRapport(): void {
    this.messageService.add({
      severity: 'info',
      summary: 'Export',
      detail: 'Génération du rapport en cours...',
    });
    // TODO: Export dashboard report
    // this.dashboardService.exportRapport().subscribe({
    //   next: (res) => {
    //     if (res.body) {
    //       const blob = new Blob([res.body], { type: 'application/pdf' });
    //       const url = window.URL.createObjectURL(blob);
    //       window.open(url);
    //       this.messageService.add({
    //         severity: 'success',
    //         summary: 'Succès',
    //         detail: 'Rapport exporté avec succès',
    //       });
    //     }
    //   },
    //   error: () => {
    //     this.messageService.add({
    //       severity: 'error',
    //       summary: 'Erreur',
    //       detail: 'Erreur lors de l\'export du rapport',
    //     });
    //   },
    // });
  }
}
