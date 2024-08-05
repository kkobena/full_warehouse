import { Injectable, signal, WritableSignal } from '@angular/core';
import { IPaymentMode } from '../../../shared/model/payment-mode.model';
import { ModePaymentService } from '../../mode-payments/mode-payment.service';
import { HttpResponse } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class SelectModeReglementService {
  modeReglements: WritableSignal<IPaymentMode[]> = signal<IPaymentMode[]>([]);
  allModeReglements: WritableSignal<IPaymentMode[]> = signal<IPaymentMode[]>([]);
  isReadonly: boolean = true;
  paymentModes: IPaymentMode[] = [];

  constructor(private modePaymentService: ModePaymentService) {
    if (this.allModeReglements().length === 0) {
      this.modePaymentService.query().subscribe((res: HttpResponse<IPaymentMode[]>) => {
        this.paymentModes = this.convertPaymentModes(res);
        this.allModeReglements.set(this.paymentModes);
        this.selectCashModePayment();
      });
    } else {
      this.selectCashModePayment();
    }
  }

  resetAllModeReglements(): void {
    this.paymentModes = this.allModeReglements();
    this.selectCashModePayment();
  }

  getaAvaillablePaymentsMode(): IPaymentMode[] {
    return this.paymentModes.filter(x => !this.modeReglements().includes(x));
  }

  update(paymentMode: IPaymentMode): void {
    this.modeReglements.update(values => {
      return [...values, paymentMode];
    });
  }

  setModePayments(paymentModes: IPaymentMode[]): void {
    this.modeReglements.set(paymentModes);
  }

  changeModePayment(paymentMode: IPaymentMode): void {
    this.modeReglements.set([paymentMode]);
  }

  remove(paymentMode: IPaymentMode): void {
    this.modeReglements.update(values => {
      return values.filter(e => e.code !== paymentMode.code);
    });
  }

  selectCashModePayment(): void {
    const cashControl = this.paymentModes.find(mode => mode.code === 'CASH') as IPaymentMode;
    this.modeReglements.set([cashControl]);
  }

  resetAmounts(): void {
    this.paymentModes.forEach(mode => {
      if (mode.amount) {
        mode.amount = null;
      }
    });
  }

  private convertPaymentModes(res: HttpResponse<IPaymentMode[]>): IPaymentMode[] {
    return res.body.map((paymentMode: IPaymentMode) => this.convertPaymentMode(paymentMode));
  }

  private convertPaymentMode(paymentMode: IPaymentMode): IPaymentMode {
    paymentMode.disabled = false;
    switch (paymentMode.code) {
      case 'CASH':
        paymentMode.styleImageClass = 'cash';
        paymentMode.styleBtnClass = 'cash-btn';
        break;
      case 'WAVE':
        paymentMode.styleImageClass = 'wave';
        paymentMode.styleBtnClass = 'wave-btn';
        paymentMode.isReadonly = this.isReadonly;
        break;
      case 'OM':
        paymentMode.styleImageClass = 'om';
        paymentMode.styleBtnClass = 'om-btn';
        paymentMode.isReadonly = true;
        break;
      case 'CB':
        paymentMode.styleImageClass = 'cb';
        paymentMode.styleBtnClass = 'cb-btn';
        paymentMode.isReadonly = this.isReadonly;
        break;
      case 'MOOV':
        paymentMode.styleImageClass = 'moov';
        paymentMode.styleBtnClass = 'moov-btn';
        paymentMode.isReadonly = this.isReadonly;
        break;
      case 'MTN':
        paymentMode.styleImageClass = 'mtn';
        paymentMode.styleBtnClass = 'mtn-btn';
        paymentMode.isReadonly = this.isReadonly;
        break;
      case 'CH':
        paymentMode.styleImageClass = 'cheque';
        paymentMode.styleBtnClass = 'cheque-btn';
        paymentMode.isReadonly = true;
        break;
      case 'VIREMENT':
        paymentMode.styleImageClass = 'virement';
        paymentMode.styleBtnClass = 'virement-btn';
        paymentMode.isReadonly = this.isReadonly;
        break;
      default:
        break;
    }
    return paymentMode;
  }
}
