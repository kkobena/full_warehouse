import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { SelectModule } from 'primeng/select';
import { TooltipModule } from 'primeng/tooltip';
import { AvoirFournisseurService } from 'app/entities/commande/retour_fournisseur/avoir-fournisseur.service';
import { IAvoirEncoursFournisseur, IAvoirFournisseur, AvoirStatut } from 'app/shared/model/avoir-fournisseur.model';
import { NotificationService } from 'app/shared/services/notification.service';
import { NgbConfirmDialogService } from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';

@Component({
  selector: 'app-avoir-encours',
  imports: [CommonModule, FormsModule, ButtonModule, TableModule, SelectModule, TooltipModule],
  templateUrl: './avoir-encours.component.html',
  styleUrl: './avoir-encours.component.scss',
})
export class AvoirEncoursComponent implements OnInit {
  private readonly avoirService = inject(AvoirFournisseurService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

  protected encours = signal<IAvoirEncoursFournisseur[]>([]);
  protected avoirs = signal<IAvoirFournisseur[]>([]);
  protected selectedFournisseurId = signal<number | null>(null);
  protected selectedFournisseurLibelle = signal<string>('');
  protected detailStatut: AvoirStatut | null = 'EN_ATTENTE';
  protected loadingEncours = signal(false);
  protected loadingDetail = signal(false);

  protected readonly statutOptions = [
    { label: 'Tous les statuts', value: null },
    { label: 'En attente', value: 'EN_ATTENTE' as AvoirStatut },
    { label: 'Remboursé', value: 'REMBOURSE' as AvoirStatut },
    { label: 'Imputé', value: 'IMPUTE' as AvoirStatut },
  ];

  ngOnInit(): void {
    this.loadEncours();
  }

  protected loadEncours(): void {
    this.loadingEncours.set(true);
    this.avoirService.getEncoursParFournisseur().subscribe({
      next: data => { this.encours.set(data); this.loadingEncours.set(false); },
      error: () => { this.notificationService.error('Erreur chargement encours avoirs'); this.loadingEncours.set(false); },
    });
  }

  protected selectFournisseur(row: IAvoirEncoursFournisseur): void {
    this.selectedFournisseurId.set(row.fournisseurId!);
    this.selectedFournisseurLibelle.set(row.fournisseurLibelle ?? '');
    this.detailStatut = 'EN_ATTENTE';
    this.loadDetail();
  }

  protected onDetailStatutChange(): void {
    this.loadDetail();
  }

  protected loadDetail(): void {
    const fournisseurId = this.selectedFournisseurId();
    if (!fournisseurId) return;
    this.loadingDetail.set(true);
    const statut = this.detailStatut ?? undefined;
    this.avoirService.query({ statut, fournisseurId, size: 200 }).subscribe({
      next: res => { this.avoirs.set(res.body ?? []); this.loadingDetail.set(false); },
      error: () => { this.notificationService.error('Erreur chargement avoirs'); this.loadingDetail.set(false); },
    });
  }

  protected confirmUpdateStatut(avoir: IAvoirFournisseur, statut: AvoirStatut): void {
    const label = statut === 'REMBOURSE' ? 'remboursé' : 'imputé';
    this.confirmDialog.onConfirm( () => this.updateStatut(avoir, statut),
      'Confirmation',
      `Marquer l'avoir ${avoir.reference ?? '#' + avoir.id} comme ${label} ?`,

    );
  }

  private updateStatut(avoir: IAvoirFournisseur, statut: AvoirStatut): void {
    this.avoirService.updateStatut(avoir.id!, statut).subscribe({
      next: () => {
        this.notificationService.success('Avoir mis à jour');
        this.loadEncours();
        this.loadDetail();
      },
      error: () => this.notificationService.error('Erreur mise à jour avoir'),
    });
  }

  protected getStatutLabel(statut: AvoirStatut | undefined): string {
    switch (statut) {
      case 'EN_ATTENTE': return 'En attente';
      case 'REMBOURSE':  return 'Remboursé';
      case 'IMPUTE':     return 'Imputé';
      default:           return statut ?? '';
    }
  }

  protected getStatutBadgeClass(statut: AvoirStatut | undefined): string {
    switch (statut) {
      case 'EN_ATTENTE': return 'pharma-badge pharma-badge-warning';
      case 'REMBOURSE':  return 'pharma-badge pharma-badge-success';
      case 'IMPUTE':     return 'pharma-badge pharma-badge-info';
      default:           return 'pharma-badge pharma-badge-secondary';
    }
  }

  protected getRowClass(avoir: IAvoirFournisseur): string {
    switch (avoir.statut) {
      case 'REMBOURSE': return 'pharma-row-success';
      case 'IMPUTE':    return 'pharma-row-info';
      default:          return '';
    }
  }

  protected getTotalEncours(): number {
    return this.encours().reduce((sum, e) => sum + (e.montantEncours ?? 0), 0);
  }

  protected getTotalAvoirsDetail(): number {
    return this.avoirs().reduce((sum, a) => sum + (a.montant ?? 0), 0);
  }
}
