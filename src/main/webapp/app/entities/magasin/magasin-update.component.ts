import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';

import { IMagasin, Magasin } from 'app/shared/model/magasin.model';
import { MagasinService } from './magasin.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';

@Component({
  selector: 'jhi-magasin-update',
  templateUrl: './magasin-update.component.html',
  standalone: true,
  imports: [WarehouseCommonModule, PanelModule, RouterModule, ReactiveFormsModule, ButtonModule, RippleModule],
})
export class MagasinUpdateComponent implements OnInit {
  isSaving = false;

  editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required]],
    fullName: [null, [Validators.required]],
    phone: [null, [Validators.required]],
    address: [],
    note: [],
    registre: [],
    welcomeMessage: [],
  });

  constructor(
    protected magasinService: MagasinService,
    protected activatedRoute: ActivatedRoute,
    private fb: UntypedFormBuilder,
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ magasin }) => {
      this.updateForm(magasin);
    });
  }

  updateForm(magasin: IMagasin): void {
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

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const magasin = this.createFromForm();
    if (magasin.id !== undefined) {
      this.subscribeToSaveResponse(this.magasinService.update(magasin));
    } else {
      this.subscribeToSaveResponse(this.magasinService.create(magasin));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IMagasin>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.previousState();
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  private createFromForm(): IMagasin {
    return {
      ...new Magasin(),
      id: this.editForm.get(['id'])!.value,
      name: this.editForm.get(['name'])!.value,
      fullName: this.editForm.get(['fullName'])!.value,
      phone: this.editForm.get(['phone'])!.value,
      address: this.editForm.get(['address'])!.value,
      note: this.editForm.get(['note'])!.value,
      registre: this.editForm.get(['registre'])!.value,
      welcomeMessage: this.editForm.get(['welcomeMessage'])!.value,
    };
  }
}
