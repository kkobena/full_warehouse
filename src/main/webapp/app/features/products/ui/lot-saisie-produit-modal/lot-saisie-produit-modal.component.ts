import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { TooltipModule } from 'primeng/tooltip';
import { IProduit } from 'app/shared/model/produit.model';
import { ILot } from 'app/shared/model/lot.model';
import { ProductsApiService } from '../../data-access/services/products-api.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';

@Component({
  selector: 'app-lot-saisie-produit-modal',
  templateUrl: './lot-saisie-produit-modal.component.html',
  styleUrls: ['./lot-saisie-produit-modal.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    TooltipModule,
    PharmaDatePickerComponent,
  ],
})
export class LotSaisieProduitModalComponent implements OnInit {
  /** Produit cible — à définir avant l'ouverture du modal */
  produit!: IProduit;

  protected form!: FormGroup;
  protected isSaving = signal(false);

  /** Date d'aujourd'hui en NgbDateStruct (minDate péremption) */
  protected todayStruct: NgbDateStruct;
  /** Date d'aujourd'hui en NgbDateStruct (maxDate fabrication) */
  protected todayMaxStruct: NgbDateStruct;

  protected readonly activeModal = inject(NgbActiveModal);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ProductsApiService);
  private readonly notif = inject(NotificationService);

  constructor() {
    const now = new Date();
    this.todayStruct = { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate() };
    this.todayMaxStruct = { ...this.todayStruct };
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      numLot: [null, [Validators.required, Validators.maxLength(20)]],
      expiryDate: [null, [Validators.required]],
      manufacturingDate: [null],
      quantityReceived: [1, [Validators.required, Validators.min(1), Validators.max(this.totalStock())]],
    });
  }

  get totalStock(): () => number {
    return () => this.produit?.totalQuantity ?? 0;
  }

  protected get isQuantiteValide(): boolean {
    const qty = this.form.get('quantityReceived')?.value ?? 0;
    return qty > 0 && qty <= this.totalStock();
  }

  protected save(): void {
    if (this.form.invalid || !this.isQuantiteValide) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSaving.set(true);
    const raw = this.form.getRawValue();

    const lot: ILot = {
      produitId: this.produit.id,
      numLot: raw.numLot,
      expiryDate: this.ngbDateToIso(raw.expiryDate),
      manufacturingDate: raw.manufacturingDate ? this.ngbDateToIso(raw.manufacturingDate) : undefined,
      quantityReceived: raw.quantityReceived,
      freeQty: 0,
    };

    this.api.addLotHorsCommande(lot).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.notif.success('Lot enregistré avec succès', 'Saisie lot');
        this.activeModal.close('saved');
      },
      error: (err) => {
        this.isSaving.set(false);
        const msg = err?.error?.detail ?? err?.error?.title ?? 'Erreur lors de la saisie du lot';
        this.notif.error(msg, 'Erreur');
      },
    });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private ngbDateToIso(d: NgbDateStruct | null): string | undefined {
    if (!d) return undefined;
    return `${d.year}-${String(d.month).padStart(2, '0')}-${String(d.day).padStart(2, '0')}`;
  }
}
