import {ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, signal, untracked} from '@angular/core';
import {DatePipe, DecimalPipe} from '@angular/common';
import {forkJoin, map, Observable, of} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {AbilityService} from 'app/core/auth/ability.service';
import {CustomerService} from 'app/entities/customer/customer.service';
import {IDossierSante, IProduitDelivre, ISituationCredit} from 'app/entities/customer/customer-fiche.model';
import {ICustomer} from 'app/shared/model/customer.model';
import {ProduitSearch} from 'app/shared/model';
import {DATE_FORMAT_ISO_DATE, IS_ISO_DATE_PAST} from 'app/shared/util/warehouse-util';
import {currencySymbol} from 'app/shared/utils/format-utils';
import {BadgeComponent, ButtonComponent, DataTableComponent, OffcanvasComponent} from 'app/shared/ui';
import {NotificationService} from 'app/shared/services/notification.service';
import {IAvoirClientDocument} from 'app/shared/model/avoir-client-document.model';
import {ProductSearchService} from '../../data-access/services/product-search.service';

interface FicheClient {
  customer: ICustomer | null;
  sante: IDossierSante | null;
  credit: ISituationCredit | null;
  avoirs: IAvoirClientDocument[];
  produits: IProduitDelivre[];
}

/**
 * Fiche client consultée sans quitter la vente : alertes, couverture, dernières délivrances.
 * « Re-délivrer » renvoie le produit à l'écran de vente, qui le présélectionne avec ses contrôles habituels.
 */
@Component({
  selector: 'app-fiche-client-panel',
  templateUrl: './fiche-client-panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, DecimalPipe, BadgeComponent, ButtonComponent, DataTableComponent, OffcanvasComponent],
})
export class FicheClientPanelComponent {
  readonly customerId = input<number | null | undefined>(null);
  readonly visible = model<boolean>(false);
  readonly redelivrer = output<ProduitSearch>();

  protected readonly fiche = signal<FicheClient | null>(null);
  protected readonly chargement = signal(false);
  protected readonly devise = currencySymbol();
  protected readonly peutOuvrirFiche = inject(AbilityService).canSignal('access', 'customer');

  protected readonly tiersPayants = computed(() =>
    [...(this.fiche()?.customer?.tiersPayants ?? [])].sort((a, b) => (a.categorie ?? 0) - (b.categorie ?? 0)),
  );
  protected readonly cartesExpirees = computed(() => this.tiersPayants().filter(tp => IS_ISO_DATE_PAST(tp.dateFinValidite)));
  protected readonly avoirsOuverts = computed(() => (this.fiche()?.avoirs ?? []).filter(a => a.statut === 'OUVERT'));
  protected readonly soldeAvoirs = computed(() => this.avoirsOuverts().reduce((s, a) => s + (a.montantRestant ?? a.montant ?? 0), 0));
  protected readonly grossesseEnCours = computed(() => {
    const d = this.fiche()?.sante;
    return !!d?.grossesse && (!d.dateTerme || !IS_ISO_DATE_PAST(d.dateTerme));
  });
  protected readonly creditDepasse = computed(() => {
    const c = this.fiche()?.credit;
    return !!c && c.limite > 0 && c.encours >= c.limite;
  });

  private readonly customerService = inject(CustomerService);
  private readonly productSearch = inject(ProductSearchService);
  private readonly notificationService = inject(NotificationService);

  constructor() {
    // Rechargée à chaque ouverture : la fiche a pu changer depuis (vente, règlement, avoir).
    effect(() => {
      const id = this.customerId();
      if (this.visible() && id) {
        untracked(() => this.charger(id));
      }
    });
  }

  protected fermer(): void {
    this.visible.set(false);
  }

  protected ouvrirFicheComplete(): void {
    const id = this.customerId();
    if (id) {
      window.open(`/customer/${id}/view`, '_blank');
    }
  }

  /** Le produit est recherché dans le catalogue de vente : prix et stock du jour, pas ceux de l'historique. */
  protected choisir(produit: IProduitDelivre): void {
    this.productSearch.search(produit.libelle, 20).subscribe(resultats => {
      const trouve = resultats.find(p => p.id === produit.produitId);
      if (trouve) {
        this.fermer();
        this.redelivrer.emit(trouve);
      } else {
        this.notificationService.error(`${produit.libelle} n'est plus disponible à la vente`);
      }
    });
  }

  private charger(id: number): void {
    this.chargement.set(true);
    const aujourdhui = new Date();
    const ilYaUnAn = new Date(aujourdhui.getFullYear() - 1, aujourdhui.getMonth(), aujourdhui.getDate());
    forkJoin({
      customer: this.lire(this.customerService.find(id).pipe(map(res => res.body ?? null))),
      sante: this.lire(this.customerService.dossierSante(id)),
      credit: this.lire(this.customerService.situationCredit(id)),
      avoirs: this.lire(this.customerService.avoirsByCustomer(id)).pipe(map(a => a ?? [])),
      produits: this.lire(
        this.customerService
          .produitsDelivres(id, {fromDate: DATE_FORMAT_ISO_DATE(ilYaUnAn), toDate: DATE_FORMAT_ISO_DATE(aujourdhui), page: 0, size: 10})
          .pipe(map(res => res.body ?? [])),
      ).pipe(map(p => p ?? [])),
    }).subscribe(fiche => {
      this.fiche.set(fiche);
      this.chargement.set(false);
    });
  }

  /** Une section indisponible ne doit pas priver le comptoir des autres. */
  private lire<T>(source: Observable<T>): Observable<T | null> {
    return source.pipe(catchError(() => of(null)));
  }
}
