import { AfterViewInit, Component, inject, viewChild } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { VoSalesService } from '../service/vo-sales.service';
import { UtilisationCleSecurite } from '../../action-autorisation/utilisation-cle-securite.model';
import { SalesService } from '../sales.service';
import { ISales } from '../../../shared/model/sales.model';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { ErrorService } from '../../../shared/error.service';
import { Button } from 'primeng/button';
import { TextareaModule } from 'primeng/textarea';
import { Password, PasswordModule } from 'primeng/password';

@Component({
  selector: 'jhi-form-action-autorisation',
  imports: [WarehouseCommonModule, FormsModule, ReactiveFormsModule, Button, TextareaModule, PasswordModule],
  templateUrl: './form-action-autorisation.component.html',
})
export class FormActionAutorisationComponent implements AfterViewInit {
  entity: ISales;
  privilege: string;
  protected fb = inject(UntypedFormBuilder);
  protected actionAuthorityKey = viewChild.required<Password>('actionAuthorityKey');
  protected editForm = this.fb.group({
    actionAuthorityKey: ['', [Validators.required]],
    commentaire: [null],
  });
  protected isSaving = false;
  protected isValid = true;
  private readonly activeModal = inject(NgbActiveModal);
  private readonly voSalesService = inject(VoSalesService);
  private readonly salesService = inject(SalesService);
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);

  cancel(): void {
    this.activeModal.dismiss();
  }

  autorize(): void {
    this.isSaving = true;
    const utilisationCleSecurite = this.createFrom();
    if (this.entity.type === 'VO') {
      this.subscribeToAuthorizeActionResponse(this.voSalesService.authorizeAction(utilisationCleSecurite));
    } else {
      this.subscribeToAuthorizeActionResponse(this.salesService.authorizeAction(utilisationCleSecurite));
    }
  }

  subscribeToAuthorizeActionResponse(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: () => {
        this.isSaving = false;
        this.activeModal.close(true);
      },
      error: err => this.onCommonError(err),
    });
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.actionAuthorityKey().el.nativeElement.focus();
    }, 100);
  }

  protected createFrom(): UtilisationCleSecurite {
    return {
      ...new UtilisationCleSecurite(),
      entityId: this.entity.id,
      privilege: this.privilege,
      actionAuthorityKey: this.editForm.get(['actionAuthorityKey']).value,
      commentaire: this.editForm.get(['commentaire']).value,
    };
  }

  private onCommonError(error: any): void {
    this.isSaving = false;
    this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger');
  }
}
