import { Component, OnInit } from '@angular/core';
import { TiersPayantService } from './tierspayant.service';
import { Observable } from 'rxjs';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { IResponseDto } from '../../shared/util/response-dto';
import { ConfirmationService, LazyLoadEvent, MenuItem, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { ITiersPayant } from '../../shared/model/tierspayant.model';
import { ActivatedRoute, Router } from '@angular/router';
import { NgxSpinnerService } from 'ngx-spinner';
import { FormTiersPayantComponent } from './form-tiers-payant/form-tiers-payant.component';
import { ErrorService } from '../../shared/error.service';

@Component({
  selector: 'jhi-tiers-payant',
  templateUrl: './tiers-payant.component.html',
  styleUrls: ['./tiers-payant.component.scss'],
  providers: [MessageService, DialogService, ConfirmationService, NgbActiveModal],
})
export class TiersPayantComponent implements OnInit {
  tiersPayants?: ITiersPayant[] = [];
  jsonDialog = false;
  responseDialog = false;
  onErrorOccur = false;
  responsedto!: IResponseDto;
  isSaving = false;
  jsonFileUploadProgress = 0;
  jsonFileUploadStatutProgress = 'Importation des tiers-payant en cours...';

  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;
  totalItems = 0;
  loading!: boolean;
  ref!: DynamicDialogRef;
  type: string[] = ['TOUT', 'ASSURANCE', 'CARNET', 'DEPOT'];
  typeSelected = '';
  search = '';
  splitbuttons: MenuItem[];
  tiersPayantSplitbuttons: MenuItem[];

  constructor(
    protected entityService: TiersPayantService,
    private messageService: MessageService,
    protected activatedRoute: ActivatedRoute,
    private dialogService: DialogService,
    protected errorService: ErrorService,
    protected confirmationService: ConfirmationService,
    protected router: Router,
    private spinner: NgxSpinnerService
  ) {
    this.splitbuttons = [
      {
        label: 'Fiche à partir csv',
        icon: 'pi pi-file-excel',
      },
      {
        label: 'Fiche à partir json',
        icon: 'pi pi-file-o',
        command: () => {
          this.jsonDialog = true;
        },
      },
    ];
    this.tiersPayantSplitbuttons = [
      {
        label: 'ASSURANCE',
        command: () => this.addTiersPayantAssurance(),
      },
      {
        label: 'CARNET',
        command: () => this.addCarnet(),
      },
    ];
  }

  ngOnInit(): void {
    this.loadPage();
  }

  onSearch(): void {
    this.loadPage();
  }

  onUploadJson(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('importjson', file, file.name);
    this.spinner.show('importation');
    this.jsonDialog = false;
    this.uploadJsonDataResponse(this.entityService.uploadJsonData(formData));
  }

  cancel(): void {
    this.jsonDialog = false;
    this.onErrorOccur = false;
    this.responseDialog = false;
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
    const pageToLoad: number = page || this.page || 1;
    this.entityService
      .query({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        type: this.typeSelected,
        search: this.search,
      })
      .subscribe(
        (res: HttpResponse<ITiersPayant[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
        () => this.onError()
      );
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first! / event.rows!;
      this.loading = true;
      this.entityService
        .query({
          page: this.page,
          size: event.rows,
          type: this.typeSelected,
          search: this.search,
        })
        .subscribe(
          (res: HttpResponse<ITiersPayant[]>) => this.onSuccess(res.body, res.headers, this.page, false),
          () => this.onError()
        );
    }
  }

  addTiersPayantAssurance(): void {
    this.ref = this.dialogService.open(FormTiersPayantComponent, {
      data: { entity: null, type: 'ASSURANCE' },
      header: 'FORMULAIRE DE CREATION DE TIERS-PAYANT ASSURANCE',
      width: '80%',
    });
    this.ref.onClose.subscribe(() => {
      this.loadPage();
    });
  }

  addCarnet(): void {
    this.ref = this.dialogService.open(FormTiersPayantComponent, {
      data: { entity: null, type: 'CARNET' },
      header: 'FORMULAIRE DE CREATION DE TIERS-PAYANT CARNET',
      width: '80%',
    });
    this.ref.onClose.subscribe(() => {
      this.loadPage();
    });
  }

  editTiersPayant(tiersPayant: ITiersPayant): void {
    this.ref = this.dialogService.open(FormTiersPayantComponent, {
      data: { entity: tiersPayant, type: tiersPayant.categorie },
      header: `MODIFICATION DU TIERS-PAYANT ${tiersPayant.fullName}`,
      width: '80%',
    });
    this.ref.onClose.subscribe(() => {
      this.loadPage();
    });
  }
  confirmRemove(tiersPayant: ITiersPayant): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment supprimer ce tiers-payant ?',
      header: 'SUPPRESSION DE TIERS-PAYANT',
      icon: 'pi pi-info-circle',
      accept: () => this.onDelete(tiersPayant),
      key: 'comfirmDialog',
    });
  }
  confirmDesactivation(tiersPayant: ITiersPayant): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment désativer ce tiers-payant ?',
      header: 'DESACTIVATION DE TIERS-PAYANT',
      icon: 'pi pi-info-circle',
      accept: () => this.onDesable(tiersPayant),
      key: 'comfirmDialog',
    });
  }
  onDelete(tiersPayant: ITiersPayant): void {
    this.entityService.delete(tiersPayant.id!).subscribe(
      () => this.loadPage(),
      error => this.onSaveError(error)
    );
  }

  onDesable(tiersPayant: ITiersPayant): void {
    this.entityService.desable(tiersPayant.id!).subscribe(
      () => this.loadPage(),
      error => this.onSaveError(error)
    );
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    if (error.error && error.error.errorKey) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(translatedErrorMessage => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: translatedErrorMessage });
      });
    } else {
      this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Erreur interne du serveur.' });
    }
  }

  protected onImportError(): void {
    this.isSaving = false;
    this.spinner.hide('importation');
    this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Enregistrement a échoué' });
  }

  protected uploadJsonDataResponse(result: Observable<HttpResponse<void>>): void {
    result.subscribe(
      () => this.onPocesJsonSuccess(),
      () => this.onImportError()
    );
  }

  protected onPocesJsonSuccess(): void {
    this.jsonDialog = false;
    this.spinner.hide('importation');
    this.loadPage();
  }

  protected onSuccess(data: ITiersPayant[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/tiers-payant'], {
        queryParams: {
          page: this.page,
          size: this.itemsPerPage,
        },
      });
    }
    this.tiersPayants = data || [];
    this.ngbPaginationPage = this.page;
    this.loading = false;
  }

  protected onError(): void {
    this.loading = false;
    this.ngbPaginationPage = this.page ?? 1;
  }
}
