import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, SelectComponent } from 'app/shared/ui';

/** Durées prédéfinies (jours) */
const DUREE_OPTIONS = [
  { label: '7 jours (1 semaine)', value: 7 },
  { label: '15 jours (2 semaines)', value: 15 },
  { label: '30 jours (1 mois)', value: 30 },
  { label: '60 jours (2 mois)', value: 60 },
  { label: '90 jours (3 mois)', value: 90 },
];

@Component({
  selector: 'app-semois-exclure-produit',
  templateUrl: './semois-exclure-produit.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, FormsModule, ButtonComponent, SelectComponent],
})
export class SemoisExclureProduitComponent {
  private readonly activeModal = inject(NgbActiveModal);

  /** Setté via componentInstance par le parent */
  produitId!: number;
  produitLibelle = '';

  readonly dureeJours = signal<number>(30);
  readonly motif = signal<string>('');
  readonly isSaving = signal<boolean>(false);

  readonly dureeOptions = DUREE_OPTIONS;

  get isValid(): boolean {
    return this.dureeJours() > 0;
  }

  confirm(): void {
    if (!this.isValid) return;
    this.activeModal.close({ dureeJours: this.dureeJours(), motif: this.motif() || undefined });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}

