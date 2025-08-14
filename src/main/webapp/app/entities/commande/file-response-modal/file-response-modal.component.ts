import { Component, inject, viewChild } from '@angular/core';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { FileUpload } from 'primeng/fileupload';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CommandeService } from '../commande.service';
import { SpinerService } from '../../../shared/spiner.service';
import { ICommande } from '../../../shared/model/commande.model';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { ErrorService } from '../../../shared/error.service';

@Component({
  selector: 'jhi-file-response-modal',
  imports: [
    Button,
    Card,
    FileUpload
  ],
  templateUrl: './file-response-modal.component.html',
  styleUrls: ['../../common-modal.component.scss']
})
export class FileResponseModalComponent {
  header: string = '';
  commandeSelected: ICommande | null = null;

  private readonly commandeService = inject(CommandeService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly spinner = inject(SpinerService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);


  protected cancel(): void {
    this.activeModal.dismiss();
  }

  protected onImporterReponseCommande(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('commande', file, file.name);
    this.spinner.show();
    this.commandeService.importerReponseCommande(this.commandeSelected.id, formData).subscribe({
      next: res => {
        this.spinner.hide();
        this.activeModal.close(res.body);
      },
      error: error => {
        this.spinner.hide();
        this.onCommonError(error);
      }
    });
  }


  private onCommonError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
  }
}
