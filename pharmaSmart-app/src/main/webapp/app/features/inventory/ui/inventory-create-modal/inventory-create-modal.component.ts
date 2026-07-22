import {Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {NgbActiveModal, NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {StorageService} from '../../../../entities/storage/storage.service';
import {RayonService} from '../../../../entities/rayon/rayon.service';
import {FamilleProduitService} from '../../../../entities/famille-produit/famille-produit.service';
import {IStorage} from '../../../../shared/model/magasin.model';
import {IRayon} from '../../../../shared/model/rayon.model';
import {IFamilleProduit} from '../../../../shared/model/famille-produit.model';
import {
  INVENTORY_CATEGORIES,
  InventoryCategoryInfo,
  InventoryCategoryType,
  StoreInventoryCreateRecord,
} from '../../models';
import {InventoryApiService} from '../../data-access/services/inventory-api.service';
import {InventoryStore} from '../../data-access/store/inventory.store';
import {NgxSpinnerService} from "ngx-spinner";
import {NGB_DATE_TO_ISO} from '../../../../shared/util/warehouse-util';
import {ButtonComponent, CardComponent, InputNumberComponent, SelectComponent, SelectSearchComponent} from '../../../../shared/ui';
import {PharmaDatePickerComponent} from '../../../../shared/date-picker/pharma-date-picker.component';

const GROUP_LABELS: Record<string, string> = {scope: 'Périmètre', thematic: 'Thématique'};

@Component({
  selector: 'app-inventory-create-modal',
  imports: [CommonModule, ReactiveFormsModule, ButtonComponent, SelectComponent, SelectSearchComponent, InputNumberComponent, PharmaDatePickerComponent, CardComponent],
  templateUrl: './inventory-create-modal.component.html',
  styleUrl: './inventory-create-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class InventoryCreateModalComponent implements OnInit {
  readonly activeModal = inject(NgbActiveModal);
  prefill?: { inventoryCategory?: InventoryCategoryType; storageId?: number; rayonId?: number };
  form!: FormGroup;
  categories = INVENTORY_CATEGORIES.map(c => ({...c, groupLabel: GROUP_LABELS[c.group]}));
  storages = signal<IStorage[]>([]);
  rayons = signal<IRayon[]>([]);
  familles = signal<IFamilleProduit[]>([]);
  selectedCategory = signal<InventoryCategoryInfo | null>(null);
  loading = signal(false);
  errorMessage = signal<string | null>(null);
  private pendingRayonId: number | null = null;
  readonly classesParetoOptions = [
    {value: null, label: 'Toutes (A + B + C)'},
    {value: 'A', label: 'Classe A — top 20% CA'},
    {value: 'B', label: 'Classe B — 30% suivants'},
    {value: 'C', label: 'Classe C — 50% restants'},
  ];
  private readonly api = inject(InventoryApiService);
  private readonly store = inject(InventoryStore);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly fb = inject(FormBuilder);
  private readonly storageService = inject(StorageService);
  private readonly rayonService = inject(RayonService);
  private readonly familleProduitService = inject(FamilleProduitService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.buildForm();
    this.loadStorages();
    this.loadFamilles();

    this.form.get('inventoryCategory')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((cat: InventoryCategoryType) => {
        const info = this.categories.find(c => c.value === cat) ?? null;
        this.selectedCategory.set(info);
        this.handleCategoryChange(info);
      });

    this.form.get('storage')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((storageId: number) => {
        if (storageId && this.selectedCategory()?.needsRayon) {
          this.loadRayons(storageId);
        } else {
          this.rayons.set([]);
          this.form.get('rayon')!.setValue(null);
        }
      });

    if (this.prefill) {
      this.applyPrefill();
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();

    const record: StoreInventoryCreateRecord = {
      inventoryCategory: value.inventoryCategory,
      description: value.description ?? undefined,
      storage: value.storage ?? undefined,
      rayon: value.rayon ?? undefined,
      famillyId: value.famillyId ?? undefined,
      dateFrom: NGB_DATE_TO_ISO(value.dateFrom),
      dateTo: NGB_DATE_TO_ISO(value.dateTo),
      alerteJours: value.alerteJours ?? undefined,
      classePareto: value.classePareto ?? undefined,
    };
    this.loading.set(true);
    this.errorMessage.set(null);
    this.spinner.show();
    this.api.create(record)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: resp => {
          this.spinner.hide();
          this.loading.set(false);
          this.store.emitEvent('INVENTORY_CREATED', resp.body);
          this.activeModal.close(resp.body);
        },
        error: err => {
          this.spinner.hide();
          this.loading.set(false);
          this.errorMessage.set(err?.error?.message ?? err?.message ?? "Erreur lors de la création de l'inventaire");
        },
      });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  getCategoryLabel(value: InventoryCategoryType): string {
    return this.categories.find(c => c.value === value)?.label ?? value;
  }

  private buildForm(): void {
    this.form = this.fb.group({
      inventoryCategory: [null, Validators.required],
      description: [null, Validators.required],
      storage: [null],
      rayon: [null],
      famillyId: [null],
      dateFrom: new FormControl<NgbDateStruct | null>(null),
      dateTo: new FormControl<NgbDateStruct | null>(null),
      alerteJours: [90],
      classePareto: [null],
    });
  }

  private handleCategoryChange(info: InventoryCategoryInfo | null): void {
    this.form.patchValue({
      storage: null,
      rayon: null,
      famillyId: null,
      dateFrom: null,
      dateTo: null,
      alerteJours: 90,
      classePareto: null,
    });
    this.rayons.set([]);

    const storageCtrl = this.form.get('storage')!;
    const rayonCtrl = this.form.get('rayon')!;
    const famillyCtrl = this.form.get('famillyId')!;
    const dateFromCtrl = this.form.get('dateFrom')!;
    const dateToCtrl = this.form.get('dateTo')!;
    const alerteCtrl = this.form.get('alerteJours')!;

    storageCtrl.clearValidators();
    rayonCtrl.clearValidators();
    famillyCtrl.clearValidators();
    dateFromCtrl.clearValidators();
    dateToCtrl.clearValidators();
    alerteCtrl.clearValidators();

    if (info?.needsStorage) {
      storageCtrl.setValidators(Validators.required);
    }
    if (info?.needsRayon) {
      rayonCtrl.setValidators(Validators.required);
    }
    if (info?.needsFamilly) {
      famillyCtrl.setValidators(Validators.required);
    }
    if (info?.needsDateRange) {
      dateFromCtrl.setValidators(Validators.required);
      dateToCtrl.setValidators(Validators.required);
    }
    if (info?.needsAlerteJours) {
      alerteCtrl.setValidators([Validators.required, Validators.min(1)]);
    }

    [storageCtrl, rayonCtrl, famillyCtrl, dateFromCtrl, dateToCtrl, alerteCtrl].forEach(c => c.updateValueAndValidity());
  }

  private loadStorages(): void {
    this.storageService
      .fetchUserStorages()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: resp => this.storages.set(resp.body ?? []),
        error: () => this.storages.set([]),
      });
  }

  private applyPrefill(): void {
    const p = this.prefill!;
    if (p.inventoryCategory) {
      this.form.get('inventoryCategory')!.setValue(p.inventoryCategory);
    }
    if (p.storageId) {
      this.pendingRayonId = p.rayonId ?? null;
      this.form.get('storage')!.setValue(p.storageId);
    }
  }

  private loadRayons(storageId: number): void {
    this.rayonService
      .query({storageId, size: 9999})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: resp => {
          this.rayons.set(resp.body ?? []);
          if (this.pendingRayonId) {
            this.form.get('rayon')!.setValue(this.pendingRayonId);
            this.pendingRayonId = null;
          }
        },
        error: () => this.rayons.set([]),
      });
  }

  private loadFamilles(): void {
    this.familleProduitService
      .query({size: 9999})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: resp => this.familles.set(resp.body ?? []),
        error: () => this.familles.set([]),
      });
  }
}
