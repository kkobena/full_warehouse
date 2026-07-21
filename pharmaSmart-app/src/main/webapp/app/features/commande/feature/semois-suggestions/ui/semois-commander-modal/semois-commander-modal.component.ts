import {Component, inject} from '@angular/core';
import {CommonModule, DecimalPipe} from '@angular/common';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {BadgeComponent, ButtonComponent} from "../../../../../../shared/ui";

export interface SemoisCommandeLine {
  produitId: number;
  fournisseurId: number;
  libelle: string;
  fournisseurLibelle: string;
  quantite: number;
  urgence: string;
}

@Component({
  selector: 'app-semois-commander-modal',
  templateUrl: './semois-commander-modal.component.html',
  imports: [CommonModule, DecimalPipe, BadgeComponent, ButtonComponent],
})
export class SemoisCommanderModalComponent {
  /** Lignes SEMOIS à commander — setté via componentInstance */
  lignes: SemoisCommandeLine[] = [];
  titre = 'Commander les suggestions SEMOIS';
  private readonly activeModal = inject(NgbActiveModal);

  /** Lignes groupées par fournisseur pour l'affichage */
  get lignesParFournisseur(): Map<string, SemoisCommandeLine[]> {
    const map = new Map<string, SemoisCommandeLine[]>();
    this.lignes.forEach(l => {
      const key = l.fournisseurLibelle;
      if (!map.has(key)) {
        map.set(key, []);
      }
      map.get(key)!.push(l);
    });
    return map;
  }

  get fournisseurEntries(): Array<{ label: string; lignes: SemoisCommandeLine[] }> {
    return Array.from(this.lignesParFournisseur.entries()).map(([label, lignes]) => ({
      label,
      lignes
    }));
  }

  get nbFournisseurs(): number {
    return this.lignesParFournisseur.size;
  }

  get urgenceSeverity(): 'danger' | 'warn' | 'success' {
    const hasUrgent = this.lignes.some(l => l.urgence === 'URGENT');
    if (hasUrgent) {
      return 'danger';
    }
    return 'warn';
  }

  confirm(): void {
    this.activeModal.close('confirmed');
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}

