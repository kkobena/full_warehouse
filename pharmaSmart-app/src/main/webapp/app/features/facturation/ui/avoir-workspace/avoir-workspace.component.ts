import { Component, DestroyRef, effect, inject, input, signal, TemplateRef, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs/operators';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { FloatLabelModule } from 'primeng/floatlabel';
import { InputNumber } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { BadgeModule } from 'primeng/badge';

import { NotificationService } from '../../../../shared/services/notification.service';
import { IAvoir, IAvoirCommand, IAvoirLine, IFacture, IFactureItem } from '../../data-access/models';
import { AvoirApiService } from '../../data-access/services/avoir-api.service';

type AvoirMode = 'global' | 'ligne';

interface LigneSaisie extends IFactureItem {
  motifRejet: string;
}

@Component({
  selector: 'app-avoir-workspace',
  imports: [
    DecimalPipe,
    FormsModule,
    BadgeModule,
    ButtonModule,
    FloatLabelModule,
    InputNumber,
    InputTextModule,
    ProgressSpinnerModule,
    TableModule,
  ],
  templateUrl: './avoir-workspace.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './avoir-workspace.component.scss',
})
export class AvoirWorkspaceComponent {
  readonly facture = input<IFacture | null>(null);
  readonly factureItems = input<IFactureItem[]>([]);

  @ViewChild('motifModal') motifModalTpl!: TemplateRef<any>;

  protected avoirs = signal<IAvoir[]>([]);
  protected loadingAvoirs = signal(false);
  protected showForm = signal(false);
  protected saving = signal(false);

  protected mode: AvoirMode = 'global';
  protected montantAvoir: number | null = null;
  protected montantTva: number | null = null;
  protected montantHt: number | null = null;
  protected motifDraft = '';
  protected lignes: LigneSaisie[] = [];
  protected selectedLignes: LigneSaisie[] = [];

  private readonly avoirApiService = inject(AvoirApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly modalService = inject(NgbModal);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const f = this.facture();
      if (f?.factureItemId) {
        this.resetForm();
        this.showForm.set(false);
        this.loadAvoirs(f);
      }
    });
  }

  get maxMontant(): number {
    return this.facture()?.montantRegle ?? 0;
  }

  get totalLignes(): number {
    return this.selectedLignes.reduce((sum, l) => sum + (l.montant ?? 0), 0);
  }

  get montantEffectif(): number {
    return this.mode === 'global' ? (this.montantAvoir ?? 0) : this.totalLignes;
  }

  protected onNouvelAvoir(): void {
    this.resetForm();
    this.initLignes();
    this.showForm.set(true);
  }

  protected onAnnuler(): void {
    this.showForm.set(false);
    this.resetForm();
  }

  protected isFieldsValid(): boolean {
    if (this.mode === 'global') {
      if (this.montantAvoir === null || this.montantAvoir <= 0 || this.montantAvoir > this.maxMontant) {
        return false;
      }
      if (this.montantTva !== null && this.montantHt !== null) {
        if (this.montantTva + this.montantHt > this.montantAvoir) return false;
      }
      return true;
    }
    return this.selectedLignes.length > 0 && this.selectedLignes.every(l => !!l.motifRejet?.trim());
  }

  get tvaHtDepasseMontant(): boolean {
    if (this.montantAvoir === null || this.montantTva === null || this.montantHt === null) return false;
    return this.montantTva + this.montantHt > this.montantAvoir;
  }

  protected isSelected(ligne: LigneSaisie): boolean {
    return this.selectedLignes.includes(ligne);
  }

  protected onEnregistrer(): void {
    if (!this.isFieldsValid()) return;
    this.motifDraft = '';
    const ref = this.modalService.open(this.motifModalTpl, { size: 'sm', centered: true });
    ref.result.then((motif: string) => this.doSave(motif)).catch(() => {});
  }

  private doSave(motif: string): void {
    const f = this.facture()!;
    const command: IAvoirCommand = {
      factureId: f.factureItemId!.id,
      factureDate: f.factureItemId!.invoiceDate,
      motif: motif.trim(),
    };

    if (this.mode === 'global') {
      command.montantAvoir = this.montantAvoir!;
      if (this.montantTva != null) command.montantTva = this.montantTva;
      if (this.montantHt != null) command.montantHt = this.montantHt;
    } else {
      command.montantAvoir = this.totalLignes;
      command.lignes = this.selectedLignes.map(l => ({
        saleLineId: l.saleId,
        montantAvoir: l.montant,
        motifRejet: l.motifRejet,
      } as IAvoirLine));
    }

    this.saving.set(true);
    this.avoirApiService
      .create(command)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: res => {
          this.notificationService.success('Avoir créé avec succès');
          this.avoirs.update(list => [res.body as IAvoir, ...list]);
          this.showForm.set(false);
          this.resetForm();
        },
        error: () => this.notificationService.error("Erreur lors de la création de l'avoir"),
      });
  }

  getStatutLabel(statut?: string): string {
    switch (statut) {
      case 'DRAFT':  return 'Brouillon';
      case 'EMIS':   return 'Émis';
      case 'IMPUTE': return 'Imputé';
      case 'ANNULE': return 'Annulé';
      default:       return statut ?? '—';
    }
  }

  getStatutSeverity(statut?: string): 'secondary' | 'info' | 'success' | 'danger' {
    switch (statut) {
      case 'DRAFT':  return 'secondary';
      case 'EMIS':   return 'info';
      case 'IMPUTE': return 'success';
      case 'ANNULE': return 'danger';
      default:       return 'secondary';
    }
  }

  private initLignes(): void {
    this.lignes = this.factureItems().map(item => ({ ...item, motifRejet: '' }));
    this.selectedLignes = [];
  }

  private resetForm(): void {
    this.mode = 'global';
    this.montantAvoir = null;
    this.montantTva = null;
    this.montantHt = null;
    this.motifDraft = '';
    this.lignes = [];
    this.selectedLignes = [];
  }

  private loadAvoirs(f: IFacture): void {
    this.loadingAvoirs.set(true);
    const invoiceDate = f.factureItemId?.invoiceDate ?? '2000-01-01';
    this.avoirApiService
      .query({ startDate: invoiceDate, endDate: new Date().toISOString().slice(0, 10) })
      .pipe(finalize(() => this.loadingAvoirs.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          const factureId = f.factureItemId?.id;
          const all = res.body ?? [];
          this.avoirs.set(factureId ? all.filter(a => a.factureOrigineId === factureId) : all);
        },
        error: () => this.avoirs.set([]),
      });
  }
}
