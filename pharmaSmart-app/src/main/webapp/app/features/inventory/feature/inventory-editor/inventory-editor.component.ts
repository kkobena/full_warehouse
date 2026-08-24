import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  OnInit,
  signal
} from "@angular/core";
import {CommonModule} from "@angular/common";
import {ActivatedRoute, Router} from "@angular/router";
import {NgbModal, NgbTooltip} from "@ng-bootstrap/ng-bootstrap";
import {NotificationService} from "../../../../shared/services/notification.service";
import {takeUntilDestroyed, toObservable} from "@angular/core/rxjs-interop";
import {filter, interval} from "rxjs";
import {InventoryApiService} from "../../data-access/services/inventory-api.service";
import {InventoryEditorFacade} from "../../data-access/facades/inventory-editor.facade";
import {InventoryListFacade} from "../../data-access/facades/inventory-list.facade";
import {InventoryStore} from "../../data-access/store/inventory.store";
import {
  InventoryProgressBarComponent
} from "../../ui/inventory-progress-bar/inventory-progress-bar.component";
import {
  InventoryLinesGridComponent
} from "../../ui/inventory-lines-grid/inventory-lines-grid.component";
import {
  InventoryImportModalComponent
} from "../../ui/inventory-import-modal/inventory-import-modal.component";
import {InventoryLotGridComponent} from "../../ui/inventory-lot-grid/inventory-lot-grid.component";
import {GapSummaryComponent} from "../../ui/gap-summary/gap-summary.component";
import {
  InventoryValuationComponent
} from "../../ui/inventory-valuation/inventory-valuation.component";
import {FormsModule} from "@angular/forms";
import {
  IInventoryLine,
  INVENTORY_CATEGORIES,
  InventoryEvent,
  InventoryLineFilter,
  isGapLineFilter
} from "../../models";
import {StorageService} from "../../../../entities/storage/storage.service";
import {RayonService} from "../../../../entities/rayon/rayon.service";
import {IStorage} from "../../../../shared/model/magasin.model";
import {IRayon} from "../../../../shared/model/rayon.model";
import {HasAuthorityService} from "../../../../entities/sales/service/has-authority.service";
import {Authority} from "../../../../shared/constants/authority.constants";
import {ConfigurationService} from "../../../../shared/configuration.service";
import {InventoryCategoryType} from "../../../../shared/model/store-inventory.model";
import {ButtonComponent, SelectComponent, ToolbarComponent} from "../../../../shared/ui";
import {AbilityService} from "app/core/auth/ability.service";
import {BlobDownloadService} from "../../../../shared/services/blob-download.service";

@Component({
  selector: "app-inventory-editor",
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    ToolbarComponent,
    SelectComponent,
    NgbTooltip,
    InventoryProgressBarComponent,
    InventoryLinesGridComponent,
    InventoryLotGridComponent,
    GapSummaryComponent,
    InventoryValuationComponent
  ],
  templateUrl: "./inventory-editor.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: "./inventory-editor.component.scss"
})
export class InventoryEditorComponent implements OnInit {
  /** Intervalle de scrutation de la progression (supervision du comptage mobile) */
  private static readonly PROGRESS_POLL_MS = 15_000;
  readonly editorFacade = inject(InventoryEditorFacade);
  readonly listFacade = inject(InventoryListFacade);
  readonly store = inject(InventoryStore);
  inventoryId = signal<number>(0);
  storages = signal<IStorage[]>([]);
  rayons = signal<IRayon[]>([]);
  gestionLot = signal(false);
  selectedLine = signal<IInventoryLine | null>(null);
  page = signal(0);
  size = signal(100);
  readonly pageSizeOptions = [10, 20, 50, 100];
  readonly totalPages = computed(() => {
    const total = this.gestionLot() ? this.store.lotTotalLines() : this.store.totalLines();
    return Math.max(1, Math.ceil(total / this.size()));
  });
  readonly isFirstPage = computed(() => this.page() === 0);
  readonly isLastPage = computed(() => this.page() >= this.totalPages() - 1);
  /** Afficher la colonne seuil mini pour SOUS_SEUIL et EN_RUPTURE */
  readonly showSeuilMini = computed(() => {
    const cat = this.store.currentInventory()?.inventoryCategory;
    const name: string | undefined = typeof cat === "string" ? cat : cat?.name;
    return name === "SOUS_SEUIL" || name === "EN_RUPTURE";
  });
  exporting = signal(false);
  /** Comptages détectés sur un autre poste alors qu'une saisie est en cours ici */
  protected readonly remoteCountsPending = signal(false);
  private lastSeenUpdatedLines: number | null = null;
  /** Comptages locaux pas encore reflétés dans la progression lue — cf. onProgressChanged */
  private pendingLocalCredit = 0;
  private selectedLineFilter: InventoryLineFilter = "NONE";
  private selectedStorageId: number | null = null;
  private selectedRayonId: number | null = null;
  private selectedSearch = "";
  /** Onglet d'où provient l'ouverture — cf. {@link resolveSourceTab}. */
  private sourceTab: string | null = null;
  private readonly route = inject(ActivatedRoute);
  private readonly blobDownloadService = inject(BlobDownloadService);
  private readonly router = inject(Router);
  private readonly modal = inject(NgbModal);
  private readonly notificationService = inject(NotificationService);
  private readonly storageService = inject(StorageService);
  private readonly rayonService = inject(RayonService);
  private readonly hasAuthority = inject(HasAuthorityService);
  /** Blind mode driven by privilege — ADMIN et STORE_INVENTORY voient le stock */
  readonly blindMode = computed(() =>
    !this.hasAuthority.hasAuthorities([Authority.PR_VOIR_STOCK_INVENTAIRE])
  );
  private readonly ability = inject(AbilityService);
  /** La clôture (irréversible) est réservée aux rôles disposant du privilège ACTION */
  protected readonly canCloseInventory = this.ability.canSignal("execute", "pr-cloture-inventaire");
  private readonly configService = inject(ConfigurationService);
  private readonly inventoryApi = inject(InventoryApiService);
  private readonly destroyRef = inject(DestroyRef);
  private lastEvent$ = toObservable(this.store.lastEvent).pipe(
    filter((e): e is InventoryEvent => e !== null)
  );

  constructor() {
    effect(() => {
      const err = this.store.error();
      if (!err) {
        return;
      }
      this.notificationService.error(err, "Erreur");
      this.store.setError(null);
    });
    effect(() => this.onProgressChanged());
  }

  protected get isInventoryClosed(): boolean {
    return this.store.currentInventory()?.statut === "CLOSED";
  }

  protected get getInventoryCategoryType(): InventoryCategoryType {
    return this.store.currentInventory()?.inventoryCategory?.name;
  }

  ngOnInit(): void {
    this.sourceTab = this.route.snapshot.queryParamMap.get("tab");
    const id = Number(this.route.snapshot.paramMap.get("id"));
    if (!id) {
      this.router.navigate(["/inventaire"]);
      return;
    }
    this.inventoryId.set(id);
    this.store.resetEditor();
    this.listFacade.loadInventory(id);
    // Le premier chargement attend le mode de saisie : sans cela il partirait sur
    // /v2 (mode produit par défaut) avant que la config n'impose /lots.
    // La progression est rafraîchie par `loadLines`, qu'enchaîne `loadGestionLot`.
    this.loadGestionLot();
    this.loadStorages();

    this.subscribeToEvents();
    this.watchRemoteProgress();
  }

  /** Rafraîchissement manuel après des comptages distants signalés */
  protected refreshAfterRemoteCounts(): void {
    this.loadLines();
  }

  /**
   * Charge une page de lignes. Seul point de rechargement de la grille : il n'est atteint
   * qu'aux transitions (page suivante ou précédente, taille de page, filtre, import,
   * rafraîchissement manuel), jamais pendant la saisie d'une page.
   */
  protected loadLines(): void {
    // Le bandeau a rempli son office : la page qu'on charge porte les comptages distants.
    this.remoteCountsPending.set(false);
    // La progression suit le même rythme que la grille — une fois par page, au lieu
    // d'une fois par ligne comptée.
    this.editorFacade.refreshProgress(this.inventoryId());
    const params: any = {
      page: this.page(),
      size: this.size(),
      storeInventoryId: this.inventoryId()
    };
    if (this.selectedLineFilter !== "NONE") {
      params.selectedFilter = this.selectedLineFilter;
    }
    if (this.selectedStorageId) {
      params.storageId = this.selectedStorageId;
    }
    if (this.selectedRayonId) {
      params.rayonId = this.selectedRayonId;
    }
    if (this.selectedSearch) {
      params.search = this.selectedSearch;
    }
    if (this.gestionLot()) {
      this.editorFacade.loadLotLines(this.inventoryId(), params);
    } else {
      this.editorFacade.loadLines(this.inventoryId(), params);
    }
  }

  protected onGridFilterChange(event: {
    lineFilter: InventoryLineFilter;
    storageId: number | null;
    rayonId: number | null;
    search: string;
  }): void {
    // Garde-fou : la grille ne propose plus les filtres d'écart en mode aveugle,
    // mais l'appel serveur ne doit pas non plus pouvoir les porter
    this.selectedLineFilter =
      this.blindMode() && isGapLineFilter(event.lineFilter) ? "NONE" : event.lineFilter;
    this.selectedStorageId = event.storageId;
    this.selectedRayonId = event.rayonId;
    this.selectedSearch = event.search ?? "";
    this.page.set(0);
    this.loadLines();
  }

  protected onPageChange(delta: number): void {
    const next = this.page() + delta;
    if (next >= 0 && next < this.totalPages()) {
      this.page.set(next);
      this.loadLines();
    }
  }

  /**
   * Fin de la page atteinte en saisie continue.
   *
   * Sous un filtre dont le comptage modifie l'appartenance, la page courante n'a pas
   * le même contenu qu'au moment où elle a été ouverte : chaque ligne comptée en sort
   * et le serveur la re-remplit par le bas. Avancer d'une page sauterait alors tout ce
   * qui vient d'y remonter — avec 20 lignes par page, les lignes 21 à 40 étaient perdues
   * pour cause de passage direct de la page 0 à la page 1. On recharge donc la page
   * courante, qui porte déjà les lignes restantes.
   */
  protected onNextPage(): void {
    if (this.filterHidesCountedLines()) {
      this.loadLines();
      return;
    }
    const next = this.page() + 1;
    if (next < this.totalPages()) {
      this.page.set(next);
      this.loadLines();
    }
  }

  protected onPageSizeChange(newSize: number): void {
    this.size.set(newSize);
    this.page.set(0);
    this.loadLines();
  }

  protected onGridStorageChange(storageId: number | null): void {
    this.rayons.set([]);
    if (storageId) {
      this.rayonService
        .query({storageId, size: 9999})
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({next: resp => this.rayons.set(resp.body ?? [])});
    }
  }

  protected openImportModal(): void {
    const ref = this.modal.open(InventoryImportModalComponent, {
      size: "xl",
      backdrop: "static",
      centered: true
    });
    ref.componentInstance.inventoryId = this.inventoryId();
    ref.result.then(() => this.loadLines(), () => {
    });
  }

  /**
   * Clôture précédée d'un récapitulatif (lignes restantes, écarts valorisés) :
   * l'action est irréversible, elle ne doit pas se confirmer à l'aveugle.
   */
  protected closeInventory(): void {
    import("../../ui/inventory-close-modal/inventory-close-modal.component").then(m => {
      const ref = this.modal.open(m.InventoryCloseModalComponent, {
        size: "xl",
        backdrop: "static",
        centered: true
      });
      ref.componentInstance.inventoryId = this.inventoryId();
      ref.result.then(
        confirmed => {
          if (confirmed) {
            this.listFacade.closeInventory(this.inventoryId());
          }
        },
        () => {
          // fermeture / annulation
        }
      );
    });
  }

  protected exportPdf(): void {
    this.exporting.set(true);
    const filterParams: Record<string, any> = {};
    if (this.selectedSearch) {
      filterParams["search"] = this.selectedSearch;
    }
    if (this.selectedStorageId) {
      filterParams["storageId"] = this.selectedStorageId;
    }
    if (this.selectedRayonId) {
      filterParams["rayonId"] = this.selectedRayonId;
    }
    if (this.selectedLineFilter !== "NONE") {
      filterParams["selectedFilter"] = this.selectedLineFilter;
    }
    this.inventoryApi.exportToPdf(this.inventoryId(), "RAYON", filterParams, this.gestionLot()).subscribe({
      next: blob => {
        this.blobDownloadService.downloadPdf(blob, `inventaire-${this.inventoryId()}`);
        this.exporting.set(false);
      },
      error: () => {
        this.exporting.set(false);
        this.notificationService.error("Échec de l'export PDF", "Erreur");
      }
    });
  }

  protected openGapAnalysis(): void {
    import("../../ui/gap-analysis-modal/gap-analysis-modal.component").then(m => {
      const ref = this.modal.open(m.GapAnalysisModalComponent, {
        size: "xl",
        backdrop: "static"
        , centered: true
      });
      ref.componentInstance.inventoryId = this.inventoryId();
    });
  }

  protected goBack(): void {
    this.router.navigate(["/inventaire"], {queryParams: {tab: this.resolveSourceTab()}});
  }

  protected getCategoryLabel(cat: any): string {
    const name = typeof cat === "string" ? cat : cat?.name;
    return INVENTORY_CATEGORIES.find(c => c.value === name)?.label ?? name ?? "-";
  }

  protected onLineSelected(line: IInventoryLine | null): void {
    this.selectedLine.set(line);
  }

  protected onLotGridClose(): void {
    this.selectedLine.set(null);
  }

  protected onLotUpdated(): void {
    this.editorFacade.refreshProgress(this.inventoryId());
  }

  /**
   * Supervision du comptage mobile : scrute la progression et rafraîchit la grille
   * quand des comptages arrivent d'un autre poste.
   *
   * Règle : on ne recharge jamais pendant une saisie locale — cela ferait perdre les
   * valeurs en cours de frappe. Dans ce cas un indicateur invite à rafraîchir.
   */
  private watchRemoteProgress(): void {
    interval(InventoryEditorComponent.PROGRESS_POLL_MS)
      .pipe(
        filter(() => !this.isInventoryClosed),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.editorFacade.refreshProgress(this.inventoryId()));
  }

  /**
   * Réagit aux variations du nombre de lignes comptées, d'où qu'elles viennent
   * (mobile, autre poste). Déclaré dans le constructeur : `effect` exige un
   * contexte d'injection.
   *
   * Ne recharge **jamais** la grille : le rechargement n'a lieu qu'aux transitions de page
   * ou de filtre (cf. plan « saisie sans rechargement »). Une progression qui bouge en
   * pleine saisie ne fait que lever le bandeau, l'opérateur rafraîchit quand il le décide.
   */
  private onProgressChanged(): void {
    const updated = this.store.progress()?.updatedLines;
    if (updated == null) {
      return;
    }
    const previous = this.lastSeenUpdatedLines;
    this.lastSeenUpdatedLines = updated;
    // Nos propres comptages font bouger la progression : les défalquer, sinon l'écran
    // annoncerait « des comptages depuis un autre poste » à chaque ligne saisie ici.
    const localCredit = this.pendingLocalCredit + this.editorFacade.consumeLocalCounts();
    if (previous === null) {
      this.pendingLocalCredit = 0;
      return;
    }

    const remoteDelta = updated - previous - localCredit;
    if (remoteDelta > 0) {
      this.remoteCountsPending.set(true);
      this.pendingLocalCredit = 0;
      return;
    }
    // Progression lue avant que des sauvegardes en vol n'y soient reflétées : le solde
    // local non encore expliqué est reporté, sans quoi il ferait passer nos propres
    // comptages pour distants au relevé suivant.
    this.pendingLocalCredit = -remoteDelta;
  }

  /**
   * Le filtre courant retire-t-il de la liste une ligne dès qu'elle est comptée ?
   *
   * « Non comptés » évidemment, mais aussi les filtres d'écart : compter une ligne change
   * son écart, donc son appartenance. « Comptés » n'en fait pas partie — il ne montre que
   * des lignes déjà saisies, qu'un nouveau comptage n'en fait pas sortir.
   */
  private filterHidesCountedLines(): boolean {
    return this.selectedLineFilter === "NOT_UPDATED" || isGapLineFilter(this.selectedLineFilter);
  }


  private resolveSourceTab(): string {
    return this.sourceTab ?? (this.isInventoryClosed ? "clotures" : "en-cours");
  }

  /**
   * Résout le mode de saisie (produit ou lot) puis déclenche le chargement initial.
   * Un seul des deux endpoints (`/v2` ou `/lots`) doit être appelé : le mode est
   * exclusif, on ne charge donc rien tant qu'il n'est pas connu.
   */
  private loadGestionLot(): void {
    this.store.setLoadingLines(true);
    this.configService.find("APP_GESTION_LOT_INVENTAIRE")
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: resp => {
          this.gestionLot.set(resp.body?.value === "1");
          this.loadLines();
        },
        error: () => {
          // Config indisponible : on retombe sur la saisie par produit
          this.gestionLot.set(false);
          this.loadLines();
        }
      });
  }

  private loadStorages(): void {
    this.storageService
      .fetchUserStorages()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({next: resp => this.storages.set(resp.body ?? [])});
  }

  private subscribeToEvents(): void {
    this.lastEvent$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event: InventoryEvent) => {
        switch (event.type) {
          case "LINE_SAVED":
            break;
          case "LINE_SAVE_ERROR": {
            // Le message est déjà porté par `store.error()`, que l'effect du constructeur
            // affiche : notifier une seconde fois ici produisait deux toasts par échec.
            const status = (event.payload as any)?.error?.status;
            if (status === 409) {
              // Comptage concurrent : la valeur serveur fait foi, on recharge la page
              this.loadLines();
            }
            break;
          }
          case "BATCH_SAVED": {
            const conflicts = (event.payload as any)?.conflictedIds ?? [];
            if (conflicts.length > 0) {
              this.notificationService.error(
                `${conflicts.length} ligne(s) comptée(s) entre-temps par un autre opérateur — valeurs rechargées`,
                "Comptage concurrent"
              );
              this.loadLines();
            }
            break;
          }
          case "IMPORT_COMPLETED":
            this.notificationService.success("Import CSV terminé", "Import");
            this.loadLines();
            break;
          case "INVENTORY_CLOSED":
            this.notificationService.success("Inventaire clôturé avec succès", "Clôture");
            this.listFacade.loadInventory(this.inventoryId());
            break;
        }
      });
  }
}
