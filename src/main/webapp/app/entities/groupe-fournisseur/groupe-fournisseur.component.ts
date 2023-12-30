import { Component, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ConfirmationService, LazyLoadEvent, MessageService } from 'primeng/api';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GroupeFournisseurService } from './groupe-fournisseur.service';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { GroupeFournisseur, IGroupeFournisseur } from '../../shared/model/groupe-fournisseur.model';
import { IResponseDto } from '../../shared/util/response-dto';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { DialogModule } from 'primeng/dialog';
import { FileUploadModule } from 'primeng/fileupload';
import { ToolbarModule } from 'primeng/toolbar';
import { TableModule } from 'primeng/table';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { TooltipModule } from 'primeng/tooltip';

@Component({
  selector: 'jhi-groupe-fournisseur',
  templateUrl: './groupe-fournisseur.component.html',
  providers: [MessageService, ConfirmationService],
  standalone: true,
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
    FormsModule,
    ReactiveFormsModule,
    InputTextareaModule,
    InputTextModule,
    KeyFilterModule,
    TooltipModule,
  ],
})
export class GroupeFournisseurComponent implements OnInit {
  fileDialog?: boolean;
  responsedto!: IResponseDto;
  responseDialog?: boolean;
  entites?: IGroupeFournisseur[];
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page = 0;
  selectedEl?: IGroupeFournisseur;
  loading = false;
  isSaving = false;
  displayDialog?: boolean;

  editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
    addresspostale: [],
    numFaxe: [],
    email: [],
    tel: [],
    odre: [],
  });

  constructor(
    protected entityService: GroupeFournisseurService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    private messageService: MessageService,
    protected modalService: ConfirmationService,
    private fb: UntypedFormBuilder,
  ) {}

  ngOnInit(): void {
    this.loadPage();
  }

  onUpload(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('importcsv', file, file.name);
    this.uploadFileResponse(this.entityService.uploadFile(formData));
  }

  loadPage(page?: number, search?: string): void {
    const pageToLoad: number = page || this.page;
    const query: string = search || '';
    this.loading = true;
    this.entityService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        search: query,
      })
      .subscribe({
        next: (res: HttpResponse<IGroupeFournisseur[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  lazyLoading(event: LazyLoadEvent): void {
    this.page = event.first / event.rows;
    this.loading = true;
    this.entityService
      .query({
        page: this.page,
        size: event.rows,
        search: '',
      })
      .subscribe({
        next: (res: HttpResponse<IGroupeFournisseur[]>) => this.onSuccess(res.body, res.headers, this.page),
        error: () => this.onError(),
      });
  }

  confirmDialog(id: number): void {
    this.modalService.confirm({
      message: 'Voulez-vous supprimer cet enregistrement ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage(0);
        });
      },
    });
  }

  updateForm(entity: IGroupeFournisseur): void {
    this.editForm.patchValue({
      id: entity.id,
      libelle: entity.libelle,
      addresspostale: entity.addresspostale,
      numFaxe: entity.numFaxe,
      email: entity.email,
      tel: entity.tel,
      odre: entity.odre,
    });
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();

    if (entity.id !== undefined) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  cancel(): void {
    this.displayDialog = false;
    this.fileDialog = false;
  }

  addNewEntity(): void {
    this.updateForm(new GroupeFournisseur());
    this.displayDialog = true;
  }

  onEdit(entity: IGroupeFournisseur): void {
    this.updateForm(entity);
    this.displayDialog = true;
  }

  delete(entity: IGroupeFournisseur): void {
    if (entity && entity.id) {
      this.confirmDelete(entity.id);
    }
  }

  confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  search(event: any): void {
    this.loadPage(0, event.target.value);
  }

  showFileDialog(): void {
    this.fileDialog = true;
  }

  protected uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.subscribe({
      next: (res: HttpResponse<IResponseDto>) => this.onPocesCsvSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected onPocesCsvSuccess(responseDto: IResponseDto | null): void {
    if (responseDto) {
      this.responsedto = responseDto;
    }
    this.responseDialog = true;
    this.fileDialog = false;
    this.loadPage(0);
  }

  protected onSuccess(data: IGroupeFournisseur[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/groupe-fournisseur'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
      },
    });
    this.entites = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.loading = false;
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.displayDialog = false;
    this.loadPage(0);
  }

  protected onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'info',
      summary: 'Enregistrement',
      detail: 'Opération effectuée avec succès',
    });
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IGroupeFournisseur>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  private createFromForm(): IGroupeFournisseur {
    return {
      ...new GroupeFournisseur(),
      id: this.editForm.get(['id'])!.value,
      libelle: this.editForm.get(['libelle'])!.value,
      addresspostale: this.editForm.get(['addresspostale'])!.value,
      numFaxe: this.editForm.get(['numFaxe'])!.value,
      email: this.editForm.get(['email'])!.value,
      tel: this.editForm.get(['tel'])!.value,
      odre: this.editForm.get(['odre'])!.value,
    };
  }
}
