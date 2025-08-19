import { Component, inject, OnInit, viewChild } from '@angular/core';
import { IFournisseur } from '../../shared/model/fournisseur.model';
import { CommandeService } from './commande.service';
import { FournisseurService } from '../fournisseur/fournisseur.service';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../shared/error.service';
import { ICommandeResponse } from '../../shared/model/commande-response.model';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { FileUploadModule } from 'primeng/fileupload';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { SpinerService } from '../../shared/spiner.service';
import { finalize } from 'rxjs/operators';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { Card } from 'primeng/card';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';

@Component({
  selector: 'jhi-importation-new-commande',
  templateUrl: './importation-new-commande.component.html',
  styleUrls: ['../common-modal.component.scss'],
  imports: [WarehouseCommonModule, FormsModule, FileUploadModule, Select, Button, ToastAlertComponent, Card, SpinnerComponent]
})
export class ImportationNewCommandeComponent implements OnInit {
  header: string = '';
  protected isSaving = false;
  fournisseurSelectedId!: number;
  fournisseurs: IFournisseur[] = [];
  modelSelected!: string;
  models: any[];
  file: any;
  commandeResponse!: ICommandeResponse | null;
  private readonly commandeService = inject(CommandeService);
  private readonly fournisseurService = inject(FournisseurService);
  protected readonly modalService = inject(NgbModal);
   private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);
  private readonly activeModal = inject(NgbActiveModal);

  constructor() {
    this.models = [
      { label: 'LABOREX', value: 'LABOREX' },
      { label: 'COPHARMED', value: 'COPHARMED' },
      { label: 'DPCI', value: 'DPCI' },
      { label: 'TEDIS', value: 'TEDIS' },
      { label: 'Cip  quantité', value: 'CIP_QTE' },
      { label: 'Cip quantité prix achat', value: 'CIP_QTE_PA' }
    ];
  }

  ngOnInit(): void {
    this.populate();
  }

  protected save(): void {
    this.isSaving = true;
    const formData: FormData = new FormData();
    const file = this.file;

    formData.append('commande', file, file.name);
    this.spinner().show();
    this.commandeService.uploadNewCommande(this.fournisseurSelectedId, this.modelSelected, formData)
      .pipe(finalize(() => {
        this.spinner().hide();
        this.isSaving = false;
      }))
      .subscribe({
        next: res => {
          this.commandeResponse = res.body;
          this.cancel();
        },
        error: error => {
          this.onCommonError(error);
        }
      });
  }

  protected uploadHandler(event: any, fileUpload: any): void {
    this.file = event.files[0];
    fileUpload.clear();
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  protected isValidForm(): boolean {
    return !!this.file && !!this.modelSelected && !!this.fournisseurSelectedId;
  }

  private populate(): void {
    this.fournisseurService.query({ size: 99999 }).subscribe((res: HttpResponse<IFournisseur[]>) => {
      this.fournisseurs = res.body || [];
    });
  }

  private onCommonError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
  }


}
