import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';

import { PrivillegeService } from './privillege.service';
import { IAuthority, Privilege } from '../../shared/model/authority.model';
import { BLOCK_SPACE } from '../../shared/util/warehouse-util';

@Component({
  selector: 'jhi-menu-update',
  templateUrl: './menu-update.component.html',
})
export class MenuUpdateComponent implements OnInit {
  isSaving = false;
  editForm = this.fb.group({
    libelle: [null, [Validators.required]],
    name: [null, [Validators.required]],
  });
  protected entity: IAuthority | null;
  protected readonly BLOCK_SPACE = BLOCK_SPACE;

  constructor(protected privillegeService: PrivillegeService, protected activatedRoute: ActivatedRoute, private fb: UntypedFormBuilder) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ privilege }) => {
      this.updateForm(privilege);
    });
  }

  updateForm(authority: IAuthority): void {
    this.editForm.patchValue({
      libelle: authority.libelle,
      name: authority.name,
    });
    if (authority.name) {
      this.editForm.get(['name']).disable();
      this.entity = authority;
    }
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const privilege = this.createFromForm();
    if (this.entity) {
      this.subscribeToSaveResponse(this.privillegeService.update(privilege));
    } else {
      this.subscribeToSaveResponse(this.privillegeService.create(privilege));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IAuthority>>): void {
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

  private createFromForm(): IAuthority {
    return {
      ...new Privilege(),
      libelle: this.editForm.get(['libelle'])!.value,
      name: this.editForm.get(['name'])!.value,
    };
  }
}
