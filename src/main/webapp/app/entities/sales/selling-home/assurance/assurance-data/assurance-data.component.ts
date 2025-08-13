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
  WritableSignal,
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
import { AyantDroitCustomerListComponent } from '../../../ayant-droit-customer-list/ayant-droit-customer-list.component';
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
    TooltipModule,
  ],
  templateUrl: './assurance-data.component.html',
  styleUrls: ['./assurance-data.component.scss'],
})
export class AssuranceDataComponent implements OnInit, AfterViewInit {
  searchInput = viewChild<ElementRef>('searchInput');
  protected search: string = null;
  protected readonly selectedCustomerService = inject(SelectedCustomerService);
  protected ayantDroit: ICustomer | null = null;
  protected items: MenuItem[] | undefined;
  protected baseSaleService = inject(BaseSaleService);
  protected readonly currentSaleService = inject(CurrentSaleService);
  protected selectedTiersPayants: WritableSignal<IClientTiersPayant[]> = signal<IClientTiersPayant[]>([]);
  protected divClass: Signal<string> = computed(() => {
    const count = this.selectedTiersPayants().length;
    return count === 2 ? 'col-md-3 col-sm-3 col-3 bon' : count > 2 ? 'col-md-2 col-sm-2 col-2 bon' : 'col-md-4 col-sm-4 col-4 bon';
  });
  protected divCustomer: Signal<string> = computed(() => {
    return this.selectedTiersPayants().length >= 2
      ? 'col-md-3 col-sm-3 col-lg-3 col-xl-3 col-sm-12 '
      : 'col-md-4 col-sm-4 col-xl-4 col-sm-12';
  });
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
        },
      },
      {
        icon: 'pi pi-times',
        label: 'Remplacer',
        command: () => {
          this.loadAyantDoits();
        },
      },
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
        headerLibelle: 'CLIENTS ASSURES',
      },
      (resp: ICustomer) => {
        if (resp) {
          this.selectedCustomerService.setCustomer(resp);
          this.handleCustomer(resp);
          this.firstRefBonFocus();
        }
      },
      '70%',
      'modal-dialog-70',
    );
  }

  ngAfterViewInit(): void {
    this.searchInput()?.nativeElement.focus();
  }

  buildIClientTiersPayantFromInputs(): IClientTiersPayant[] {
    return this.selectedTiersPayants();
  }

  onChangeCustomerClick(): void {
    this.confimDialog().onConfirm(
      () => this.openAssuredCustomerListTable(),
      'Changer le client',
      'Etes-vous sûr de vouloir changer le client?',
      null,
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
        assure: currentCustomer,
      },
      (resp: IClientTiersPayant) => {
        if (resp) {
          if (this.currentSaleService.currentSale()) {
            this.baseSaleService.onAddThirdPartySale(this.currentSaleService.currentSale()?.id, resp);
          }
          this.selectedTiersPayants.set([...this.selectedTiersPayants(), resp]);
          this.bonInputFocusOnAddTiersPayant(null);
        }
      },
      'xl',
    );
  }

  protected removeTiersPayant(tiersPayant: IClientTiersPayant): void {
    const currentTp = this.selectedTiersPayants();
    const updatedTp = currentTp.filter(tp => tp.id !== tiersPayant.id);
    this.confimDialog().onConfirm(
      () => {
        if (this.currentSaleService.currentSale()) {
          this.baseSaleService.onRemoveThirdPartySaleLineToSalesSuccess(tiersPayant.id);
          this.selectedTiersPayants.set(updatedTp);
        } else {
          this.selectedTiersPayants.set(updatedTp);
        }
      },
      'Supprimer tiers payant',
      'Etes-vous sûr de vouloir supprimer ce tiers payant?',
      null,
    );
  }

  protected editAssuredCustomer(): void {
    this.openAssuredCustomerForm(this.selectedCustomerService.selectedCustomerSignal());
  }

  protected addAssuredCustomer(): void {
    if (this.currentSaleService.typeVo() === 'ASSURANCE') {
      this.openAssuredCustomerForm(null);
    } else if (this.currentSaleService.typeVo() === 'CARNET') {
      showCommonModal(
        this.modalService,
        CustomerCarnetComponent,
        {
          entity: null,
          header: "FORMULAIRE D'AJOUT DE NOUVEAU DE CLIENT",
          categorie: 'CARNET',
        },
        (resp: ICustomer) => {
          this.setCustomer(resp);
        },
        'xl',
        'modal-dialog-70',
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
        header: 'LISTE DES AYANTS DROITS DU CLIENT [' + currentCustomer.fullName + ']',
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
      'xl',
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
    const saleType = this.currentSaleService.typeVo();
    if (this.search) {
      this.customerService
        .queryAssuredCustomer({
          search: this.search,
          size: 2,
          typeTiersPayant: saleType,
        })
        .subscribe({
          next: (res: HttpResponse<ICustomer[]>) => {
            const assuredCustomers = res.body;
            if (assuredCustomers && assuredCustomers.length) {
              if (assuredCustomers.length === 1) {
                const assuredCustomer = assuredCustomers[0];
                this.selectedCustomerService.setCustomer(assuredCustomer);
                if (saleType === 'ASSURANCE') {
                  this.selectedTiersPayants.set(assuredCustomer.tiersPayants || []);
                  /*  this.ayantDroit =
                    assuredCustomer.ayantDroits?.find(ad => ad.id === assuredCustomer.id || ad.num === assuredCustomer.num) || assuredCustomer;*/
                } else if (saleType === 'CARNET' && assuredCustomer.tiersPayants?.length) {
                  this.selectedTiersPayants.set([assuredCustomer.tiersPayants[0]]);
                }
                this.firstRefBonFocus();
              } else {
                this.openAssuredCustomerListTable();
              }
              this.search = null;
            } else {
              this.addAssuredCustomer();
              this.search = null;
            }
          },
          error() {},
        });
    }
  }

  private openAssuredCustomerForm(customer: ICustomer | null): void {
    const isEdit = !!customer;
    const header = isEdit ? 'FORMULAIRE DE MODIFICATION DE CLIENT' : "FORMULAIRE D'AJOUT DE NOUVEAU DE CLIENT";
    showCommonModal(
      this.modalService,
      AssureFormStepComponent,
      {
        entity: customer,
        typeAssure: this.currentSaleService.typeVo(),
        header: header,
      },
      (resp: ICustomer) => {
        this.setCustomer(resp);
      },
      'xl',
      'modal-dialog-80',
    );
  }

  private openAyantDroitForm(ayantDroit: ICustomer): void {
    showCommonModal(
      this.modalService,
      FormAyantDroitComponent,
      {
        entity: ayantDroit,
        assure: this.selectedCustomerService.selectedCustomerSignal(),
        header: 'FORMULAIRE DE MODIFICATION ',
      },
      (resp: ICustomer) => {
        if (resp) {
          this.ayantDroit = resp;
        }
      },
      'xl',
    );
  }

  private setCustomer(cust: ICustomer): void {
    if (cust) {
      this.selectedCustomerService.setCustomer(cust);
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

  private handleCustomer(assuredCustomer: ICustomer): void {
    if (assuredCustomer && !this.currentSaleService.isEdit()) {
      const saleType = this.currentSaleService.typeVo();
      if (saleType === 'ASSURANCE') {
        this.selectedTiersPayants.set(assuredCustomer.tiersPayants || []);
        this.ayantDroit =
          assuredCustomer.ayantDroits?.find(ad => ad.id === assuredCustomer.id || ad.num === assuredCustomer.num) || assuredCustomer;
      } else if (saleType === 'CARNET' && assuredCustomer.tiersPayants?.length) {
        this.selectedTiersPayants.set([assuredCustomer.tiersPayants[0]]);
      }
    }
  }
}
