import { Component, inject, OnInit, viewChild } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';

import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';

import { IMagasin, Magasin } from 'app/shared/model/magasin.model';
import { MagasinService } from './magasin.service';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { ToolbarModule } from 'primeng/toolbar';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { ErrorService } from '../../shared/error.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { Textarea } from 'primeng/textarea';

@Component({
  selector: 'jhi-magasin-update',
  templateUrl: './magasin-update.component.html',
  styleUrl: './magasin-update.component.scss',
  imports: [
    RouterModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    ToolbarModule,
    ToastAlertComponent,
    WarehouseCommonModule,
    Textarea,
  ],
})
export class MagasinUpdateComponent implements OnInit {
  protected fb = inject(UntypedFormBuilder);
  protected isSaving = false;
  protected editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required]],
    fullName: [null, [Validators.required]],
    phone: [null, [Validators.required]],
    address: [],
    note: [],
    registre: [],
    welcomeMessage: [],
  });
  private readonly magasinService = inject(MagasinService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ magasin }) => {
      this.updateForm(magasin);
    });
  }

  protected updateForm(magasin: IMagasin): void {
    this.editForm.patchValue({
      id: magasin.id,
      name: magasin.name,
      fullName: magasin.fullName,
      phone: magasin.phone,
      address: magasin.address,
      note: magasin.note,
      registre: magasin.registre,
      welcomeMessage: magasin.welcomeMessage,
    });
  }

  protected previousState(): void {
    window.history.back();
  }

  protected save(): void {
    this.isSaving = true;
    const magasin = this.createFromForm();
    if (magasin.id !== undefined) {
      this.subscribeToSaveResponse(this.magasinService.update(magasin));
    } else {
      this.subscribeToSaveResponse(this.magasinService.create(magasin));
    }
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IMagasin>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: err => this.onSaveError(err),
    });
  }

  private onSaveSuccess(): void {
    this.isSaving = false;
    this.previousState();
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private createFromForm(): IMagasin {
    return {
      ...new Magasin(),
      id: this.editForm.get(['id']).value,
      name: this.editForm.get(['name']).value,
      fullName: this.editForm.get(['fullName']).value,
      phone: this.editForm.get(['phone']).value,
      address: this.editForm.get(['address']).value,
      note: this.editForm.get(['note']).value,
      registre: this.editForm.get(['registre']).value,
      welcomeMessage: this.editForm.get(['welcomeMessage']).value,
    };
  }
}
