import {
  AfterViewInit,
  Component,
  effect,
  ElementRef,
  inject,
  OnInit,
  signal,
  viewChild,
  WritableSignal
} from '@angular/core';
import {CustomerService} from '../../../../customer/customer.service';
import {HttpResponse} from '@angular/common/http';
import {ICustomer} from '../../../../../shared/model/customer.model';
import {SelectedCustomerService} from '../../../service/selected-customer.service';
import {AssuredCustomerListComponent} from '../../../assured-customer-list/assured-customer-list.component';
import {DialogService, DynamicDialogRef} from 'primeng/dynamicdialog';
import {MenuItem} from 'primeng/api';
import {FormsModule} from '@angular/forms';
import {KeyFilterModule} from 'primeng/keyfilter';
import {PanelModule} from 'primeng/panel';
import {InputTextModule} from 'primeng/inputtext';
import {DOCUMENT} from '@angular/common';
import {IClientTiersPayant} from '../../../../../shared/model/client-tiers-payant.model';
import {FormAyantDroitComponent} from '../../../../customer/form-ayant-droit/form-ayant-droit.component';
import {SplitButtonModule} from 'primeng/splitbutton';
import {AyantDroitCustomerListComponent} from '../../../ayant-droit-customer-list/ayant-droit-customer-list.component';
import {ConfirmPopupModule} from 'primeng/confirmpopup';
import {CurrentSaleService} from '../../../service/current-sale.service';
import {AssureFormStepComponent} from '../../../../customer/assure-form-step/assure-form-step.component';
import {BaseSaleService} from '../../../service/base-sale.service';
import {AddComplementaireComponent} from '../add-complementaire/add-complementaire.component';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {ConfirmDialogComponent} from "../../../../../shared/dialog/confirm-dialog/confirm-dialog.component";

@Component({
  selector: 'jhi-assurance-data',
  providers: [DialogService],
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

  ],
  templateUrl: './assurance-data.component.html',
})
export class AssuranceDataComponent implements OnInit, AfterViewInit {
  searchInput = viewChild<ElementRef>('searchInput');
  protected search: string = null;
  protected readonly selectedCustomerService = inject(SelectedCustomerService);
  protected ref: DynamicDialogRef;
  protected ayantDroit: ICustomer | null = null;
  protected selectedTiersPayants: WritableSignal<IClientTiersPayant[]> = signal<IClientTiersPayant[]>([]);
  protected items: MenuItem[] | undefined;
  protected baseSaleService = inject(BaseSaleService);
  protected readonly currentSaleService = inject(CurrentSaleService);
  protected divClass = signal('col-md-4 col-sm-4 col-4 bon');
  protected divCustomer = signal('col-md-4 col-sm-4 col-4');
  private readonly dialogService = inject(DialogService);
  private readonly document = inject<Document>(DOCUMENT);
  private readonly customerService = inject(CustomerService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');

  constructor() {
    effect(() => {
      const assuredCustomer = this.selectedCustomerService.selectedCustomerSignal();
      if (assuredCustomer) {
        if (!this.currentSaleService.isEdit()) {
          if (this.currentSaleService.typeVo() === 'ASSURANCE') {
            this.selectedTiersPayants.set(assuredCustomer.tiersPayants || []);
            this.ayantDroit =
              assuredCustomer.ayantDroits.find(ad => ad.id === assuredCustomer.id || ad.num === assuredCustomer.num) || null;
            if (!this.ayantDroit) {
              this.ayantDroit = assuredCustomer;
            }
          } else if (this.currentSaleService.typeVo() === 'CARNET') {
            this.selectedTiersPayants.set([assuredCustomer.tiersPayants[0]]);
          }
        }
      }

      const count = this.selectedTiersPayants().length;
      this.divClass.set(count === 2 ? 'col-md-3 col-sm-3 col-3 bon' : count > 2 ? 'col-md-2 col-sm-2 col-2 bon' : 'col-md-4 col-sm-4 col-4 bon');
      this.divCustomer.set(count >= 2 ? 'col-md-3 col-sm-3 col-3' : 'col-md-4 col-sm-4 col-4');
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
            if (assuredCustomers && assuredCustomers.length) {
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
          error() {
          },
        });
    }
  }

  openAssuredCustomerListTable(): void {
    this.ref = this.dialogService.open(AssuredCustomerListComponent, {
      data: {searchString: this.search},
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
    this.searchInput().nativeElement.focus();
  }

  addComplementaire(): void {
    const currentCustomer = this.selectedCustomerService.selectedCustomerSignal();
    this.ref = this.dialogService.open(AddComplementaireComponent, {
      data: {tiersPayantsExisting: this.selectedTiersPayants(), assure: currentCustomer},
      header: 'AJOUTER UN TIERS PAYANT COMPLEMENTAIRE',
      width: '45%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: IClientTiersPayant) => {
      if (resp) {
        if (this.currentSaleService.currentSale()) {
          this.baseSaleService.onAddThirdPartySale(this.currentSaleService.currentSale()?.id, resp);
        }
        this.selectedTiersPayants.set([...this.selectedTiersPayants(), resp]);
        this.bonInputFocusOnAddTiersPayant(null);
      }
    });
  }

  removeTiersPayant(tiersPayant: IClientTiersPayant): void {
    this.confimDialog().onConfirm(() => {
        if (this.currentSaleService.currentSale()) {
          this.baseSaleService.onRemoveThirdPartySaleLineToSalesSuccess(tiersPayant.id);
          this.selectedTiersPayants.set(this.selectedTiersPayants().filter(tp => tp.id !== tiersPayant.id));
        } else {
          this.selectedTiersPayants.set(this.selectedTiersPayants().filter(tp => tp.id !== tiersPayant.id));
        }
      }, 'Supprimer tiers payant', 'Etes-vous sûr de vouloir supprimer ce tiers payant?', null
    );

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
      data: {entity: null, typeAssure: this.currentSaleService.typeVo()},
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
      data: {assure: currentCustomer},
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
    this.confimDialog().onConfirm(() => this.openAssuredCustomerListTable(), 'Changer le client', 'Etes-vous sûr de vouloir changer le client?', null
    );

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
