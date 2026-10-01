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
} from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ICustomer } from "app/shared/model/customer.model";
import { IAvoirClientDocument } from "app/shared/model/avoir-client-document.model";
import { IClientTiersPayant } from "app/shared/model/client-tiers-payant.model";
import { CustomerService } from "./customer.service";
import { HttpErrorResponse } from "@angular/common/http";
import { MagasinService } from "../magasin/magasin.service";
import { IMagasin } from "app/shared/model/magasin.model";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { NgbModal, NgbNavModule, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { FormsModule } from "@angular/forms";
import {
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  DetailFieldComponent,
  DetailGridComponent,
  DetailSectionComponent,
  NavTabsComponent
} from "../../shared/ui";
import { CommonModule } from "@angular/common";
import { ErrorService } from "../../shared/error.service";
import { currencySymbol } from "../../shared/utils/format-utils";
import { AbilityService } from "app/core/auth/ability.service";
import { BlobDownloadService } from "app/shared/services/blob-download.service";
import { ICustomerSynthese, IDossierSante, ITraitementChronique } from "./customer-fiche.model";
import { IS_ISO_DATE_PAST } from "../../shared/util/warehouse-util";
import { NotificationService } from "../../shared/services/notification.service";
import { NgbConfirmDialogService } from "../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { DossierSanteTabComponent } from "./dossier-sante/dossier-sante-tab.component";
import { TraitementsChroniquesComponent } from "./traitements-chroniques/traitements-chroniques.component";
import {
  A_RELANCER,
  echeanceTraitement,
  libelleTraitement,
  SUIVI_TRAITEMENT
} from "./traitements-chroniques/suivi-traitement";
import { ConsentementsComponent } from "./consentements/consentements.component";
import { showCommonModal } from "../sales/selling-home/sale-helper";
import { AssureFormStepComponent } from "./assure-form-step/assure-form-step.component";
import { UninsuredCustomerFormComponent } from "./uninsured-customer-form/uninsured-customer-form.component";
import { CustomerAchatsTabComponent } from "./achats-tab/customer-achats-tab.component";
import { CustomerProduitsTabComponent } from "./produits-delivres-tab/customer-produits-tab.component";
import { CustomerCouvertureTabComponent } from "./couverture-tab/customer-couverture-tab.component";
import { CustomerCreditTabComponent } from "./credit-tab/customer-credit-tab.component";
import { CustomerAvoirsTabComponent } from "./avoirs-tab/customer-avoirs-tab.component";


@Component({
  selector: "app-customer-detail",
  templateUrl: "./customer-detail.component.html",
  styleUrls: ["./customer-detail.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    NgbNavModule,
    NgbTooltip,
    FormsModule,
    BadgeComponent,
    ButtonComponent,
    NavTabsComponent,
    CommonModule,
    DataTableComponent,
    DetailGridComponent,
    DetailSectionComponent,
    DetailFieldComponent,
    DossierSanteTabComponent,
    TraitementsChroniquesComponent,
    ConsentementsComponent,
    CustomerAchatsTabComponent,
    CustomerProduitsTabComponent,
    CustomerCouvertureTabComponent,
    CustomerCreditTabComponent,
    CustomerAvoirsTabComponent
  ]
})
export class CustomerDetailComponent implements OnInit, OnDestroy {
  /** Renseigné quand la fiche est le panneau de détail de la liste ; sinon, lue sur la route. */
  readonly customerInput = input<ICustomer | null>(null, { alias: "customer" });
  readonly closePanel = output<void>();
  /** La fiche a modifié le client : la liste se recharge. */
  readonly customerChanged = output<void>();
  protected readonly panelMode = computed(() => this.customerInput() !== null);

  protected readonly devise = currencySymbol();

  protected readonly customer = signal<ICustomer | null>(null);
  protected readonly synthese = signal<ICustomerSynthese | null>(null);
  protected readonly magasin = signal<IMagasin | undefined>(undefined);

  // ── Avoirs : juste de quoi afficher la pastille de l'onglet ; l'onglet charge lui-même sa liste ──
  protected readonly avoirs = signal<IAvoirClientDocument[]>([]);
  protected readonly avoirsOuverts = computed(() => this.avoirs().filter(a => a.statut === "OUVERT"));
  protected readonly soldeTotalAvoirs = computed(() => this.avoirsOuverts().reduce((sum, a) => sum + (a.montant ?? 0), 0));

  // ── En-tête ──
  protected readonly estAssure = computed(() => this.customer()?.categorie === "ASSURE");
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

  protected activeTab = "synthese";
  protected activatedRoute = inject(ActivatedRoute);
  protected customerService = inject(CustomerService);
  protected magasinService = inject(MagasinService);
  private readonly modalService = inject(NgbModal);
  private readonly ability = inject(AbilityService);
  private readonly notificationService = inject(NotificationService);
  private readonly blobDownloadService = inject(BlobDownloadService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly errorService = inject(ErrorService);
  protected readonly canEdit = this.ability.canSignal("edit", "customer");
  protected readonly canDonneesPersonnelles = this.ability.canSignal("execute", "pr-donnees-personnelles-client");
  protected readonly canOpenDifferes = this.ability.canSignal("access", "differes");
  // Même périmètre que le serveur : le comptoir renseigne aussi le dossier santé.
  protected readonly canEditSante = computed(
    () => this.canEdit() || this.ability.can("edit", "nouvelle-vente") || this.ability.can("edit", "nouvelle-prevente") || this.ability.can("edit", "ventes")
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
      this.activatedRoute.data.pipe(takeUntil(this.destroy$)).subscribe(({ customer }) => this.ouvrir(customer));
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  previousState(): void {
    window.history.back();
  }

  /** Remet la fiche à zéro pour un client, puis charge l'en-tête. */
  private ouvrir(customer: ICustomer): void {
    this.customer.set(customer);
    this.activeTab = "synthese";
    this.synthese.set(null);
    this.dossierSante.set(null);
    this.traitements.set([]);
    this.avoirs.set([]);
    this.loadSynthese();
    this.loadAvoirs();
    this.customerService
      .dossierSante(customer.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: dossier => this.dossierSante.set(dossier),
        error: this.signaler("Le dossier santé n'a pas pu être chargé")
      });
    this.customerService
      .traitementsChroniques(customer.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: traitements => this.traitements.set(traitements),
        error: this.signaler("Les traitements chroniques n'ont pas pu être chargés")
      });
  }

  /** L'encours de l'en-tête mène à l'onglet Crédit. */
  voirCredit(): void {
    this.activeTab = "credit";
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
        { entity: customer, header: `FORMULAIRE DE MODIFICATION DE CLIENT  [ ${customer.fullName}  ]` },
        onSaved,
        "xl",
        "modal-dialog-80"
      );
    } else {
      showCommonModal(
        this.modalService,
        UninsuredCustomerFormComponent,
        { entity: customer, title: `FORMULAIRE DE MODIFICATION DE CLIENT  [ ${customer.fullName}  ]` },
        onSaved,
        "xl"
      );
    }
  }

  // ── Avoirs : pastille de l'onglet ──

  private loadAvoirs(): void {
    if (!this.customer()?.id) {
      return;
    }
    this.customerService
      .avoirsByCustomer(this.customer().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: avoirs => this.avoirs.set(avoirs),
        error: this.signaler("Les avoirs n'ont pas pu être chargés")
      });
  }

  // ── Données personnelles ──

  /** Droit d'accès : tout ce que l'officine détient sur le client, en JSON. */
  exporterDonnees(): void {
    this.customerService
      .donneesPersonnelles(this.customer().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: blob => this.blobDownloadService.download(blob, `donnees-client-${this.customer().code}`, "json"),
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
              this.notificationService.success("Les données du client ont été effacées");
              this.reloadCustomer();
              this.dossierSante.set(null);
            },
            error: this.signaler("L'effacement a échoué")
          }),
      "EFFACEMENT DES DONNÉES",
      "Nom, téléphone, e-mail, date de naissance, numéros d'assuré et dossier santé seront définitivement effacés, et la fiche désactivée. " +
      "L'historique des ventes est conservé, sans identité. Continuer ?"
    );
  }

  // ── Outils ──

  private loadSynthese(): void {
    this.customerService
      .synthese(this.customer().id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: synthese => this.synthese.set(synthese),
        error: this.signaler("La synthèse du client n'a pas pu être chargée")
      });
  }

  /** Après une modification de tiers payant/ayant droit depuis l'onglet Couverture. */
  protected reloadCustomer(): void {
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


}
