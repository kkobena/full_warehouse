import {
  AfterViewInit,
  Component,
  computed,
  ElementRef,
  inject,
  OnInit,
  output,
  Signal,
  signal,
  viewChild,
  viewChildren,
  WritableSignal,
  input,
} from '@angular/core';
import { CustomerService } from '../../../../entities/customer/customer.service';
import { HttpResponse } from '@angular/common/http';
import { ICustomer } from '../../../../shared/model';
import { FormsModule } from '@angular/forms';
import { KeyFilterModule } from 'primeng/keyfilter';
import { PanelModule } from 'primeng/panel';
import { InputTextModule } from 'primeng/inputtext';
import { IClientTiersPayant } from '../../../../shared/model';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { Button } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonGroupModule } from 'primeng/buttongroup';

@Component({
  selector: 'app-insurance-data-bar',
  imports: [
    FormsModule,
    KeyFilterModule,
    PanelModule,
    InputTextModule,
    IconField,
    InputIcon,
    InputGroupModule,
    InputGroupAddonModule,
    Button,
    TooltipModule,
    ButtonGroupModule,
  ],
  templateUrl: './insurance-data-bar.component.html',
  styleUrls: ['./insurance-data-bar.component.scss'],
})
export class InsuranceDataBarComponent implements OnInit, AfterViewInit {
  searchInput = viewChild<ElementRef>('searchInput');

  // Inputs
  readonly customer = input<ICustomer | null>(null);
  readonly tiersPayants = input<IClientTiersPayant[]>([]);
  readonly saleType = input<string>('ASSURANCE');
  readonly ayantDroit = input<ICustomer | null>(null);

  // Outputs
  readonly tiersPayantsChanged = output<IClientTiersPayant[]>();
  readonly customerSelected = output<ICustomer>();
  readonly openCustomerList = output<void>();
  readonly editCustomer = output<void>();
  readonly addCustomer = output<void>();
  readonly editAyantDroit = output<void>();
  readonly loadAyantDroits = output<void>();
  readonly addComplementaire = output<void>();
  readonly removeTiersPayant = output<IClientTiersPayant>();
  readonly focusProductSearch = output<void>();

  protected search: string = '';
  protected selectedTiersPayants: WritableSignal<IClientTiersPayant[]> = signal<IClientTiersPayant[]>([]);
  protected divClass: Signal<string> = computed(() => this.getDivClassForCount(this.selectedTiersPayants().length));
  protected divCustomer: Signal<string> = computed(() => this.getDivCustomerClassForCount(this.selectedTiersPayants().length));
  private readonly customerService = inject(CustomerService);
  private bonInputs = viewChildren<ElementRef>('tpInput');

  constructor() {
    // Pas d'effect - l'initialisation se fait dans ngOnInit comme l'original
  }

  ngOnInit(): void {
    // Initialisation une seule fois comme l'original (pas d'effect réactif)
    const tiersPayantsInput = this.tiersPayants();
    if (tiersPayantsInput && tiersPayantsInput.length > 0) {
      // Priorité aux tiers payants de la vente (input)
      this.selectedTiersPayants.set([...tiersPayantsInput]);
    } else {
      // Sinon, utiliser ceux du customer
      const customer = this.customer();
      if (customer) {
        this.handleCustomer(customer);
      }
    }
  }

  ngAfterViewInit(): void {
    // Délai pour s'assurer que le focus reste sur la recherche client
    // après le rendu complet de tous les composants enfants
    setTimeout(() => {
      this.searchInput()?.nativeElement.focus();
    }, 250);
  }

  buildIClientTiersPayantFromInputs(): IClientTiersPayant[] {
    const tiersPayants = this.selectedTiersPayants();
    tiersPayants.forEach((tp, index) => {
      const inputEl = this.bonInputs().find(bonInput => bonInput.nativeElement.id === `bon-${tp.id}`);
      if (inputEl) {
        tp.numBon = inputEl.nativeElement.value;
      }
    });
    this.selectedTiersPayants.set(tiersPayants);
    this.tiersPayantsChanged.emit(this.selectedTiersPayants());
    return this.selectedTiersPayants();
  }

  onChangeCustomerClick(): void {
    this.openCustomerList.emit();
  }

  reset(): void {
    this.selectedTiersPayants.set([]);
  }

  protected onAddComplementaire(): void {
    this.addComplementaire.emit();
  }

  protected onRemoveTiersPayant(tiersPayant: IClientTiersPayant): void {
    this.removeTiersPayant.emit(tiersPayant);
  }

  /**
   * Retire un tiers payant de l'état local (sans passer par le parent)
   * Utilisé quand la vente n'existe pas encore (currentSale undefined)
   */
  public removeTiersPayantLocally(tiersPayant: IClientTiersPayant): void {
    const current = this.selectedTiersPayants();
    const updated = current.filter(tp => tp.id !== tiersPayant.id);
    this.selectedTiersPayants.set(updated);
    this.tiersPayantsChanged.emit(updated);
  }

  /**
   * Retourne la liste actuelle des tiers payants sélectionnés
   */
  public getSelectedTiersPayants(): IClientTiersPayant[] {
    return this.selectedTiersPayants();
  }

  /**
   * Initialise les tiers payants à partir d'un customer (appelé par le parent)
   * Remplace l'ancien effect réactif pour éviter les problèmes de performance
   */
  public initializeFromCustomer(customer: ICustomer): void {
    if (customer) {
      this.handleCustomer(customer);
    }
  }

  /**
   * Met à jour les tiers payants localement (appelé par le parent)
   */
  public updateTiersPayants(tiersPayants: IClientTiersPayant[]): void {
    this.selectedTiersPayants.set([...tiersPayants]);
  }

  protected editAssuredCustomer(): void {
    this.editCustomer.emit();
  }

  protected addAssuredCustomer(): void {
    this.addCustomer.emit();
  }

  protected editAyantDroitClick(): void {
    this.editAyantDroit.emit();
  }

  protected loadAyantDoitsClick(): void {
    this.loadAyantDroits.emit();
  }

  protected onBonEnter(tp: IClientTiersPayant): void {
    const tiersPayants = this.selectedTiersPayants();
    const currentIndex = tiersPayants.findIndex(item => item.id === tp.id);
    if (currentIndex < tiersPayants.length - 1) {
      // Focus sur le prochain champ de numéro de bon
      this.focusAndSelectBonInput(currentIndex + 1);
    } else {
      // C'est le dernier bon, focus sur la recherche produit
      this.focusProductSearch.emit();
    }
  }

  protected load(): void {
    if (!this.search) {
      return;
    }

    this.queryAssuredCustomers();
  }

  private queryAssuredCustomers(): void {
    const currentSaleType = this.saleType();
    this.customerService
      .queryAssuredCustomer({
        search: this.search,
        size: 5,
        typeTiersPayant: currentSaleType,
      })
      .subscribe({
        next: (res: HttpResponse<ICustomer[]>) => this.handleQueryResponse(res.body),
      });
  }

  private handleQueryResponse(assuredCustomers: ICustomer[] | null): void {
    if (!assuredCustomers?.length) {
      this.handleNoCustomersFound();
      return;
    }

    if (assuredCustomers.length === 1) {
      this.handleSingleCustomerFound(assuredCustomers[0]);
    } else {
      this.handleMultipleCustomersFound();
    }
  }

  private handleSingleCustomerFound(customer: ICustomer): void {
    this.customerSelected.emit(customer);
    this.setTiersPayants(customer);
    this.firstRefBonFocus();
    this.clearSearch();
  }

  private handleMultipleCustomersFound(): void {
    this.openCustomerList.emit();
    this.clearSearch();
  }

  private handleNoCustomersFound(): void {
    this.addCustomer.emit();
    this.clearSearch();
  }

  private setTiersPayants(customer: ICustomer): void {
    const currentSaleType = this.saleType();
    if (currentSaleType === 'ASSURANCE') {
      this.selectedTiersPayants.set(customer.tiersPayants || []);
    } else if (currentSaleType === 'CARNET' && customer.tiersPayants?.length) {
      this.selectedTiersPayants.set([customer.tiersPayants[0]]);
    }
    this.tiersPayantsChanged.emit(this.selectedTiersPayants());
  }

  private clearSearch(): void {
    this.search = '';
  }

  public focusFirstBon(): void {
    this.focusAndSelectBonInput(0);
  }

  public focusLastBon(): void {
    this.focusAndSelectBonInput(null);
  }

  private firstRefBonFocus(): void {
    this.focusAndSelectBonInput(0);
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

  private handleCustomer(customer: ICustomer): void {
    if (!customer) {
      return;
    }

    const saleType = this.saleType();
    this.setTiersPayantsForSaleType(customer, saleType);
  }

  private setTiersPayantsForSaleType(customer: ICustomer, saleType: string): void {
    if (saleType === 'ASSURANCE') {
      this.selectedTiersPayants.set(customer.tiersPayants || []);
    } else if (saleType === 'CARNET' && customer.tiersPayants?.length) {
      this.selectedTiersPayants.set([customer.tiersPayants[0]]);
    }
    this.tiersPayantsChanged.emit(this.selectedTiersPayants());
  }
}
