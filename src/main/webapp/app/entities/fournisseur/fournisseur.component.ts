import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { FournisseurService } from './fournisseur.service';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ConfirmationService, LazyLoadEvent, MessageService, SelectItem } from 'primeng/api';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GroupeFournisseurService } from '../groupe-fournisseur/groupe-fournisseur.service';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { IResponseDto } from '../../shared/util/response-dto';
import { Fournisseur, IFournisseur } from '../../shared/model/fournisseur.model';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { IGroupeFournisseur } from '../../shared/model/groupe-fournisseur.model';
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
import { DropdownModule } from 'primeng/dropdown';
import { KeyFilterModule } from 'primeng/keyfilter';

@Component({
  selector: 'jhi-fournisseur',
  templateUrl: './fournisseur.component.html',
  styles: [
    `
      body .ui-inputtext {
        width: 100% !important;
      }

      body .ui-dropdown {
        width: 100% !important;
      }
    `,
  ],
  providers: [MessageService, ConfirmationService],
  encapsulation: ViewEncapsulation.None,
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
    NgxSpinnerModule,
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    DropdownModule,
    KeyFilterModule,
  ],
})
export class FournisseurComponent implements OnInit {
  fileDialog?: boolean;
  responseDialog?: boolean;
  responsedto!: IResponseDto;
  entites?: IFournisseur[];
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page = 0;
  selectedEl?: IFournisseur;
  loading = false;
  isSaving = false;
  displayDialog?: boolean;
  groupes: SelectItem[] = [];
  editForm = this.fb.group({
    id: [],
    code: [null, [Validators.required]],
    libelle: [null, [Validators.required]],
    addresspostale: [],
    phone: [],
    mobile: [],
    groupeFournisseurId: [],
  });

  constructor(
    protected entityService: FournisseurService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: ConfirmationService,
    private fb: UntypedFormBuilder,
    protected groupeFournisseurService: GroupeFournisseurService,
    private spinner: NgxSpinnerService,
    private messageService: MessageService,
  ) {}

  ngOnInit(): void {
    this.loadPage();
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
        next: (res: HttpResponse<IFournisseur[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
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
        next: (res: HttpResponse<IFournisseur[]>) => this.onSuccess(res.body, res.headers, this.page),
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

  updateForm(entity: IFournisseur): void {
    this.groupeFournisseurService
      .query({
        search: '',
      })
      .subscribe((res: HttpResponse<IGroupeFournisseur[]>) => {
        if (res.body) {
          res.body.forEach(item => {
            this.groupes.push({ label: item.libelle, value: item.id });
          });
        }
        this.editForm.patchValue({
          id: entity.id,
          code: entity.code,
          libelle: entity.libelle,
          groupeFournisseurId: entity.groupeFournisseurId,
          addresspostale: entity.addressePostal,
          phone: entity.phone,
          mobile: entity.mobile,
        });
      });
  }

  save(): void {
    this.isSaving = true;
    this.spinner.show();
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
    this.spinner.hide();
  }

  addNewEntity(): void {
    this.updateForm(new Fournisseur());
    this.displayDialog = true;
  }

  onEdit(entity: IFournisseur): void {
    this.updateForm(entity);
    this.displayDialog = true;
  }

  delete(entity: IFournisseur): void {
    if (entity && entity.id) {
      this.confirmDelete(entity.id);
    }
  }

  confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  trackById(index: number, item: IGroupeFournisseur): any {
    return item.id;
  }

  onFilterTable(event: any): void {
    if (event.key === 'Enter') {
      this.loadPage(0, event.target.value);
    }
  }

  search(event: any): void {
    this.loadPage(0, event.target.value);
  }

  showFileDialog(): void {
    this.fileDialog = true;
  }

  onUpload(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('importcsv', file, file.name);
    this.uploadFileResponse(this.entityService.uploadFile(formData));
  }

  protected onSuccess(data: IFournisseur[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/fournisseur'], {
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
    this.spinner.hide();
    this.messageService.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Enregistrement effectué avec success',
    });
  }

  protected onSaveError(): void {
    this.isSaving = false;
    this.spinner.hide();
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Enregistrement a échoué',
    });
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IFournisseur>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.subscribe({
      next: res => this.onPocesCsvSuccess(res.body),
      error: () => this.onError(),
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

  private createFromForm(): IFournisseur {
    return {
      ...new Fournisseur(),
      id: this.editForm.get(['id'])!.value,
      code: this.editForm.get(['code'])!.value,
      libelle: this.editForm.get(['libelle'])!.value,
      groupeFournisseurId: this.editForm.get(['groupeFournisseurId'])!.value,
      addressePostal: this.editForm.get(['addresspostale'])!.value,
      // numFaxe: this.editForm.get(['numFaxe'])!.value,
      phone: this.editForm.get(['phone'])!.value,
      mobile: this.editForm.get(['mobile'])!.value,
      // site: this.editForm.get(['site'])!.value
    };
  }
}
