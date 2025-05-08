import { Component, inject, OnInit, viewChild } from '@angular/core';
import { Button } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { Fieldset } from 'primeng/fieldset';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { ConfirmationService, PrimeTemplate } from 'primeng/api';
import { TableModule } from 'primeng/table';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../shared/error.service';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';
import { Differe } from '../model/differe.model';
import { ReglementDiffereFormComponent } from './reglement-differe-form/reglement-differe-form.component';
import { DiffereService } from '../differe.service';
import { NewReglementDiffere } from '../model/new-reglement-differe.model';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'jhi-faire-reglement-differe',
  providers: [ConfirmationService],
  imports: [
    ReglementDiffereFormComponent,
    ConfirmDialog,
    DatePipe,
    DecimalPipe,
    Fieldset,
    IconField,
    InputIcon,
    InputText,
    PrimeTemplate,
    TableModule,
    CommonModule,
  ],
  templateUrl: './faire-reglement-differe.component.html',
})
export class FaireReglementDiffereComponent implements OnInit {
  protected differe: Differe | null = null;
  protected reglementFormComponent = viewChild(ReglementDiffereFormComponent);
  protected isSaving = false;
  protected monnaie = 0;

  private readonly modalService = inject(NgbModal);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly errorService = inject(ErrorService);
  private readonly differeService = inject(DiffereService);
  private readonly activatedRoute = inject(ActivatedRoute);

  onError(error: any): void {
    this.isSaving = false;
    this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger');
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ differe }) => {
      this.differe = differe;
    });
  }
  protected onMonnaieChange(montant: number): void {
    this.monnaie = montant;
  }
  protected onSaveReglement(params: NewReglementDiffere): void {
    this.isSaving = true;
    this.differeService.doReglement(params).subscribe({
      next: res => {
        this.isSaving = false;
        if (res.body) {
          this.onPrintReceipt(res.body.idReglement);
        }
      },
      error: err => this.onError(err),
    });
  }
  previousState(): void {
    window.history.back();
  }

  private onPrintReceipt(id: number): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous imprimer le ticket ?',
      header: 'TICKET REGLEMENT',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.differeService.printReceipt(id).subscribe();
        this.reset();
      },
      reject: () => this.reset(),
      key: 'printReceipt',
    });
  }

  private reset(): void {
    this.reglementFormComponent().reset();
    this.differe = null;
    this.previousState();
  }
}
