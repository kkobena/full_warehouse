import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnDestroy,
  OnInit,
  viewChild, signal } from "@angular/core";
import {HttpErrorResponse, HttpHeaders, HttpResponse} from "@angular/common/http";
import {ActivatedRoute, Data, ParamMap, Router} from "@angular/router";
import {combineLatest, Observable, Subject} from "rxjs";
import {takeUntil} from "rxjs/operators";
import {NgbModal, NgbTooltip} from "@ng-bootstrap/ng-bootstrap";
import {ICustomer} from "app/shared/model/customer.model";
import {ITEMS_PER_PAGE} from "app/shared/constants/pagination.constants";
import {CustomerService} from "./customer.service";
import {
  UninsuredCustomerFormComponent
} from "./uninsured-customer-form/uninsured-customer-form.component";
import {FormsModule} from "@angular/forms";
import {AssureFormStepComponent} from "./assure-form-step/assure-form-step.component";
import {CustomerCarnetComponent} from "./carnet/customer-carnet.component";
import {FusionClientModalComponent} from "./fusion-client/fusion-client-modal.component";
import {TraitementsARenouvelerModalComponent} from "./traitements-chroniques/traitements-a-renouveler-modal.component";
import {CustomerDetailComponent} from "./customer-detail.component";
import {IFusionClientResult} from "./customer-fiche.model";
import {showCommonModal} from "../sales/selling-home/sale-helper";
import {SpinnerComponent} from "../../shared/spinner/spinner.component";
import {CommonModule} from "@angular/common";
import {
  NgbConfirmDialogService
} from "../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import {NotificationService} from "../../shared/services/notification.service";
import {ErrorService} from "../../shared/error.service";
import {AbilityService} from "app/core/auth/ability.service";
import { CardComponent } from 'app/shared/ui';
import {
  AppSplitButtonItem,
  AppTableLazyLoadEvent,
  ButtonComponent,
  CheckboxComponent,
  DataTableComponent,
  FloatLabelComponent,
  IconFieldComponent,
  SelectComponent,
  SplitButtonComponent,
  ToolbarComponent
} from "../../shared/ui";
import {
  JsonImportDialogComponent
} from "../../shared/json-import-dialog/json-import-dialog.component";

@Component({
  selector: "app-customer",
  templateUrl: "./customer.component.html",
  styleUrls: ["./customer.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CardComponent, 
    CommonModule,
    FormsModule,
    SpinnerComponent,
    ButtonComponent,
    CheckboxComponent,
    DataTableComponent,
    FloatLabelComponent,
    IconFieldComponent,
      SelectComponent,
    SplitButtonComponent,
    ToolbarComponent,
    NgbTooltip,
    CustomerDetailComponent
  ]
})
export class CustomerComponent implements OnInit, OnDestroy {
  protected readonly customers = signal<ICustomer[] | undefined>(undefined);
  // Carnet et dépôt sont des assurés rattachés à un tiers payant de cette catégorie.
  types = [
    {value: "TOUT", label: "Tous"},
    {value: "ASSURE", label: "Assurés"},
    {value: "CARNET", label: "Carnets"},
    {value: "DEPOT", label: "Dépôts"},
    {value: "STANDARD", label: "Standards"}
  ];
  statuts: object[] = [
    {value: "ENABLE", label: "Actifs"},
    {value: "DISABLE", label: "Désactivés"}
  ];
  protected readonly typeSelected = signal("");
  protected readonly statutSelected = signal("ENABLE");
  search = "";
  protected readonly totalItems = signal(0);
  itemsPerPage = ITEMS_PER_PAGE;
  protected readonly page = signal<number | undefined>(undefined);
  protected readonly predicate = signal<string | undefined>(undefined);
  protected readonly ascending = signal<boolean | undefined>(undefined);
  protected readonly loading = signal<boolean | undefined>(undefined);
  protected readonly ngbPaginationPage = signal(1);
  private readonly ability = inject(AbilityService);
  protected readonly canCreate = this.ability.canSignal("create", "customer");
  protected readonly canEdit = this.ability.canSignal("edit", "customer");
  protected readonly canDelete = this.ability.canSignal("delete", "customer");
  protected readonly canMerge = this.ability.canSignal("execute", "pr-fusion-client");
  /** Client affiché dans le panneau de détail. */
  protected readonly selectedCustomer = signal<ICustomer | null>(null);
  protected readonly panelOpen = computed(() => this.selectedCustomer() !== null);
  /** Sélection limitée à la page affichée : elle se vide à chaque chargement. */
  protected readonly selectedIds = signal<Set<number>>(new Set());
  protected readonly selectedCustomers = computed(() => (this.customers() ?? []).filter(c => this.selectedIds().has(c.id)));
  protected readonly hasSelection = computed(() => this.selectedIds().size > 0);
  protected readonly allSelected = computed(() => {
    const customers = this.customers() ?? [];
    return customers.length > 0 && customers.every(c => this.selectedIds().has(c.id));
  });
  protected readonly newCustomerbuttons = computed<AppSplitButtonItem[]>(() => [
    {label: "Assuré", icon: "pi pi-user-plus", command: () => this.addAssureCustomer("ASSURANCE")},
    {label: "Carnet", icon: "pi pi-user-plus", command: () => this.addCarnet("CARNET")},
    {label: "Dépôt", icon: "pi pi-user-plus", command: () => this.addCarnet("DEPOT")},
    {label: "Standard", icon: "pi pi-user-plus", command: () => this.addUninsuredCustomer()},
    {label: "Importer (JSON)", icon: "pi pi-upload", command: () => this.openJsonImport()}
  ]);
  responseDialog = false;
  protected customerService = inject(CustomerService);
  protected activatedRoute = inject(ActivatedRoute);
  protected router = inject(Router);
  private readonly modalService = inject(NgbModal);
  private destroy$ = new Subject<void>();
  private readonly spinner = viewChild.required<SpinnerComponent>("spinner");
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);


  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearch(): void {
    this.loadPage();
  }

  onTypeChange(): void {
    this.loadPage();
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
    const pageToLoad: number = page || this.page() || 1;
    this.customerService
      .query({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        sort: this.sort(),
        type: this.typeSelected(),
        search: this.search,
        status: this.statutSelected()
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body, res.headers, pageToLoad, dontNavigate),
        error: () => this.onError()
      });
  }

  lazyLoading(event: AppTableLazyLoadEvent): void {
    if (event) {
      this.page.set(event.first / event.rows);
      this.loading.set(true);
      this.customerService
        .query({
          page: this.page(),
          size: event.rows,
          sort: this.sort(),
          type: this.typeSelected(),
          search: this.search,
          status: this.statutSelected()
        })
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body, res.headers, this.page(), false),
          error: () => this.onError()
        });
    }
  }

  ngOnInit(): void {
    this.typeSelected.set("TOUT");
    this.statutSelected.set("ENABLE");
    this.handleNavigation();
    this.loadPage();
    //   this.registerChangeInCustomers();
  }

  /**
   * Ouvre la modale d'import JSON. Le composant partagé rend un FormData prêt à poster.
   */
  openJsonImport(): void {
    showCommonModal(this.modalService, JsonImportDialogComponent, {}, (formData: FormData) => {
      if (formData) {
        this.handleServiceCall(this.customerService.uploadJsonData(formData), () => this.onPocesJsonSuccess());
      }
    });
  }

  /** Patients à relancer pour un traitement chronique ; en choisir un ouvre sa fiche. */
  openRenouvellements(): void {
    showCommonModal(this.modalService, TraitementsARenouvelerModalComponent, {}, (customerId: number) => {
      if (customerId) {
        this.customerService
          .find(customerId)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: res => res.body && this.onCustomerSelected(res.body),
            error: (err: HttpErrorResponse) => this.notificationService.error(this.errorService.getErrorMessage(err, "La fiche n'a pas pu être ouverte"))
          });
      }
    }, "xl");
  }

  // ── Panneau de détail ──

  protected onCustomerSelected(customer: ICustomer): void {
    this.selectedCustomer.set(customer);
  }

  protected onClosePanel(): void {
    this.selectedCustomer.set(null);
  }

  /** Le client affiché vient d'être supprimé ou désactivé : sa fiche n'a plus lieu de rester ouverte. */
  private fermerSiAffiche(customer: ICustomer): void {
    if (this.selectedCustomer()?.id === customer.id) {
      this.onClosePanel();
    }
  }

  // ── Sélection et fusion ──

  protected isChecked(customer: ICustomer): boolean {
    return this.selectedIds().has(customer.id);
  }

  protected toggleCheckbox(customer: ICustomer): void {
    this.selectedIds.update(ids => {
      const next = new Set(ids);
      if (next.has(customer.id)) next.delete(customer.id); else next.add(customer.id);
      return next;
    });
  }

  protected toggleAll(): void {
    this.selectedIds.set(this.allSelected() ? new Set() : new Set((this.customers() ?? []).map(c => c.id)));
  }

  protected onClearSelection(): void {
    this.selectedIds.set(new Set());
  }

  protected onBulkMerge(): void {
    const list = this.selectedCustomers();
    if (list.length < 2) {
      return;
    }
    this.confirmDialog.onConfirm(
      () => this.openMergeModal(list),
      "Fusionner les clients",
      `Fusionner ${list.length} client(s) sélectionné(s) ? Cette action est irréversible : les fiches en doublon seront désactivées au profit du client choisi.`
    );
  }

  private openMergeModal(list: ICustomer[]): void {
    const ref = this.modalService.open(FusionClientModalComponent, {
      size: "xl",
      centered: true,
      backdrop: "static"
    });
    (ref.componentInstance as FusionClientModalComponent).clients = list;
    ref.closed.subscribe((result: IFusionClientResult) => {
      this.notificationService.success(`${result.mergedIds?.length ?? 0} client(s) fusionné(s) avec succès.`, "Fusion terminée");
      this.onClearSelection();
      this.loadPage();
    });
  }

  cancel(): void {
    this.responseDialog = false;
  }

  delete(customer: ICustomer): void {
    this.handleServiceCall(this.customerService.delete(customer.id), () => this.apresRetrait(customer), this.proposerDesactivation(customer));
  }

  deleteAssuredCustomer(customer: ICustomer): void {
    this.handleServiceCall(this.customerService.deleteAssuredCustomer(customer.id), () => this.apresRetrait(customer), this.proposerDesactivation(customer));
  }

  private changeStatus(customer: ICustomer, status: "ENABLE" | "DISABLE"): void {
    this.handleServiceCall(this.customerService.changeStatus(customer.id, status), () => this.apresRetrait(customer));
  }

  private apresRetrait(customer: ICustomer): void {
    this.fermerSiAffiche(customer);
    this.loadPage();
  }

  confirmRemove(customer: ICustomer): void {
    this.confirmDialog.onConfirm(
      () => {
        if (customer.categorie === "ASSURE") {
          this.deleteAssuredCustomer(customer);
        } else {
          this.delete(customer);
        }
      },
      "SUPPRESSION DE CLIENT",
      "Voulez-vous vraiment supprimer ce client ?"
    );
  }

  confirmDesactivation(customer: ICustomer): void {
    this.confirmDialog.onConfirm(
      () => this.changeStatus(customer, "DISABLE"),
      "DESACTIVATION DE CLIENT",
      "Voulez-vous vraiment désactiver ce client ?"
    );
  }

  confirmReactivation(customer: ICustomer): void {
    this.confirmDialog.onConfirm(
      () => this.changeStatus(customer, "ENABLE"),
      "REACTIVATION DE CLIENT",
      "Voulez-vous vraiment réactiver ce client ?"
    );
  }

  sort(): string[] {
    const result = [this.predicate() + "," + (this.ascending() ? "asc" : "desc")];
    if (this.predicate() !== "id") {
      result.push("id");
    }
    return result;
  }

  addCarnet(categorie: string): void {
    showCommonModal(
      this.modalService,
      CustomerCarnetComponent,
      {
        entity: null,
        categorie,
        title: `FORMULAIRE DE CREATION DE CLIENT [ ${categorie} ]`
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      },
      "xl"
    );
  }

  addAssureCustomer(typeAssure: string): void {
    showCommonModal(
      this.modalService,
      AssureFormStepComponent,
      {
        entity: null,
        typeAssure,
        header: "FORMULAIRE DE CREATION DE CLIENT "
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      },
      "xl",
      "modal-dialog-80"
    );
  }

  editAssureCustomer(customer: ICustomer): void {
    showCommonModal(
      this.modalService,
      AssureFormStepComponent,
      {
        entity: customer,
        header: `FORMULAIRE DE MODIFICATION DE CLIENT  [ ${customer.fullName}  ]`
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      },
      "xl",
      "modal-dialog-80"
    );
  }

  protected addUninsuredCustomer(): void {
    showCommonModal(
      this.modalService,
      UninsuredCustomerFormComponent,
      {
        entity: null,
        title: "FORMULAIRE DE CREATION DE CLIENT "
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      },
      "xl"
    );
  }

  protected editUninsuredCustomer(customer: ICustomer): void {
    showCommonModal(
      this.modalService,
      UninsuredCustomerFormComponent,
      {
        entity: customer,
        title: `FORMULAIRE DE MODIFICATION DE CLIENT  [ ${customer.fullName}  ]`
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      },
      "xl"
    );
  }

  protected onPocesJsonSuccess(): void {
    this.spinner().hide();
    this.loadPage();
  }

  private handleNavigation(): void {
    combineLatest(this.activatedRoute.data, this.activatedRoute.queryParamMap, (data: Data, params: ParamMap) => {
      const page = params.get("page");
      const pageNumber = page !== null ? +page : 1;
      const sort = (params.get("sort") ?? data["defaultSort"]).split(",");
      const predicate = sort[0];
      const ascending = sort[1] === "asc";
      if (pageNumber !== this.page() || predicate !== this.predicate() || ascending !== this.ascending()) {
        this.predicate.set(predicate);
        this.ascending.set(ascending);
        this.loadPage(pageNumber, true);
      }
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe();
  }

  private onSuccess(data: ICustomer[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems.set(Number(headers.get("X-Total-Count")));
    this.page.set(page);
    if (navigate) {
      this.router.navigate(["/customer"], {
        queryParams: {
          page: this.page(),
          size: this.itemsPerPage,
          sort: this.predicate() + "," + (this.ascending() ? "asc" : "desc")
        }
      });
    }
    this.customers.set(data || []);
    this.selectedIds.set(new Set());
    this.ngbPaginationPage.set(this.page());
    this.loading.set(false);
  }

  private onError(): void {
    this.loading.set(false);
    this.ngbPaginationPage.set(this.page() ?? 1);
  }

  /** Le message du serveur est affiché tel quel : les clés d'erreur renvoyées n'ont pas de traduction. */
  private handleServiceCall(observable: Observable<any>, successCallback: () => void, onError?: (error: HttpErrorResponse) => boolean): void {
    this.spinner().show();
    observable.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.spinner().hide();
        successCallback();
      },
      error: (error: HttpErrorResponse) => {
        this.spinner().hide();
        if (!onError?.(error)) {
          this.notificationService.error(this.errorService.getErrorMessage(error));
        }
      }
    });
  }

  /**
   * Un client qui a un historique ne se supprime pas (décision n° 5 du plan fiche client) : on
   * propose aussitôt de le désactiver.
   */
  private proposerDesactivation(customer: ICustomer): (error: HttpErrorResponse) => boolean {
    return error => {
      if (error.error?.errorKey !== "clientAvecHistorique") {
        return false;
      }
      this.confirmDialog.onConfirm(
        () => this.changeStatus(customer, "DISABLE"),
        "SUPPRESSION IMPOSSIBLE",
        `${this.errorService.getErrorMessage(error)} Le désactiver maintenant ?`
      );
      return true;
    };
  }
}
