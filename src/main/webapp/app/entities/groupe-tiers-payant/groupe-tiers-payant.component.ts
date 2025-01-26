import { Component, OnInit } from '@angular/core';
import { IResponseDto } from 'app/shared/util/response-dto';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { IGroupeTiersPayant } from 'app/shared/model/groupe-tierspayant.model';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { GroupeTiersPayantService } from 'app/entities/groupe-tiers-payant/groupe-tierspayant.service';
import { HttpResponse } from '@angular/common/http';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FormGroupeTiersPayantComponent } from 'app/entities/groupe-tiers-payant/form-groupe-tiers-payant/form-groupe-tiers-payant.component';
import { ErrorService } from 'app/shared/error.service';
import { Observable } from 'rxjs';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { DialogModule } from 'primeng/dialog';
import { FileUploadModule } from 'primeng/fileupload';
import { ToolbarModule } from 'primeng/toolbar';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'jhi-groupe-tiers-payant',
  templateUrl: './groupe-tiers-payant.component.html',
  providers: [MessageService, ConfirmationService, DialogService],
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RippleModule,
    ConfirmDialogModule,
    ToastModule,
    DialogModule,
    FileUploadModule,
    ToolbarModule,
    TableModule,
    RouterModule,
    InputTextModule,
    TooltipModule,
    DynamicDialogModule,
    FormsModule,
  ],
})
export class GroupeTiersPayantComponent implements OnInit {
  fileDialog?: boolean;
  responsedto!: IResponseDto;
  responseDialog?: boolean;
  entites?: IGroupeTiersPayant[];
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page = 0;
  selectedEl?: IGroupeTiersPayant;
  loading = false;
  isSaving = false;
  displayDialog?: boolean;
  search = '';
  ref!: DynamicDialogRef;

  constructor(
    protected entityService: GroupeTiersPayantService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    private messageService: MessageService,
    private dialogService: DialogService,
    protected confirmationService: ConfirmationService,
    protected errorService: ErrorService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.entityService
      .query({
        search: this.search,
      })
      .subscribe((res: HttpResponse<IGroupeTiersPayant[]>) => this.onSuccess(res.body));
  }

  onSearch(): void {
    this.load();
  }

  addGroupeTiersPayant(): void {
    this.ref = this.dialogService.open(FormGroupeTiersPayantComponent, {
      data: { entity: null },
      header: 'FORMULAIRE DE CREATION DE GROUPE TIERS-PAYANT ',
      width: '50%',
    });
    this.ref.onClose.subscribe((resp: IGroupeTiersPayant) => {
      if (resp) {
        this.load();
      }
    });
  }

  showFileDialog(): void {
    this.fileDialog = true;
  }

  editGroupeTiersPayant(groupeTiersPyant: IGroupeTiersPayant): void {
    this.ref = this.dialogService.open(FormGroupeTiersPayantComponent, {
      data: { entity: groupeTiersPyant },
      header: 'FORMULAIRE DE MODIFICATION DE GROUPE TIERS-PAYANT ',
      width: '50%',
    });
    this.ref.onClose.subscribe((resp: IGroupeTiersPayant) => {
      if (resp) {
        this.load();
      }
    });
  }

  delete(groupeTiersPyant: IGroupeTiersPayant): void {
    this.entityService.delete(groupeTiersPyant.id).subscribe(
      () => this.load(),
      err => this.onSaveError(err),
    );
  }

  onConfirmDelete(groupeTiersPyant: IGroupeTiersPayant): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment supprimer ce groupe ?',
      header: 'SUPPRESSION',
      icon: 'pi pi-info-circle',
      accept: () => this.delete(groupeTiersPyant),
      key: 'deleteGroupe',
    });
  }

  cancel(): void {
    this.displayDialog = false;
    this.fileDialog = false;
  }

  onUpload(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('importcsv', file, file.name);
    this.uploadFileResponse(this.entityService.uploadFile(formData));
  }

  protected uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.subscribe(
      (res: HttpResponse<IResponseDto>) => this.onPocesCsvSuccess(res.body),
      err => this.onSaveError(err),
    );
  }

  protected onPocesCsvSuccess(responseDto: IResponseDto | null): void {
    if (responseDto) {
      this.responsedto = responseDto;
    }
    this.responseDialog = true;
    this.fileDialog = false;
    this.load();
  }

  protected onSuccess(data: IGroupeTiersPayant[] | null): void {
    this.entites = data || [];
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    if (error.error?.errorKey) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(translatedErrorMessage => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: translatedErrorMessage,
        });
      });
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Erreur interne du serveur.',
      });
    }
  }
}
