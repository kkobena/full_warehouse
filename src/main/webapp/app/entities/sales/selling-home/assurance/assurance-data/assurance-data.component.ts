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
import { TiersPayantCustomerListComponent } from '../../../../customer/tiers-payant-customer-list/tiers-payant-customer-list.component';
import { FormAyantDroitComponent } from '../../../../customer/form-ayant-droit/form-ayant-droit.component';
import { FormAssuredCustomerComponent } from '../../../../customer/form-assured-customer/form-assured-customer.component';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { TableModule } from 'primeng/table';
import { SpeedDialModule } from 'primeng/speeddial';
import { SplitButtonModule } from 'primeng/splitbutton';
import { AyantDroitCustomerListComponent } from '../../../ayant-droit-customer-list/ayant-droit-customer-list.component';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CurrentSaleService } from '../../../service/current-sale.service';

@Component({
  selector: 'jhi-assurance-data',
  providers: [ConfirmationService, DialogService, MessageService],
  standalone: true,
  imports: [
    AssuredCustomerListComponent,
    FormsModule,
    KeyFilterModule,
    PanelModule,
    InputTextModule,
    OverlayPanelModule,
    TableModule,
    SpeedDialModule,
    SplitButtonModule,
    AyantDroitCustomerListComponent,
    ConfirmPopupModule,
    ConfirmDialogModule,
  ],
  templateUrl: './assurance-data.component.html',
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

  constructor(@Inject(DOCUMENT) private document: Document) {
    effect(
      () => {
        const assuredCustomer = this.selectedCustomerService.selectedCustomerSignal();
        this.selectedTiersPayants.set(assuredCustomer.tiersPayants || []);
        this.ayantDroit = assuredCustomer?.ayantDroits.find(ad => ad.id === assuredCustomer.id || ad.num === assuredCustomer.num) || null;
        if (!this.ayantDroit) {
          this.ayantDroit = assuredCustomer;
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
    //    this.load();
  }

  load(): void {
    if (this.search) {
      this.customerService
        .queryAssuredCustomer({
          search: this.search,
          // search: '2201268',
          // search: '1807874',
          typeTiersPayant: 'ASSURANCE',
        })
        .subscribe({
          next: (res: HttpResponse<ICustomer[]>) => {
            const assuredCustomers = res.body;
            if (assuredCustomers && assuredCustomers.length > 0) {
              if (assuredCustomers.length === 1) {
                this.selectedCustomerService.setCustomer(assuredCustomers[0]);
                this.firstRefBonFocus();
              } else {
                this.openAssuredCustomerListTable(assuredCustomers);
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

  openAssuredCustomerListTable(customers: ICustomer[] | []): void {
    this.ref = this.dialogService.open(AssuredCustomerListComponent, {
      data: { customers },
      header: 'CLIENTS  ASSURES',
      width: '70%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.selectedCustomerService.setCustomer(resp);
        this.firstRefBonFocus();
        /*   setTimeout(() => {
             this.firstRefBonFocus();
           }, 100);*/
      }
    });
  }

  ngAfterViewInit(): void {
    this.searchInput().nativeElement.focus();
  }

  addComplementaire(): void {
    const currentCustomer = this.selectedCustomerService.selectedCustomerSignal();
    this.ref = this.dialogService.open(TiersPayantCustomerListComponent, {
      data: { tiersPayants: this.selectedTiersPayants(), assure: currentCustomer },
      header: 'AJOUTER UN TIERS PAYANT COMPLEMENTAIRE',
      width: '70%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: IClientTiersPayant) => {
      if (resp) {
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
        this.selectedTiersPayants.set(this.selectedTiersPayants().filter(tp => tp.id !== tiersPayant.id));
        if (this.currentSaleService.currentSale()) {
          //process to delete tiers payant from sale
        }
      },
      key: 'deleteTiersPayant',
    });
  }

  editAssuredCustomer(): void {
    this.ref = this.dialogService.open(FormAssuredCustomerComponent, {
      data: { entity: this.selectedCustomerService.selectedCustomerSignal() },
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
    this.ref = this.dialogService.open(FormAssuredCustomerComponent, {
      data: { entity: null },
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

  private buildIClientTiersPayantFromInputs(): IClientTiersPayant[] {
    const inputs = this.getInputElement();
    return inputs.map(input => {
      const tiersPayant = {} as IClientTiersPayant;
      tiersPayant.num = input.value;
      tiersPayant.id = Number(input.id);
      return tiersPayant;
    });
  }
}
