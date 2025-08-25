import { Component, inject, OnInit, viewChild } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { TableModule } from 'primeng/table';
import { FileUpload, FileUploadModule } from 'primeng/fileupload';
import { ProduitService } from '../produit.service';
import { Observable } from 'rxjs';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { IResponseDto } from '../../../shared/util/response-dto';
import { ToastModule } from 'primeng/toast';
import { IFournisseur } from '../../../shared/model/fournisseur.model';
import { FournisseurService } from '../../fournisseur/fournisseur.service';
import { ButtonModule } from 'primeng/button';
import { Select } from 'primeng/select';
import { Card } from 'primeng/card';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { ErrorService } from '../../../shared/error.service';
import { finalize } from 'rxjs/operators';
import { SpinnerComponent } from '../../../shared/spinner/spinner.component';

@Component({
  selector: 'jhi-import-produit-modal',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    PanelModule,
    TableModule,
    FileUploadModule,
    ToastModule,
    ButtonModule,
    Select,
    Card,
    ToastAlertComponent,
    SpinnerComponent,
  ],
  templateUrl: './import-produit-modal.component.html',
  styleUrls: ['../../common-modal.component.scss'],
})
export class ImportProduitModalComponent implements OnInit {
  type: string | null = null;
  fileUpload = viewChild.required<FileUpload>('fileUpload');
  fournisseur = viewChild<Select>('fournisseur');
  fournisseurService = inject(FournisseurService);
  protected isSaving = false;
  protected title: string | null = null;
  protected fournisseurs: IFournisseur[] = [];
  protected accept = '.csv';
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly produitService = inject(ProduitService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');

  get isFileUploadValid(): boolean {
    return this.fileUpload().hasFiles() && !this.isSaving && this.fournisseur().value;
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  onUpload(): void {
    this.spinner().show();
    this.isSaving = true;
    this.uploadFileResponse(this.produitService.uploadFile(this.buildFormData()));
  }

  ngOnInit(): void {
    // this.spinner.show();
    if (this.type === 'NOUVELLE_INSTALLATION') {
      this.title = 'Nouvelle Installation';
    } else if (this.type === 'BASCULEMENT') {
      this.title = 'Basculement';
    } else {
      this.title = 'Basculement de perstige';
      // this.accept = '.json';
    }
    this.fournisseurService
      .query({
        page: 0,
        size: 9999,
      })
      .subscribe((res: HttpResponse<IFournisseur[]>) => {
        this.fournisseurs = res.body || [];
      });
  }

  private buildFormData(): FormData {
    const file = this.fileUpload().files[0];
    const formData: FormData = new FormData();
    const body = new Blob(
      [
        JSON.stringify({
          typeImportation: this.type,
          fournisseurId: this.fournisseur().value,
        }),
      ],
      {
        type: 'application/json',
      },
    );
    formData.append('data', body);
    formData.append('fichier', file, file.name);
    return formData;
  }

  private uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result
      .pipe(
        finalize(() => {
          this.spinner().hide();
          this.isSaving = false;
        }),
      )
      .subscribe({
        next: (res: HttpResponse<IResponseDto>) => this.onPocesCsvSuccess(res.body),
        error: err => this.onSaveError(err),
      });
  }

  private onPocesCsvSuccess(responseDto: IResponseDto | null): void {
    this.activeModal.close(responseDto);
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.spinner().hide();
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }
}
