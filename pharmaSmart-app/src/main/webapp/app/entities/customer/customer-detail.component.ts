import {ChangeDetectionStrategy, Component, computed, inject, OnDestroy, OnInit, signal} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {ISales} from 'app/shared/model/sales.model';
import {ISalesLine} from 'app/shared/model/sales-line.model';
import {ICustomer} from 'app/shared/model/customer.model';
import {IAvoirClientDocument} from 'app/shared/model/avoir-client-document.model';
import {IClientTiersPayant} from 'app/shared/model/client-tiers-payant.model';
import {CustomerService} from './customer.service';
import {HttpResponse} from '@angular/common/http';
import {MagasinService} from '../magasin/magasin.service';
import {IMagasin} from 'app/shared/model/magasin.model';
import {SalesService} from '../sales/sales.service';
import {Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {NgbDateStruct, NgbModal, NgbNavModule, NgbPagination} from '@ng-bootstrap/ng-bootstrap';
import {FormsModule} from '@angular/forms';
import {
  BadgeComponent,
  ButtonComponent,
  CardComponent,
  DataTableComponent,
  KpiItemComponent,
  KpiStripComponent,
  NavTabsComponent
} from '../../shared/ui';
import {CommonModule} from '@angular/common';
import {AlertErrorComponent} from '../../shared/alert/alert-error.component';
import {PharmaDatePickerComponent} from '../../shared/date-picker/pharma-date-picker.component';
import {DeviseDirective} from '../../shared/utils/devise';
import {currencySymbol} from '../../shared/utils/format-utils';
import {AbilityService} from 'app/core/auth/ability.service';
import {BlobDownloadService} from 'app/shared/services/blob-download.service';
import {IDiffere, IReglementDiffere} from 'app/features/differes/data-access/models/differe.model';
import {ICustomerSynthese, IDossierSante, IProduitDelivre, IRelanceDiffere, ISituationCredit} from './customer-fiche.model';
import {IS_ISO_DATE_PAST, NGB_DATE_TO_ISO} from '../../shared/util/warehouse-util';
import {NotificationService} from '../../shared/services/notification.service';
import {DossierSanteTabComponent} from './dossier-sante/dossier-sante-tab.component';
import {showCommonModal} from '../sales/selling-home/sale-helper';
import {AssureFormStepComponent} from './assure-form-step/assure-form-step.component';
import {UninsuredCustomerFormComponent} from './uninsured-customer-form/uninsured-customer-form.component';

/**
 * Fiche client « 360° » (docs/PLAN-FICHE-CLIENT.md, lot 1) : un en-tête permanent — identité,
 * couverture, situation financière — et un onglet par question du comptoir.
 */
@Component({
  selector: 'app-customer-detail',
  templateUrl: './customer-detail.component.html',
  styleUrls: ['./customer-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    NgbNavModule,
    NgbPagination,
    FormsModule,
    RouterLink,
    PharmaDatePickerComponent,
    DeviseDirective,
    BadgeComponent,
    ButtonComponent,
    KpiItemComponent,
    KpiStripComponent,
    NavTabsComponent,
    CommonModule,
    AlertErrorComponent,
    CardComponent,
    DataTableComponent,
    DossierSanteTabComponent
  ]
})
export class CustomerDetailComponent implements OnInit, OnDestroy {
  protected readonly pageSize = 20;
  protected readonly devise = currencySymbol();

  protected readonly customer = signal<ICustomer | null>(null);
  protected readonly synthese = signal<ICustomerSynthese | null>(null);
  protected readonly magasin = signal<IMagasin | undefined>(undefined);

  // ── Achats ──
  protected readonly sales = signal<ISales[]>([]);
  protected readonly selectedRowIndex = signal<number | undefined>(undefined);
  protected readonly selectedRowSaleLines = signal<ISalesLine[]>([]);
  protected readonly saleSelected = signal<ISales | undefined>(undefined);
  // Douze derniers mois par défaut : charger tout l'historique d'un client fidèle était lent.
  protected readonly fromDate = signal<NgbDateStruct>(this.ngbDate(-12));
  protected readonly toDate = signal<NgbDateStruct>(this.ngbDate(0));
  protected readonly page = signal(1);
  protected readonly totalSales = signal(0);

  // ── Produits délivrés ──
  protected readonly produits = signal<IProduitDelivre[]>([]);
  protected readonly produitsFrom = signal<NgbDateStruct>(this.ngbDate(-12));
  protected readonly produitsTo = signal<NgbDateStruct>(this.ngbDate(0));
  protected readonly produitsSearch = signal('');
  protected readonly produitsPage = signal(1);
  protected readonly totalProduits = signal(0);
  private produitsCharges = false;

  // ── Crédit ──
  protected readonly differe = signal<IDiffere | null>(null);
  protected readonly reglements = signal<IReglementDiffere[]>([]);
  protected readonly lignesReglement = computed(() => this.reglements().flatMap(r => r.items ?? []));
  protected readonly reglementsPage = signal(1);
  protected readonly totalReglements = signal(0);
  protected readonly situationCredit = signal<ISituationCredit | null>(null);
  protected readonly relances = signal<IRelanceDiffere[]>([]);
  protected readonly relanceEnCours = signal(false);
  private creditCharge = false;

  // ── Avoirs ──
  protected readonly avoirs = signal<IAvoirClientDocument[]>([]);
  protected readonly avoirsOuverts = computed(() => this.avoirs().filter(a => a.statut === 'OUVERT'));
  protected readonly soldeTotalAvoirs = computed(() => this.avoirsOuverts().reduce((sum, a) => sum + (a.montant ?? 0), 0));

  // ── En-tête ──
  protected readonly estAssure = computed(() => this.customer()?.categorie === 'ASSURE');
  protected readonly age = computed(() => this.ageDe(this.customer()?.datNaiss));
  /** Tiers payants par priorité : le premier est la couverture principale. */
  protected readonly tiersPayants = computed<IClientTiersPayant[]>(() =>
    [...(this.customer()?.tiersPayants ?? [])].sort((a, b) => (a.categorie ?? 0) - (b.categorie ?? 0))
  );
  protected readonly couverturePrincipale = computed(() => this.tiersPayants()[0]);
  protected readonly carteExpiree = IS_ISO_DATE_PAST;
  /** Cartes d'assuré dont la date de fin est passée : signalées dans l'en-tête. */
  protected readonly cartesExpirees = computed(() => this.tiersPayants().filter(tp => IS_ISO_DATE_PAST(tp.dateFinValidite)));
  /** Dossier santé : alimente les pastilles d'alerte de l'en-tête. */
  protected readonly dossierSante = signal<IDossierSante | null>(null);
  protected readonly grossesseEnCours = computed(() => {
    const d = this.dossierSante();
    return !!d?.grossesse && (!d.dateTerme || new Date(d.dateTerme) >= new Date(new Date().toDateString()));
  });

  protected activeTab = 'achats';
  protected activatedRoute = inject(ActivatedRoute);
  protected customerService = inject(CustomerService);
  protected magasinService = inject(MagasinService);
  protected salesService = inject(SalesService);
  private readonly modalService = inject(NgbModal);
  private readonly ability = inject(AbilityService);
  private readonly notificationService = inject(NotificationService);
  private readonly blobDownloadService = inject(BlobDownloadService);
  protected readonly canEdit = this.ability.canSignal('edit', 'customer');
  protected readonly canOpenDifferes = this.ability.canSignal('access', 'differes');
  // Même périmètre que le serveur : le comptoir renseigne aussi le dossier santé.
  protected readonly canEditSante = computed(
    () => this.canEdit() || this.ability.can('edit', 'nouvelle-vente') || this.ability.can('edit', 'nouvelle-prevente') || this.ability.can('edit', 'ventes')
  );
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.activatedRoute.data.pipe(takeUntil(this.destroy$)).subscribe(({customer}) => this.customer.set(customer));
    this.loadSynthese();
    this.loadSales();
    this.loadAvoirs();
    this.customerService
      .dossierSante(this.customer().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe(dossier => this.dossierSante.set(dossier));
    this.magasinService.findCurrentUserMagasin().then(magasin => this.magasin.set(magasin));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  previousState(): void {
    window.history.back();
  }

  /** Les onglets secondaires ne se chargent qu'à leur première ouverture. */
  onTabChange(tab: string): void {
    if (tab === 'produits' && !this.produitsCharges) {
      this.produitsCharges = true;
      this.loadProduits();
    }
    if (tab === 'credit' && !this.creditCharge) {
      this.creditCharge = true;
      this.loadCredit();
    }
  }

  /** L'encours de l'en-tête mène à l'onglet Crédit. */
  voirCredit(): void {
    this.activeTab = 'credit';
    this.onTabChange('credit');
  }

  edit(): void {
    const customer = this.customer();
    if (!customer) {
      return;
    }
    const onSaved = (resp: ICustomer): void => {
      if (resp) {
        this.reloadCustomer();
      }
    };
    if (this.estAssure()) {
      showCommonModal(
        this.modalService,
        AssureFormStepComponent,
        {entity: customer, header: `FORMULAIRE DE MODIFICATION DE CLIENT  [ ${customer.fullName}  ]`},
        onSaved,
        'xl',
        'modal-dialog-80'
      );
    } else {
      showCommonModal(
        this.modalService,
        UninsuredCustomerFormComponent,
        {entity: customer, title: `FORMULAIRE DE MODIFICATION DE CLIENT  [ ${customer.fullName}  ]`},
        onSaved,
        'xl'
      );
    }
  }

  // ── Achats ──

  loadSales(): void {
    this.customerService
      .purchases({
        customerId: this.customer().id,
        fromDate: NGB_DATE_TO_ISO(this.fromDate()),
        toDate: NGB_DATE_TO_ISO(this.toDate()),
        page: this.page() - 1,
        size: this.pageSize
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe((res: HttpResponse<ISales[]>) => {
        this.totalSales.set(Number(res.headers.get('X-Total-Count') ?? 0));
        this.sales.set(res.body ?? []);
      });
  }

  /** Nouvelle période : on repart de la première page. */
  searchSales(): void {
    this.page.set(1);
    this.saleSelected.set(undefined);
    this.loadSales();
  }

  changePage(page: number): void {
    this.page.set(page);
    this.loadSales();
  }

  clickRow(item: ISales): void {
    this.selectedRowIndex.set(item.id);
    this.selectedRowSaleLines.set(item.salesLines);
    this.saleSelected.set(item);
  }

  print(): void {
    const sale = this.saleSelected();
    if (sale) {
      this.salesService
        .print(sale.saleId)
        .pipe(takeUntil(this.destroy$))
        .subscribe(blob => this.blobDownloadService.downloadPdf(blob, `facture-${sale.numberTransaction}`));
    }
  }

  // ── Produits délivrés ──

  loadProduits(): void {
    const req: Record<string, unknown> = {
      fromDate: NGB_DATE_TO_ISO(this.produitsFrom()),
      toDate: NGB_DATE_TO_ISO(this.produitsTo()),
      page: this.produitsPage() - 1,
      size: this.pageSize
    };
    // createRequestOption envoie « undefined » en toutes lettres : la recherche ne part que si elle est saisie.
    if (this.produitsSearch().trim()) {
      req['search'] = this.produitsSearch().trim();
    }
    this.customerService
      .produitsDelivres(this.customer().id, req)
      .pipe(takeUntil(this.destroy$))
      .subscribe(res => {
        this.totalProduits.set(Number(res.headers.get('X-Total-Count') ?? 0));
        this.produits.set(res.body ?? []);
      });
  }

  searchProduits(): void {
    this.produitsPage.set(1);
    this.loadProduits();
  }

  changeProduitsPage(page: number): void {
    this.produitsPage.set(page);
    this.loadProduits();
  }

  // ── Crédit ──

  loadCredit(): void {
    const id = this.customer().id;
    this.customerService
      .differes(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe(differe => this.differe.set(differe));
    this.customerService
      .situationCredit(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe(situation => this.situationCredit.set(situation));
    this.loadRelances();
    this.loadReglements();
  }

  /** Relance SMS du solde des différés ; la dernière relance reste visible pour ne pas en abuser. */
  relancer(): void {
    this.relanceEnCours.set(true);
    this.customerService
      .relancerDifferes(this.customer().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: relance => {
          this.relanceEnCours.set(false);
          this.relances.update(liste => [relance, ...liste]);
          this.notificationService.success(`Relance envoyée au ${relance.telephone}`);
        },
        error: err => {
          this.relanceEnCours.set(false);
          this.notificationService.error(err?.error?.message ?? "La relance n'a pas pu être envoyée");
        }
      });
  }

  private loadRelances(): void {
    this.customerService
      .relancesDifferes(this.customer().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe(relances => this.relances.set(relances));
  }

  loadReglements(): void {
    this.customerService
      .reglementsDifferes(this.customer().id, {page: this.reglementsPage() - 1, size: this.pageSize})
      .pipe(takeUntil(this.destroy$))
      .subscribe(res => {
        this.totalReglements.set(Number(res.headers.get('X-Total-Count') ?? 0));
        this.reglements.set(res.body ?? []);
      });
  }

  changeReglementsPage(page: number): void {
    this.reglementsPage.set(page);
    this.loadReglements();
  }

  // ── Avoirs ──

  loadAvoirs(): void {
    if (!this.customer()?.id) {
      return;
    }
    this.customerService
      .avoirsByCustomer(this.customer().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({next: avoirs => this.avoirs.set(avoirs), error: () => undefined});
  }

  openAvoirPdf(avoirId: number): void {
    window.open(`/api/sales/retours/avoirs/${avoirId}/pdf`, '_blank');
  }

  // ── Outils ──

  private loadSynthese(): void {
    this.customerService
      .synthese(this.customer().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe(synthese => this.synthese.set(synthese));
  }

  private reloadCustomer(): void {
    this.customerService
      .find(this.customer().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe(res => {
        if (res.body) {
          this.customer.set(res.body);
        }
      });
    this.loadSynthese();
  }

  private ageDe(dateNaissance?: string): number | null {
    if (!dateNaissance) {
      return null;
    }
    const naissance = new Date(dateNaissance);
    const aujourdHui = new Date();
    let age = aujourdHui.getFullYear() - naissance.getFullYear();
    const anniversairePasse =
      aujourdHui.getMonth() > naissance.getMonth() ||
      (aujourdHui.getMonth() === naissance.getMonth() && aujourdHui.getDate() >= naissance.getDate());
    if (!anniversairePasse) {
      age--;
    }
    return age;
  }

  /** Aujourd'hui décalé de `mois` mois. */
  private ngbDate(mois: number): NgbDateStruct {
    const d = new Date();
    d.setMonth(d.getMonth() + mois);
    return {year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate()};
  }

}
