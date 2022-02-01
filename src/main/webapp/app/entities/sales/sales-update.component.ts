import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { ActivatedRoute } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import * as moment from 'moment';
import { ISales, Sales } from 'app/shared/model/sales.model';
import { SalesService } from './sales.service';
import { ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from 'app/entities/customer/customer.service';
import { IProduit } from 'app/shared/model/produit.model';
import { ProduitService } from '../produit/produit.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { PackDialogueComponent } from './pack-dialogue.component';
import { AlertInfoComponent } from 'app/shared/alert/alert-info.component';
import { SalesLineService } from '../sales-line/sales-line.service';
import { ISalesLine, SalesLine } from 'app/shared/model/sales-line.model';
import { saveAs } from 'file-saver';
import { BtnRemoveComponent } from './btn-remove/btn-remove.component';
import { INatureVente } from '../../shared/model/nature-vente.model';
import { ITypePrescription } from '../../shared/model/prescription-vente.model';
import { IUser, User } from '../../core/user/user.model';
import { IRemiseProduit } from '../../shared/model/remise-produit.model';
import { IPaymentMode } from '../../shared/model/payment-mode.model';
import { IPayment } from '../../shared/model/payment.model';
import { UserService } from '../../core/user/user.service';
import { AccountService } from '../../core/auth/account.service';
import { ConfirmationService } from 'primeng/api';
import { ErrorService } from '../../shared/error.service';

type SelectableEntity = ICustomer | IProduit;

@Component({
  selector: 'jhi-sales-update',
  styles: [
    `
      .table tr:hover {
        cursor: pointer;
      }
    `,
  ],
  templateUrl: './sales-update.component.html',
  providers: [ConfirmationService],
})
export class SalesUpdateComponent implements OnInit {
  isSaving = false;
  showTiersPayantCard = false;
  showAyantDroitCard = false;
  showClientSearchCard = false;
  showStock = false;
  canUpdatePu = true;
  isDiffere = false;
  showInfosBancaire = false;
  showModeReglementCard = true;
  canForceStock = true;
  showInfosComplementaireReglementCard = true;
  naturesVentes: INatureVente[] = [];
  naturesVente: INatureVente | null = null;
  users: IUser[] = [];
  modeReglements: IPaymentMode[] = [];
  payments: IPayment[] = [];
  modeReglementSelected: any[] = [];
  modeReglementEmitter = new Subject<any>();
  userCaissier?: IUser | null;
  userSeller?: IUser | null = null;
  typePrescriptions: ITypePrescription[] = [];
  typePrescription?: ITypePrescription | null;
  customers: ICustomer[] = [];
  produits: IProduit[] = [];
  produitSelected?: IProduit | null = null;
  searchValue?: string;
  imagesPath!: string;
  customerSelected: ICustomer | null = null;
  selectedRowIndex?: number;
  produitsSelected?: IProduit[] = [];
  remiseProduits: IRemiseProduit[] = [];
  remiseProduit?: IRemiseProduit | null;
  sale?: ISales | null = null;
  salesLines: ISalesLine[] = [];
  quantiteSaisie = 1;
  columnDefs: any[];
  rowData: any = [];
  base64!: string;
  defaultColDef: any;
  frameworkComponents: any;
  context: any;
  event: any;
  @ViewChild('clientSearchBox', { static: false })
  clientSearchBox?: ElementRef;
  @ViewChild('quantyBox', { static: false })
  quantyBox?: ElementRef;
  @ViewChild('produitbox', { static: false })
  produitbox?: ElementRef;
  clientSearchValue?: string;
  clientBoxHeader = 'INFORMATION DU CLIENT';
  stockSeverity = 'success';
  produitClass = 'col-6 row';
  rayonClass = 'col-2';
  reglementInputClass = 'p-inputgroup';
  reglementInputParentClass = '';
  tableSearchValue?: string;
  montantCash?: number | null = null;
  montantCb?: number | null = null;
  montantVirement?: number | null = null;
  montantMtn?: number | null = null;
  montantOrange?: number | null = null;
  montantMoov?: number | null = null;
  montantWave?: number | null = null;
  montantCheque?: number | null = null;
  commentaire?: string;
  telephone?: string;
  referenceBancaire?: string;
  banque?: string;
  lieux?: string;
  montantCashDiv = false;
  montantCbDiv = false;
  montantVirementDiv = false;
  montantMtnDiv = false;
  montantOrangeDiv = false;
  montantMoovDiv = false;
  montantWaveDiv = false;
  montantChequeDiv = false;
  qtyMaxToSel = 100;

  constructor(
    protected salesService: SalesService,
    protected customerService: CustomerService,
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    protected modalService: NgbModal,
    protected saleItemService: SalesLineService,
    protected userService: UserService,
    private accountService: AccountService,
    protected confirmationService: ConfirmationService,
    protected errorService: ErrorService
  ) {
    this.imagesPath = 'data:image/';
    this.base64 = ';base64,';
    this.selectedRowIndex = 0;
    this.searchValue = '';
    this.columnDefs = [
      {
        headerName: 'Libellé',
        field: 'produitLibelle',
        sortable: true,
        filter: 'agTextColumnFilter',
        minWidth: 300,
        flex: 2,
      },
      {
        headerName: 'Quantité',
        width: 100,
        field: 'quantitySold',
        editable: true,
        type: ['rightAligned', 'numericColumn'],
      },

      {
        headerName: 'Prix unitaire',
        width: 150,
        field: 'regularUnitPrice',
        type: ['rightAligned', 'numericColumn'],
        editable: true,
        cellEditorParams: {
          color: 'red',
        },
        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Montant',
        width: 100,
        field: 'salesAmount',
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: this.formatNumber,
      },
      {
        field: ' ',
        cellRenderer: 'btnCellRenderer',
        width: 50,
      },
    ];
    this.defaultColDef = {
      // flex: 1,
      // cellClass: 'align-right',
      enableCellChangeFlash: true,
      //   resizable: true,
      /* valueFormatter: function (params) {
         return formatNumber(params.value);
       },*/
    };
    this.frameworkComponents = {
      btnCellRenderer: BtnRemoveComponent,
    };
    this.context = { componentParent: this };
  }

  ngOnInit(): void {
    this.loadAllUsers();
    this.accountService.identity().subscribe(account => {
      if (account) {
        this.userCaissier = account;
        if (!this.userSeller) {
          this.userSeller = account;
        }
      }
    });

    this.modeReglementEmitter.subscribe(() => {
      this.manageShowInfosComplementaireReglementCard();
      this.manageShowInfosBancaire();
      this.manageReglementInputParentClass();
      this.manageAmontDiv();
    });

    this.modeReglements = [
      { code: 'CASH', libelle: 'ESPECE' },
      { code: 'OM', libelle: 'ORANGE' },
      { code: 'MTN', libelle: 'MTN' },
      { code: 'MOOV', libelle: 'MOOV' },
      { code: 'WAVE', libelle: 'WAVE' },
      { code: 'CB', libelle: 'CB' },
      { code: 'VIREMENT', libelle: 'VIREMENT' },
      { code: 'CH', libelle: 'CHEQUE' },
    ];
    this.modeReglementSelected = ['CASH'];
    this.modeReglementEmitter.next(1);
    this.sale = {
      id: 1,
      salesAmount: 500,
      netAmount: 400,
      taxAmount: 100,
      discountAmount: 100,
      salesLines: [
        {
          id: 1,
          salesAmount: 100,
          netAmount: 30,
          regularUnitPrice: 100,
          produitLibelle: '22222222222222222222',
          code: '000000000',
          quantitySold: 2,
          quantityRequested: 2,
        },
      ],
    };
    if (this.sale.salesLines) {
      this.salesLines = this.sale.salesLines;
    }

    this.naturesVentes = [
      { code: 'COMPTANT', name: 'COMPTANT' } /* , {code: 'ASSURANCE', name: 'ASSURANCE'}, {
      code: 'CARNET',
      name: 'CARNET'
    }*/,
    ];

    this.typePrescriptions = [
      { code: 'PRESCRIPTION', name: 'PRESCRIPTION' },
      {
        code: 'CONSEIL',
        name: 'CONSEIL',
      },
      { code: 'DEPOT', name: 'DEPÔT' },
    ];
    this.naturesVente = { code: 'COMPTANT', name: 'COMPTANT' };
    this.typePrescription = { code: 'PRESCRIPTION', name: 'PRESCRIPTION' };
    this.activatedRoute.data.subscribe(({ sales }) => {
      if (!sales.id) {
        const today = moment().startOf('day');
        sales.createdAt = today;
        sales.updatedAt = today;
      }
      if (!this.showStock) {
        if (this.remiseProduits.length === 0) {
          this.produitClass = 'col-9 row';
          this.rayonClass = 'col-3';
        } else {
          this.produitClass = 'col-7 row';
        }
      } else {
        if (this.remiseProduits.length === 0) {
          this.produitClass = 'col-8 row';
          this.rayonClass = 'col-3';
        }
      }
      this.customerService.queryVente().subscribe((res: HttpResponse<ICustomer[]>) => (this.customers = res.body || []));
      this.loadProduits();
    });
  }

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => (this.users = res.body || []));
  }

  manageAmontDiv(): void {
    this.montantCashDiv = this.modeReglementSelected.find(e => e === 'CASH');
    this.montantCbDiv = this.modeReglementSelected.find(e => e === 'CB');
    this.montantVirementDiv = this.modeReglementSelected.find(e => e === 'VIREMENT');
    this.montantMtnDiv = this.modeReglementSelected.find(e => e === 'MTN');
    this.montantOrangeDiv = this.modeReglementSelected.find(e => e === 'OM');
    this.montantMoovDiv = this.modeReglementSelected.find(e => e === 'MOOV');
    this.montantWaveDiv = this.modeReglementSelected.find(e => e === 'WAVE');
    this.montantChequeDiv = this.modeReglementSelected.find(e => e === 'CH');
    this.resetReglementInput();
  }

  resetReglementInput(): void {
    if (!this.montantCashDiv) {
      this.montantCash = null;
    }
    if (!this.montantCbDiv) {
      this.montantCb = null;
    }

    if (!this.montantVirementDiv) {
      this.montantVirement = null;
    }
    if (!this.montantMtnDiv) {
      this.montantMtn = null;
    }
    if (!this.montantOrangeDiv) {
      this.montantOrange = null;
    }
    if (!this.montantMoovDiv) {
      this.montantMoov = null;
    }
    if (!this.montantWaveDiv) {
      this.montantWave = null;
    }
    if (!this.montantChequeDiv) {
      this.montantCheque = null;
    }
  }

  manageShowInfosComplementaireReglementCard(): void {
    const mode = (element: string) => {
      if (element === 'CB' || element === 'VIREMENT' || element === 'CH' || this.isDiffere) return true;
      return false;
    };
    this.showInfosComplementaireReglementCard = this.modeReglementSelected.some(mode);
  }

  manageShowInfosBancaire(): void {
    const mode = (element: string) => {
      if (element === 'CB' || element === 'VIREMENT' || element === 'CH') return true;
      return false;
    };
    this.showInfosBancaire = this.modeReglementSelected.some(mode);
  }

  manageReglementInputParentClass(): void {
    if (this.modeReglementSelected && this.modeReglementSelected.length < 2) {
      this.reglementInputParentClass = '';
      this.reglementInputClass = 'p-inputgroup';
    } else {
      if (this.modeReglementSelected.length === 2) {
        this.reglementInputParentClass = 'row';
        this.reglementInputClass = 'p-inputgroup col-6';
      }
      if (this.modeReglementSelected.length === 3 && !this.showInfosComplementaireReglementCard) {
        this.reglementInputParentClass = 'row';
        this.reglementInputClass = 'p-inputgroup col-4';
      }
      if (this.modeReglementSelected.length === 3 && this.showInfosComplementaireReglementCard) {
        this.reglementInputParentClass = 'row';
        this.reglementInputClass = 'p-inputgroup col-6';
      }
      if (this.modeReglementSelected.length > 3 && !this.showInfosComplementaireReglementCard) {
        this.reglementInputParentClass = 'row';
        this.reglementInputClass = 'p-inputgroup col-3';
      }
      if (this.modeReglementSelected.length > 3 && this.showInfosComplementaireReglementCard) {
        this.reglementInputParentClass = 'row';
        this.reglementInputClass = 'p-inputgroup col-6';
      }
    }
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    this.subscribeToFinalyseResponse(this.salesService.save(this.sale!));
  }

  saveAntPrint(): void {
    this.isSaving = true;
    this.subscribeToFinalyseResponse(this.salesService.save(this.sale!));
  }

  searchUser(event: any): void {
    const key = event.key;
    if (
      key !== 'ArrowDown' &&
      key !== 'ArrowUp' &&
      key !== 'ArrowRight' &&
      key !== 'ArrowLeft' &&
      key !== 'NumLock' &&
      key !== 'CapsLock' &&
      key !== 'Control' &&
      key !== 'PageUp' &&
      key !== 'PageDown'
    ) {
      this.loadProduits();
    }
  }

  onSelectUser(event: any): void {
    this.userCaissier = event;
  }

  searchFn(event: any): void {
    const key = event.key;
    if (
      key !== 'ArrowDown' &&
      key !== 'ArrowUp' &&
      key !== 'ArrowRight' &&
      key !== 'ArrowLeft' &&
      key !== 'NumLock' &&
      key !== 'CapsLock' &&
      key !== 'Control' &&
      key !== 'PageUp' &&
      key !== 'PageDown'
    ) {
      this.searchValue = event.target.value;
      this.loadProduits();
    }
  }

  produitComponentSearch(term: string, item: IProduit): boolean {
    if (item) return true;
    return false;
  }

  onSelect(event: any): void {
    this.event = event;
    if (this.quantyBox) {
      this.quantyBox.nativeElement.focus();
      this.quantyBox.nativeElement.select();
    }
  }

  onDiffereChange(e: any): void {
    const isChecked = e.checked;
    if (isChecked && this.sale && !this.customerSelected) {
      this.showClientSearchCard = true;
    } else {
      if (!this.customerSelected) {
        this.showClientSearchCard = false;
      }
    }
    this.manageShowInfosComplementaireReglementCard();
  }

  onModeReglementChange(option: any): void {
    this.modeReglementEmitter.next(1);
  }

  trackId(index: number, item: IProduit): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  trackById(index: number, item: SelectableEntity): any {
    return item.id;
  }

  onFilterTextBoxChanged(event: any): void {
    this.searchValue = event.target.value;
    this.loadProduits();
  }

  loadProduits(): void {
    this.produitService
      .query({
        page: 0,
        size: 5,
        withdetail: true,
        search: this.searchValue,
      })
      .subscribe((res: HttpResponse<any[]>) => this.onProduitSuccess(res.body));
  }

  clickRow(item: IProduit): void {
    this.selectedRowIndex = item.id;
  }

  onQuantityBoxAction(event: any): void {
    const qytMvt = Number(event.target.value);
    if (this.produitSelected !== null && this.produitSelected !== undefined && qytMvt !== 0) {
      const currentStock = this.produitSelected.totalQuantity;
      if (currentStock) {
        if (currentStock < qytMvt) {
          // TODO forcer le stock à la vente
          if (this.canForceStock) {
            this.confirmForceStock(qytMvt, ' La quantité saisie est supérieure à la quantité stock du produit. Voullez-vous continuer ?');
          } else {
            this.openInfoDialog('La quantité saisie est supérieure à la quantité stock du produit', 'alert alert-danger');
          }
        } else if (qytMvt >= this.qtyMaxToSel) {
          if (this.canForceStock) {
            this.confirmForceStock(qytMvt, ' La quantité saisie est supérieure à maximale à vendre. Voullez-vous continuer ?');
          } else {
            this.openInfoDialog('La quantité saisie est supérieure à maximale à vendre', 'alert alert-danger');
          }
        } else {
          this.onAddProduit(qytMvt);
        }
      }
    }
  }

  onAddProduit(qytMvt: number): void {
    if (this.sale) {
      if (this.produitSelected) {
        if (this.naturesVente && this.naturesVente.code === 'COMPTANT') {
          this.subscribeToSaveLineResponse(this.salesService.addItemComptant(this.createSalesLine(this.produitSelected, qytMvt)));
        } else {
          this.subscribeToSaveLineResponse(this.salesService.addItemAssurance(this.createSalesLine(this.produitSelected, qytMvt)));
        }
      }
    } else {
      if (this.produitSelected) {
        if (this.naturesVente && this.naturesVente.code === 'COMPTANT') {
          this.subscribeToCreateSaleComptantResponse(
            this.salesService.createComptant(this.createSaleComptant(this.produitSelected, qytMvt))
          );
        }
      }
    }
  }

  onAddPack(item: IProduit): void {
    if (this.customerSelected === null || this.customerSelected === undefined) {
      const message = ' Veuillez choisir le client';
      this.openInfoDialog(message, 'alert alert-info');
    } else {
      const modalRef = this.modalService.open(PackDialogueComponent, {
        backdrop: 'static',
        centered: true,
        size: '50%',
      });
      modalRef.componentInstance.produit = item;
      modalRef.componentInstance.sale = this.sale;
      modalRef.componentInstance.customer = this.customerSelected;
      modalRef.result.then(res => {
        this.sale = res;
        this.subscribeToSaveResponse(this.salesService.find(res.id));
      });
    }
  }

  print(sale: ISales | null): void {
    if (sale !== null && sale !== undefined) {
      this.salesService.print(sale.id!).subscribe(blod => saveAs(blod));
      this.sale = null;
      this.loadProduits();
      this.customerSelected = null;
    }
  }

  removeLine(data: any): void {
    this.saleItemService.delete(data.id).subscribe(() => {
      this.refresh();
    });
  }

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, { backdrop: 'static', centered: true });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  onCellValueChanged(params: any): void {
    if (Number(params.data.quantitySold) > params.data.quantityStock) {
      this.openInfoDialog('La quantité saisie est supérieure à la quantité stock du produit', 'alert alert-danger');
    } else {
      // this.subscribeToSaveLineResponse(this.saleItemService.update(this.createSalesLine(params.data)));
    }
  }

  confirmDeleteItem(item: ISalesLine): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous detacher  ce produit ?',
      header: 'RUPPRESSION DE PRODUIT ',
      icon: 'pi pi-info-circle',
      accept: () => console.error(item),

      key: 'deleteItem',
    });
  }

  confirmForceStock(qytMvt: number, message: string): void {
    this.confirmationService.confirm({
      message,
      header: 'FORCER LE STOCK',
      icon: 'pi pi-info-circle',
      accept: () => this.onAddProduit(qytMvt),
      key: 'forcerStock',
    });
  }

  restAll(): void {
    this.sale = null;
    this.salesLines = [];
    this.modeReglementSelected = [];
    this.modeReglementSelected = ['CASH'];
    this.modeReglementEmitter.next(1);
    this.naturesVente = { code: 'COMPTANT', name: 'COMPTANT' };
    this.typePrescription = { code: 'PRESCRIPTION', name: 'PRESCRIPTION' };
    this.userSeller = this.userCaissier;
    this.produitbox?.nativeElement.focus();
  }

  protected subscribeToSaveLineResponse(result: Observable<HttpResponse<ISalesLine>>): void {
    result.subscribe(
      (res: HttpResponse<ISalesLine>) => this.subscribeToSaveResponse(this.salesService.find(res.body?.saleId!)),
      () => this.onSaveError()
    );
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe(
      (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      () => this.onSaveError()
    );
  }

  protected onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  private createSaleComptant(produit: IProduit, quantitySold: number): ISales {
    return {
      ...new Sales(),
      salesLines: [this.createSalesLine(produit, quantitySold)],
      customer: this.customerSelected!,
      natureVente: this.naturesVente?.code,
      typePrescription: this.typePrescription?.code,
      cassier: this.userCaissier!,
      seller: this.userSeller!,
      type: 'VNO',
    };
  }

  private createSalesLine(produit: IProduit, quantitySold: number): ISalesLine {
    return {
      ...new SalesLine(),
      produitId: produit.id,
      regularUnitPrice: produit.regularUnitPrice,
      saleId: this.sale?.id,
      quantitySold,
      sales: this.sale,
    };
  }

  private updateSalesLine(item: ISalesLine): ISalesLine {
    return {
      ...new SalesLine(),
      produitId: item.produit?.id,
      regularUnitPrice: item.regularUnitPrice,
      saleId: this.sale?.id,
      quantitySold: item.quantitySold,
      sales: this.sale,
    };
  }

  protected onSaveSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.sale = sale!;
    this.rowData = this.sale.salesLines;
  }

  protected onSaveError(): void {
    this.isSaving = false;
    const message = 'Une erruer est survenue';
    this.openInfoDialog(message, 'alert alert-danger');
  }

  protected onFinalyseSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.print(sale);
  }

  protected subscribeToFinalyseResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe(
      (res: HttpResponse<ISales>) => this.onFinalyseSuccess(res.body),
      () => this.onSaveError()
    );
  }

  protected refresh(): void {
    this.subscribeToSaveResponse(this.salesService.find(this.sale?.id!));
  }

  protected subscribeToCreateSaleComptantResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe(
      (res: HttpResponse<ISales>) => this.onSaleComptantResponseSuccess(res.body),
      error => this.onCommonError(error)
    );
  }

  protected onSaleComptantResponseSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.sale = sale;
    if (sale && sale.salesLines) this.salesLines = sale?.salesLines;
  }

  protected onActionError(el: ISalesLine, error: any): void {
    if (error.error) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(translatedErrorMessage => {
        this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
      });
    }
  }

  protected onCommonError(error: any): void {
    if (error.error) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(
        translatedErrorMessage => {
          this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
        },
        () => this.openInfoDialog(error.title, 'alert alert-danger')
      );
    }
  }
}
