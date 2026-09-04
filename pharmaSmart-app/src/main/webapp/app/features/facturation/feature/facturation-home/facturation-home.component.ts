import { Component, computed, DestroyRef, effect, inject, OnInit, signal, untracked, ChangeDetectionStrategy, input} from "@angular/core";
import { HintComponent } from 'app/shared/ui/hint/hint.component';
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { catchError, finalize, switchMap, tap } from "rxjs/operators";
import { forkJoin, of, Subject } from "rxjs";
import { FormsModule } from "@angular/forms";
import { NgbDateStruct } from "@ng-bootstrap/ng-bootstrap";
import {
  AppPillOption,
  ButtonComponent,
  FloatLabelComponent,
  MultiSelectComponent,
  PillSelectorComponent,
  SelectComponent,
  ToolbarComponent
} from "../../../../shared/ui";
import { PharmaDatePickerComponent } from "../../../../shared/date-picker/pharma-date-picker.component";

import { NGB_DATE_TO_ISO, TODAY_NGB_DATE } from "../../../../shared/util/warehouse-util";
import { INVOICES_STATUT } from "../../../../shared/constants/data-constants";
import { CodeValue } from "../../../../shared/code-value";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { ITiersPayant } from "../../../../shared/model";
import { IGroupeTiersPayant } from "../../../../shared/model/groupe-tierspayant.model";
import { TiersPayantService } from "../../../../entities/tiers-payant/tierspayant.service";
import { GroupeTiersPayantService } from "../../../../entities/groupe-tiers-payant/groupe-tierspayant.service";

import { AbilityService } from "app/core/auth/ability.service";
import { FacturationStore } from "../../data-access/store/facturation.store";
import { FactureApiService } from "../../data-access/services/facture-api.service";
import { IFacture, IFactureKpiParams, IInvoiceSearchParams, TypeFacture } from "../../data-access/models";
import { FactureKpiBannerComponent } from "../../ui/facture-kpi-banner/facture-kpi-banner.component";
import { FactureListComponent } from "../../ui/facture-list/facture-list.component";
import { FactureDetailPanelComponent } from "../../ui/facture-detail-panel/facture-detail-panel.component";
import { TranslateService } from "@ngx-translate/core";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";

@Component({
  selector: "app-facturation-home",
  imports: [
    HintComponent,
    FormsModule,
    ButtonComponent,
    FloatLabelComponent,
    MultiSelectComponent,
    PharmaDatePickerComponent,
    PillSelectorComponent,
    SelectComponent,
    ToolbarComponent,
    FactureKpiBannerComponent,
    FactureListComponent,
    FactureDetailPanelComponent
  ],
  templateUrl: "./facturation-home.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: "./facturation-home.component.scss"
})
export class FacturationHomeComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit le libellé du menu — ou son `titre_long`
   * quand la barre nomme plus longuement. Un écran atteint depuis deux menus affiche donc le nom
   * de celui par lequel on est entré.
   */
  readonly navCode = input<string>('');

  // Store & computed
  protected readonly store = inject(FacturationStore);
  protected readonly panelOpen = computed(() => this.store.panelOpen());
  protected readonly requestedDetailTab = signal<string | null>(null);

  private readonly ability = inject(AbilityService);
  protected readonly canExecute = this.ability.canSignal('execute', 'factures');
  protected readonly canDelete  = this.ability.canSignal('delete',  'factures');
  protected readonly canExport  = this.ability.canSignal('export',  'factures');

  // Toolbar state
  protected readonly statutOptions: CodeValue[] = INVOICES_STATUT;
  protected readonly minLength = 2;
  /** Individuelles ou groupées : les deux familles n'ont ni les mêmes colonnes ni les mêmes
   * jointures côté serveur, il n'y a donc pas de « Toutes » ici. */
  protected typeFacture: TypeFacture = 'INDIVIDUAL';
  protected readonly typeFactureOptions: AppPillOption[] = [
    { label: 'Individuelles', value: 'INDIVIDUAL', icon: 'pi pi-file' },
    { label: 'Groupées', value: 'GROUPED', icon: 'pi pi-copy' }
  ];

  /** `null` = définitives ET provisoires : le paramètre n'est alors pas envoyé. */
  protected factureProvisoire: boolean | null = false;
  protected readonly factureProvisoireOptions: AppPillOption[] = [
    { label: 'Définitives', value: false },
    { label: 'Provisoires', value: true }
  ];

  /** Le type pilote aussi le filtre affiché (tiers-payant ou groupe) et les indicateurs. */
  protected get factureGroupees(): boolean {
    return this.typeFacture === 'GROUPED';
  }
  protected readonly deleteAllSpinner = signal(false);
  protected readonly modelStartDate = signal<NgbDateStruct | undefined>(undefined);
  protected modelEndDate: NgbDateStruct = TODAY_NGB_DATE();
  protected search = "";
  protected selectedStatut: string | null = null;
  protected readonly tiersPayants = signal<ITiersPayant[]>([]);
  protected readonly selectedTiersPayants = signal<ITiersPayant[]>([]);
  protected readonly groupeTiersPayants = signal<IGroupeTiersPayant[]>([]);
  protected readonly selectedGroupeTiersPayants = signal<IGroupeTiersPayant[]>([]);
  protected readonly loadingExport = signal(false);

  // Signal transmis à la liste pour déclencher la recherche
  protected readonly currentSearchParams = signal<IInvoiceSearchParams | null>(null);

  /**
   * Demandes de rechargement des indicateurs.
   *
   * <p>Passer par un sujet plutôt que d'appeler l'API directement sert à une chose : une
   * suppression en lot de N factures signale N mutations, dont les réponses arrivent dans des
   * tours de boucle distincts. Le `switchMap` de l'abonnement annule alors la demande encore en
   * vol, de sorte que la bannière ne peut pas afficher le résultat d'une requête périmée arrivée
   * en retard.
   */
  private readonly kpiReload$ = new Subject<void>();

  // Hint premier usage
  private readonly translate = inject(TranslateService);
  private readonly factureApiService = inject(FactureApiService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly downloadDocumentService = inject(BlobDownloadService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.translate.use("fr");

    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    this.modelStartDate.set({ year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() });

    this.kpiReload$
      .pipe(
        // Pas de `debounceTime` : l'appel doit partir immédiatement, sans quoi il devient
        // impossible de le suivre dans l'onglet Réseau. `switchMap` suffit à tenir la rafale
        // d'une suppression en lot — la demande précédente est annulée, la dernière gagne.
        // Les filtres sont relus ICI, donc au moment de l'appel : la requête part avec l'état
        // réellement affiché.
        switchMap(() =>
          this.factureApiService.getKpi(this.buildKpiParams()).pipe(
            catchError((err: unknown) => {
              // Une erreur avalée en silence laissait la bannière vide sans rien dire — donc
              // indiscernable d'un appel qui n'aurait jamais eu lieu.
              this.notificationService.error(
                this.errorService.getErrorMessage(err),
                "Indicateurs de facturation"
              );
              return of(null);
            })
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(res => this.store.setKpi(res?.body ?? null));

    // Les indicateurs sont faux dès qu'une facture est supprimée ou réglée, y compris depuis
    // la liste ou le panneau de détail — qui n'ont aucune raison de connaître la bannière.
    // Le store porte le signalement, cet effet recharge. Le premier passage est ignoré :
    // `ngOnInit` déclenche déjà la recherche, qui charge les indicateurs.
    let premierPassage = true;
    effect(() => {
      this.store.kpiDirty();
      if (premierPassage) {
        premierPassage = false;
        return;
      }
      // `untracked` : sans lui, l'écriture de `kpiLoading` par `loadKpi` referait de cet effet
      // sa propre dépendance, donc une boucle.
      untracked(() => this.loadKpi());
    });
  }

  ngOnInit(): void {
    this.onSearch();
  }

  onCreateAvoir(facture: IFacture): void {
    this.requestedDetailTab.set('avoirs');
    this.store.selectFacture(facture);
  }

  onSearch(): void {
    this.currentSearchParams.set(this.buildSearchParams());
    // La bannière doit parler de la MÊME période que la liste : sans cet appel, elle restait
    // sur les indicateurs du chargement initial pendant que les filtres, eux, bougeaient.
    this.loadKpi();
  }

  onTypeFactureChange(): void {
    this.selectedTiersPayants.set([]);
    this.selectedGroupeTiersPayants.set([]);
    this.onSearch();
  }

  onExport(): void {
    this.loadingExport.set(true);
    this.factureApiService
      .exportExcel(this.buildSearchParams())
      .pipe(finalize(() => (this.loadingExport.set(false))), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          this.downloadDocumentService.downloadExcel(blob, "factures");

        },
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Export Excel")
      });
  }

  onDeleteSelected(): void {
    const selected = this.store.selectedFactures();
    this.confirmDialog.onConfirm(
      () => this.deleteAll(selected),
      "Suppression",
      `Supprimer les ${selected.length} facture(s) sélectionnée(s) ?`
    );
  }



  searchTiersPayant(query: string): void {
    this.tiersPayantService
      .query({ page: 0, search: query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => (this.tiersPayants.set(res.body ?? [])));
  }

  searchGroupTiersPayant(query: string): void {
    this.groupeTiersPayantService
      .query({ page: 0, search: query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => (this.groupeTiersPayants.set(res.body ?? [])));
  }

  private deleteAll(selected: IFacture[]): void {
    const deletable = selected.filter(f => f.factureItemId && f.statut === 'NOT_PAID');
    if (!deletable.length) {
      this.store.clearSelection();
      return;
    }

    this.deleteAllSpinner.set(true);

    // Chaque requête gère sa propre erreur → forkJoin peut toujours se terminer
    const deletes$ = deletable.map(f =>
      this.factureApiService.delete(f.factureItemId!).pipe(
        tap(() => this.store.removeFactureFromList(f.factureItemId!.id)),
        catchError(err => {
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Suppression');
          return of(null); // ne pas bloquer forkJoin sur une erreur partielle
        }),
      ),
    );

    // PAS de takeUntilDestroyed : on laisse les requêtes HTTP se terminer
    // naturellement pour éviter "message channel closed before a response was received"
    forkJoin(deletes$)
      .pipe(finalize(() => (this.deleteAllSpinner.set(false))))
      .subscribe({
        next: () => {
          this.store.clearSelection();
          this.notificationService.success(`${deletable.length} facture(s) supprimée(s)`);
        },
      });
  }

  private loadKpi(): void {
    this.store.setKpiLoading(true);
    this.kpiReload$.next();
  }

  /**
   * Filtres transmis aux indicateurs.
   *
   * <p>L'appel se faisait auparavant sans aucun paramètre : le serveur retombait alors sur le
   * mois calendaire en cours, alors que l'écran s'ouvre sur le mois glissant précédent. La
   * bannière et la liste ne parlaient donc pas de la même période.
   */
  private buildKpiParams(): IFactureKpiParams {
    const params: IFactureKpiParams = {
      fromDate: NGB_DATE_TO_ISO(this.modelStartDate()),
      toDate: NGB_DATE_TO_ISO(this.modelEndDate),
      typeFacture: this.typeFacture
    };


    if (this.factureProvisoire !== null) {
      params.factureProvisoire = this.factureProvisoire;
    }


    const tiersPayants = this.selectedTiersPayants();
    if (!this.factureGroupees && tiersPayants.length === 1) {
      params.organismeId = tiersPayants[0].id;
    }
    const groupes = this.selectedGroupeTiersPayants();
    if (this.factureGroupees && groupes.length === 1) {
      params.groupeId = groupes[0].id;
    }
    return params;
  }

  private buildSearchParams(): IInvoiceSearchParams {
    const params: IInvoiceSearchParams = {
      startDate: NGB_DATE_TO_ISO(this.modelStartDate()),
      endDate: NGB_DATE_TO_ISO(this.modelEndDate),
      search: this.search || undefined,
      typeFacture: this.typeFacture,
      groupIds: this.selectedGroupeTiersPayants().map(g => g.id),
      tiersPayantIds: this.selectedTiersPayants().map(t => t.id)
    };
    // Omettre le paramètre plutôt que d'envoyer `null` : c'est son absence qui vaut
    // « définitives ET provisoires » côté serveur.
    if (this.factureProvisoire !== null) {
      params.factureProvisoire = this.factureProvisoire;
    }
    if (this.selectedStatut) {
      params.statuts = [this.selectedStatut];
    }
    return params;
  }
}
