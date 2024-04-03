import { Component, EventEmitter, Inject, Input, OnInit, Output, ViewChild } from '@angular/core';
import { ISales } from '../../../shared/model/sales.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { InputSwitchModule } from 'primeng/inputswitch';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { FormsModule } from '@angular/forms';
import { IPaymentMode, PaymentModeControl } from '../../../shared/model/payment-mode.model';
import { DOCUMENT } from '@angular/common';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { CurrentSaleService } from '../service/current-sale.service';
import { SelectModeReglementService } from '../service/select-mode-reglement.service';
import { CustomerDataTableComponent } from '../uninsured-customer-list/customer-data-table.component';

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
    CustomerDataTableComponent,
  ],
  templateUrl: './mode-reglement.component.html',
  styleUrls: ['./mode-reglement.component.scss'],
})
export class ModeReglementComponent implements OnInit {
  @Input() showModeReglementCard: boolean = true;
  @Output() isVenteDiffere = new EventEmitter<boolean>();
  @Output() paymentModeControlEvent = new EventEmitter<PaymentModeControl>();
  showInfosComplementaireReglementCard: boolean = false;
  showInfosBancaire: boolean = false;
  commentaire: string = null;
  referenceBancaire: string = null;
  banque: string = null;
  lieux: string = null;
  readonly CASH = 'CASH';
  readonly COMPTANT = 'COMPTANT';
  readonly CARNET = 'CARNET';
  readonly ASSURANCE = 'ASSURANCE';
  readonly OM = 'OM';
  readonly CB = 'CB';
  readonly CH = 'CH';
  readonly VIREMENT = 'VIREMENT';
  readonly WAVE = 'WAVE';
  readonly MOOV = 'MOOV';
  readonly MTN = 'MTN';
  reglementsModes: IPaymentMode[];

  //configurationService = inject(ConfigurationService);
  protected isDiffere = false;
  protected printTicket = true;
  protected printInvoice = false;
  @ViewChild('addOverlayPanel')
  protected addOverlayPanel?: any;
  protected paymentModeToChange: IPaymentMode | null;
  protected showAddModePaimentBtn = false;
  @ViewChild('removeOverlayPanel')
  protected removeOverlayPanel?: any;
  protected maxModePayementNumber = 2;
  protected isReadonly = false;
  protected sale: ISales;

  constructor(
    protected selectModeReglementService: SelectModeReglementService,
    @Inject(DOCUMENT) private document: Document,
    //  private configurationService: ConfigurationService,
    private currentSaleService: CurrentSaleService,
  ) {
    this.sale = this.currentSaleService.currentSale();
    this.updateAvalibleMode();
  }

  ngOnInit(): void {
    this.maxModePaymentNumber();
    this.buildReglementInput();
  }

  onDiffereChange(): void {
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
    this.paymentModeControlEvent.emit({ paymentMode: modePay, control: evt });
  }

  onModeBtnClick(paymentMode: IPaymentMode): void {
    this.paymentModeToChange = paymentMode;
  }

  onRemovePaymentModeToggle(old: IPaymentMode, evt: any): void {
    if (this.selectModeReglementService.modeReglements().length === 1) {
      this.onModeBtnClick(old);
      this.removeOverlayPanel.toggle(evt);
    } else {
      const mds = this.selectModeReglementService.modeReglements();
      const modeToRemove = mds.find((el: IPaymentMode) => el.code === old.code);
      if (modeToRemove) {
        this.selectModeReglementService.remove(modeToRemove);
        this.updateAvalibleMode();
      }

      this.showAddModePaymentButton(this.selectModeReglementService.modeReglements()[0]);
    }
  }

  maxModePaymentNumber(): void {
    this.maxModePayementNumber = 2;
    /*  this.configurationService.find('APP_MODE_REGL_NUMBER').subscribe({
        next: (res: HttpResponse<IConfiguration>) => {
          if (res.body) {
            this.maxModePayementNumber = Number(res.body.value);
          }
        },
        error: () => (this.maxModePayementNumber = 2),
      });*/
  }

  showAddModePaymentButton(mode: IPaymentMode): void {
    this.showAddModePaimentBtn =
      this.selectModeReglementService.modeReglements().length < this.maxModePayementNumber &&
      mode.amount > 0 &&
      mode.amount < this.sale?.amountToBePaid;
  }

  manageShowInfosBancaire(): void {
    const mode = (element: IPaymentMode) => element.code === this.CB || this.VIREMENT || element.code === this.CH;
    this.showInfosBancaire = this.selectModeReglementService.modeReglements().some(mode);
  }

  buildReglementInput(): void {
    if (this.sale && this.sale.payments.length > 0) {
      this.selectModeReglementService.paymentModes.forEach((mode: IPaymentMode) => {
        const el = this.sale.payments.find(payment => payment.paymentMode.code === mode.code);
        if (el) {
          mode.amount = el.paidAmount;
          if (mode.code === this.CASH && el.montantVerse) {
            mode.amount = el.montantVerse;
            this.sale.montantVerse = el.montantVerse;
          }
          this.selectModeReglementService.update(mode);
          this.updateAvalibleMode();
          /*this.reglements.set(this.selectModeReglementService.paymentModes.filter(x => !this.modeReglementSelected.includes(x)));*/
          // this.modeReglementSelected.push(mode);
          // this.computeMonnaie(null);
        }
      });
    } else {
      this.resetCashInput();
    }
    //   this.getReglements();
    this.setFirstInputFocused();
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

  resetCashInput(): void {
    /*  this.modeReglementSelected = [];
     // this.modeReglementSelected[0] = this.cashModePayment;
      this.modeReglementSelected[0].amount = null;
      this.selectedModeReglement.emit(this.modeReglementSelected);*/
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
    const oldModes = this.selectModeReglementService.modeReglements();
    if (oldModes.length < this.maxModePayementNumber) {
      oldModes[oldModes.length++] = newMode;
      this.selectModeReglementService.setModePayments(oldModes);
      this.updateAvalibleMode();
      this.addOverlayPanel.hide();
      // this.getReglements();
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
    const oldModes = this.selectModeReglementService.modeReglements();
    const oldIndex = oldModes.findIndex((el: IPaymentMode) => (el.code = this.paymentModeToChange.code));
    oldModes[oldIndex] = newPaymentMode;

    // this.getReglements();
    this.selectModeReglementService.setModePayments(oldModes);
    this.updateAvalibleMode();
    /* //  this.updateComponent();
     setTimeout(() => {
       //  this.manageAmountDiv();
     }, 50);*/
  }

  manageAmountDiv(): void {
    const input = this.getInputAtIndex(0);

    if (input) {
      this.selectModeReglementService.modeReglements().find((e: IPaymentMode) => e.code === input.id).amount = this.sale.amountToBePaid;
      input.focus();
      setTimeout(() => {
        input.select();
      }, 50);
    }
  }

  private updateAvalibleMode(): void {
    this.reglementsModes = this.selectModeReglementService.getaAvaillablePaymentsMode();
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

  private getInputs(): Element[] {
    const inputs = this.document.querySelectorAll('.payment-mode-input');
    return Array.from(inputs);
  }

  private modePaymentEqualCheck(mode1: IPaymentMode, mode2: IPaymentMode) {
    return mode1.code === mode2.code;
  }
}
