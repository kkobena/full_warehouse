import { Component, inject, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ICustomer } from '../../../shared/model/customer.model';
import { FormArray, FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { ISales } from '../../../shared/model/sales.model';
import { TableModule } from 'primeng/table';
import { IThirdPartySaleLine } from '../../../shared/model/third-party-sale-line';
import { CardModule } from 'primeng/card';
import { KeyFilter } from 'primeng/keyfilter';
import { AssuranceService } from '../assurance.service';
import { UpdateSale } from './update-sale.model';
import { ErrorService } from '../../../shared/error.service';
import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';

@Component({
  selector: 'jhi-customer-edit-modal',
  templateUrl: './customer-edit-modal.component.html',
  styleUrls: ['./customer-edit-modal.component.scss'],
  providers: [MessageService],
  imports: [
    FormsModule,
    WarehouseCommonModule,
    ButtonModule,
    InputTextModule,
    ReactiveFormsModule,
    TableModule,
    CardModule,
    KeyFilter,
    Toast,
  ],
})
export class CustomerEditModalComponent implements OnInit {
  @Input() sale: ISales;
  customer: ICustomer;
  ayantDroit: ICustomer;
  thirdPartySaleLines: IThirdPartySaleLine[];
  protected fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    customer: this.fb.group({
      id: [],
      num: [null, [Validators.required]],
      firstName: [null, [Validators.required]],
      lastName: [null, [Validators.required]],
      phone: [],
    }),
  });

  protected isSaving = false;
  private activeModal = inject(NgbActiveModal);
  private assuranceService = inject(AssuranceService);
  private readonly errorService = inject(ErrorService);
  private messageService = inject(MessageService);
  get thirdPartySaleLinesFomArray(): FormArray {
    return this.editForm.get('thirdPartySaleLines') as FormArray;
  }

  ngOnInit(): void {
    this.customer = this.sale.customer;
    this.ayantDroit = this.sale.ayantDroit?.id !== this.customer.id ? this.sale.ayantDroit : null;
    this.thirdPartySaleLines = this.sale.thirdPartySaleLines;

    this.editForm.patchValue({
      customer: {
        id: this.customer.id,
        num: this.sale.tiersPayants[0].num,
        firstName: this.customer.firstName,
        lastName: this.customer.lastName,
        phone: this.customer.phone,
      },
    });

    if (this.ayantDroit) {
      this.editForm.addControl(
        'ayantDroit',
        this.fb.group({
          id: [],
          num: [null, [Validators.required]],
          firstName: [null, [Validators.required]],
          lastName: [null, [Validators.required]],
        }),
      );
      this.editForm.patchValue({
        ayantDroit: {
          id: this.ayantDroit.id,
          num: this.ayantDroit.numAyantDroit,
          firstName: this.ayantDroit.firstName,
          lastName: this.ayantDroit.lastName,
        },
      });
    }

    if (this.thirdPartySaleLines && this.thirdPartySaleLines.length > 0) {
      this.editForm.addControl('thirdPartySaleLines', this.fb.array([]));
      this.thirdPartySaleLines.forEach(line => {
        this.thirdPartySaleLinesFomArray.push(
          this.fb.group({
            id: [line.id],
            numBon: [line.numBon],
            taux: [line.taux],
            montant: [line.montant],
            tiersPayantFullName: [line.tiersPayantFullName],
          }),
        );
      });
    }
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    this.isSaving = true;
    const updateSale = this.createFromForm();
    this.subscribeToSaveResponse(this.assuranceService.update(updateSale));
  }

  protected createFromForm(): UpdateSale {
    const customerFromForm = this.editForm.get('customer').value;

    return {
      id: this.sale.id,
      customer: {
        id: customerFromForm.id,
        num: customerFromForm.num,
        firstName: customerFromForm.firstName,
        lastName: customerFromForm.lastName,
        phone: customerFromForm.phone,
      },
      ayantDroit: this.buildAyantDroit(),
      thirdPartySaleLines: this.buildTiersPayant(),
    };
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: res => this.onSaveSuccess(res.body),
      error: err => this.onSaveError(err),
    });
  }

  private onSaveSuccess(sales: ISales | null): void {
    this.isSaving = false;
    this.messageService.add({ severity: 'success', summary: 'Confirmation', detail: 'Vente modifiée avec succès' });
    this.activeModal.close(sales);
  }

  private onSaveError(error: unknown): void {
    this.isSaving = false;
    this.messageService.add({ severity: 'error', summary: 'Erreur', detail: this.errorService.getErrorMessage(error) });
  }

  private buildAyantDroit(): ICustomer {
    const ayantDroit = this.editForm.get('ayantDroit')?.value;
    if (ayantDroit) {
      return {
        id: ayantDroit.id,
        num: ayantDroit.num,
        firstName: ayantDroit.firstName,
        lastName: ayantDroit.lastName,
      };
    }
    return null;
  }

  private buildTiersPayant(): IThirdPartySaleLine[] {
    const tiersPayants = this.editForm.get('thirdPartySaleLines')?.value;
    if (tiersPayants) {
      return tiersPayants.map((tiersPayant: IThirdPartySaleLine) => {
        return {
          id: tiersPayant.id,
          numBon: tiersPayant.numBon,
        };
      });
    }
    return [];
  }
}
