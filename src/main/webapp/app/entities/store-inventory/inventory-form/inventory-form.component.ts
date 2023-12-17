import { Component, OnInit } from '@angular/core';
import {
  CATEGORY_INVENTORY,
  InventoryCategory,
  InventoryCategoryType,
  IStoreInventory,
  StoreInventory,
} from '../../../shared/model/store-inventory.model';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { StoreInventoryService } from '../store-inventory.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { StorageService } from '../../storage/storage.service';
import { Storage } from '../../storage/storage.model';
import { RayonService } from '../../rayon/rayon.service';
import { IRayon, Rayon } from '../../../shared/model/rayon.model';
import { APPEND_TO } from '../../../shared/constants/pagination.constants';
import { NgxSpinnerService } from 'ngx-spinner';

@Component({
  selector: 'jhi-init-inventory',
  templateUrl: './inventory-form.component.html',
})
export class InventoryFormComponent implements OnInit {
  protected isSaving: boolean = false;
  protected categories: InventoryCategory[] = CATEGORY_INVENTORY;
  protected storages: Storage[];
  protected rayons: Rayon[];
  protected readonly appendTo = APPEND_TO;
  protected editForm: FormGroup;
  protected entity: IStoreInventory;

  constructor(
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    private fb: FormBuilder,
    private messageService: MessageService,
    private storeInventoryService: StoreInventoryService,
    private storageService: StorageService,
    private rayonService: RayonService,
    private spinner: NgxSpinnerService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.entity = this.config.data.entity;
    if (this.entity) {
      this.updateForm(this.entity);
      this.loadRayons(this.entity.storage?.id);
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
    return {
      ...new StoreInventory(),
      id: this.editForm.get(['id'])!.value,
      storage: this.editForm.get(['storage'])?.value?.id,
      rayon: this.editForm.get(['rayon'])?.value?.id,
      inventoryCategory: this.editForm.get(['inventoryCategory'])!.value?.name,
    };
  }

  private updateForm(entity: IStoreInventory): void {
    this.editForm.patchValue({
      id: entity.id,
      storage: entity.storage?.id,
      rayon: entity.rayon?.id,
      inventoryCategory: entity.inventoryCategory.name,
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
          storageId: storageId,
        })
        .subscribe((res: HttpResponse<IRayon[]>) => {
          this.rayons = res.body || [];
        });
    }
  }

  private initForm(): void {
    this.editForm = this.fb.group({
      id: new FormControl<number | null>(null, {}),
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
      })
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
      })
    );
  }

  private removeRayonControl(): void {
    // this.editForm.get('rayon').reset();
    this.editForm.removeControl('rayon');
  }
}
