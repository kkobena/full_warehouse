import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { interval, Subject } from 'rxjs';
import { takeUntil, switchMap, startWith } from 'rxjs/operators';

import { VendeurDashboardService } from './vendeur-dashboard.service';
import {
  IVendeurDashboard,
  IMesPerformances,
  IMesClients,
  IVentesParType,
  ICommission,
  ITopProduitVendeur,
  IVenteRecenteVendeur,
  IOpportuniteVente,
  IObjectifMensuel,
  IClientFidele,
} from './vendeur-dashboard.model';

import { ButtonModule } from 'primeng/button';
import { Card } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ChartModule } from 'primeng/chart';
import { ProgressBarModule } from 'primeng/progressbar';
import { Tag } from 'primeng/tag';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';
import { Tooltip } from 'primeng/tooltip';
import { Toast } from 'primeng/toast';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'jhi-vendeur-dashboard',
  providers: [MessageService],
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule,
    TableModule,
    ChartModule,
    ProgressBarModule,
    Tag,
    WarehouseCommonModule,
    SpinnerComponent,
    Tooltip,
    Toast,
  ],
  templateUrl: './vendeur-dashboard.component.html',
  styleUrl: './vendeur-dashboard.component.scss',
})
export class VendeurDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // Signals for reactive data
  protected mesPerformances = signal<IMesPerformances | null>(null);
  protected mesClients = signal<IMesClients | null>(null);
  protected ventesParType = signal<IVentesParType | null>(null);
  protected commission = signal<ICommission | null>(null);
  protected topProduits = signal<ITopProduitVendeur[]>([]);
  protected ventesRecentes = signal<IVenteRecenteVendeur[]>([]);
  protected opportunites = signal<IOpportuniteVente[]>([]);
  protected objectifsMensuels = signal<IObjectifMensuel[]>([]);
  protected clientsFideles = signal<IClientFidele[]>([]);
  protected isLoading = signal(false);

  // Chart data
  protected ventesParTypeChart: any;
  protected chartOptions: any;

  constructor(private vendeurService: VendeurDashboardService) {
    this.initChartOptions();
  }

  ngOnInit(): void {
    this.loadDashboard();
    this.setupAutoRefresh();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadDashboard(): void {
    this.isLoading.set(true);
    this.vendeurService
      .getDashboardData()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: res => {
          if (res.body) {
            this.updateDashboardData(res.body);
          }
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        },
      });
  }

  private updateDashboardData(data: IVendeurDashboard): void {
    this.mesPerformances.set(data.mesPerformances);
    this.mesClients.set(data.mesClients);
    this.ventesParType.set(data.ventesParType);
    this.commission.set(data.commission ?? null);
    this.topProduits.set(data.topProduits ?? []);
    this.ventesRecentes.set(data.ventesRecentes ?? []);
    this.opportunites.set(data.opportunites ?? []);
    this.objectifsMensuels.set(data.objectifsMensuels ?? []);
    this.clientsFideles.set(data.clientsFideles ?? []);

    this.updateVentesParTypeChart();
  }

  private setupAutoRefresh(): void {
    // Auto-refresh every 30 seconds
    interval(30000)
      .pipe(
        startWith(0),
        switchMap(() => this.vendeurService.getDashboardData()),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: res => {
          if (res.body) {
            this.updateDashboardData(res.body);
          }
        },
      });
  }

  private initChartOptions(): void {
    this.chartOptions = {
      plugins: {
        legend: {
          labels: {
            color: '#F1F5F9',
          },
        },
      },
      responsive: true,
      maintainAspectRatio: false,
    };
  }

  private updateVentesParTypeChart(): void {
    const ventes = this.ventesParType();
    if (!ventes) return;

    this.ventesParTypeChart = {
      labels: ['Ordonnance', 'Conseil', 'Parapharmacie'],
      datasets: [
        {
          data: [ventes.ordonnance, ventes.conseil, ventes.parapharmacie],
          backgroundColor: ['#3B82F6', '#06B6D4', '#10B981'],
          hoverBackgroundColor: ['#60A5FA', '#22D3EE', '#34D399'],
        },
      ],
    };
  }

  protected refreshDashboard(): void {
    this.loadDashboard();
  }

  protected getBadgeIcon(badge: string): string {
    switch (badge) {
      case 'PLATINE':
        return '🏅';
      case 'OR':
        return '🥇';
      case 'ARGENT':
        return '🥈';
      case 'BRONZE':
        return '🥉';
      default:
        return '🏆';
    }
  }

  protected getBadgeSeverity(badge: string): 'success' | 'info' | 'warn' | 'secondary' {
    switch (badge) {
      case 'PLATINE':
      case 'OR':
        return 'success';
      case 'ARGENT':
        return 'info';
      case 'BRONZE':
        return 'warn';
      default:
        return 'secondary';
    }
  }

  protected getOpportuniteIcon(type: string): string {
    switch (type) {
      case 'ABONNEMENT':
        return 'pi pi-calendar';
      case 'COMPLEMENTAIRE':
        return 'pi pi-shopping-cart';
      case 'FORT_POTENTIEL':
        return 'pi pi-star';
      default:
        return 'pi pi-info-circle';
    }
  }

  protected getClientCategoryBadge(category: string): 'success' | 'warn' | 'info' {
    switch (category) {
      case 'VIP':
        return 'success';
      case 'FIDELE':
        return 'info';
      case 'POTENTIEL':
        return 'warn';
      default:
        return 'info';
    }
  }

  protected formatCurrency(value: number): string {
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  }

  protected formatPercent(value: number): string {
    return `${value.toFixed(1)}%`;
  }
}
