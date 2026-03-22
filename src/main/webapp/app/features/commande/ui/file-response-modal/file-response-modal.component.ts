import { Component, inject, viewChild } from '@angular/core';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { FileUpload } from 'primeng/fileupload';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { CommandeService } from '../../../../entities/commande/commande.service';
import { ICommande } from 'app/shared/model/commande.model';
import { HttpErrorResponse } from '@angular/common/http';
import { NotificationService } from 'app/shared/services/notification.service';
import { ErrorService } from 'app/shared/error.service';
import { SpinnerComponent } from 'app/shared/spinner/spinner.component';

@Component({
  selector: 'jhi-file-response-modal',
  imports: [Button, Card, FileUpload, SpinnerComponent],
  templateUrl: './file-response-modal.component.html',
  styleUrls: ['file-response.scss'],
})
export class FileResponseModalComponent {
  header = '';
  commandeSelected: ICommande | null = null;

  private readonly commandeService = inject(CommandeService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  protected onImporterReponseCommande(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('commande', file, file.name);
    this.spinner().show();
    this.commandeService.importerReponseCommande(this.commandeSelected.commandeId, formData).subscribe({
      next: res => {
        this.spinner().hide();
        this.activeModal.close(res.body);
      },
      error: error => {
        this.spinner().hide();
        this.onCommonError(error);
      },
    });
  }

  private onCommonError(error: HttpErrorResponse): void {
    this.notificationService.error(this.errorService.getErrorMessage(error), 'Erreur');
  }
}
