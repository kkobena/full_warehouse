import { Component, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ErrorService } from '../../../shared/error.service';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';
import { CustomerService } from '../customer.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ToastModule } from 'primeng/toast';
import { NgSelectModule } from '@ng-select/ng-select';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InputTextModule } from 'primeng/inputtext';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { SelectButtonModule } from 'primeng/selectbutton';
import { RadioButtonModule } from 'primeng/radiobutton';
import { DropdownModule } from 'primeng/dropdown';
import { CalendarModule } from 'primeng/calendar';
import { DividerModule } from 'primeng/divider';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputMaskModule } from 'primeng/inputmask';
import { ClientTiersPayant, IClientTiersPayant } from '../../../shared/model/client-tiers-payant.model';
import { ICustomer } from '../../../shared/model/customer.model';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';

@Component({
  selector: 'jhi-customer-tiers-payant',
  standalone: true,
  providers: [MessageService, DialogService, ConfirmationService],
  imports: [
    WarehouseCommonModule,
    ToastModule,
    FormsModule,
    NgSelectModule,
    ButtonModule,
    RippleModule,
    ConfirmDialogModule,
    InputTextModule,
    AutoCompleteModule,
    SelectButtonModule,
    RadioButtonModule,
    DropdownModule,
    ReactiveFormsModule,
    CalendarModule,
    DividerModule,
    KeyFilterModule,
    InputMaskModule,
  ],
  templateUrl: './customer-tiers-payant.component.html',
})
export class CustomerTiersPayantComponent implements OnInit {
  minLength = 3;
  plafonds = [
    { label: 'Non', value: false },
    { label: 'Oui', value: true },
  ];
  editForm = this.fb.group({
    id: [],
    tiersPayant: [null, [Validators.required]],
    taux: [null, [Validators.required, Validators.min(5), Validators.max(100)]],
    num: [null, [Validators.required]],
    plafondConso: [],
    plafondJournalier: [],
    plafondAbsolu: [],
  });
  entity: IClientTiersPayant | null = null;
  customer: ICustomer | null = null;
  isSaving = false;
  isValid = true;
  tiersPayants: ITiersPayant[] = [];

  constructor(
    protected errorService: ErrorService,
    private fb: UntypedFormBuilder,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    protected tiersPayantService: TiersPayantService,
    protected customerService: CustomerService,
    private messageService: MessageService,
  ) {}

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    this.customer = this.config.data.customer;
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }

  save(): void {
    this.isSaving = true;
    const clientTiersPayant = this.createFromForm();
    if (clientTiersPayant.id !== undefined && clientTiersPayant.id) {
      this.subscribeToSaveResponse(this.customerService.updateTiersPayant(clientTiersPayant));
    } else {
      this.subscribeToSaveResponse(this.customerService.addTiersPayant(clientTiersPayant));
    }
  }

  cancel(): void {
    this.ref.close();
  }

  searchTiersPayant(event: any): void {
    this.loadTiersPayants(event.query);
  }

  loadTiersPayants(search?: string): void {
    const query: string = search || '';
    this.tiersPayantService
      .query({
        page: 0,
        size: 10,
        type: 'ASSURANCE',
        search: query,
      })
      .subscribe((res: HttpResponse<ITiersPayant[]>) => (this.tiersPayants = res.body!));
  }

  protected updateForm(clientTiersPayant: IClientTiersPayant): void {
    this.editForm.patchValue({
      id: clientTiersPayant.id,
      num: clientTiersPayant.num,
      tiersPayant: clientTiersPayant.tiersPayant,
      plafondConso: clientTiersPayant.plafondConso,
      plafondJournalier: clientTiersPayant.plafondJournalier,
      plafondAbsolu: clientTiersPayant.plafondAbsolu,
      taux: clientTiersPayant.taux,
    });
  }

  protected createFromForm(): IClientTiersPayant {
    return {
      ...new ClientTiersPayant(),
      customerId: this.customer?.id,
      id: this.editForm.get(['id'])!.value,
      num: this.editForm.get(['num'])!.value,
      tiersPayantId: this.editForm.get(['tiersPayant'])!.value.id,
      plafondConso: this.editForm.get(['plafondConso'])!.value,
      plafondJournalier: this.editForm.get(['plafondJournalier'])!.value,
      plafondAbsolu: this.editForm.get(['plafondAbsolu'])!.value,
      taux: this.editForm.get(['taux'])!.value,
      priorite: this.computePriorite(),
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
    result.subscribe({
      next: (res: HttpResponse<ICustomer>) => this.onSaveSuccess(res.body),
      error: (error: any) => this.onSaveError(error),
    });
  }

  protected onSaveSuccess(iCustomer: ICustomer | null): void {
    this.isSaving = false;
    this.ref.close(iCustomer);
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    if (error.error?.errorKey) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(translatedErrorMessage => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: translatedErrorMessage,
        });
      });
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Erreur interne du serveur.',
      });
    }
  }

  private computePriorite(): number {
    if (this.customer.tiersPayants?.length > 0) {
      return this.customer.tiersPayants.length++;
    }
    return 0;
  }
}
