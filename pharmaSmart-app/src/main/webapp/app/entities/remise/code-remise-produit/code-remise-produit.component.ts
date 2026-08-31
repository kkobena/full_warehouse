import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { NgbModal, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';

import { RemiseService } from '../remise.service';
import { CodeRemise, IRemise } from '../../../shared/model/remise.model';
import { IProduit } from '../../../shared/model/produit.model';
import { ProduitService } from '../../produit/produit.service';
import { AppTableLazyLoadEvent } from '../../../shared/ui/data-table/table.types';
import {
  ButtonComponent,
  CardComponent,
  DataTableComponent,
  HintComponent,
  SelectableRowDirective,
  ToolbarComponent,
} from '../../../shared/ui';
// Non réexporté par shared/ui : import direct, comme sur l'écran DCI.
import { PageLayoutComponent } from '../../../shared/ui/page-layout/page-layout.component';
import {
  CodeRemiseProduitsModalComponent
} from '../code-remise-produits-modal/code-remise-produits-modal.component';

/**
 * Codes de remise, en maître / détail.
 *
 * <p>L'écran ne listait que les neuf codes et leurs taux, sans jamais dire CE QU'ILS
 * COUVRENT : pour savoir si un produit était remisé, et à quel taux, il fallait ouvrir sa
 * fiche une par une. La question se pose pourtant dans l'autre sens — « qu'est-ce que la
 * grille à 15 % touche exactement ? » — au moment de la négocier ou de la corriger.
 *
 * <p>Le panneau de droite y répond en listant les produits porteurs du code retenu. Le bouton
 * d'association reste, mais il n'est plus le seul chemin : on regarde avant de modifier.
 */
@Component({
  selector: 'app-code-remise-produit',
  imports: [
    DecimalPipe,
    NgbTooltip,
    ButtonComponent,
    CardComponent,
    DataTableComponent,
    HintComponent,
    PageLayoutComponent,
    SelectableRowDirective,
    ToolbarComponent,
  ],
  templateUrl: './code-remise-produit.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./code-remise-produit.component.scss'],
})
export class CodeRemiseProduitComponent implements OnInit {
  protected readonly entites = signal<CodeRemise[] | undefined>(undefined);
  protected readonly selection = signal<CodeRemise | null>(null);
  protected readonly produits = signal<IProduit[]>([]);
  protected readonly totalProduits = signal(0);
  protected readonly chargementDetail = signal(false);
  /** Rang de la première ligne affichée : la table le rend au paginateur après un rechargement. */
  protected readonly premierRang = signal(0);
  protected readonly taillePage = signal(15);
  protected readonly panelOpen = computed(() => this.selection() !== null);

  private readonly entityService = inject(RemiseService);
  private readonly produitService = inject(ProduitService);
  private readonly ngModalService = inject(NgbModal);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.entityService.queryFullCodes().subscribe({
      next: (res: HttpResponse<CodeRemise[]>) => {
        this.entites.set(res.body || []);
      },
    });
  }

  protected onSelection(code: CodeRemise | null): void {
    this.selection.set(code);
    this.produits.set([]);
    this.totalProduits.set(0);
    // Changer de code, c'est repartir de la première page : conserver le rang courant
    // afficherait une page vide sur un code moins fourni que le précédent.
    this.premierRang.set(0);
    if (!code) {
      return;
    }
    this.chargerProduits(code, 0);
  }

  /**
   * Pagination SERVEUR : un code de remise couvre couramment plusieurs centaines de produits
   * — six cents pour le code 2 du jeu de démonstration. Les charger tous pour n'en montrer
   * quinze ferait payer au poste le prix d'une liste que personne ne lit d'un bloc.
   */
  protected onLazyLoad(evenement: AppTableLazyLoadEvent): void {
    const code = this.selection();
    if (!code || !evenement) {
      return;
    }
    this.taillePage.set(evenement.rows);
    this.premierRang.set(evenement.first);
    this.chargerProduits(code, Math.floor(evenement.first / evenement.rows));
  }

  /** Le regard, puis éventuellement la main : on ouvre le détail sans passer par la modal. */
  protected voirDetail(code: CodeRemise, evenement: MouseEvent): void {
    evenement.stopPropagation();
    this.onSelection(code);
  }

  protected fermerDetail(): void {
    this.selection.set(null);
    this.produits.set([]);
    this.totalProduits.set(0);
  }

  protected onOpenModal(codeRemise?: CodeRemise, evenement?: MouseEvent): void {
    evenement?.stopPropagation();
    const modalRef = this.ngModalService.open(CodeRemiseProduitsModalComponent, {
      backdrop: 'static',
      size: 'xl',
      centered: true,
    });
    modalRef.componentInstance.codeRemise = codeRemise;

    modalRef.closed.subscribe(() => {
      this.load();
      // Le panneau montrait l'état d'AVANT l'association : on le relit, sinon il ment.
      const courant = this.selection();
      if (courant) {
        this.chargerProduits(courant, Math.floor(this.premierRang() / this.taillePage()));
      }
    });
  }

  protected getVnoTaux(entity: IRemise): string {
    const taux = entity?.grilles.filter(grille => grille.grilleType === 'VNO')[0]?.remiseValue;
    return taux ? `${taux} %` : '';
  }

  protected getVoTaux(entity: IRemise): string {
    const taux = entity?.grilles.filter(grille => grille.grilleType === 'VO')[0]?.remiseValue;
    return taux ? `${taux} %` : '';
  }

  /**
   * Les produits porteurs du code.
   *
   * <p>Le filtre passe par la VALEUR du code — « 1 », « 2 »… — que le serveur retraduit en
   * constante : c'est ce que le DTO expose, et le front n'a pas à connaître le vocabulaire
   * interne de l'énumération.
   *
   * <p>La liste est paginée côté serveur : on n'en demande qu'une page à la fois.
   */
  private chargerProduits(code: CodeRemise, page: number): void {
    this.chargementDetail.set(true);
    this.produitService
      .query({ codeRemise: code.value, page, size: this.taillePage() })
      .subscribe({
        next: (res: HttpResponse<IProduit[]>) => {
          this.produits.set(res.body ?? []);
          this.totalProduits.set(Number(res.headers.get('X-Total-Count')) || (res.body?.length ?? 0));
          this.chargementDetail.set(false);
        },
        error: () => this.chargementDetail.set(false),
      });
  }
}
