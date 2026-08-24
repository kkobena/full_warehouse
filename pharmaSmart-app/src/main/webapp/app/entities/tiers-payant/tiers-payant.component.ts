import {ChangeDetectionStrategy, Component, inject, OnInit, viewChild, input, signal } from "@angular/core";
import {TiersPayantService} from "./tierspayant.service";
import {Observable} from "rxjs";
import {HttpHeaders, HttpResponse} from "@angular/common/http";
import {IResponseDto} from "../../shared/util/response-dto";
import {NgbActiveModal, NgbModal, NgbProgressbar, NgbTooltip} from "@ng-bootstrap/ng-bootstrap";
import {
  AppSplitButtonItem,
  AppTableLazyLoadEvent,
  ButtonComponent,
  DataTableComponent,
  IconFieldComponent,
  SelectComponent,
  SplitButtonComponent,
  ToolbarComponent
} from "../../shared/ui";
import {
  JsonImportDialogComponent
} from "../../shared/json-import-dialog/json-import-dialog.component";
import {ITEMS_PER_PAGE} from "../../shared/constants/pagination.constants";
import {ITiersPayant} from "../../shared/model";
import {Router, RouterModule} from "@angular/router";
import {FormTiersPayantComponent} from "./form-tiers-payant/form-tiers-payant.component";
import {ErrorService} from "../../shared/error.service";

import {FormsModule} from "@angular/forms";
import {showCommonModal} from "../sales/selling-home/sale-helper";
import {SpinnerComponent} from "../../shared/spinner/spinner.component";
import {
  NgbConfirmDialogService
} from "../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import {NotificationService} from "../../shared/services/notification.service";
import {CommonModule} from "@angular/common";

@Component({
  selector: "app-tiers-payant",
  templateUrl: "./tiers-payant.component.html",
  styleUrls: ["./tiers-payant.component.scss"],
  providers: [NgbActiveModal],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    NgbProgressbar,
    NgbTooltip,
    SpinnerComponent,
    ButtonComponent,
    DataTableComponent,
    IconFieldComponent,
    SelectComponent,
    SplitButtonComponent,
    ToolbarComponent
  ]
})
export class TiersPayantComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit le libellé du menu — ou son `titre_long`
   * quand la barre nomme plus longuement. Un écran atteint depuis deux menus affiche donc le nom
   * de celui par lequel on est entré.
   */
  readonly navCode = input<string>('');

  protected readonly tiersPayants = signal<ITiersPayant[]>([]);
  protected readonly responseDialog = signal(false);
  protected onErrorOccur = false;
  protected responsedto!: IResponseDto;
  protected readonly isSaving = signal(false);
  protected jsonFileUploadProgress = 0;
  protected jsonFileUploadStatutProgress = "Importation des tiers-payant en cours...";
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected readonly page = signal<number | undefined>(undefined);
  protected readonly ngbPaginationPage = signal(1);
  protected readonly totalItems = signal(0);
  protected readonly loading = signal<boolean | undefined>(undefined);
  protected type: string[] = ["TOUT", "ASSURANCE", "CARNET", "DEPOT"];
  protected typeSelected = "TOUT";
  protected search = "";
  protected readonly tiersPayantSplitbuttons = signal<AppSplitButtonItem[] | undefined>(undefined);
  private readonly entityService = inject(TiersPayantService);
  private readonly errorService = inject(ErrorService);
  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);
  private readonly spinner = viewChild.required<SpinnerComponent>("spinner");
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);

  constructor() {
    this.tiersPayantSplitbuttons.set([
      {
        label: "ASSURANCE",
        command: () => this.addTiersPayantAssurance()
      },
      {
        label: "CARNET",
        command: () => this.addCarnet()
      },
      {
        label: "DEPOT",
        command: () => this.addDepot()
      }
    ]);
  }

  ngOnInit(): void {
    this.loadPage();
  }

  onSearch(): void {
    this.loadPage();
  }


  openJsonImport(): void {
    showCommonModal(this.modalService, JsonImportDialogComponent, {}, (formData: FormData) => {
      if (!formData) {
        return;
      }
      this.spinner().show();
      this.uploadJsonDataResponse(this.entityService.uploadJsonData(formData));
    });
  }

  cancel(): void {
    this.onErrorOccur = false;
    this.responseDialog.set(false);
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
    const pageToLoad: number = page || this.page() || 1;
    this.entityService
      .query({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        type: this.typeSelected,
        search: this.search
      })
      .subscribe({
        next: (res: HttpResponse<ITiersPayant[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
        error: () => this.onError()
      });
  }

  lazyLoading(event: AppTableLazyLoadEvent): void {
    if (event) {
      this.page.set(event.first / event.rows);
      this.loading.set(true);
      this.entityService
        .query({
          page: this.page(),
          size: event.rows,
          type: this.typeSelected,
          search: this.search
        })
        .subscribe({
          next: (res: HttpResponse<ITiersPayant[]>) => this.onSuccess(res.body, res.headers, this.page(), false),
          error: () => this.onError()
        });
    }
  }

  addTiersPayantAssurance(): void {
    showCommonModal(
      this.modalService,
      FormTiersPayantComponent,
      {
        entity: null,
        categorie: "ASSURANCE",
        title: "FORMULAIRE DE CREATION DE TIERS-PAYANT ASSURANCE"
      },
      () => {
        this.loadPage();
      },
      "xl",
      "modal-dialog-70"
    );
  }

  addCarnet(): void {
    showCommonModal(
      this.modalService,
      FormTiersPayantComponent,
      {
        entity: null,
        categorie: "CARNET",
        title: "FORMULAIRE DE CREATION DE TIERS-PAYANT CARNET"
      },
      () => {
        this.loadPage();
      },
      "xl",
      "modal-dialog-70"
    );
  }

  addDepot(): void {
    showCommonModal(
      this.modalService,
      FormTiersPayantComponent,
      {
        entity: null,
        categorie: "DEPOT",
        title: "FORMULAIRE DE CREATION DE COMME DEPOT"
      },
      () => {
        this.loadPage();
      },
      "xl",
      "modal-dialog-70"
    );
  }

  editTiersPayant(tiersPayant: ITiersPayant): void {
    showCommonModal(
      this.modalService,
      FormTiersPayantComponent,
      {
        entity: tiersPayant,
        categorie: tiersPayant.categorie,
        title: `MODIFICATION DU TIERS-PAYANT [ ${tiersPayant.fullName}  ]`
      },
      () => {
        this.loadPage();
      },
      "xl",
      "modal-dialog-70"
    );
  }

  confirmRemove(tiersPayant: ITiersPayant): void {
    this.confirmDialog.onConfirm(
      () => this.onDelete(tiersPayant),
      "SUPPRESSION DE TIERS-PAYANT",
      "Voulez-vous vraiment supprimer ce tiers-payant ?"
    );
  }

  confirmDesactivation(tiersPayant: ITiersPayant): void {
    this.confirmDialog.onConfirm(
      () => this.onDesable(tiersPayant),
      "DESACTIVATION DE TIERS-PAYANT",
      "Voulez-vous vraiment désativer ce tiers-payant ?"
    );
  }

  onDelete(tiersPayant: ITiersPayant): void {
    this.entityService.delete(tiersPayant.id).subscribe({
      next: () => this.loadPage(),
      error: error => this.onSaveError(error)
    });
  }

  onDesable(tiersPayant: ITiersPayant): void {
    this.entityService.desable(tiersPayant.id).subscribe({
      next: () => this.loadPage(),
      error: error => this.onSaveError(error)
    });
  }

  protected onSaveError(error: any): void {
    this.isSaving.set(false);
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }

  protected onImportError(): void {
    this.isSaving.set(false);
    this.spinner().hide();
    this.notificationService.error("Enregistrement a échoué");
  }

  protected uploadJsonDataResponse(result: Observable<HttpResponse<void>>): void {
    result.subscribe({
      next: () => this.onPocesJsonSuccess(),
      error: () => this.onImportError()
    });
  }

  protected onPocesJsonSuccess(): void {
    this.spinner().hide();
    this.loadPage();
  }

  protected onSuccess(data: ITiersPayant[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems.set(Number(headers.get("X-Total-Count")));
    this.page.set(page);
    if (navigate) {
      this.router.navigate(["/tiers-payant"], {
        queryParams: {
          page: this.page(),
          size: this.itemsPerPage
        }
      });
    }
    this.tiersPayants.set(data || []);
    this.ngbPaginationPage.set(this.page());
    this.loading.set(false);
  }

  protected onError(): void {
    this.loading.set(false);
    this.ngbPaginationPage.set(this.page() ?? 1);
  }
}
