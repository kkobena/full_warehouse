import { Component, inject, OnInit, viewChild } from '@angular/core';
import { TiersPayantService } from './tierspayant.service';
import { Observable } from 'rxjs';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { IResponseDto } from '../../shared/util/response-dto';
import { MenuItem, MessageService } from 'primeng/api';
import { DynamicDialogModule } from 'primeng/dynamicdialog';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { ITiersPayant } from '../../shared/model/tierspayant.model';
import { Router, RouterModule } from '@angular/router';
import { FormTiersPayantComponent } from './form-tiers-payant/form-tiers-payant.component';
import { ErrorService } from '../../shared/error.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';

import { FileUploadModule } from 'primeng/fileupload';
import { ToolbarModule } from 'primeng/toolbar';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { SplitButtonModule } from 'primeng/splitbutton';
import { ProgressBarModule } from 'primeng/progressbar';
import { Select } from 'primeng/select';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Panel } from 'primeng/panel';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';

@Component({
  selector: 'jhi-tiers-payant',
  templateUrl: './tiers-payant.component.html',
  providers: [MessageService, NgbActiveModal],
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RippleModule,
    FileUploadModule,
    ToolbarModule,
    TableModule,
    RouterModule,
    InputTextModule,
    TooltipModule,
    DynamicDialogModule,
    FormsModule,
    DialogModule,
    SplitButtonModule,
    ProgressBarModule,
    Select,
    IconField,
    InputIcon,
    Panel,
    ConfirmDialogComponent,
    SpinnerComponent,
  ],
})
export class TiersPayantComponent implements OnInit {
  protected tiersPayants?: ITiersPayant[] = [];
  protected jsonDialog = false;
  protected responseDialog = false;
  protected onErrorOccur = false;
  protected responsedto!: IResponseDto;
  protected isSaving = false;
  protected jsonFileUploadProgress = 0;
  protected jsonFileUploadStatutProgress = 'Importation des tiers-payant en cours...';
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page!: number;
  protected predicate!: string;
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected totalItems = 0;
  protected loading!: boolean;
  protected type: string[] = ['TOUT', 'ASSURANCE', 'CARNET', 'DEPOT'];
  protected typeSelected = 'TOUT';
  protected search = '';
  protected tiersPayantSplitbuttons: MenuItem[];
  private readonly entityService = inject(TiersPayantService);
  private readonly errorService = inject(ErrorService);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);
  private readonly modalService = inject(NgbModal);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');

  constructor() {
    this.tiersPayantSplitbuttons = [
      {
        label: 'ASSURANCE',
        command: () => this.addTiersPayantAssurance(),
      },
      {
        label: 'CARNET',
        command: () => this.addCarnet(),
      },
      {
        label: 'DEPOT',
        command: () => this.addDepot(),
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
    this.spinner().show();
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
      .subscribe({
        next: (res: HttpResponse<ITiersPayant[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
        error: () => this.onError(),
      });
  }

  lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.entityService
        .query({
          page: this.page,
          size: event.rows,
          type: this.typeSelected,
          search: this.search,
        })
        .subscribe({
          next: (res: HttpResponse<ITiersPayant[]>) => this.onSuccess(res.body, res.headers, this.page, false),
          error: () => this.onError(),
        });
    }
  }

  addTiersPayantAssurance(): void {
    showCommonModal(
      this.modalService,
      FormTiersPayantComponent,
      {
        entity: null,
        categorie: 'ASSURANCE',
        header: 'FORMULAIRE DE CREATION DE TIERS-PAYANT ASSURANCE',
      },
      () => {
        this.loadPage();
      },
      'xl',
      'modal-dialog-70',
    );
  }

  addCarnet(): void {
    showCommonModal(
      this.modalService,
      FormTiersPayantComponent,
      {
        entity: null,
        categorie: 'CARNET',
        header: 'FORMULAIRE DE CREATION DE TIERS-PAYANT CARNET',
      },
      () => {
        this.loadPage();
      },
      'xl',
      'modal-dialog-70',
    );
  }

  addDepot(): void {
    showCommonModal(
      this.modalService,
      FormTiersPayantComponent,
      {
        entity: null,
        categorie: 'DEPOT',
        header: 'FORMULAIRE DE CREATION DE COMME DEPOT',
      },
      () => {
        this.loadPage();
      },
      'xl',
      'modal-dialog-70',
    );
  }

  editTiersPayant(tiersPayant: ITiersPayant): void {
    showCommonModal(
      this.modalService,
      FormTiersPayantComponent,
      {
        entity: tiersPayant,
        categorie: tiersPayant.categorie,
        header: `MODIFICATION DU TIERS-PAYANT [ ${tiersPayant.fullName}  ]`,
      },
      () => {
        this.loadPage();
      },
      'xl',
      'modal-dialog-70',
    );
  }

  confirmRemove(tiersPayant: ITiersPayant): void {
    this.confimDialog().onConfirm(
      () => this.onDelete(tiersPayant),
      'SUPPRESSION DE TIERS-PAYANT',
      'Voulez-vous vraiment supprimer ce tiers-payant ?',
    );
  }

  confirmDesactivation(tiersPayant: ITiersPayant): void {
    this.confimDialog().onConfirm(
      () => this.onDesable(tiersPayant),
      'DESACTIVATION DE TIERS-PAYANT',
      'Voulez-vous vraiment désativer ce tiers-payant ?',
    );
  }

  onDelete(tiersPayant: ITiersPayant): void {
    this.entityService.delete(tiersPayant.id).subscribe({
      next: () => this.loadPage(),
      error: error => this.onSaveError(error),
    });
  }

  onDesable(tiersPayant: ITiersPayant): void {
    this.entityService.desable(tiersPayant.id).subscribe({
      next: () => this.loadPage(),
      error: error => this.onSaveError(error),
    });
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: this.errorService.getErrorMessage(error),
    });
  }

  protected onImportError(): void {
    this.isSaving = false;
    this.spinner().hide();
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Enregistrement a échoué',
    });
  }

  protected uploadJsonDataResponse(result: Observable<HttpResponse<void>>): void {
    result.subscribe({
      next: () => this.onPocesJsonSuccess(),
      error: () => this.onImportError(),
    });
  }

  protected onPocesJsonSuccess(): void {
    this.jsonDialog = false;
    this.spinner().hide();
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
