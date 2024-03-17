import { Component, EventEmitter, Inject, Input, OnInit, Output, ViewChild } from '@angular/core';
import { ISales } from '../../../shared/model/sales.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { InputSwitchModule } from 'primeng/inputswitch';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { FormsModule } from '@angular/forms';
import { IPaymentMode } from '../../../shared/model/payment-mode.model';
import { ModePaymentService } from '../../mode-payments/mode-payment.service';
import { HttpResponse } from '@angular/common/http';
import { DOCUMENT } from '@angular/common';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { IConfiguration } from '../../../shared/model/configuration.model';
import { ConfigurationService } from '../../../shared/configuration.service';

@Component({
  selector: 'jhi-mode-reglement',
  standalone: true,
  imports: [
    WarehouseCommonModule,
    InputSwitchModule,
    KeyFilterModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    FormsModule,
    OverlayPanelModule,
  ],
  templateUrl: './mode-reglement.component.html',
  styleUrl: './mode-reglement.component.scss',
})
export class ModeReglementComponent implements OnInit {
  @Input('sale') sale: ISales;
  @Input() showModeReglementCard: boolean = true;
  isDiffere = false;
  printTicket = true;
  printInvoice = false;
  modeReglementSelected: IPaymentMode[] = [];
  @ViewChild('addOverlayPanel')
  addOverlayPanel?: any;
  selectedMode: IPaymentMode | null;
  showAddModePaimentBtn = false;
  @ViewChild('removeOverlayPanel')
  removeOverlayPanel?: any;
  maxModePayementNumber = 2;
  modeReglements: IPaymentMode[] = [];
  reglements: IPaymentMode[] = [];
  isReadonly = false;
  cashModePayment: IPaymentMode | null;
  readonly CASH = 'CASH';
  @Output('selectedModeReglement') selectedModeReglement = new EventEmitter<IPaymentMode[]>();
  @Output() isVenteDiffere = new EventEmitter<boolean>();

  constructor(
    private modePaymentService: ModePaymentService,
    @Inject(DOCUMENT) private document: Document,
    private configurationService: ConfigurationService,
  ) {}

  ngOnInit(): void {
    this.maxModePaymentNumber();
    this.loadPaymentMode();
  }

  onDiffereChange(): void {
    console.warn(this.sale);
    if (this.sale) {
      this.isVenteDiffere.emit(this.isDiffere);
    } else {
      this.isVenteDiffere.emit(false);
    }
  }

  trackPaymentModeId(index: number, item: IPaymentMode): string {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.code!;
  }

  save(): void {}

  onAddPaymentModeToggle(old: IPaymentMode, evt: any): void {
    this.onModeBtnClick(old);
    this.addOverlayPanel.toggle(evt);
  }

  manageCashPaymentMode(evt: any, modePay: IPaymentMode): void {
    /*  if (this.modeReglementSelected.length === 2) {
        const secondInputDefaultAmount = this.sale.amountToBePaid - modePay.amount; /* Number(evt.target.value)*/
    // this.modeReglementSelected.find((e: IPaymentMode) => e.code !== evt.target.id).amount = secondInputDefaultAmount;
    // const amount = this.getEntryAmount();
    // this.computeMonnaie(amount);
    //   } else {
    // this.computeMonnaie(null);
    // }
    //  this.showAddModePaymentButton(modePay);
  }

  onModeBtnClick(paymentMode: IPaymentMode): void {
    this.selectedMode = paymentMode;
  }

  onRemovePaymentModeToggle(old: IPaymentMode, evt: any): void {
    if (this.modeReglementSelected.length === 1) {
      this.onModeBtnClick(old);
      this.removeOverlayPanel.toggle(evt);
    } else {
      const oldIndex = this.modeReglementSelected.findIndex((el: IPaymentMode) => el.code === old.code);
      this.modeReglementSelected.splice(oldIndex, 1);
      this.showAddModePaymentButton(this.modeReglementSelected[0]);
    }
  }

  maxModePaymentNumber(): void {
    this.configurationService.find('APP_MODE_REGL_NUMBER').subscribe({
      next: (res: HttpResponse<IConfiguration>) => {
        if (res.body) {
          this.maxModePayementNumber = Number(res.body.value);
        }
      },
      error: () => (this.maxModePayementNumber = 2),
    });
  }

  showAddModePaymentButton(mode: IPaymentMode): void {
    this.showAddModePaimentBtn = this.modeReglementSelected?.length < this.maxModePayementNumber && mode.amount < this.sale?.amountToBePaid;
  }

  buildReglementInput(): void {
    if (this.sale && this.sale.payments.length > 0) {
      this.modeReglements.forEach((mode: IPaymentMode) => {
        const el = this.sale.payments.find(payment => payment.paymentMode.code === mode.code);
        if (el) {
          mode.amount = el.paidAmount;
          if (mode.code === this.CASH && el.montantVerse) {
            mode.amount = el.montantVerse;
            this.sale.montantVerse = el.montantVerse;
          }
          this.modeReglementSelected.push(mode);
          // this.computeMonnaie(null);
          this.setFirstInputFocused();
        }
      });
    } else {
      this.resetCashInput();
    }
    this.getReglements();
  }

  /* addModeConfirmDialog(): void {
     this.confirmationService.confirm({
       message: 'Voullez-vous ajouter un autre moyen  de payment',
       header: "AJOUT D'UN AUTRE MOYEN DE PAYMENT",
       icon: 'pi pi-info-circle',
       accept: () => {
         this.addOverlayPanel.toggle(this.getAddModePaymentButton());
       },
       reject: () => {
         this.displayErrorEntryAmountModal = true;
         this.errorEntryAmountBtn.nativeElement.focus();
       },
       key: 'addModePaymentConfirmDialog',
     });
     this.addModePaymentConfirmDialogBtn.nativeElement.focus();
   }*/

  getReglements(): void {
    this.reglements = this.modeReglements.filter(x => !this.modeReglementSelected.includes(x));
  }

  resetCashInput(): void {
    this.modeReglementSelected = [];
    this.modeReglementSelected[0] = this.cashModePayment;
    this.modeReglementSelected[0].amount = null;
    this.selectedModeReglement.emit(this.modeReglementSelected);
  }

  getInputAtIndex(index: number | null): HTMLInputElement {
    const modeInputs = this.getInputs() as HTMLInputElement[];
    const indexAt = index === 0 ? index : modeInputs.length - 1;
    if (modeInputs && modeInputs.length > 0) {
      return modeInputs[indexAt];
    }
    return null;
  }

  onAddPaymentMode(newMode: IPaymentMode): void {
    if (this.modeReglementSelected.length < this.maxModePayementNumber) {
      this.modeReglementSelected[this.modeReglementSelected.length++] = newMode;
      this.addOverlayPanel.hide();
      this.getReglements();
      //  this.updateComponent();
      setTimeout(() => {
        //   this.focusLastAddInput();
      }, 50);
    }
  }

  onRemovePaymentMode(newMode: IPaymentMode): void {
    this.changePaimentMode(newMode);
    this.removeOverlayPanel.hide();
  }

  changePaimentMode(newPaymentMode: IPaymentMode): void {
    const oldIndex = this.modeReglementSelected.findIndex((el: IPaymentMode) => (el.code = this.selectedMode.code));
    this.modeReglementSelected[oldIndex] = newPaymentMode;
    this.getReglements();
    //  this.updateComponent();
    setTimeout(() => {
      //  this.manageAmountDiv();
    }, 50);
  }

  private convertPaymentMode(res: HttpResponse<IPaymentMode[]>): IPaymentMode[] {
    this.isReadonly = this.modeReglementSelected.length > 1;
    return res.body.map((paymentMode: IPaymentMode) => this.convertmodeReglement(paymentMode));
  }

  private loadPaymentMode(): void {
    this.modePaymentService.query().subscribe((res: HttpResponse<IPaymentMode[]>) => {
      this.modeReglements = this.convertPaymentMode(res);
      this.cashModePayment = this.modeReglements.find(mode => mode.code === 'CASH');
      this.buildReglementInput();
    });
  }

  private setFirstInputFocused(): void {
    const input = this.getInputAtIndex(0);
    if (input) {
      input.focus();
      setTimeout(() => {
        input.select();
      }, 50);
    }
  }

  private convertmodeReglement(paymentMode: IPaymentMode): IPaymentMode {
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

  private getInputs(): Element[] {
    const inputs = this.document.querySelectorAll('.payment-mode-input');
    return Array.from(inputs);
  }
}
