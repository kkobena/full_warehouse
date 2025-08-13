import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { GroupeTiersPayant, IGroupeTiersPayant } from 'app/shared/model/groupe-tierspayant.model';
import { ErrorService } from 'app/shared/error.service';
import { GroupeTiersPayantService } from 'app/entities/groupe-tiers-payant/groupe-tierspayant.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { KeyFilterModule } from 'primeng/keyfilter';
import { OrdreTrisFacture } from '../../../shared/model/tierspayant.model';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { Card } from 'primeng/card';
import { Select } from 'primeng/select';

@Component({
  selector: 'jhi-form-groupe-tiers-payant',
  templateUrl: './form-groupe-tiers-payant.component.html',
  styleUrls: ['./form-groupe-tiers-payant.component.scss'],
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    KeyFilterModule,
    ToastAlertComponent,
    Card,
    Select,
  ],
})
export class FormGroupeTiersPayantComponent implements OnInit, AfterViewInit {
  header: string = '';
  entity?: IGroupeTiersPayant;
  protected ordreTrisFacture: OrdreTrisFacture[] = [];
  protected isSaving = false;
  protected isValid = true;
  protected fb = inject(UntypedFormBuilder);
  protected name = viewChild.required<ElementRef>('name');
  protected editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required]],
    adresse: [],
    telephone: [],
    telephoneFixe: [],
    ordreTrisFacture: [],
  });
  private readonly errorService = inject(ErrorService);
  private readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }
    this.loadOrdreTrisFacture();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.name().nativeElement.focus();
    }, 100);
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
    this.activeModal.dismiss();
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
      id: this.editForm.get(['id']).value,
      name: this.editForm.get(['name']).value,
      adresse: this.editForm.get(['adresse']).value,
      telephone: this.editForm.get(['telephone']).value,
      telephoneFixe: this.editForm.get(['telephoneFixe']).value,
      ordreTrisFacture: this.editForm.get(['ordreTrisFacture']).value,
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
    this.alert().showInfo('Opération effectuée avec succès');
    this.activeModal.close(groupeTiersPayant);
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }
}
