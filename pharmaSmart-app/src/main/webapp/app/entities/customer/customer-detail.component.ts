import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  OnDestroy,
  OnInit,
  output,
  signal,
  untracked
} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {ISales} from 'app/shared/model/sales.model';
import {ISalesLine} from 'app/shared/model/sales-line.model';
import {ICustomer} from 'app/shared/model/customer.model';
import {IAvoirClientDocument} from 'app/shared/model/avoir-client-document.model';
import {IClientTiersPayant} from 'app/shared/model/client-tiers-payant.model';
import {CustomerService} from './customer.service';
import {HttpErrorResponse, HttpResponse} from '@angular/common/http';
import {MagasinService} from '../magasin/magasin.service';
import {IMagasin} from 'app/shared/model/magasin.model';
import {SalesService} from '../sales/sales.service';
import {Observable, Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {NgbDateStruct, NgbModal, NgbNavModule, NgbPagination, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
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
import {ErrorService} from '../../shared/error.service';
import {PharmaDatePickerComponent} from '../../shared/date-picker/pharma-date-picker.component';
import {DeviseDirective} from '../../shared/utils/devise';
import {currencySymbol} from '../../shared/utils/format-utils';
import {AbilityService} from 'app/core/auth/ability.service';
import {BlobDownloadService} from 'app/shared/services/blob-download.service';
import {IDiffere, IReglementDiffere} from 'app/features/differes/data-access/models/differe.model';
import {ICustomerSynthese, IDossierSante, IProduitDelivre, IRelanceDiffere, ISituationCredit, ITraitementChronique} from './customer-fiche.model';
import {IS_ISO_DATE_PAST, NGB_DATE_TO_ISO} from '../../shared/util/warehouse-util';
import {NotificationService} from '../../shared/services/notification.service';
import {NgbConfirmDialogService} from '../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import {DossierSanteTabComponent} from './dossier-sante/dossier-sante-tab.component';
import {TraitementsChroniquesComponent} from './traitements-chroniques/traitements-chroniques.component';
import {A_RELANCER, echeanceTraitement, libelleTraitement, SUIVI_TRAITEMENT} from './traitements-chroniques/suivi-traitement';
import {ConsentementsComponent} from './consentements/consentements.component';
import {showCommonModal} from '../sales/selling-home/sale-helper';
import {AssureFormStepComponent} from './assure-form-step/assure-form-step.component';
import {UninsuredCustomerFormComponent} from './uninsured-customer-form/uninsured-customer-form.component';
import {CustomerTiersPayantComponent} from './customer-tiers-payant/customer-tiers-payant.component';
import {FormAyantDroitComponent} from './form-ayant-droit/form-ayant-droit.component';

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
    NgbTooltip,
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
    CardComponent,
    DataTableComponent,
    DossierSanteTabComponent,
    TraitementsChroniquesComponent,
    ConsentementsComponent
  ]
})
export class CustomerDetailComponent implements OnInit, OnDestroy {
  /** Renseigné quand la fiche est le panneau de détail de la liste ; sinon, lue sur la route. */
  readonly customerInput = input<ICustomer | null>(null, {alias: 'customer'});
  readonly closePanel = output<void>();
  /** La fiche a modifié le client : la liste se recharge. */
  readonly customerChanged = output<void>();
  protected readonly panelMode = computed(() => this.customerInput() !== null);

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
  protected readonly releveFrom = signal<NgbDateStruct>(this.ngbDate(-3));
  protected readonly releveTo = signal<NgbDateStruct>(this.ngbDate(0));
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
  /** Traitements chroniques : alimentent les pastilles de renouvellement de l'en-tête. */
  protected readonly traitements = signal<ITraitementChronique[]>([]);
  protected readonly traitementsARelancer = computed(() => this.traitements().filter(t => A_RELANCER.includes(t.suivi)));
  protected readonly suivis = SUIVI_TRAITEMENT;
  protected readonly libelleTraitement = libelleTraitement;
  protected readonly echeanceTraitement = echeanceTraitement;
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
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly errorService = inject(ErrorService);
  protected readonly canEdit = this.ability.canSignal('edit', 'customer');
  protected readonly canDonneesPersonnelles = this.ability.canSignal('execute', 'pr-donnees-personnelles-client');
  protected readonly canOpenDifferes = this.ability.canSignal('access', 'differes');
  // Même périmètre que le serveur : le comptoir renseigne aussi le dossier santé.
  protected readonly canEditSante = computed(
    () => this.canEdit() || this.ability.can('edit', 'nouvelle-vente') || this.ability.can('edit', 'nouvelle-prevente') || this.ability.can('edit', 'ventes')
  );
  private destroy$ = new Subject<void>();

  constructor() {
    // Panneau de la liste : chaque nouveau client sélectionné recharge la fiche.
    effect(() => {
      const client = this.customerInput();
      if (client?.id && client.id !== this.customer()?.id) {
        untracked(() => this.ouvrir(client));
      }
    });
  }

  ngOnInit(): void {
    this.magasinService.findCurrentUserMagasin().then(magasin => this.magasin.set(magasin));
    if (!this.panelMode()) {
      this.activatedRoute.data.pipe(takeUntil(this.destroy$)).subscribe(({customer}) => this.ouvrir(customer));
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  previousState(): void {
    window.history.back();
  }

  /** Remet la fiche à zéro pour un client, puis charge l'en-tête et l'onglet Achats. */
  private ouvrir(customer: ICustomer): void {
    this.customer.set(customer);
    this.activeTab = 'achats';
    this.produitsCharges = false;
    this.creditCharge = false;
    this.page.set(1);
    this.saleSelected.set(undefined);
    this.selectedRowIndex.set(undefined);
    this.selectedRowSaleLines.set([]);
    this.synthese.set(null);
    this.dossierSante.set(null);
    this.traitements.set([]);
    this.differe.set(null);
    this.situationCredit.set(null);
    this.relances.set([]);
    this.reglements.set([]);
    this.produits.set([]);
    this.loadSynthese();
    this.loadSales();
    this.loadAvoirs();
    this.customerService
      .dossierSante(customer.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({next: dossier => this.dossierSante.set(dossier), error: this.signaler("Le dossier santé n'a pas pu être chargé")});
    this.customerService
      .traitementsChroniques(customer.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({next: traitements => this.traitements.set(traitements), error: this.signaler("Les traitements chroniques n'ont pas pu être chargés")});
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
      .subscribe({
        next: (res: HttpResponse<ISales[]>) => {
          this.totalSales.set(Number(res.headers.get('X-Total-Count') ?? 0));
          this.sales.set(res.body ?? []);
        },
        error: this.signaler("Les achats n'ont pas pu être chargés")
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
        .subscribe({
          next: blob => this.blobDownloadService.downloadPdf(blob, `facture-${sale.numberTransaction}`),
          error: this.signaler("La facture n'a pas pu être imprimée")
        });
    }
  }

  /** Attestation de dépenses sur la période des achats. */
  imprimerAttestation(): void {
    this.customerService
      .attestationDepensesPdf(this.customer().id, NGB_DATE_TO_ISO(this.fromDate()), NGB_DATE_TO_ISO(this.toDate()))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: blob => this.blobDownloadService.downloadPdf(blob, `attestation-depenses-${this.customer().code}`),
        error: this.signaler("L'attestation n'a pas pu être éditée")
      });
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
      .subscribe({
        next: res => {
          this.totalProduits.set(Number(res.headers.get('X-Total-Count') ?? 0));
          this.produits.set(res.body ?? []);
        },
        error: this.signaler("Les produits délivrés n'ont pas pu être chargés")
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
      .subscribe({next: differe => this.differe.set(differe), error: this.signaler("Les différés n'ont pas pu être chargés")});
    this.customerService
      .situationCredit(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({next: situation => this.situationCredit.set(situation), error: this.signaler("La limite de crédit n'a pas pu être chargée")});
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
          this.signaler("La relance n'a pas pu être envoyée")(err);
        }
      });
  }

  imprimerReleve(): void {
    this.customerService
      .relevePdf(this.customer().id, NGB_DATE_TO_ISO(this.releveFrom()), NGB_DATE_TO_ISO(this.releveTo()))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: blob => this.blobDownloadService.downloadPdf(blob, `releve-compte-${this.customer().code}`),
        error: this.signaler("Le relevé n'a pas pu être édité")
      });
  }

  private loadRelances(): void {
    this.customerService
      .relancesDifferes(this.customer().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({next: relances => this.relances.set(relances), error: this.signaler("Les relances n'ont pas pu être chargées")});
  }

  loadReglements(): void {
    this.customerService
      .reglementsDifferes(this.customer().id, {page: this.reglementsPage() - 1, size: this.pageSize})
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: res => {
          this.totalReglements.set(Number(res.headers.get('X-Total-Count') ?? 0));
          this.reglements.set(res.body ?? []);
        },
        error: this.signaler("Les règlements n'ont pas pu être chargés")
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
      .subscribe({next: avoirs => this.avoirs.set(avoirs), error: this.signaler("Les avoirs n'ont pas pu être chargés")});
  }

  openAvoirPdf(avoirId: number): void {
    window.open(`/api/sales/retours/avoirs/${avoirId}/pdf`, '_blank');
  }

  // ── Couverture : tiers payants et ayants droit ──

  /** Quatre tiers payants au plus (R0 à R3), et seulement pour un assuré. */
  protected readonly peutAjouterTiersPayant = computed(() => {
    const client = this.customer();
    return client?.typeTiersPayant === 'ASSURANCE' && (client.tiersPayants?.length ?? 0) < 4;
  });

  protected addTiersPayant(): void {
    showCommonModal(
      this.modalService,
      CustomerTiersPayantComponent,
      {entity: null, customer: this.customer(), title: "FORMULAIRE D'AJOUT DE TIERS PAYANT "},
      (resp: ICustomer) => this.siEnregistre(resp),
      'xl'
    );
  }

  protected editTiersPayant(tiersPayant: IClientTiersPayant): void {
    showCommonModal(
      this.modalService,
      CustomerTiersPayantComponent,
      {entity: tiersPayant, customer: this.customer(), title: `FORMULAIRE DE MODIFICATION DE TIERS PAYANT [ ${tiersPayant.tiersPayantName} ]`},
      (resp: ICustomer) => this.siEnregistre(resp),
      'xl'
    );
  }

  protected removeTiersPayant(tiersPayant: IClientTiersPayant): void {
    this.confirmDialog.onConfirm(
      () => this.appeler(this.customerService.deleteTiersPayant(tiersPayant.id)),
      'SUPPRESSION DE TIERS PAYANT',
      'Voulez-vous vraiment supprimer ce tiers payant ?'
    );
  }

  protected addAyantDroit(): void {
    showCommonModal(
      this.modalService,
      FormAyantDroitComponent,
      {entity: null, assure: this.customer(), title: "FORMULAIRE D'AJOUT D'AYANT DROIT "},
      (resp: ICustomer) => this.siEnregistre(resp),
      'xl'
    );
  }

  protected editAyantDroit(ayantDroit: ICustomer): void {
    showCommonModal(
      this.modalService,
      FormAyantDroitComponent,
      {entity: ayantDroit, assure: this.customer(), title: `FORMULAIRE DE MODIFICATION D'AYANT DROIT [ ${ayantDroit.fullName}  ]`},
      (resp: ICustomer) => this.siEnregistre(resp)
    );
  }

  protected removeAyantDroit(ayantDroit: ICustomer): void {
    this.confirmDialog.onConfirm(
      () => this.appeler(this.customerService.deleteAssuredCustomer(ayantDroit.id)),
      'SUPPRESSION',
      'Voulez-vous vraiment supprimer cet ayant droit ?'
    );
  }

  private siEnregistre(resp: unknown): void {
    if (resp) {
      this.reloadCustomer();
    }
  }

  private appeler(requete: Observable<unknown>): void {
    requete.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.reloadCustomer(),
      error: this.signaler("L'opération a échoué")
    });
  }

  // ── Données personnelles ──

  /** Droit d'accès : tout ce que l'officine détient sur le client, en JSON. */
  exporterDonnees(): void {
    this.customerService
      .donneesPersonnelles(this.customer().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: blob => this.blobDownloadService.download(blob, `donnees-client-${this.customer().code}`, 'json'),
        error: this.signaler("L'export des données a échoué")
      });
  }

  /** Droit à l'effacement : l'identité disparaît, les délivrances restent tracées. */
  confirmerAnonymisation(): void {
    this.confirmDialog.onConfirm(
      () =>
        this.customerService
          .anonymiser(this.customer().id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.notificationService.success('Les données du client ont été effacées');
              this.reloadCustomer();
              this.dossierSante.set(null);
            },
            error: this.signaler("L'effacement a échoué")
          }),
      'EFFACEMENT DES DONNÉES',
      "Nom, téléphone, e-mail, date de naissance, numéros d'assuré et dossier santé seront définitivement effacés, et la fiche désactivée. " +
        "L'historique des ventes est conservé, sans identité. Continuer ?"
    );
  }

  // ── Outils ──

  private loadSynthese(): void {
    this.customerService
      .synthese(this.customer().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({next: synthese => this.synthese.set(synthese), error: this.signaler("La synthèse du client n'a pas pu être chargée")});
  }

  private reloadCustomer(): void {
    this.customerService
      .find(this.customer().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: res => {
          if (res.body) {
            this.customer.set(res.body);
          }
        },
        error: this.signaler("Le client n'a pas pu être rechargé")
      });
    this.loadSynthese();
    this.customerChanged.emit();
  }

  /** Erreur d'un appel : le message du serveur s'il en donne un, sinon l'action qui a échoué. */
  private signaler(action: string): (err: HttpErrorResponse) => void {
    return err => this.notificationService.error(this.errorService.getErrorMessage(err, action));
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
