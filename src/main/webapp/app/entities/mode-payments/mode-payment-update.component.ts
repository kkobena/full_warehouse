import { AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, viewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { Observable, Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { IPaymentMode, PaymentMode } from '../../shared/model/payment-mode.model';
import { PaymentGroup } from '../../shared/model/enumerations/payment-group.model';
import { ModePaymentService } from './mode-payment.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { Card } from 'primeng/card';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { MessageService } from 'primeng/api';
import { ErrorService } from '../../shared/error.service';

@Component({
  selector: 'jhi-mode-payment-update',
  templateUrl: './mode-payment-update.component.html',
  styleUrls: ['./mode-payment-update.component.scss'],
  providers: [MessageService],
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ToastModule,
    ButtonModule,
    RippleModule,
    InputTextModule,
    Card,
  ],
})
export class ModePaymentUpdateComponent implements OnInit, AfterViewInit, OnDestroy {
  title: string | null = null;
  entity?: IPaymentMode;
  protected isSaving = false;
  protected isValid = true;
  qrCodeFile: File | null = null;
  qrCodePreview: string | null = null;

  protected codeField = viewChild.required<ElementRef>('codeField');
  protected fb = inject(UntypedFormBuilder);
  private readonly messageService = inject(MessageService);
  private readonly errorService = inject(ErrorService);
  private readonly modePaymentService = inject(ModePaymentService);
  private readonly activeModal = inject(NgbActiveModal);
  private destroy$ = new Subject<void>();

  protected editForm = this.fb.group({
    code: [null, [Validators.required, Validators.maxLength(50)]],
    libelle: [null, [Validators.required]],
    order: [0, [Validators.required]],
  });

  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.codeField().nativeElement.focus();
    }, 100);
  }

  updateForm(paymentMode: IPaymentMode): void {
    this.editForm.patchValue({
      code: paymentMode.code,
      libelle: paymentMode.libelle,
      order: paymentMode.order,
    });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    this.isSaving = true;
    const paymentMode = this.createFromForm();
    if (paymentMode.code) {
      this.subscribeToSaveResponse(this.modePaymentService.update(paymentMode));
    } else {
      // Note: Create endpoint needs to be added to the backend
      this.subscribeToSaveResponse(this.modePaymentService.update(paymentMode));
    }
  }

  onQrCodeSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.qrCodeFile = file;

      // Preview
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.qrCodePreview = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  onQrCodeRemove(): void {
    this.qrCodeFile = null;
    this.qrCodePreview = null;
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IPaymentMode>>): void {
    result
      .pipe(
        finalize(() => (this.isSaving = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (res: HttpResponse<IPaymentMode>) => this.onSaveSuccess(res.body),
        error: (error: any) => this.onSaveError(error),
      });
  }

  private onSaveSuccess(paymentMode: IPaymentMode | null): void {
    this.activeModal.close(paymentMode);
  }

  private onSaveError(error: any): void {
    if (error.error?.errorKey) {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: this.errorService.getErrorMessage(error),
      });
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Erreur interne du serveur.',
      });
    }
  }

  private createFromForm(): IPaymentMode {
    const formValue = this.editForm.value;
    return {
      ...new PaymentMode(),
      code: formValue.code,
      libelle: formValue.libelle,
      order: formValue.order,
    };
  }
}
