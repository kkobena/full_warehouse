import { Component, OnInit } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IFournisseur } from '../../shared/model/fournisseur.model';
import { CommandeService } from './commande.service';
import { FournisseurService } from '../fournisseur/fournisseur.service';
import { NgxSpinnerService } from 'ngx-spinner';
import { AlertInfoComponent } from '../../shared/alert/alert-info.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../shared/error.service';
import { ICommandeResponse } from '../../shared/model/commande-response.model';
import { HttpResponse } from '@angular/common/http';

@Component({
  selector: 'jhi-importation-new-commande',
  templateUrl: './importation-new-commande.component.html',
  providers: [DialogService, ConfirmationService],
})
export class ImportationNewCommandeComponent implements OnInit {
  isSaving = false;
  fournisseurSelected!: IFournisseur | null;
  fournisseurSelectedId!: number;
  fournisseurs: IFournisseur[] = [];
  modelSelected!: string;
  models: any[];
  file: any;
  appendTo = 'body';
  commandeResponse!: ICommandeResponse | null;

  constructor(
    protected commandeService: CommandeService,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    protected fournisseurService: FournisseurService,
    private spinner: NgxSpinnerService,
    protected modalService: NgbModal,
    private errorService: ErrorService
  ) {
    this.models = [
      { label: 'LABOREX', value: 'LABOREX' },
      { label: 'COPHARMED', value: 'COPHARMED' },
      { label: 'DPCI', value: 'DPCI' },
      { label: 'TEDIS', value: 'TEDIS' },
    ];
  }

  ngOnInit(): void {
    this.populate();
  }

  save(): void {
    this.isSaving = true;
    const formData: FormData = new FormData();
    const file = this.file;

    formData.append('commande', file, file.name);
    this.spinner.show('upload-commande-spinner');
    this.commandeService.uploadNewCommande(this.fournisseurSelectedId, this.modelSelected, formData).subscribe(
      res => {
        this.isSaving = false;
        this.spinner.hide('upload-commande-spinner');
        this.commandeResponse = res.body;
        this.cancel();
      },
      error => {
        this.spinner.hide('upload-commande-spinner');
        this.isSaving = false;
        this.onCommonError(error);
      }
    );
  }

  uploadHandler(event: any, fileUpload: any): void {
    this.file = event.files[0];
    fileUpload.clear();
  }

  cancel(): void {
    this.ref.close(this.commandeResponse!);
  }

  isValidForm(): boolean {
    return !!this.file && !!this.modelSelected && !!this.fournisseurSelectedId;
  }

  populate(): void {
    this.fournisseurService.query({ size: 99999 }).subscribe((res: HttpResponse<IFournisseur[]>) => {
      this.fournisseurs = res.body || [];
    });
  }

  protected onCommonError(error: any): void {
    if (error.error && error.error.status === 500) {
      this.openInfoDialog('Erreur applicative', 'alert alert-danger');
    } else {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(
        translatedErrorMessage => {
          this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
        },
        () => this.openInfoDialog(error.error.title, 'alert alert-danger')
      );
    }
  }

  protected openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }
}
