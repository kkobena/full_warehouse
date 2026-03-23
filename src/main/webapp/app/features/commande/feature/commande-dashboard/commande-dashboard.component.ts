import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { TagModule } from 'primeng/tag';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { SkeletonModule } from 'primeng/skeleton';
import { ToolbarModule } from 'primeng/toolbar';
import { CommandeService, ICommandeDashboard, ICommandeResumee, IPharmaMlEnvoiResumee } from '../../../../entities/commande/commande.service';

@Component({
  selector: 'app-commande-dashboard',
  templateUrl: './commande-dashboard.component.html',
  styleUrls: ['./commande-dashboard.component.scss'],
  imports: [DatePipe, RouterModule, TagModule, TableModule, ButtonModule, TooltipModule, SkeletonModule, ToolbarModule],
})
export class CommandeDashboardComponent implements OnInit {
  readonly dashboard = signal<ICommandeDashboard | null>(null);
  readonly loading = signal(true);

  private readonly commandeService = inject(CommandeService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.commandeService.getDashboard().subscribe({
      next: data => {
        this.dashboard.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  refresh(): void {
    this.loading.set(true);
    this.dashboard.set(null);
    this.ngOnInit();
  }

  openCommande(row: ICommandeResumee): void {
    this.router.navigate(['/commande', row.id, row.orderDate, 'edit']);
  }

  openCommandeFromEnvoi(row: IPharmaMlEnvoiResumee): void {
    this.router.navigate(['/commande', row.commandeId, row.commandeOrderDate, 'edit']);
  }



  pharmamlSeverity(statut: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (statut) {
      case 'PENDING': return 'info';
      case 'SUBMITTED': return 'success';
      case 'PARTIAL': return 'warn';
      case 'REJECTED': return 'danger';
      case 'ERROR': return 'danger';
      default: return 'secondary';
    }
  }

  formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-FR').format(Math.round(amount / 100));
  }
}
