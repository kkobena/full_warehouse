import { AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, viewChild } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { DateNaissDirective } from '../../../shared/date-naiss.directive';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { RadioButton, RadioButtonModule } from 'primeng/radiobutton';
import TranslateDirective from '../../../shared/language/translate.directive';
import { CommonModule } from '@angular/common';
import { DividerModule } from 'primeng/divider';
import { InputMaskModule } from 'primeng/inputmask';
import { SelectButtonModule } from 'primeng/selectbutton';
import { CardModule } from 'primeng/card';
import { Customer, ICustomer } from '../../../shared/model/customer.model';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { HttpResponse } from '@angular/common/http';
import { showCommonModal } from '../../sales/selling-home/sale-helper';
import { FormTiersPayantComponent } from '../../tiers-payant/form-tiers-payant/form-tiers-payant.component';
import { Observable, Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { ErrorService } from '../../../shared/error.service';
import { CustomerService } from '../customer.service';
import { Button } from 'primeng/button';
import { PRODUIT_COMBO_MIN_LENGTH } from '../../../shared/constants/pagination.constants';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';

@Component({
  selector: 'jhi-carnet',
  imports: [
    CommonModule,
    AutoCompleteModule,
    RadioButton,
    RadioButtonModule,
    DividerModule,
    InputMaskModule,
    InputTextModule,
    KeyFilterModule,
    RadioButtonModule,
    ReactiveFormsModule,
    SelectButtonModule,
    TranslateDirective,
    CardModule,
    DateNaissDirective,
    Button,
    ToastAlertComponent,
  ],
  templateUrl: './customer-carnet.component.html',
  styleUrls: ['./customer-carnet-component.scss'],
})
export class CustomerCarnetComponent implements OnInit, AfterViewInit, OnDestroy {
  header: string | null = null;
  entity?: ICustomer;
  categorie: string | null = null;

  protected isSaving = false;
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected tiersPayant!: ITiersPayant | null;
  protected tiersPayants: ITiersPayant[] = [];
  protected firstName = viewChild.required<ElementRef>('firstName');
  protected fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    id: [],
    firstName: [null, [Validators.required]],
    lastName: [null, [Validators.required]],
    tiersPayantId: [null, [Validators.required]],
    taux: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
    num: [null, [Validators.required]],
    phone: [],
    email: [],
    adresse: [],
    sexe: [],
    datNaiss: [],
    remiseId: [],
  });
  private readonly customerService = inject(CustomerService);
  private readonly activeModal = inject(NgbActiveModal);
  private alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);
  private readonly modalService = inject(NgbModal);
  private readonly tiersPayantService = inject(TiersPayantService);
  private destroy$ = new Subject<void>();

  cancel(): void {
    this.activeModal.dismiss();
  }

  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.firstName().nativeElement.focus();
    }, 100);
  }

  addTiersPayantAssurance(): void {
    showCommonModal(
      this.modalService,
      FormTiersPayantComponent,
      {
        entity: null,
        categorie: this.categorie,
        header: 'FORMULAIRE DE CREATION DE CARNET',
      },
      (resp: ITiersPayant) => {
        if (resp) {
          this.tiersPayants.push(resp);
          this.editForm.patchValue({ tiersPayantId: resp });
        }
      },
      'xl',
      'modal-dialog-70',
    );
  }

  onSaveError(error: any): void {
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  save(): void {
    this.isSaving = true;

    const customer = this.createFromForm();

    if (customer.id !== undefined && customer.id) {
      this.subscribeToSaveResponse(this.customerService.update(customer));
    } else {
      this.subscribeToSaveResponse(this.customerService.create(customer));
    }
  }

  protected searchTiersPayant(event: any): void {
    this.loadTiersPayants(event.query);
  }

  protected loadTiersPayants(search?: string): void {
    const query: string = search || '';

    this.tiersPayantService
      .query({
        page: 0,
        size: 10,
        type: this.categorie,
        search: query,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe((res: HttpResponse<ITiersPayant[]>) => {
        this.tiersPayants = res.body!;
        if (this.tiersPayants.length === 0) {
          this.tiersPayants.push({ id: null, fullName: 'Ajouter un nouveau tiers-payant' });
        }
      });
  }

  protected onSelectTiersPayant(event: any): void {
    if (event.value?.id === null) {
      this.addTiersPayantAssurance();
    } else {
      this.tiersPayant = event.value;
    }
  }

  protected createFromForm(): ICustomer {
    return {
      ...new Customer(),
      id: this.editForm.get('id')?.value,
      firstName: this.editForm.get('firstName')?.value,
      lastName: this.editForm.get('lastName')?.value,
      email: this.editForm.get('email')?.value,
      phone: this.editForm.get('phone')?.value,
      type: 'ASSURE',
      num: this.editForm.get('num')?.value,
      datNaiss: this.editForm.get('datNaiss')?.value,
      sexe: this.editForm.get('sexe')?.value,
      tiersPayantId: this.editForm.get('tiersPayantId')?.value?.id,
      taux: this.editForm.get('taux')?.value,
    };
  }

  protected updateForm(customer: ICustomer): void {
    this.editForm.patchValue({
      id: customer.id,
      firstName: customer.firstName,
      lastName: customer.lastName,
      email: customer.email,
      phone: customer.phone,
      num: customer.num,
      datNaiss: customer.datNaiss,
      sexe: customer.sexe,
      tiersPayantId: customer.tiersPayant,
      taux: customer.taux,
    });
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
    result.pipe(finalize(() => (this.isSaving = false))).subscribe({
      next: (res: HttpResponse<ICustomer>) => this.onSaveSuccess(res.body),
      error: (error: any) => this.onSaveError(error),
    });
  }

  private onSaveSuccess(customer: ICustomer | null): void {
    this.alert().showInfo('Client ajouté avec succès');
    this.activeModal.close(customer);
  }
}
