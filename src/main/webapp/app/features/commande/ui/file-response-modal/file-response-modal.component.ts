import { Component, ElementRef, inject, Renderer2, viewChild } from "@angular/core";
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { FileUpload } from 'primeng/fileupload';
import { Select } from 'primeng/select';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { CommandeService } from '../../../../entities/commande/commande.service';
import { ICommande } from 'app/shared/model/commande.model';
import { HttpErrorResponse } from '@angular/common/http';
import { NotificationService } from 'app/shared/services/notification.service';
import { ErrorService } from 'app/shared/error.service';
import { SpinnerComponent } from 'app/shared/spinner/spinner.component';

@Component({
  selector: 'app-file-response-modal',
  imports: [FormsModule, Button, Card, FileUpload, Select, SpinnerComponent],
  templateUrl: './file-response-modal.component.html',
  styleUrls: ['file-response.scss'],
})
export class FileResponseModalComponent {
  header = '';
  commandeSelected: ICommande | null = null;

  protected selectedModel: string | null = null;
  protected readonly models = [
    { label: 'LABOREX', value: 'LABOREX' },
    { label: 'COPHARMED', value: 'COPHARMED' },
    { label: 'DPCI', value: 'DPCI' },
    { label: 'TEDIS', value: 'TEDIS' },
    { label: 'Cip quantité', value: 'CIP_QTE' },
    { label: 'Cip quantité prix achat', value: 'CIP_QTE_PA' },
  ];

  private readonly commandeService = inject(CommandeService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly renderer = inject(Renderer2);
  private readonly elementRef = inject(ElementRef);
  protected cancel(): void {
    this.activeModal.dismiss();
  }
  protected onDropdownShow(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector('.modal-body');
    if (modalBody) {
      this.renderer.addClass(modalBody, 'overflow-visible');
    }
  }

  protected onDropdownHide(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector('.modal-body');
    if (modalBody) {
      this.renderer.removeClass(modalBody, 'overflow-visible');
    }
  }
  protected onImporterReponseCommande(event: any, fileUpload: any): void {
    if (!this.selectedModel) return;
    const file = event.files[0];
    fileUpload.clear(); // réinitialise l'état PrimeNG pour éviter le double spinner
    const formData: FormData = new FormData();
    formData.append('commande', file, file.name);
    this.spinner().show();
    this.commandeService.importerReponseCommande(this.commandeSelected!.commandeId, this.selectedModel, formData).subscribe({
      next: res => {
        this.spinner().hide();
        this.activeModal.close(res.body);
      },
      error: (error: HttpErrorResponse) => {
        this.spinner().hide();
        this.onCommonError(error);
      },
    });
  }

  private onCommonError(error: HttpErrorResponse): void {
    this.notificationService.error(this.errorService.getErrorMessage(error), 'Erreur');
  }
}
