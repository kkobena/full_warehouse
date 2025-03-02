import { AfterViewInit, Component, ElementRef, OnInit, viewChild, inject } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ErrorService } from 'app/shared/error.service';
import { DialogService, DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { TiersPayantService } from 'app/entities/tiers-payant/tierspayant.service';
import { GroupeTiersPayantService } from 'app/entities/groupe-tiers-payant/groupe-tierspayant.service';
import { ITiersPayant, ModelFacture, TiersPayant } from 'app/shared/model/tierspayant.model';
import { IGroupeTiersPayant } from 'app/shared/model/groupe-tierspayant.model';
import { HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { NgSelectModule } from '@ng-select/ng-select';
import { KeyFilterModule } from 'primeng/keyfilter';
import { ToastModule } from 'primeng/toast';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { Select } from 'primeng/select';
import { ToggleSwitch } from 'primeng/toggleswitch';

@Component({
  selector: 'jhi-form-tiers-payant',
  templateUrl: './form-tiers-payant.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    DynamicDialogModule,
    NgSelectModule,
    KeyFilterModule,
    ToastModule,
    AutoCompleteModule,
    Select,
    ToggleSwitch,
  ],
})
export class FormTiersPayantComponent implements OnInit, AfterViewInit {
  protected errorService = inject(ErrorService);
  private fb = inject(UntypedFormBuilder);
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  protected tiersPayantService = inject(TiersPayantService);
  protected groupeTiersPayantService = inject(GroupeTiersPayantService);
  private messageService = inject(MessageService);

  name = viewChild.required<ElementRef>('name');
  entity?: ITiersPayant;
  categorie?: string | null = null;
  isSaving = false;
  isValid = true;
  groupeTiersPayants: IGroupeTiersPayant[] = [];
  modelFacture: ModelFacture[] = [];
  editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required]],
    fullName: [null, [Validators.required]],
    groupeTiersPayantId: [],
    codeOrganisme: [],
    telephone: [],
    montantMaxParFcture: [],
    nbreBordereaux: [1],
    email: [],
    remiseForfaitaire: [],
    plafondConso: [],
    plafondAbsolu: [],
    cmu: [],
    useReferencedPrice: [],
    modelFacture: [],
    toBeExclude: [],
  });

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    this.categorie = this.config.data.type;
    if (this.entity) {
      this.updateForm(this.entity);
    }
    this.loadModelFacture();
    this.populate().then(r => {
      this.groupeTiersPayants = r;
    });
    this.displayDisplayCmu();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.name().nativeElement.focus();
    }, 30);
  }

  async populate(): Promise<IGroupeTiersPayant[]> {
    return await this.groupeTiersPayantService.queryPromise({ search: '' });
  }

  loadModelFacture(): void {
    this.tiersPayantService.getModelFacture().subscribe(res => {
      this.modelFacture = res.body || [];
    });
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

  onCmuChange(event: ToggleSwitch): void {
    if (event.checked()) {
      if (this.editForm.get('useReferencedPrice').value) {
        this.editForm.get('useReferencedPrice').setValue(false);
      }
      this.editForm.get('useReferencedPrice').disable();
    } else {
      this.editForm.get('useReferencedPrice').enable();
    }
  }

  onReferencedPriceChange(event: ToggleSwitch): void {
    if (event.checked()) {
      if (this.editForm.get('cmu').value) {
        this.editForm.get('cmu').setValue(false);
      }
      this.editForm.get('cmu').disable();
    } else {
      this.editForm.get('cmu').enable();
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ITiersPayant>>): void {
    result.subscribe({
      next: (res: HttpResponse<ITiersPayant>) => this.onSaveSuccess(res.body),
      error: (res: any) => this.onSaveError(res),
    });
  }

  protected onSaveSuccess(tiersPayant: ITiersPayant | null): void {
    this.isSaving = false;
    this.ref.close(tiersPayant);
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: this.errorService.getErrorMessage(error),
    });
  }

  private updateForm(tiersPayant: ITiersPayant): void {
    this.editForm.patchValue({
      id: tiersPayant.id,
      name: tiersPayant.name,
      fullName: tiersPayant.fullName,
      groupeTiersPayantId: tiersPayant.groupeTiersPayant.id,
      codeOrganisme: tiersPayant.codeOrganisme,
      telephone: tiersPayant.telephone,
      montantMaxParFcture: tiersPayant.montantMaxParFcture,
      nbreBordereaux: tiersPayant.nbreBordereaux,
      email: tiersPayant.email,
      remiseForfaitaire: tiersPayant.remiseForfaitaire,
      plafondConso: tiersPayant.plafondConso,
      plafondAbsolu: tiersPayant.plafondAbsolu,
      categorie: tiersPayant.categorie,
      useReferencedPrice: tiersPayant.useReferencedPrice,
      cmu: tiersPayant.cmu,
      modelFacture: tiersPayant.modelFacture,
      ordreTrisFacture: tiersPayant.ordreTrisFacture,
      toBeExclude: tiersPayant.toBeExclude,
    });
  }

  private createFromForm(): ITiersPayant {
    return {
      ...new TiersPayant(),
      id: this.editForm.get(['id']).value,
      name: this.editForm.get(['name']).value,
      fullName: this.editForm.get(['fullName']).value,
      nbreBordereaux: this.editForm.get(['nbreBordereaux']).value,
      remiseForfaitaire: this.editForm.get(['remiseForfaitaire']).value,
      email: this.editForm.get(['email']).value,
      categorie: this.categorie,
      plafondAbsolu: this.editForm.get(['plafondAbsolu']).value,
      plafondConso: this.editForm.get(['plafondConso']).value,
      montantMaxParFcture: this.editForm.get(['montantMaxParFcture']).value,
      codeOrganisme: this.editForm.get(['codeOrganisme']).value,
      telephone: this.editForm.get(['telephone']).value,
      groupeTiersPayantId: this.editForm.get(['groupeTiersPayantId']).value,
      useReferencedPrice: this.editForm.get(['useReferencedPrice']).value,
      cmu: this.editForm.get(['cmu']).value,
      modelFacture: this.editForm.get(['modelFacture']).value,
      toBeExclude: this.editForm.get(['toBeExclude']).value,
    };
  }

  private displayDisplayCmu(): void {
    if (this.editForm.get('useReferencedPrice').value) {
      this.editForm.get('cmu').disable();
    }
    if (this.editForm.get('cmu').value) {
      this.editForm.get('useReferencedPrice').disable();
    }
  }
}
