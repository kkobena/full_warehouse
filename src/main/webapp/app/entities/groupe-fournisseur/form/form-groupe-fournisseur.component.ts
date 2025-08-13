import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';

import { ErrorService } from '../../../shared/error.service';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { GroupeFournisseur, IGroupeFournisseur } from '../../../shared/model/groupe-fournisseur.model';
import { GroupeFournisseurService } from '../groupe-fournisseur.service';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { KeyFilterModule } from 'primeng/keyfilter';
import { Card } from 'primeng/card';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';

@Component({
  selector: 'jhi-form-groupe-fournisseur',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    KeyFilterModule,
    Card,
    ToastAlertComponent,
  ],
  templateUrl: './form-groupe-fournisseur.component.html',
  styleUrls: ['./form-groupe-fournisseur.component.scss'],
})
export class FormGroupeFournisseurComponent implements OnInit, AfterViewInit {
  header: string = '';
  entity?: IGroupeFournisseur;
  protected blockSpace: RegExp = /[^s]/;
  protected isSaving = false;
  protected isValid = true;
  protected fb = inject(UntypedFormBuilder);

  protected editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
    addresspostale: [],
    numFaxe: [],
    email: [],
    tel: [],
    odre: [],
    codeRecepteurPharmaMl: [],
    codeOfficePharmaMl: [],
    urlPharmaMl: [],
  });
  private readonly errorService = inject(ErrorService);
  private readonly entityService = inject(GroupeFournisseurService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private libelleInput = viewChild.required<ElementRef>('libelleInput');
  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }
  ngAfterViewInit(): void {
    setTimeout(() => {
      this.libelleInput().nativeElement.focus();
    }, 100);
  }
  cancel(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();

    if (entity.id !== undefined) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IGroupeFournisseur>>): void {
    result.subscribe({
      next: res => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error),
    });
  }

  private onSaveSuccess(entity: IGroupeFournisseur | null): void {
    this.isSaving = false;
    this.activeModal.close(entity);
  }

  private updateForm(entity: IGroupeFournisseur): void {
    this.editForm.patchValue({
      id: entity.id,
      libelle: entity.libelle,
      addresspostale: entity.addresspostale,
      numFaxe: entity.numFaxe,
      email: entity.email,
      tel: entity.tel,
      odre: entity.odre,
      urlPharmaMl: entity.urlPharmaMl,
      codeRecepteurPharmaMl: entity.codeRecepteurPharmaMl,
      codeOfficePharmaMl: entity.codeOfficePharmaMl,
    });
  }

  private onSaveError(error: any): void {
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private createFromForm(): IGroupeFournisseur {
    return {
      ...new GroupeFournisseur(),
      id: this.editForm.get(['id']).value,
      libelle: this.editForm.get(['libelle']).value,
      addresspostale: this.editForm.get(['addresspostale']).value,
      numFaxe: this.editForm.get(['numFaxe']).value,
      email: this.editForm.get(['email']).value,
      tel: this.editForm.get(['tel']).value,
      odre: this.editForm.get(['odre']).value,
      codeRecepteurPharmaMl: this.editForm.get(['codeRecepteurPharmaMl']).value,
      codeOfficePharmaMl: this.editForm.get(['codeOfficePharmaMl']).value,
      urlPharmaMl: this.editForm.get(['urlPharmaMl']).value,
    };
  }
}
