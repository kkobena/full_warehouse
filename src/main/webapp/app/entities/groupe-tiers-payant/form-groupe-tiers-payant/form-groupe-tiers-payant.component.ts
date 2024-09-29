import { Component, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { GroupeTiersPayant, IGroupeTiersPayant } from 'app/shared/model/groupe-tierspayant.model';
import { ErrorService } from 'app/shared/error.service';
import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { GroupeTiersPayantService } from 'app/entities/groupe-tiers-payant/groupe-tierspayant.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ToastModule } from 'primeng/toast';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { KeyFilterModule } from 'primeng/keyfilter';
import { OrdreTrisFacture } from '../../../shared/model/tierspayant.model';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';

@Component({
  selector: 'jhi-form-groupe-tiers-payant',
  templateUrl: './form-groupe-tiers-payant.component.html',
  providers: [MessageService, ConfirmationService],
  standalone: true,
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ToastModule,
    DropdownModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    KeyFilterModule,
    DynamicDialogModule,
  ],
})
export class FormGroupeTiersPayantComponent implements OnInit {
  entity?: IGroupeTiersPayant;
  ordreTrisFacture: OrdreTrisFacture[] = [];
  isSaving = false;
  isValid = true;
  editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required]],
    adresse: [],
    telephone: [],
    telephoneFixe: [],
    ordreTrisFacture: [],
  });

  constructor(
    protected errorService: ErrorService,
    private fb: UntypedFormBuilder,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    protected groupeTiersPayantService: GroupeTiersPayantService,
    private messageService: MessageService,
    private tiersPayantService: TiersPayantService,
  ) {}

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    if (this.entity) {
      this.updateForm(this.entity);
    }
    this.loadOrdreTrisFacture();
  }

  loadOrdreTrisFacture(): void {
    this.tiersPayantService.getOrdreTrisFacture().subscribe(res => {
      this.ordreTrisFacture = res.body || [];
    });
  }

  updateForm(groupeTiersPayant: IGroupeTiersPayant): void {
    this.editForm.patchValue({
      id: groupeTiersPayant.id,
      name: groupeTiersPayant.name,
      adresse: groupeTiersPayant.adresse,
      telephone: groupeTiersPayant.telephone,
      telephoneFixe: groupeTiersPayant.telephoneFixe,
      ordreTrisFacture: groupeTiersPayant.ordreTrisFacture,
    });
  }

  cancel(): void {
    this.ref.close();
  }

  save(): void {
    this.isSaving = true;
    const groupeTiersPayant = this.createFromForm();
    if (groupeTiersPayant.id !== undefined && groupeTiersPayant.id) {
      this.subscribeToSaveResponse(this.groupeTiersPayantService.update(groupeTiersPayant));
    } else {
      this.subscribeToSaveResponse(this.groupeTiersPayantService.create(groupeTiersPayant));
    }
  }

  protected createFromForm(): IGroupeTiersPayant {
    return {
      ...new GroupeTiersPayant(),
      id: this.editForm.get(['id'])!.value,
      name: this.editForm.get(['name'])!.value,
      adresse: this.editForm.get(['adresse'])!.value,
      telephone: this.editForm.get(['telephone'])!.value,
      telephoneFixe: this.editForm.get(['telephoneFixe'])!.value,
      ordreTrisFacture: this.editForm.get(['ordreTrisFacture'])!.value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IGroupeTiersPayant>>): void {
    result.subscribe({
      next: res => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error),
    });
  }

  protected onSaveSuccess(groupeTiersPayant: IGroupeTiersPayant | null): void {
    this.isSaving = false;
    this.ref.close(groupeTiersPayant);
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: this.errorService.getErrorMessage(error),
    });
  }
}
