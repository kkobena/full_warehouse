import { AfterViewInit, Component, DestroyRef, ElementRef, inject, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ButtonComponent, CardComponent, InputNumberComponent } from 'app/shared/ui';
import { IProduit } from 'app/shared/model/produit.model';
import { NotificationService } from 'app/shared/services/notification.service';
import { ProductsApiService } from '../../data-access/services/products-api.service';

@Component({
  selector: 'app-produit-decondition-modal',
  templateUrl: './produit-decondition-modal.component.html',
  styleUrls: ['./produit-decondition-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, InputNumberComponent, ButtonComponent, CardComponent],
})
export class ProduitDeconditionModalComponent implements AfterViewInit {
  produit!: IProduit;

  activeModal = inject(NgbActiveModal);
  isSaving = false;
  isNotValid = false;

  protected fb = inject(UntypedFormBuilder);
  editForm = this.fb.group({
    qtyMvt: [1, [Validators.required, Validators.min(1)]],
  });

  private readonly api = inject(ProductsApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly itemQty = viewChild.required<ElementRef<HTMLElement>>('qtyMvt');

  constructor() {
    this.editForm
      .get('qtyMvt')!
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => this.onQuantitySoldBoxChanged(value));
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      const input = this.itemQty()?.nativeElement.querySelector('input');
      if (input) {
        input.focus();
        input.select();
      }
    }, 100);
  }

  save(): void {
    this.isSaving = true;
    this.subscribeToSaveResponse(
      this.api.createDecondition({
        qtyMvt: this.editForm.get(['qtyMvt']).value,
        produitId: this.produit.id,
      }),
    );
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  onQuantitySoldBoxChanged(qty: number | null): void {
    this.isNotValid = (this.produit.totalQuantity ?? 0) < Number(qty ?? 0);
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<unknown>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.activeModal.close();
  }

  protected onSaveError(): void {
    this.isSaving = false;
    this.notificationService.error('Erreur lors du déconditionnement', 'Erreur');
  }
}
