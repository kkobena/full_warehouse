import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, CardComponent, DataTableComponent, RadioComponent } from 'app/shared/ui';
import { ICustomer } from 'app/shared/model/customer.model';
import { NotificationService } from 'app/shared/services/notification.service';
import { ErrorService } from 'app/shared/error.service';
import { CustomerService } from '../customer.service';

const ENTITY_LABELS: Record<string, string> = {
  ventes: 'Ventes',
  ventesAyantDroit: "Ventes en tant qu'ayant droit",
  reglementsDifferes: 'Règlements de différés',
  avoirs: 'Avoirs',
  retours: 'Retours',
  ayantsDroit: 'Ayants droit',
  tiersPayants: 'Tiers payants',
  allergies: 'Allergies',
  dossierSante: 'Dossier santé',
  compteCarnet: 'Compte carnet',
};

/**
 * Fusion des clients sélectionnés dans la liste (docs/PLAN-FICHE-CLIENT.md, lot 5), sur le modèle
 * de la fusion de produits : choisir la fiche à conserver, lire l'analyse, confirmer. Les autres
 * fiches sont désactivées.
 */
@Component({
  selector: 'app-fusion-client-modal',
  templateUrl: './fusion-client-modal.component.html',
  styleUrls: ['./fusion-client-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, ButtonComponent, CardComponent, DataTableComponent, RadioComponent],
})
export class FusionClientModalComponent {
  clients!: ICustomer[];

  activeModal = inject(NgbActiveModal);

  private readonly customerService = inject(CustomerService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  protected targetId = signal<number | null>(null);
  protected loadingPreview = signal(false);
  protected isConfirming = signal(false);
  protected previewError = signal<string | null>(null);
  protected entityCounts = signal<Record<string, number>>({});
  protected rejets = signal<Record<number, string>>({});
  protected avertissements = signal<string[]>([]);

  protected sourceIds = computed(() => this.clients.filter(c => c.id !== this.targetId()).map(c => c.id));

  protected rejetEntries = computed(() => Object.entries(this.rejets()).map(([id, raison]) => ({ id: Number(id), raison })));

  protected entityCountEntries = computed(() =>
    Object.entries(this.entityCounts())
      .filter(([, count]) => count > 0)
      .map(([key, count]) => ({ label: ENTITY_LABELS[key] ?? key, count })),
  );

  protected canConfirm = computed(
    () =>
      this.targetId() !== null &&
      this.sourceIds().length > 0 &&
      !this.loadingPreview() &&
      !this.previewError() &&
      this.rejetEntries().length === 0,
  );

  protected onTargetChange(): void {
    this.runPreview();
  }

  protected runPreview(): void {
    const targetId = this.targetId();
    const sourceIds = this.sourceIds();
    if (targetId === null || sourceIds.length === 0) {
      return;
    }

    this.loadingPreview.set(true);
    this.previewError.set(null);

    this.customerService.apercuFusion(targetId, sourceIds).subscribe({
      next: preview => {
        this.entityCounts.set(preview.counts ?? {});
        this.rejets.set(preview.rejets ?? {});
        this.avertissements.set(preview.avertissements ?? []);
        this.loadingPreview.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.previewError.set(this.errorService.getErrorMessage(err));
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
        this.loadingPreview.set(false);
      },
    });
  }

  protected clientLibelle(id: number): string {
    const client = this.clients.find(c => c.id === id);
    return client ? this.nom(client) : `#${id}`;
  }

  protected nom(client: ICustomer): string {
    return client.fullName || `${client.firstName} ${client.lastName}`;
  }

  protected confirm(): void {
    const targetId = this.targetId();
    if (targetId === null || !this.canConfirm()) {
      return;
    }

    this.isConfirming.set(true);
    this.customerService.fusionner(targetId, this.sourceIds()).subscribe({
      next: result => {
        this.isConfirming.set(false);
        this.activeModal.close(result);
      },
      error: (err: HttpErrorResponse) => {
        this.isConfirming.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
      },
    });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
