import { Component, inject, OnInit, viewChild } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { TableModule } from 'primeng/table';
import { FileUpload, FileUploadModule } from 'primeng/fileupload';
import { ProduitService } from '../produit.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { IResponseDto } from '../../../shared/util/response-dto';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { Dropdown, DropdownModule } from 'primeng/dropdown';
import { IFournisseur } from '../../../shared/model/fournisseur.model';
import { FournisseurService } from '../../fournisseur/fournisseur.service';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { ButtonModule } from 'primeng/button';
import { Select } from 'primeng/select';

@Component({
  selector: 'jhi-import-produit-modal',
  providers: [MessageService, ConfirmationService],
  imports: [
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    PanelModule,
    TableModule,
    FileUploadModule,
    ToastModule,
    DropdownModule,
    NgxSpinnerModule,
    ButtonModule,
    Select,
  ],
  templateUrl: './import-produit-modal.component.html',
  styles: ``,
})
export class ImportProduitModalComponent implements OnInit {
  type: string | null = null;
  modalService = inject(NgbModal);
  activeModal = inject(NgbActiveModal);
  produitService = inject(ProduitService);
  messageService = inject(MessageService);
  fileUpload = viewChild.required<FileUpload>('fileUpload');
  fournisseur = viewChild<Dropdown>('fournisseur');
  fournisseurService = inject(FournisseurService);
  protected isSaving = false;
  protected title: string | null = null;
  protected fournisseurs: IFournisseur[] = [];
  protected accept = '.csv';
  private spinner = inject(NgxSpinnerService);

  get isFileUploadValid(): boolean {
    return this.fileUpload().hasFiles() && !this.isSaving && this.fournisseur().value;
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  onUpload(): void {
    this.spinner.show();
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
    const file = this.fileUpload()?.files[0];
    const formData: FormData = new FormData();
    const body = new Blob(
      [
        JSON.stringify({
          typeImportation: this.type,
          fournisseurId: this.fournisseur()?.value,
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
    result.subscribe({
      next: (res: HttpResponse<IResponseDto>) => this.onPocesCsvSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  private onPocesCsvSuccess(responseDto: IResponseDto | null): void {
    this.spinner.hide();
    this.isSaving = false;
    this.activeModal.close(responseDto);
  }

  private onSaveError(): void {
    this.spinner.hide();
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Enregistrement a échoué',
    });
  }
}
