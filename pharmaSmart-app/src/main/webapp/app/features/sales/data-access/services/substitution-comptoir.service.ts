import { inject, Injectable } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { from, Observable } from 'rxjs';
import { ProduitSearch } from '../../../../shared/model';
import { OrigineSubstitution, SubstitutsModalComponent } from '../../ui/substituts-modal/substituts-modal.component';

/**
 * Ouvre la liste des équivalents disponibles d'un produit et rend celui que le préparateur choisit. Tous les écrans de vente
 * y viennent par le même chemin : rupture à l'ajout, alerte de stock avant l'ajout, ligne du panier.
 */
@Injectable({ providedIn: 'root' })
export class SubstitutionComptoirService {
  private readonly modalService = inject(NgbModal);

  /** Émet le produit choisi, ou `null` si la fenêtre est fermée sans choix ; se complète ensuite. */
  choisir(origine: OrigineSubstitution): Observable<ProduitSearch | null> {
    const ref = this.modalService.open(SubstitutsModalComponent, { centered: true, size: 'lg', scrollable: true });
    (ref.componentInstance as SubstitutsModalComponent).origine = origine;
    return from(ref.result.then((produit: ProduitSearch): ProduitSearch | null => produit, (): ProduitSearch | null => null));
  }
}
