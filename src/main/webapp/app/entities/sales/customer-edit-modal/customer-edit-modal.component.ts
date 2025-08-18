import { Component, inject, Input, OnInit, viewChild } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ICustomer } from '../../../shared/model/customer.model';
import {
  AbstractControl,
  FormArray,
  FormsModule,
  ReactiveFormsModule,
  UntypedFormBuilder,
  Validators
} from '@angular/forms';
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
import { showCommonModal } from '../selling-home/sale-helper';
import { AssuredCustomerListComponent } from '../assured-customer-list/assured-customer-list.component';
import { CurrentSaleService } from '../service/current-sale.service';
import { AddComplementaireComponent } from '../selling-home/assurance/add-complementaire/add-complementaire.component';
import { IClientTiersPayant } from '../../../shared/model/client-tiers-payant.model';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { AyantDroitCustomerListComponent } from '../ayant-droit-customer-list/ayant-droit-customer-list.component';
import { FormAyantDroitComponent } from '../../customer/form-ayant-droit/form-ayant-droit.component';

@Component({
  selector: 'jhi-customer-edit-modal',
  templateUrl: './customer-edit-modal.component.html',
  styleUrls: ['./customer-edit-modal.component.scss'],
  imports: [
    FormsModule,
    WarehouseCommonModule,
    ButtonModule,
    InputTextModule,
    ReactiveFormsModule,
    TableModule,
    CardModule,
    KeyFilter,
    ToastAlertComponent
  ]
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
      phone: []
    })
  });
  protected isSaving = false;
  private initialFormValue: any;
  private activeModal = inject(NgbActiveModal);
  private assuranceService = inject(AssuranceService);
  private readonly errorService = inject(ErrorService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly modalService = inject(NgbModal);
  private readonly currentSaleService = inject(CurrentSaleService);

  get thirdPartySaleLinesFomArray(): FormArray {
    return this.editForm.get('thirdPartySaleLines') as FormArray;
  }

  ngOnInit(): void {
    this.customer = this.sale.customer;
    this.ayantDroit = this.sale.ayantDroit?.id !== this.customer.id ? this.sale.ayantDroit : null;
    this.thirdPartySaleLines = this.sale.thirdPartySaleLines;
    this.currentSaleService.setTypeVo(this.sale.natureVente);
    this.patchCustomerForm();

    this.patchAyantDroitForm();

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
            clientTiersPayantId: [line.clientTiersPayantId]
          })
        );
      });
    }
    this.initialFormValue = this.editForm.getRawValue();
  }

  hasFormChanged(): boolean {
    return JSON.stringify(this.initialFormValue) !== JSON.stringify(this.editForm.getRawValue());
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  protected save(): void {
    this.isSaving = true;
    const updateSale = this.createFromForm();
    if (this.isValidTauxCouverture(updateSale)) {
      this.subscribeToSaveResponse(this.assuranceService.updateCustomerInformation(updateSale));
    } else {
      this.alert().showError('Les taux sont différents');
    }
  }

  protected openAssuredCustomerListTable(): void {
    showCommonModal(
      this.modalService,
      AssuredCustomerListComponent,
      {
        headerLibelle: 'CLIENTS ASSURES'
      },
      (resp: ICustomer) => {
        if (resp) {
          this.customer = resp;
          this.patchCustomerForm();
          this.rebuildThirdPartySaleLines();
        }
      },
      '70%',
      'modal-dialog-70'
    );
  }

  protected onChangeTierPayant(index: number): void {
    const tps = this.thirdPartySaleLinesFomArray.at(index) as AbstractControl;
    showCommonModal(
      this.modalService,
      AddComplementaireComponent,
      {
        tiersPayantsExisting: [],
        assure: this.customer
      },
      (resp: IClientTiersPayant) => {
        if (resp) {
          tps.patchValue({
            clientTiersPayantId: resp.id,
            taux: resp.taux
          });
        }
      },
      'xl'
    );
  }

  protected loadAyantDoits(): void {
    showCommonModal(
      this.modalService,
      AyantDroitCustomerListComponent,
      {
        assure: this.customer,
        header: 'LISTE DES AYANTS DROITS DU CLIENT [' + this.customer.fullName + ']'
      },
      (resp: ICustomer) => {
        if (resp) {
          if (resp.id) {
            this.ayantDroit = resp;
            this.patchAyantDroitForm();
          } else {
            this.openAyantDroitForm(resp);
          }
        }
      },
      'xl'
    );
  }

  private openAyantDroitForm(ayantDroit: ICustomer): void {
    showCommonModal(
      this.modalService,
      FormAyantDroitComponent,
      {
        entity: ayantDroit,
        assure: this.customer,
        header: 'FORMULAIRE D\'AJOUT D\'UN AYANT DROIT'
      },
      (resp: ICustomer) => {
        if (resp) {
          this.ayantDroit = resp;
          this.patchAyantDroitForm();
        }
      },
      'xl'
    );
  }

  private createFromForm(): UpdateSale {
    const customerFromForm = this.editForm.get('customer').value;

    return {
      id: this.sale.id,
      initialValue: this.initialFormValue,
      finalValue: this.editForm.getRawValue(),
      customer: {
        id: customerFromForm.id,
        num: customerFromForm.num,
        firstName: customerFromForm.firstName,
        lastName: customerFromForm.lastName,
        phone: customerFromForm.phone,
        type: 'ASSURE'
      },
      ayantDroit: this.buildAyantDroit(),
      thirdPartySaleLines: this.buildTiersPayant()
    };
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: err => this.onSaveError(err)
    });
  }

  private onSaveSuccess(): void {
    this.isSaving = false;
    this.alert().showInfo('Vente modifiée avec succès');
    this.activeModal.close();
  }

  private onSaveError(error: unknown): void {
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private buildAyantDroit(): ICustomer {
    const ayantDroit = this.editForm.get('ayantDroit')?.value;
    if (ayantDroit) {
      return {
        id: ayantDroit.id,
        num: ayantDroit.num,
        firstName: ayantDroit.firstName,
        lastName: ayantDroit.lastName,
        type: 'ASSURE'
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
          clientTiersPayantId: tiersPayant.clientTiersPayantId,
          taux: tiersPayant.taux
        };
      });
    }
    return [];
  }

  private rebuildThirdPartySaleLines(): void {
    this.thirdPartySaleLinesFomArray.clear();
    const tps = this.customer?.tiersPayants || [];
    tps.forEach((line, index) => {
      const tpData = this.thirdPartySaleLines.at(index);
      this.thirdPartySaleLinesFomArray.push(
        this.fb.group({
          id: tpData?.id,
          numBon: tpData?.numBon,
          montant: tpData?.montant,
          taux: [line.taux],
          tiersPayantFullName: [line.tiersPayantName],
          clientTiersPayantId: [line.id]
        })
      );
    });
  }

  private checkCustomerHasBeenChanged(): boolean {
    const customerFromForm = this.editForm.get('customer').value;
    return customerFromForm.id !== this.sale.customer.id;
  }

  private isValidTauxCouverture(updateSale: UpdateSale): boolean {
    if (this.checkCustomerHasBeenChanged()) {
      return (
        this.thirdPartySaleLines?.reduce((sum, current) => sum + current.taux, 0) ===
        updateSale?.thirdPartySaleLines?.reduce((sum, current) => sum + current.taux, 0)
      );
    }
    return true;
  }

  private patchAyantDroitForm(): void {
    if (this.ayantDroit) {
      const ayantDroitControl = this.editForm.get('ayantDroit') as AbstractControl;
      if (!ayantDroitControl) {
        this.editForm.addControl(
          'ayantDroit',
          this.fb.group({
            id: [null, [Validators.required]],
            num: [null, [Validators.required]],
            firstName: [null, [Validators.required]],
            lastName: [null, [Validators.required]]
          })
        );
      }

      this.editForm.patchValue({
        ayantDroit: {
          id: this.ayantDroit.id,
          num: this.ayantDroit.numAyantDroit,
          firstName: this.ayantDroit.firstName,
          lastName: this.ayantDroit.lastName
        }
      });
    }
  }

  private patchCustomerForm(): void {
    const tiersPayants = this.customer?.tiersPayants || [];
    this.editForm.patchValue({
      customer: {
        id: this.customer.id,
        num: tiersPayants.length > 0 ? tiersPayants[0].num : this.sale.tiersPayants.at(0).num,
        firstName: this.customer.firstName,
        lastName: this.customer.lastName,
        phone: this.customer.phone
      }
    });
  }
}
