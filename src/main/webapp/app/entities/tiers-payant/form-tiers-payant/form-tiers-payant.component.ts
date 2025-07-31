import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ErrorService } from 'app/shared/error.service';
import { DynamicDialogModule } from 'primeng/dynamicdialog';
import { TiersPayantService } from 'app/entities/tiers-payant/tierspayant.service';
import {
  GroupeTiersPayantService
} from 'app/entities/groupe-tiers-payant/groupe-tierspayant.service';
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
import { InputNumber } from 'primeng/inputnumber';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';

@Component({
  selector: 'jhi-form-tiers-payant',
  templateUrl: './form-tiers-payant.component.html',
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
    InputNumber,
    ToastAlertComponent,
  ],
})
export class FormTiersPayantComponent implements OnInit, AfterViewInit {
  entity?: ITiersPayant;
  header?: string;
  categorie?: string | null = null;
  protected fb = inject(UntypedFormBuilder);
  protected name = viewChild.required<ElementRef>('name');
  protected isSaving = false;
  protected isValid = true;
  protected groupeTiersPayants: IGroupeTiersPayant[] = [];
  protected modelFacture: ModelFacture[] = [];
  protected editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required]],
    fullName: [null, [Validators.required]],
    ncc: [],
    groupeTiersPayantId: [],
    codeOrganisme: [],
    telephone: [],
    montantMaxParFcture: [],
    nbreBordereaux: [1],
    email: [],
    remiseForfaitaire: [],
    plafondConso: [],
    plafondAbsolu: [],
    modelFacture: [],
    toBeExclude: [],
    plafondConsoClient: [],
    plafondJournalierClient: [],
    plafondAbsoluClient: [],
  });
  private readonly errorService = inject(ErrorService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');

  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }
    this.loadModelFacture();
    this.populate().then(r => {
      this.groupeTiersPayants = r;
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.name().nativeElement.focus();
    }, 100);
  }

  async populate(): Promise<IGroupeTiersPayant[]> {
    return await this.groupeTiersPayantService.queryPromise({ search: '' });
  }

  loadModelFacture(): void {
    this.tiersPayantService.getModelFacture().subscribe(res => {
      this.modelFacture = res.body;
    });
  }

  cancel(): void {
    this.activeModal.dismiss();
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

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ITiersPayant>>): void {
    result.subscribe({
      next: (res: HttpResponse<ITiersPayant>) => this.onSaveSuccess(res.body),
      error: (res: any) => this.onSaveError(res),
    });
  }

  protected onSaveSuccess(tiersPayant: ITiersPayant | null): void {
    this.isSaving = false;
    this.activeModal.close(tiersPayant);
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private updateForm(tiersPayant: ITiersPayant): void {
    this.editForm.patchValue({
      id: tiersPayant.id,
      name: tiersPayant.name,
      fullName: tiersPayant.fullName,
      groupeTiersPayantId: tiersPayant.groupeTiersPayant?.id,
      codeOrganisme: tiersPayant.codeOrganisme,
      telephone: tiersPayant.telephone,
      montantMaxParFcture: tiersPayant.montantMaxParFcture,
      nbreBordereaux: tiersPayant.nbreBordereaux,
      email: tiersPayant.email,
      remiseForfaitaire: tiersPayant.remiseForfaitaire,
      plafondConso: tiersPayant.plafondConso,
      plafondAbsolu: tiersPayant.plafondAbsolu,
      categorie: tiersPayant.categorie,
      modelFacture: tiersPayant.modelFacture,
      ordreTrisFacture: tiersPayant.ordreTrisFacture,
      toBeExclude: tiersPayant.toBeExclude,
      plafondConsoClient: tiersPayant.plafondConsoClient,
      plafondJournalierClient: tiersPayant.plafondJournalierClient,
      plafondAbsoluClient: tiersPayant.plafondAbsoluClient,
      ncc: tiersPayant.ncc,
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
      modelFacture: this.editForm.get(['modelFacture']).value,
      toBeExclude: this.editForm.get(['toBeExclude']).value,
      plafondConsoClient: this.editForm.get(['plafondConsoClient']).value,
      plafondJournalierClient: this.editForm.get(['plafondJournalierClient']).value,
      plafondAbsoluClient: this.editForm.get(['plafondAbsoluClient']).value,
      ncc: this.editForm.get(['ncc']).value,
    };
  }
}
