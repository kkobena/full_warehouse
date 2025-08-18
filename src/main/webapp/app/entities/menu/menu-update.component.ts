import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';

import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';

import { PrivillegeService } from './privillege.service';
import { IAuthority, Privilege } from '../../shared/model/authority.model';
import { BLOCK_SPACE } from '../../shared/util/warehouse-util';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { PanelModule } from 'primeng/panel';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputTextModule } from 'primeng/inputtext';

@Component({
  selector: 'jhi-menu-update',
  templateUrl: './menu-update.component.html',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RippleModule,
    PanelModule,
    FormsModule,
    KeyFilterModule,
    ReactiveFormsModule,
    InputTextModule
  ]
})
export class MenuUpdateComponent implements OnInit {
  protected privillegeService = inject(PrivillegeService);
  protected activatedRoute = inject(ActivatedRoute);
  private fb = inject(UntypedFormBuilder);

  isSaving = false;
  editForm = this.fb.group({
    libelle: [null, [Validators.required]],
    name: [null, [Validators.required]]
  });
  protected entity: IAuthority | null;
  protected readonly BLOCK_SPACE = BLOCK_SPACE;

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ privilege }) => {
      this.updateForm(privilege);
    });
  }

  updateForm(authority: IAuthority): void {
    this.editForm.patchValue({
      libelle: authority.libelle,
      name: authority.name
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
      error: () => this.onSaveError()
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
      libelle: this.editForm.get(['libelle']).value,
      name: this.editForm.get(['name']).value
    };
  }
}
