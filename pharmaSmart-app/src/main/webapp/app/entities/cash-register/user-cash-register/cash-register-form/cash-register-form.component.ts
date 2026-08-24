import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild, ChangeDetectionStrategy, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { CashRegisterService } from '../../cash-register.service';
import { ConfigurationService } from '../../../../shared/configuration.service';
import { ErrorService } from '../../../../shared/error.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ButtonComponent, InputNumberComponent } from '../../../../shared/ui';

import { DeviseDirective } from 'app/shared/utils/devise';
@Component({
  selector: 'jhi-cash-register-form',
  imports: [DeviseDirective, ButtonComponent, ReactiveFormsModule, InputNumberComponent],
  templateUrl: './cash-register-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './cash-register-form.component.scss',
})
export class CashRegisterFormComponent implements OnInit, AfterViewInit {
  protected isSaving = false;
  protected readonly cashFundAmount = signal<number | null>(null);
  protected cashFundAmountInput = viewChild('cashFundAmountInput', { read: ElementRef<HTMLElement> });
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
  private readonly notificationService = inject(NotificationService);

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
          this.cashFundAmount.set(parseInt(otherValue));
        }
        this.editForm.get(['cashFundAmount']).setValue(this.cashFundAmount());
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
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
        },
      });
    }
  }

  private setCashFundControlFocus(): void {
    setTimeout(() => {
      const inputElement = this.cashFundAmountInput()?.nativeElement.querySelector('input');
      inputElement?.focus();
      this.editForm.get(['cashFundAmount'])?.setValue(this.cashFundAmount());
      inputElement?.select();
    }, 100);
  }
}
