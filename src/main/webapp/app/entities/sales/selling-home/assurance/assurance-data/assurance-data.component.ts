import {
  AfterViewInit,
  Component,
  computed,
  ElementRef,
  inject,
  OnInit,
  Signal,
  signal,
  viewChild,
  viewChildren,
  WritableSignal
} from '@angular/core';
import { CustomerService } from '../../../../customer/customer.service';
import { HttpResponse } from '@angular/common/http';
import { ICustomer } from '../../../../../shared/model/customer.model';
import { SelectedCustomerService } from '../../../service/selected-customer.service';
import { AssuredCustomerListComponent } from '../../../assured-customer-list/assured-customer-list.component';
import { MenuItem } from 'primeng/api';
import { FormsModule } from '@angular/forms';
import { KeyFilterModule } from 'primeng/keyfilter';
import { PanelModule } from 'primeng/panel';
import { InputTextModule } from 'primeng/inputtext';
import { IClientTiersPayant } from '../../../../../shared/model/client-tiers-payant.model';
import { FormAyantDroitComponent } from '../../../../customer/form-ayant-droit/form-ayant-droit.component';
import { SplitButtonModule } from 'primeng/splitbutton';
import {
  AyantDroitCustomerListComponent
} from '../../../ayant-droit-customer-list/ayant-droit-customer-list.component';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { CurrentSaleService } from '../../../service/current-sale.service';
import { AssureFormStepComponent } from '../../../../customer/assure-form-step/assure-form-step.component';
import { BaseSaleService } from '../../../service/base-sale.service';
import { AddComplementaireComponent } from '../add-complementaire/add-complementaire.component';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { ConfirmDialogComponent } from '../../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { Button } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { showCommonModal } from '../../sale-helper';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CustomerCarnetComponent } from '../../../../customer/carnet/customer-carnet.component';

@Component({
  selector: 'jhi-assurance-data',
  imports: [
    FormsModule,
    KeyFilterModule,
    PanelModule,
    InputTextModule,
    SplitButtonModule,
    ConfirmPopupModule,
    IconField,
    InputIcon,
    ConfirmDialogComponent,
    InputGroupModule,
    InputGroupAddonModule,
    Button,
    TooltipModule
  ],
  templateUrl: './assurance-data.component.html',
  styleUrls: ['./assurance-data.component.scss']
})
export class AssuranceDataComponent implements OnInit, AfterViewInit {
  searchInput = viewChild<ElementRef>('searchInput');
  protected search: string = null;
  protected readonly selectedCustomerService = inject(SelectedCustomerService);
  protected ayantDroit: ICustomer | null = null;
  protected items: MenuItem[] | undefined;
  protected assureBtnActions: MenuItem[] | undefined;
  protected baseSaleService = inject(BaseSaleService);
  protected readonly currentSaleService = inject(CurrentSaleService);
  protected selectedTiersPayants: WritableSignal<IClientTiersPayant[]> = signal<IClientTiersPayant[]>([]);
  protected divClass: Signal<string> = computed(() => this.getDivClassForCount(this.selectedTiersPayants().length));
  protected divCustomer: Signal<string> = computed(() => this.getDivCustomerClassForCount(this.selectedTiersPayants().length));
  private readonly customerService = inject(CustomerService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly modalService = inject(NgbModal);
  private bonInputs = viewChildren<ElementRef>('tpInput');

  constructor() {
    const assuredCustomer = this.selectedCustomerService.selectedCustomerSignal();
    this.handleCustomer(assuredCustomer);
  }

  ngOnInit(): void {
    this.items = [
      {
        icon: this.ayantDroit ? 'pi pi-pencil' : 'pi pi-plus',
        label: this.ayantDroit ? 'Modifier' : 'Nouveau',
        command: () => {
          this.addAyantDroit(this.ayantDroit);
        }
      },
      {
        icon: 'pi pi-times',
        label: 'Remplacer',
        command: () => {
          this.loadAyantDoits();
        }
      }
    ];
    this.assureBtnActions = [
      {
        icon: 'pi pi-pencil',
        label: 'Modifier',
        command: () => {
          this.editAssuredCustomer();
        }
      },
      {
        icon: 'pi pi-times',
        label: 'Changer de client',
        command: () => {
          this.onChangeCustomerClick();
        }
      }
    ];
    const currSale = this.currentSaleService.currentSale();
    if (currSale) {
      this.selectedTiersPayants.set(currSale.tiersPayants || []);
      if (this.currentSaleService.typeVo()) {
        this.ayantDroit = currSale.ayantDroit || currSale.customer;
      }
    }
  }

  openAssuredCustomerListTable(): void {
    showCommonModal(
      this.modalService,
      AssuredCustomerListComponent,
      {
        searchString: this.search,
        headerLibelle: 'CLIENTS ASSURES'
      },
      (resp: ICustomer) => {
        if (resp) {
          this.handleCustomerSelection(resp);
        }
      },
      '70%',
      'modal-dialog-70'
    );
  }

  ngAfterViewInit(): void {
    this.searchInput()?.nativeElement.focus();
  }


  buildIClientTiersPayantFromInputs(): IClientTiersPayant[] {
    const tiersPayants = this.selectedTiersPayants();
    tiersPayants.forEach((tp, index) => {
      // id="bon-{{ tp.id }}"
      const inputEl = this.bonInputs().find(bonInput => bonInput.nativeElement.id === `bon-${tp.id}`);
      if (inputEl) {
        tp.numBon = inputEl.nativeElement.value;
      }
    });
    this.selectedTiersPayants.set(tiersPayants);
    return this.selectedTiersPayants();


  }

  onChangeCustomerClick(): void {
    this.confimDialog().onConfirm(
      () => this.openAssuredCustomerListTable(),
      'Changer le client',
      'Etes-vous sûr de vouloir changer le client?',
      null
    );
  }

  reset(): void {
    this.selectedTiersPayants.set([]);
    this.ayantDroit = null;
  }

  protected addComplementaire(): void {
    const currentCustomer = this.selectedCustomerService.selectedCustomerSignal();
    showCommonModal(
      this.modalService,
      AddComplementaireComponent,
      {
        tiersPayantsExisting: this.selectedTiersPayants(),
        assure: currentCustomer
      },
      (resp: IClientTiersPayant) => {
        if (resp) {
          if (this.currentSaleService.currentSale()) {
            this.baseSaleService.onAddThirdPartySale(this.currentSaleService.currentSale()?.saleId, resp);
          }
          this.selectedTiersPayants.set([...this.selectedTiersPayants(), resp]);
          this.bonInputFocusOnAddTiersPayant(null);
        }
      },
      'xl'
    );
  }

  protected removeTiersPayant(tiersPayant: IClientTiersPayant): void {
    const updatedTp = this.selectedTiersPayants().filter(tp => tp.id !== tiersPayant.id);
    this.confimDialog().onConfirm(
      () => {
        if (this.currentSaleService.currentSale()) {
          this.baseSaleService.onRemoveThirdPartySaleLineToSalesSuccess(tiersPayant.id);
        }
        this.selectedTiersPayants.set(updatedTp);
      },
      'Supprimer tiers payant',
      'Etes-vous sûr de vouloir supprimer ce tiers payant?',
      null
    );
  }

  protected editAssuredCustomer(): void {
    this.openAssuredCustomerForm(this.selectedCustomerService.selectedCustomerSignal());
  }

  protected addAssuredCustomer(): void {
    const saleType = this.getCurrentSaleType();
    if (saleType === 'ASSURANCE') {
      this.openAssuredCustomerForm(null);
    } else if (saleType === 'CARNET') {
      showCommonModal(
        this.modalService,
        CustomerCarnetComponent,
        {
          entity: null,
          title: 'FORMULAIRE D\'AJOUT DE NOUVEAU DE CLIENT',
          categorie: 'CARNET'
        },
        (resp: ICustomer) => {
          this.setCustomer(resp, saleType);
        },
        'xl',
        'modal-dialog-70'
      );
    }
  }

  protected addAyantDroit(ayantDroit: ICustomer): void {
    this.openAyantDroitForm(ayantDroit);
  }

  protected loadAyantDoits(): void {
    const currentCustomer = this.selectedCustomerService.selectedCustomerSignal();
    showCommonModal(
      this.modalService,
      AyantDroitCustomerListComponent,
      {
        assure: currentCustomer,
        header: 'LISTE DES AYANTS DROITS DU CLIENT [' + currentCustomer.fullName + ']'
      },
      (resp: ICustomer) => {
        if (resp) {
          if (resp.id) {
            this.ayantDroit = resp;
          } else {
            this.openAyantDroitForm(resp);
          }
        }
      },
      'xl'
    );
  }

  protected onBonEnter(tp: IClientTiersPayant): void {
    const tiersPayants = this.selectedTiersPayants();
    const currentIndex = tiersPayants.findIndex(item => item.id === tp.id);
    if (currentIndex < tiersPayants.length - 1) {
      this.bonInputFocusOnAddTiersPayant(currentIndex + 1);
    } else {
      // this.focusProduct.emit();
    }
  }

  protected load(): void {
    if (!this.search) {
      return;
    }

    this.queryAssuredCustomers(this.getCurrentSaleType());
  }

  private queryAssuredCustomers(saleType: string): void {
    this.customerService
      .queryAssuredCustomer({
        search: this.search,
        size: 2,
        typeTiersPayant: saleType
      })
      .subscribe({
        next: (res: HttpResponse<ICustomer[]>) => this.handleQueryResponse(res.body, saleType)
      });
  }

  private handleQueryResponse(assuredCustomers: ICustomer[] | null, saleType: string): void {
    if (!assuredCustomers?.length) {
      this.handleNoCustomersFound();
      return;
    }

    if (assuredCustomers.length === 1) {
      this.handleSingleCustomerFound(assuredCustomers[0], saleType);
    } else {
      this.handleMultipleCustomersFound();
    }
  }

  private handleSingleCustomerFound(customer: ICustomer, saleType: string): void {
    this.selectedCustomerService.setCustomer(customer);
    this.setTiersPayantsForSaleType(customer, saleType);
    this.firstRefBonFocus();
    this.clearSearch();
  }

  private handleMultipleCustomersFound(): void {
    this.openAssuredCustomerListTable();
    this.clearSearch();
  }

  private handleNoCustomersFound(): void {
    this.addAssuredCustomer();
    this.clearSearch();
  }

  private setTiersPayantsForSaleType(customer: ICustomer, saleType: string): void {
    if (saleType === 'ASSURANCE') {
      this.selectedTiersPayants.set(customer.tiersPayants || []);
    } else if (saleType === 'CARNET' && customer.tiersPayants?.length) {
      this.selectedTiersPayants.set([customer.tiersPayants[0]]);
    }
  }

  private clearSearch(): void {
    this.search = null;
  }

  private openAssuredCustomerForm(customer: ICustomer | null): void {
    const isEdit = !!customer;
    const saleType = this.getCurrentSaleType();
    const header = isEdit ? 'FORMULAIRE DE MODIFICATION DE CLIENT' : 'FORMULAIRE D\'AJOUT DE NOUVEAU DE CLIENT';
    showCommonModal(
      this.modalService,
      AssureFormStepComponent,
      {
        entity: customer,
        typeAssure: saleType,
        header: header
      },
      (resp: ICustomer) => {
        this.setCustomer(resp, saleType);
      },
      'xl',
      'modal-dialog-80'
    );
  }

  private openAyantDroitForm(ayantDroit: ICustomer): void {
    showCommonModal(
      this.modalService,
      FormAyantDroitComponent,
      {
        entity: ayantDroit,
        assure: this.selectedCustomerService.selectedCustomerSignal(),
        title: 'FORMULAIRE DE MODIFICATION '
      },
      (resp: ICustomer) => {
        if (resp) {
          this.ayantDroit = resp;
        }
      },
      'xl'
    );
  }

  private setCustomer(cust: ICustomer, saleType: string): void {
    if (cust) {
      this.selectedCustomerService.setCustomer(cust);
      this.setTiersPayantsForSaleType(cust, saleType);
      this.firstRefBonFocus();
    }
  }

  private firstRefBonFocus(): void {
    this.focusAndSelectBonInput(0);
  }

  private bonInputFocusOnAddTiersPayant(index: number | null): void {
    this.focusAndSelectBonInput(index);
  }

  private focusAndSelectBonInput(index: number | null): void {
    setTimeout(() => {
      const inputsArray = this.bonInputs();
      const input = index !== null ? inputsArray[index] : inputsArray[inputsArray.length - 1];
      if (input) {
        input.nativeElement.focus();
        input.nativeElement.select();
      }
    }, 100);
  }

  private getDivClassForCount(count: number): string {
    if (count === 2) return 'card-wrapper tp-card-wrapper tp-count-2';
    if (count > 2) return 'card-wrapper tp-card-wrapper tp-count-many';
    return 'card-wrapper tp-card-wrapper tp-count-1';
  }

  private getDivCustomerClassForCount(count: number): string {
    return count >= 2
      ? 'card-wrapper customer-card-wrapper customer-with-multiple-tp'
      : 'card-wrapper customer-card-wrapper customer-with-single-tp';
  }

  private handleCustomerSelection(customer: ICustomer): void {
    this.selectedCustomerService.setCustomer(customer);
    this.handleCustomer(customer);
    this.firstRefBonFocus();
  }

  private getCurrentSaleType(): string {
    return this.currentSaleService.typeVo();
  }

  private handleCustomer(assuredCustomer: ICustomer): void {
    if (!assuredCustomer || this.currentSaleService.isEdit()) {
      return;
    }

    const saleType = this.getCurrentSaleType();
    this.setTiersPayantsForSaleType(assuredCustomer, saleType);

    if (saleType === 'ASSURANCE') {
      this.ayantDroit =
        assuredCustomer.ayantDroits?.find(ad => ad.id === assuredCustomer.id || ad.num === assuredCustomer.num) || assuredCustomer;
    }
  }
}
