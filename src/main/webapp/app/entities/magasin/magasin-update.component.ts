import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';

import { IMagasin, Magasin } from 'app/shared/model/magasin.model';
import { MagasinService } from './magasin.service';

@Component({
  selector: 'jhi-magasin-update',
  templateUrl: './magasin-update.component.html',
})
export class MagasinUpdateComponent implements OnInit {
  isSaving = false;

  editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required]],
    phone: [null, [Validators.required]],
    address: [],
    note: [],
    registre: [],
  });

  constructor(protected magasinService: MagasinService, protected activatedRoute: ActivatedRoute, private fb: FormBuilder) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ magasin }) => {
      this.updateForm(magasin);
    });
  }

  updateForm(magasin: IMagasin): void {
    this.editForm.patchValue({
      id: magasin.id,
      name: magasin.name,
      phone: magasin.phone,
      address: magasin.address,
      note: magasin.note,
      registre: magasin.registre,
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

  private createFromForm(): IMagasin {
    return {
      ...new Magasin(),
      id: this.editForm.get(['id'])!.value,
      name: this.editForm.get(['name'])!.value,
      phone: this.editForm.get(['phone'])!.value,
      address: this.editForm.get(['address'])!.value,
      note: this.editForm.get(['note'])!.value,
      registre: this.editForm.get(['registre'])!.value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IMagasin>>): void {
    result.subscribe(
      () => this.onSaveSuccess(),
      () => this.onSaveError()
    );
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.previousState();
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }
}
