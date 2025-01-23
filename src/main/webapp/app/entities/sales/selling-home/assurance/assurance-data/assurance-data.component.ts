import { AfterViewInit, Component, effect, ElementRef, Inject, inject, OnInit, signal, viewChild, WritableSignal } from '@angular/core';
import { CustomerService } from '../../../../customer/customer.service';
import { HttpResponse } from '@angular/common/http';
import { ICustomer } from '../../../../../shared/model/customer.model';
import { SelectedCustomerService } from '../../../service/selected-customer.service';
import { AssuredCustomerListComponent } from '../../../assured-customer-list/assured-customer-list.component';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { FormsModule } from '@angular/forms';
import { KeyFilterModule } from 'primeng/keyfilter';
import { PanelModule } from 'primeng/panel';
import { InputTextModule } from 'primeng/inputtext';
import { DOCUMENT } from '@angular/common';
import { IClientTiersPayant } from '../../../../../shared/model/client-tiers-payant.model';
import { FormAyantDroitComponent } from '../../../../customer/form-ayant-droit/form-ayant-droit.component';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { TableModule } from 'primeng/table';
import { SpeedDialModule } from 'primeng/speeddial';
import { SplitButtonModule } from 'primeng/splitbutton';
import { AyantDroitCustomerListComponent } from '../../../ayant-droit-customer-list/ayant-droit-customer-list.component';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CurrentSaleService } from '../../../service/current-sale.service';
import { AssureFormStepComponent } from '../../../../customer/assure-form-step/assure-form-step.component';
import { BaseSaleService } from '../../../service/base-sale.service';
import { AddComplementaireComponent } from '../add-complementaire/add-complementaire.component';

@Component({
    selector: 'jhi-assurance-data',
    providers: [ConfirmationService, DialogService, MessageService],
    imports: [
        FormsModule,
        KeyFilterModule,
        PanelModule,
        InputTextModule,
        OverlayPanelModule,
        TableModule,
        SpeedDialModule,
        SplitButtonModule,
        ConfirmPopupModule,
        ConfirmDialogModule,
    ],
    templateUrl: './assurance-data.component.html'
})
export class AssuranceDataComponent implements OnInit, AfterViewInit {
  customerService = inject(CustomerService);
  search: string = null;
  selectedCustomerService = inject(SelectedCustomerService);
  confirmationService = inject(ConfirmationService);
  currentSaleService = inject(CurrentSaleService);
  searchInput = viewChild<ElementRef>('searchInput');
  dialogService = inject(DialogService);
  divClass = 'col-md-4 col-sm-4 col-4 bon';
  divCustomer = 'col-md-4 col-sm-4 col-4';
  ref: DynamicDialogRef;
  ayantDroit: ICustomer | null = null;
  selectedTiersPayants: WritableSignal<IClientTiersPayant[]> = signal<IClientTiersPayant[]>([]);
  items: MenuItem[] | undefined;
  messageService = inject(MessageService);
  baseSaleService = inject(BaseSaleService);

  constructor(@Inject(DOCUMENT) private document: Document) {
    effect(
      () => {
        const assuredCustomer = this.selectedCustomerService.selectedCustomerSignal();
        if (!this.currentSaleService.isEdit()) {
          if (this.currentSaleService.typeVo() === 'ASSURANCE') {
            this.selectedTiersPayants.set(assuredCustomer?.tiersPayants || []);
            this.ayantDroit =
              assuredCustomer?.ayantDroits.find(ad => ad.id === assuredCustomer.id || ad.num === assuredCustomer.num) || null;
            if (!this.ayantDroit) {
              this.ayantDroit = assuredCustomer;
            }
          } else if (this.currentSaleService.typeVo() === 'CARNET') {
            this.selectedTiersPayants.set([assuredCustomer?.tiersPayants[0]]);
          }
        }
      },
      { allowSignalWrites: true },
    );
    effect(() => {
      const tpSize = this.selectedTiersPayants()?.length || 0;
      if (tpSize === 2) {
        this.divClass = 'col-md-3 col-sm-3 col-3 bon';
        this.divCustomer = 'col-md-3 col-sm-3 col-3';
      } else if (tpSize > 2) {
        this.divClass = 'col-md-2 col-sm-2 col-2 bon';
        this.divCustomer = 'col-md-3 col-sm-3 col-3';
      } else {
        this.divClass = 'col-md-4 col-sm-4 col-4 bon';
        this.divCustomer = 'col-md-4 col-sm-4 col-4';
      }
    });
  }

  ngOnInit(): void {
    this.items = [
      {
        icon: 'pi pi-pencil',
        label: 'Modifier',
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

  load(): void {
    if (this.search) {
      this.customerService
        .queryAssuredCustomer({
          search: this.search,
          // search: '2201268',
          // search: '1807874',
          size: 2,
          typeTiersPayant: this.currentSaleService.typeVo(),
        })
        .subscribe({
          next: (res: HttpResponse<ICustomer[]>) => {
            const assuredCustomers = res.body;
            if (assuredCustomers && assuredCustomers.length > 0) {
              if (assuredCustomers.length === 1) {
                this.selectedCustomerService.setCustomer(assuredCustomers[0]);
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
          error: () => {},
        });
    }
  }

  openAssuredCustomerListTable(): void {
    this.ref = this.dialogService.open(AssuredCustomerListComponent, {
      data: { searchString: this.search },
      header: 'CLIENTS  ASSURES',
      width: '70%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.selectedCustomerService.setCustomer(resp);
        this.firstRefBonFocus();
      }
    });
  }

  ngAfterViewInit(): void {
    this.searchInput()?.nativeElement.focus();
  }

  addComplementaire(): void {
    const currentCustomer = this.selectedCustomerService.selectedCustomerSignal();
    this.ref = this.dialogService.open(AddComplementaireComponent, {
      data: { tiersPayantsExisting: this.selectedTiersPayants(), assure: currentCustomer },
      header: 'AJOUTER UN TIERS PAYANT COMPLEMENTAIRE',
      width: '45%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: IClientTiersPayant) => {
      if (resp) {
        if (this.currentSaleService.currentSale()) {
          this.baseSaleService.onAddThirdPartySale(this.currentSaleService.currentSale().id, resp);
        }
        this.selectedTiersPayants.set([...this.selectedTiersPayants(), resp]);
        this.bonInputFocusOnAddTiersPayant(null);
      }
    });
  }

  removeTiersPayant(tiersPayant: IClientTiersPayant): void {
    this.confirmationService.confirm({
      message: 'Etes-vous sûr de vouloir supprimer ce tiers payant?',
      header: 'Supprimer tiers payant',
      icon: 'pi pi-info-circle',
      accept: () => {
        if (this.currentSaleService.currentSale()) {
          this.baseSaleService.onRemoveThirdPartySaleLineToSalesSuccess(tiersPayant.id);
          this.selectedTiersPayants.set(this.selectedTiersPayants().filter(tp => tp.id !== tiersPayant.id));
        } else {
          this.selectedTiersPayants.set(this.selectedTiersPayants().filter(tp => tp.id !== tiersPayant.id));
        }
      },
      key: 'deleteTiersPayant',
    });
  }

  editAssuredCustomer(): void {
    this.ref = this.dialogService.open(AssureFormStepComponent, {
      data: {
        entity: this.selectedCustomerService.selectedCustomerSignal(),
        typeAssure: this.currentSaleService.typeVo(),
      },
      header: 'FORMULAIRE DE MODIFICATION DE CLIENT ',
      width: '80%',
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.selectedCustomerService.setCustomer(resp);
      }
    });
  }

  addAssuredCustomer(): void {
    this.ref = this.dialogService.open(AssureFormStepComponent, {
      data: { entity: null, typeAssure: this.currentSaleService.typeVo() },
      header: "FORMULAIRE D'AJOUT DE NOUVEAU DE CLIENT",
      width: '80%',
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.selectedCustomerService.setCustomer(resp);
      }
    });
  }

  addAyantDroit(ayantDroit: ICustomer): void {
    this.ref = this.dialogService.open(FormAyantDroitComponent, {
      data: {
        entity: ayantDroit,
        assure: this.selectedCustomerService.selectedCustomerSignal(),
      },
      header: 'FORMULAIRE DE MODIFICATION ',
      width: '45%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.ayantDroit = resp;
      }
    });
  }

  loadAyantDoits(): void {
    const currentCustomer = this.selectedCustomerService.selectedCustomerSignal();
    this.ref = this.dialogService.open(AyantDroitCustomerListComponent, {
      data: { assure: currentCustomer },
      header: 'LISTE DES AYANTS DROITS DU CLIENT [' + currentCustomer.fullName + ']',
      width: '60%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        if (resp.id) {
          this.ayantDroit = resp;
        } else {
          this.addAyantDroit(resp);
        }
      }
    });
  }

  buildIClientTiersPayantFromInputs(): IClientTiersPayant[] {
    const inputs = this.getInputElement();
    return inputs.map(input => {
      const tiersPayant = {} as IClientTiersPayant;
      const ids = input.id.split('-');
      tiersPayant.numBon = input.value;
      tiersPayant.id = Number(ids[0]);
      tiersPayant.taux = Number(ids[1]);
      tiersPayant.categorie = Number(ids[2]);
      return tiersPayant;
    });
  }

  onChangeCustomerClick(): void {
    this.confirmationService.confirm({
      message: 'Etes-vous sûr de vouloir changer le client?',
      header: 'Changer le client',
      icon: 'pi pi-info-circle',
      accept: () => this.openAssuredCustomerListTable(),
      key: 'changeCustomer',
    });
  }

  reset(): void {
    this.selectedTiersPayants.set([]);
    this.ayantDroit = null;
  }

  private getInputs(): Element[] {
    const inputs = this.document.querySelectorAll('.tiersPayant-input');
    return Array.from(inputs);
  }

  private getInputElement(): HTMLInputElement[] {
    return this.getInputs() as HTMLInputElement[];
  }

  private firstRefBonFocus(): void {
    setTimeout(() => {
      const input = this.getInputElement()[0];
      if (input) {
        input.focus();
        input.select();
      }
    }, 50);
  }

  private bonInputFocusOnAddTiersPayant(index: number | null): void {
    setTimeout(() => {
      const inputsArray = this.getInputElement();
      const input = index ? inputsArray[index] : inputsArray[inputsArray.length - 1];
      if (input) {
        input.focus();
        input.select();
      }
    }, 50);
  }
}
