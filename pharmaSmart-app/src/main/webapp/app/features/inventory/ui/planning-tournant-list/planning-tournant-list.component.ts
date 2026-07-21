import {Component, inject, OnInit, signal, ChangeDetectionStrategy} from '@angular/core';
import {CommonModule, DatePipe} from '@angular/common';
import {Router} from '@angular/router';
import {NgbModal, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';

import {NotificationService} from '../../../../shared/services/notification.service';
import {IPlanningInventaireTournant, ITournantDashboard} from '../../models';
import {PlanningTournantApiService} from '../../data-access/services/planning-tournant-api.service';
import {
  PlanningTournantModalComponent
} from '../planning-tournant-modal/planning-tournant-modal.component';
import {
  NgbConfirmDialogService
} from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import {BadgeComponent, ButtonComponent, DataTableComponent} from '../../../../shared/ui';

@Component({
  selector: 'app-planning-tournant-list',
  imports: [CommonModule, ButtonComponent, BadgeComponent, DataTableComponent, NgbTooltip, DatePipe],
  templateUrl: './planning-tournant-list.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './planning-tournant-list.component.scss',
})
export class PlanningTournantListComponent implements OnInit {
  plannings = signal<IPlanningInventaireTournant[]>([]);
  dashboard = signal<ITournantDashboard | null>(null);
  loading = signal(false);

  private readonly api = inject(PlanningTournantApiService);
  private readonly modal = inject(NgbModal);
  private readonly router = inject(Router);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    this.loadAll();
    this.loadDashboard();
  }

  loadAll(): void {
    this.loading.set(true);
    this.api.findAll().subscribe({
      next: list => {
        this.plannings.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadDashboard(): void {
    this.api.getDashboard().subscribe({
      next: d => this.dashboard.set(d),
      error: () => {
      },
    });
  }

  openCreateModal(): void {
    const ref = this.modal.open(PlanningTournantModalComponent, {size: 'lg', backdrop: 'static'});
    ref.closed.subscribe(() => {
      this.loadAll();
      this.loadDashboard();
    });
  }

  openEditModal(planning: IPlanningInventaireTournant): void {
    const ref = this.modal.open(PlanningTournantModalComponent, {size: 'lg', backdrop: 'static'});
    ref.componentInstance.planning = planning;
    ref.closed.subscribe(() => {
      this.loadAll();
      this.loadDashboard();
    });
  }

  toggleActif(planning: IPlanningInventaireTournant): void {
    this.api.toggleActif(planning.id!).subscribe({
      next: updated => {
        this.plannings.update(list => list.map(p => (p.id === updated.id ? updated : p)));
        this.notificationService.success(updated.actif ? 'Planning activé' : 'Planning désactivé', 'Succès');
      },
      error: () => this.notificationService.error('Impossible de modifier le statut', 'Erreur'),
    });
  }

  delete(planning: IPlanningInventaireTournant): void {
    this.confirmDialog.onConfirm(
      () => this.doDelete(planning),
      'Supprimer le planning',
      `Supprimer "${planning.libelle}" ?`,
      'pi pi-trash',
    );
  }

  executerManuellement(planning: IPlanningInventaireTournant): void {
    this.confirmDialog.onConfirm(
      () => this.doExecuter(planning),
      "Exécuter le planning",
      `Créer maintenant l'inventaire tournant pour "${planning.libelle}" ?`,
      'pi pi-play',
    );
  }

  openInventory(inventoryId: number): void {
    this.router.navigate(['/inventaire', inventoryId, 'edit']);
  }

  getFrequenceLabel(f?: string): string {
    const labels: Record<string, string> = {
      QUOTIDIEN: 'Quotidien',
      HEBDO: 'Hebdomadaire',
      MENSUEL: 'Mensuel',
      TRIMESTRIEL: 'Trimestriel',
    };
    return labels[f ?? ''] ?? f ?? '-';
  }

  getCritereLabel(c?: string): string {
    const labels: Record<string, string> = {
      RAYON: 'Rayon',
      FAMILLE: 'Famille',
      CLASSIFICATION_ABC: 'Classification ABC',
    };
    return labels[c ?? ''] ?? c ?? '-';
  }

  isOverdue(planning: IPlanningInventaireTournant): boolean {
    return planning.actif && planning.prochaineExecution < new Date().toISOString().substring(0, 10);
  }

  private doDelete(planning: IPlanningInventaireTournant): void {
    this.api.delete(planning.id!).subscribe({
      next: () => {
        this.plannings.update(list => list.filter(p => p.id !== planning.id));
        this.loadDashboard();
        this.notificationService.success('Planning supprimé', 'Succès');
      },
      error: () => this.notificationService.error('Impossible de supprimer', 'Erreur'),
    });
  }

  private doExecuter(planning: IPlanningInventaireTournant): void {
    this.api.executerManuellement(planning.id!).subscribe({
      next: res => {
        this.loadAll();
        this.loadDashboard();
        this.notificationService.success(`Inventaire #${res.inventoryId} créé. Cliquez pour ouvrir.`, 'Inventaire créé');
      },
      error: () =>
        this.notificationService.error("Échec de l'exécution", 'Erreur'),
    });
  }
}
