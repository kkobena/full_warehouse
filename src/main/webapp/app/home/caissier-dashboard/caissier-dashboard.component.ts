import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {HttpResponse} from '@angular/common/http';
import {Router} from '@angular/router';

import {TableModule} from 'primeng/table';
import {ChartModule} from 'primeng/chart';
import {ButtonModule} from 'primeng/button';
import {CardModule} from 'primeng/card';
import {BadgeModule} from 'primeng/badge';
import {TooltipModule} from 'primeng/tooltip';
import {ProgressBarModule} from 'primeng/progressbar';
import {TagModule} from 'primeng/tag';
import {ToastModule} from 'primeng/toast';
import {MessageService} from 'primeng/api';

import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {
  faCashRegister,
  faChartLine,
  faClock,
  faExclamationTriangle,
  faFileExport,
  faMoneyBillWave,
  faPrint,
  faShoppingCart,
  faStar,
  faSync,
  faUsers,
} from '@fortawesome/free-solid-svg-icons';

import {CaissierDashboardService} from './caissier-dashboard.service';
import {
  IAlerteCaisse,
  ICaisseStatus,
  ICaissierDashboard,
  IPerformanceVendeur,
  IStatistiquesRapides,
  ITopProduit,
  IVenteRecente,
  IVentesJour,
} from './caissier-dashboard.model';

@Component({
  selector: 'jhi-caissier-dashboard',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ChartModule,
    ButtonModule,
    CardModule,
    BadgeModule,
    TooltipModule,
    ProgressBarModule,
    TagModule,
    FontAwesomeModule,
    ToastModule,
  ],
  providers: [MessageService],
  templateUrl: './caissier-dashboard.component.html',
  styleUrls: ['./caissier-dashboard.component.scss'],
})
export class CaissierDashboardComponent implements OnInit {
  // Font Awesome icons
  faCashRegister = faCashRegister;
  faMoneyBillWave = faMoneyBillWave;
  faChartLine = faChartLine;
  faSync = faSync;
  faFileExport = faFileExport;
  faShoppingCart = faShoppingCart;
  faUsers = faUsers;
  faClock = faClock;
  faExclamationTriangle = faExclamationTriangle;
  faStar = faStar;
  faPrint = faPrint;
  // Signals pour les données
  protected ventesJour = signal<IVentesJour | null>(null);
  protected caisseStatus = signal<ICaisseStatus | null>(null);
  protected statistiquesRapides = signal<IStatistiquesRapides | null>(null);
  protected ventesRecentes = signal<IVenteRecente[]>([]);
  protected topProduits = signal<ITopProduit[]>([]);
  protected performanceVendeurs = signal<IPerformanceVendeur[]>([]);
  protected alertes = signal<IAlerteCaisse[]>([]);
  protected isLoading = signal<boolean>(false);
  protected lastRefresh = signal<Date | null>(null);
  // Computed signals
  protected totalAlertes = computed(() => {
    return this.alertes().filter(a => a.type === 'URGENT' || a.type === 'ATTENTION').length;
  });
  protected isCaisseOuverte = computed(() => {
    return this.caisseStatus()?.etat === 'OUVERTE';
  });
  protected objectifAtteint = computed(() => {
    const ventes = this.ventesJour();
    if (!ventes?.objectifJour) {
      return false;
    }
    return (ventes.tauxAtteinte || 0) >= 100;
  });
  // Chart data
  protected repartitionPaiementsData = computed(() => {
    const ventes = this.ventesJour();
    if (!ventes) {
      return null;
    }

    return {
      labels: ['Espèces', 'CB', 'Chèque', 'Mobile Money', 'Virement', 'Assurance'],
      datasets: [
        {
          data: [
            ventes.montantEspeces,
            ventes.montantCB,
            ventes.montantCheque,
            ventes.montantMobileMoney,
            ventes.montantVirement,
            ventes.montantAssurance,
          ],
          backgroundColor: ['#10b981', '#3b82f6', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4'],
          borderColor: '#1e293b',
          borderWidth: 2,
        },
      ],
    };
  });
  protected chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: '#cbd5e1',
          font: {size: 12},
        },
      },
    },
  };
  // Services
  private dashboardService = inject(CaissierDashboardService);
  private messageService = inject(MessageService);
  private router = inject(Router);

  ngOnInit(): void {
    this.loadDashboardData();
  }

  protected loadDashboardData(): void {
    this.isLoading.set(true);

    this.dashboardService.getDashboardData().subscribe({
      next: (res: HttpResponse<ICaissierDashboard>) => {
        const data = res.body;
        if (data) {
          this.ventesJour.set(data.ventesJour);
          this.caisseStatus.set(data.caisseStatus);
          this.statistiquesRapides.set(data.statistiquesRapides);
          if (data.ventesRecentes) {
            this.ventesRecentes.set(data.ventesRecentes);
          }
          if (data.topProduits) {
            this.topProduits.set(data.topProduits);
          }
          if (data.performanceVendeurs) {
            this.performanceVendeurs.set(data.performanceVendeurs);
          }
          if (data.alertes) {
            this.alertes.set(data.alertes);
          }
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

  protected getStarArray(note: number): number[] {
    return Array(5)
      .fill(0)
      .map((_, i) => (i < note ? 1 : 0));
  }

  protected formatCurrency(value: number): string {
    return new Intl.NumberFormat('fr-FR', {style: 'currency', currency: 'XOF'}).format(value);
  }

  protected formatNumber(value: number): string {
    return new Intl.NumberFormat('fr-FR').format(value);
  }

  // Quick Actions Methods
  protected nouvelleVente(): void {
    this.router.navigate(['/sales-home']);
  }

  protected ouvrirCaisse(): void {
    // my-cash-register
    this.router.navigate(['/my-cash-register']);
  }

  protected imprimerRapport(): void {
    this.dashboardService.imprimerRapportCaisse().subscribe({
      next: res => {
        if (res.body) {
          const blob = new Blob([res.body], {type: 'application/pdf'});
          const url = window.URL.createObjectURL(blob);
          window.open(url);
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Rapport imprimé avec succès',
          });
        }
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: "Erreur lors de l'impression du rapport",
        });
      },
    });
  }

  protected consulterVentes(): void {
    this.router.navigate(['/sales']);
  }

  protected gererAlertes(): void {
    this.router.navigate(['/alertes']);
  }

  protected voirDetailVente(vente: IVenteRecente): void {
    this.router.navigate(['/sales', vente.saleId]);
  }
}
