import { Component, inject } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { SuggestionLigneEnrichie } from '../../data-access/suggestion-enrichie.model';

@Component({
  selector: 'app-suggestion-commander-modal',
  templateUrl: './suggestion-commander-modal.component.html',
  styleUrls: ['./suggestion-commander-modal.scss'],
  imports: [CommonModule, ButtonModule, TagModule, DecimalPipe],
})
export class SuggestionCommanderModalComponent {
  private readonly activeModal = inject(NgbActiveModal);

  // Plain properties set via componentInstance (NOT input signals)
  fournisseurLibelle: string = '';
  lignes: SuggestionLigneEnrichie[] = [];
  budgetRestant?: number;

  get montantTotal(): number {
    return this.lignes.reduce((s, l) => s + l.quantite * l.prixAchat, 0);
  }

  get lignesModifiees(): SuggestionLigneEnrichie[] {
    return this.lignes.filter(l => l.quantiteModifiee);
  }

  confirm(): void {
    this.activeModal.close('confirmed');
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}
