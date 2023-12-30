import { Component, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ErrorService } from 'app/shared/error.service';
import { DialogService, DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { TiersPayantService } from 'app/entities/tiers-payant/tierspayant.service';
import { GroupeTiersPayantService } from 'app/entities/groupe-tiers-payant/groupe-tierspayant.service';
import { ITiersPayant, TiersPayant } from 'app/shared/model/tierspayant.model';
import { IGroupeTiersPayant } from 'app/shared/model/groupe-tierspayant.model';
import { HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ICustomer } from 'app/shared/model/customer.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { NgSelectModule } from '@ng-select/ng-select';
import { InputSwitchModule } from 'primeng/inputswitch';
import { KeyFilterModule } from 'primeng/keyfilter';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'jhi-form-tiers-payant',
  templateUrl: './form-tiers-payant.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
  standalone: true,
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    DynamicDialogModule,
    NgSelectModule,
    InputSwitchModule,
    KeyFilterModule,
    ToastModule,
  ],
})
export class FormTiersPayantComponent implements OnInit {
  entity?: ITiersPayant;
  type?: string | null = null;
  isSaving = false;
  isValid = true;
  selectedGroupe!: IGroupeTiersPayant | null;
  groupeTiersPayants: IGroupeTiersPayant[] = [];
  editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required]],
    fullName: [null, [Validators.required]],
    groupeTiersPayantId: [],
    codeOrganisme: [],
    codeRegroupement: [],
    telephone: [],
    adresse: [],
    montantMaxParFcture: [],
    nbreBordereaux: [1],
    email: [],
    remiseForfaitaire: [],
    plafondConso: [],
    plafondAbsolu: [],
  });

  constructor(
    protected errorService: ErrorService,
    private fb: UntypedFormBuilder,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    protected tiersPayantService: TiersPayantService,
    protected groupeTiersPayantService: GroupeTiersPayantService,
    private messageService: MessageService,
  ) {}

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    this.type = this.config.data.type;
    if (this.entity) {
      this.updateForm(this.entity);
    }
    this.populate().then(r => {
      this.groupeTiersPayants = r;
      if (this.entity) {
        this.selectedGroupe = this.entity.groupeTiersPayant || null;
      }
    });
  }

  async populate(): Promise<IGroupeTiersPayant[]> {
    return await this.groupeTiersPayantService.queryPromise({ search: '' });
  }

  cancel(): void {
    this.ref.close();
  }

  save(): void {
    this.isSaving = true;
    const tiersPayant = this.createFromForm();
    if (tiersPayant.id !== undefined && tiersPayant.id) {
      this.subscribeToSaveResponse(this.tiersPayantService.update(tiersPayant));
    } else {
      this.subscribeToSaveResponse(this.tiersPayantService.create(tiersPayant));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
    result.subscribe(
      res => this.onSaveSuccess(res.body),
      error => this.onSaveError(error),
    );
  }

  protected onSaveSuccess(customer: ICustomer | null): void {
    this.isSaving = false;
    this.ref.close(customer);
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

  private updateForm(tiersPayant: ITiersPayant): void {
    this.editForm.patchValue({
      id: tiersPayant.id,
      name: tiersPayant.name,
      fullName: tiersPayant.fullName,
      groupeTiersPayantId: tiersPayant.groupeTiersPayant,
      codeOrganisme: tiersPayant.codeOrganisme,
      codeRegroupement: tiersPayant.codeRegroupement,
      telephone: tiersPayant.telephone,
      adresse: tiersPayant.adresse,
      montantMaxParFcture: tiersPayant.montantMaxParFcture,
      nbreBordereaux: tiersPayant.nbreBordereaux,
      email: tiersPayant.email,
      remiseForfaitaire: tiersPayant.remiseForfaitaire,
      plafondConso: tiersPayant.plafondConso,
      plafondAbsolu: tiersPayant.plafondAbsolu,
      categorie: tiersPayant.categorie,
    });
  }

  private createFromForm(): ITiersPayant {
    return {
      ...new TiersPayant(),
      id: this.editForm.get(['id'])!.value,
      name: this.editForm.get(['name'])!.value,
      fullName: this.editForm.get(['fullName'])!.value,
      adresse: this.editForm.get(['adresse'])!.value,
      nbreBordereaux: this.editForm.get(['nbreBordereaux'])!.value,
      remiseForfaitaire: this.editForm.get(['remiseForfaitaire'])!.value,
      email: this.editForm.get(['email'])!.value,
      categorie: this.type,
      plafondAbsolu: this.editForm.get(['plafondAbsolu'])!.value,
      plafondConso: this.editForm.get(['plafondConso'])!.value,
      montantMaxParFcture: this.editForm.get(['montantMaxParFcture'])!.value,
      codeOrganisme: this.editForm.get(['codeOrganisme'])!.value,
      codeRegroupement: this.editForm.get(['codeRegroupement'])!.value,
      telephone: this.editForm.get(['telephone'])!.value,
      groupeTiersPayantId: this.editForm.get(['groupeTiersPayantId'])!.value.id,
    };
  }
}
