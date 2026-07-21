import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { BadgeComponent, ButtonComponent } from 'app/shared/ui';
import { IFournisseurProduit } from 'app/shared/model/fournisseur-produit.model';
import { SuggestionFacadeService } from '../../data-access/suggestion-facade.service';

@Component({
  selector: 'app-suggestion-comparaison',
  templateUrl: './suggestion-comparaison.component.html',
  styleUrls: ['./suggestion-comparaison.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, BadgeComponent, ButtonComponent, DecimalPipe],
})
export class SuggestionComparaisonComponent implements OnInit {
  private readonly facade = inject(SuggestionFacadeService);
  readonly modal = inject(NgbActiveModal);

  // Set by caller before opening
  produitId!: number;
  produitLibelle!: string;
  currentFournisseurProduitId?: number;

  readonly fournisseurs = this.facade.fournisseursProduit;
  readonly loading = this.facade.loadingComparaison;

  ngOnInit(): void {
    this.facade.loadFournisseursProduit(this.produitId);
  }

  isCurrent(fp: IFournisseurProduit): boolean {
    return fp.id === this.currentFournisseurProduitId;
  }

  prixMinimal(): number {
    const prix = this.fournisseurs()
      .map(f => f.prixAchat ?? Infinity)
      .filter(p => p > 0);
    return prix.length ? Math.min(...prix) : 0;
  }

  close(): void {
    this.modal.dismiss();
  }
}
