import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { InputNumber } from 'primeng/inputnumber';
import { CashRegisterService } from '../../cash-register.service';
import { ConfigurationService } from '../../../../shared/configuration.service';
import { ErrorService } from '../../../../shared/error.service';
import { Toast } from 'primeng/toast';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'jhi-cash-register-form',
  providers: [MessageService],
  imports: [Button, ReactiveFormsModule, InputNumber, Toast],
  templateUrl: './cash-register-form.component.html',
  styleUrl: './cash-register-form.component.scss',
})
export class CashRegisterFormComponent implements OnInit, AfterViewInit {
  protected isSaving = false;
  protected cashFundAmount: number | null = null;
  protected cashFundAmountInput = viewChild<InputNumber>('cashFundAmountInput');
  protected fb = inject(FormBuilder);
  protected editForm = this.fb.group({
    cashFundAmount: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0), Validators.max(1000000)],
      nonNullable: true,
    }),
  });
  private readonly activeModal = inject(NgbActiveModal);
  private readonly entityService = inject(CashRegisterService);
  private readonly configService = inject(ConfigurationService);
  private readonly errorService = inject(ErrorService);
  private readonly messageService = inject(MessageService);

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  ngAfterViewInit(): void {
    this.setCashFundControlFocus();
  }

  ngOnInit(): void {
    this.configService.find('APP_CASH_FUND').subscribe(res => {
      if (res.body) {
        const otherValue = res.body.otherValue;
        if (otherValue) {
          this.cashFundAmount = parseInt(otherValue);
        }
        this.editForm.get(['cashFundAmount']).setValue(this.cashFundAmount);
      }
    });
  }

  protected openCashRegister(): void {
    if (this.editForm.valid) {
      this.entityService.openCashRegister({ cashFundAmount: this.editForm.get(['cashFundAmount']).value }).subscribe({
        next: res => {
          if (res.body) {
            this.activeModal.close(true);
          }
        },
        error: err => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: this.errorService.getErrorMessage(err),
          });
        },
      });
    }
  }

  private setCashFundControlFocus(): void {
    setTimeout(() => {
      const cashFundAmountInput = this.cashFundAmountInput();
      const inputElement = cashFundAmountInput?.input?.nativeElement;
      inputElement?.focus();
      this.editForm.get(['cashFundAmount'])?.setValue(this.cashFundAmount);
      inputElement?.select();
    }, 100);
  }
}
