import { Component, inject, Input, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NgbActiveModal, NgbDateStruct, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, InputNumberComponent, SelectComponent } from '../../../shared/ui';
import { RouterLink } from '@angular/router';

import { LotPerimes } from '../model/lot-perimes';
import {
  FournisseurSimple,
  ResolutionStatut,
  RetourBonLotResolution,
  RetourFournisseurRequest,
} from '../model/retour-fournisseur-request';
import { IMotifRetourProduit } from '../../../shared/model/motif-retour-produit.model';
import { ModifRetourProduitService } from '../../motif-retour-produit/motif-retour-produit.service';
import { RetourBonService } from '../../commande/retour_fournisseur/retour-bon.service';
import { NotificationService } from '../../../shared/services/notification.service';
import { PharmaDatePickerComponent } from '../../../shared/date-picker/pharma-date-picker.component';
import { NGB_DATE_TO_ISO } from '../../../shared/util/warehouse-util';

@Component({
  selector: 'app-retour-fournisseur-perime-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonComponent,
    SelectComponent,
    InputNumberComponent,
    NgbTooltip,
    RouterLink,
    PharmaDatePickerComponent,
  ],
  templateUrl: './retour-fournisseur-perime-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./retour-lot-pereme.scss'],
})
export class RetourFournisseurPerimeDialogComponent implements OnInit {

  @Input() lot!: LotPerimes;

  protected readonly activeModal = inject(NgbActiveModal);
  private readonly fb = inject(FormBuilder);
  private readonly motifService = inject(ModifRetourProduitService);
  private readonly retourBonService = inject(RetourBonService);
  private readonly notificationService = inject(NotificationService);

  protected motifs = signal<IMotifRetourProduit[]>([]);
  protected isSaving = signal(false);
  protected isResolving = signal(true);
  protected resolutionStatut = signal<ResolutionStatut | null>(null);
  protected commandeNotFound = signal(false);
  protected fournisseurs = signal<FournisseurSimple[]>([]);
  protected fournisseurLibelle = signal<string>('');
  protected form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      motifRetourId: [null, Validators.required],
      quantity: [this.lot.quantity, [Validators.required, Validators.min(1), Validators.max(this.lot.quantity)]],
      commentaire: [''],
      commandeRef: [''],
      commandeDate: [null as NgbDateStruct | null],
      fournisseurId: [null as number | null],
    });
    this.loadMotifs();
    this.resolveFromBackend();
  }

  protected get avoirEstime(): number {
    return (this.form?.get('quantity')?.value ?? 0) * (this.lot?.prixAchat ?? 0);
  }

  protected get fournisseurName(): string {
    return (this.lot as any)?.fournisseur ?? (this.lot as any)?.founisseur ?? '';
  }

  protected get quantityValue(): number {
    return this.form?.get('quantity')?.value ?? 0;
  }

  protected get isHorsCommande(): boolean {
    const s = this.resolutionStatut();
    return s === 'HORS_COMMANDE_UN_FOURN' || s === 'HORS_COMMANDE_MULTI' || this.commandeNotFound();
  }

  protected canSave(): boolean {
    if (!this.form?.valid || this.isSaving() || this.isResolving()) return false;
    if (this.resolutionStatut() === 'FOURNISSEUR_INCONNU') return false;
    if (this.resolutionStatut() === 'HORS_COMMANDE_MULTI') return !!this.form.get('fournisseurId')?.value;
    if (this.commandeNotFound()) return !!this.form.get('commandeRef')?.value;
    return true;
  }

  protected save(): void {
    if (!this.canSave()) { this.form.markAllAsTouched(); return; }

    const request: RetourFournisseurRequest = {
      lotId: this.lot.id,
      motifRetourId: this.form.get('motifRetourId')!.value,
      quantity: this.form.get('quantity')!.value,
      commentaire: this.form.get('commentaire')!.value || undefined,
    };

    if (this.resolutionStatut() === 'HORS_COMMANDE_MULTI') {
      request.fournisseurId = this.form.get('fournisseurId')!.value;
    } else if (this.commandeNotFound() && this.form.get('commandeRef')?.value) {
      request.commandeId = parseInt(this.form.get('commandeRef')!.value, 10);
      const dateVal: NgbDateStruct | null = this.form.get('commandeDate')?.value;
      if (dateVal) { request.commandeOrderDate = NGB_DATE_TO_ISO(dateVal); }
    }

    this.isSaving.set(true);
    this.retourBonService.createFromExpiredLot(request).subscribe({
      next: () => { this.isSaving.set(false); this.activeModal.close({ success: true }); },
      error: (err: any) => {
        this.isSaving.set(false);
        const errorKey = err?.error?.errorKey;
        if (errorKey === 'commandeNotFound') {
          this.commandeNotFound.set(true); this.resolutionStatut.set(null);
          this.notificationService.warning('Aucune commande source trouvée. Saisissez la référence manuellement.', 'Commande introuvable');
        } else if (errorKey === 'multipleFournisseurs') {
          this.fournisseurs.set(err?.error?.payload ?? []);
          this.resolutionStatut.set('HORS_COMMANDE_MULTI');
          this.notificationService.warning('Plusieurs fournisseurs associés. Sélectionnez-en un.', 'Sélection requise');
        } else if (errorKey === 'fournisseurIntrouvable') {
          this.resolutionStatut.set('FOURNISSEUR_INCONNU');
          this.notificationService.error('Aucun fournisseur associé à ce produit. Complétez la fiche produit.', 'Fournisseur introuvable');
        } else {
          this.notificationService.error('Erreur lors de la création du retour fournisseur.', 'Erreur');
        }
      },
    });
  }

  protected cancel(): void { this.activeModal.dismiss(); }

  protected switchToCommandeManuelle(): void { this.commandeNotFound.set(true); this.resolutionStatut.set(null); }

  private loadMotifs(): void {
    this.motifService.query({ page: 0, size: 999 }).subscribe(res => this.motifs.set(res.body ?? []));
  }

  private resolveFromBackend(): void {
    this.isResolving.set(true);
    this.retourBonService.resolveLot(this.lot.id).subscribe({
      next: res => {
        const dto: RetourBonLotResolution = res.body!;
        this.resolutionStatut.set(dto.statut);
        if (dto.statut === 'HORS_COMMANDE_UN_FOURN') this.fournisseurLibelle.set(dto.fournisseurLibelle ?? '');
        else if (dto.statut === 'HORS_COMMANDE_MULTI') this.fournisseurs.set(dto.fournisseurs ?? []);
        this.isResolving.set(false);
      },
      error: () => { this.resolutionStatut.set(null); this.isResolving.set(false); },
    });
  }
}
