import { Component, inject, OnInit } from '@angular/core';
import {
  CATEGORY_INVENTORY,
  InventoryCategory,
  InventoryCategoryType,
  IStoreInventory,
  StoreInventory,
} from '../../../shared/model/store-inventory.model';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { StoreInventoryService } from '../store-inventory.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { StorageService } from '../../storage/storage.service';
import { Storage } from '../../storage/storage.model';
import { RayonService } from '../../rayon/rayon.service';
import { IRayon, Rayon } from '../../../shared/model/rayon.model';
import { APPEND_TO } from '../../../shared/constants/pagination.constants';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { TableModule } from 'primeng/table';
import { RouterModule } from '@angular/router';
import { Select } from 'primeng/select';
import { InputText } from 'primeng/inputtext';

@Component({
  selector: 'jhi-init-inventory',
  templateUrl: './inventory-form.component.html',
  imports: [
    WarehouseCommonModule,
    ConfirmDialogModule,
    ButtonModule,
    RippleModule,
    TooltipModule,
    ToastModule,
    NgxSpinnerModule,
    TableModule,
    RouterModule,
    DynamicDialogModule,
    ReactiveFormsModule,
    Select,
    InputText,
  ],
})
export class InventoryFormComponent implements OnInit {
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  protected isSaving = false;
  protected categories: InventoryCategory[] = CATEGORY_INVENTORY;
  protected storages: Storage[];
  protected rayons: Rayon[];
  protected readonly appendTo = APPEND_TO;
  protected editForm: FormGroup;
  protected entity: IStoreInventory;
  private fb = inject(FormBuilder);
  private messageService = inject(MessageService);
  private storeInventoryService = inject(StoreInventoryService);
  private storageService = inject(StorageService);
  private rayonService = inject(RayonService);
  private spinner = inject(NgxSpinnerService);

  ngOnInit(): void {
    this.initForm();
    this.entity = this.config.data.entity;
    if (this.entity) {
      this.updateForm(this.entity);
      this.loadRayons(this.entity.storage.id);
    }
    this.populate();
  }

  cancel(): void {
    this.ref.destroy();
  }

  protected save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    this.spinner.show();
    if (entity.id) {
      this.subscribeToSaveResponse(this.storeInventoryService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.storeInventoryService.create(entity));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IStoreInventory>>): void {
    result.subscribe({
      next: (res: HttpResponse<IStoreInventory>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
      complete: () => this.spinner.hide(),
    });
  }

  protected onSelectCategory(evt: any): void {
    const category = evt.value.name as InventoryCategoryType;
    this.manageStorageInput(category);
    this.manageRayonInput(category);
  }

  protected onSelectStrorage(evt: any): void {
    this.loadRayons(evt.value.id);
  }

  private manageRayonInput(category: InventoryCategoryType): void {
    switch (category) {
      case 'MAGASIN':
      case 'STORAGE':
        this.removeRayonControl();
        break;
      case 'RAYON':
        this.addRayonControl();
        break;
    }
  }

  private manageStorageInput(category: InventoryCategoryType): void {
    switch (category) {
      case 'MAGASIN':
        this.removeStorageControl();
        break;
      case 'STORAGE':
      case 'RAYON':
        this.addStorageControl();
        break;
    }
  }

  private onSaveError(): void {
    this.spinner.hide();
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Enregistrement a échoué',
    });
  }

  private onSaveSuccess(response: IStoreInventory | null): void {
    this.spinner.hide();
    this.ref.close(response);
  }

  private createFromForm(): IStoreInventory {
    const inventoryCategory = this.editForm.get(['inventoryCategory']).value.name;
    if (inventoryCategory === 'MAGASIN') {
      return {
        ...new StoreInventory(),
        id: this.editForm.get(['id']).value,
        description: this.editForm.get(['description']).value,
        inventoryCategory,
      };
    }
    return {
      ...new StoreInventory(),
      description: this.editForm.get(['description']).value,
      id: this.editForm.get(['id']).value,
      storage: this.editForm.get(['storage']).value?.id,
      rayon: this.editForm.get(['rayon']).value?.id,
      inventoryCategory,
    };
  }

  private updateForm(entity: IStoreInventory): void {
    this.editForm.patchValue({
      id: entity.id,
      storage: entity.storage.id,
      rayon: entity.rayon.id,
      inventoryCategory: entity.inventoryCategory.name,
      description: entity.description,
    });
  }

  private populate(): void {
    this.storageService.query().subscribe((res: HttpResponse<Storage[]>) => {
      this.storages = res.body || [];
    });
  }

  private loadRayons(storageId: number): void {
    if (storageId) {
      this.rayonService
        .query({
          storageId,
          size: 9999,
        })
        .subscribe((res: HttpResponse<IRayon[]>) => {
          this.rayons = res.body || [];
        });
    }
  }

  private initForm(): void {
    this.editForm = this.fb.group({
      id: new FormControl<number | null>(null, {}),
      description: new FormControl<string | null>(null, {
        validators: [Validators.required, Validators.maxLength(255)],
        nonNullable: true,
      }),
      inventoryCategory: new FormControl<InventoryCategory | null>(this.categories[0], {
        validators: [Validators.required],
        nonNullable: true,
      }),
    });
  }

  private addStorageControl(): void {
    this.editForm.addControl(
      'storage',
      new FormControl<number | null>(null, {
        validators: [Validators.required],
        nonNullable: true,
      }),
    );
  }

  private removeStorageControl(): void {
    this.editForm.removeControl('storage');
  }

  private addRayonControl(): void {
    this.editForm.addControl(
      'rayon',
      new FormControl<number | null>(null, {
        validators: [Validators.required],
        nonNullable: true,
      }),
    );
  }

  private removeRayonControl(): void {
    // this.editForm.get('rayon').reset();
    this.editForm.removeControl('rayon');
  }
}
