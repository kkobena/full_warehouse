import { Component, ElementRef, input, output, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonComponent, FloatLabelComponent, KeyFilterDirective } from '../ui';

@Component({
  selector: 'jhi-quantite-produt-saisie',
  imports: [FormsModule, FloatLabelComponent, ButtonComponent, TranslatePipe, KeyFilterDirective],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <app-float-label label="{{ 'warehouseApp.gestionPerimes.labels.quantiteSaisie' | translate }}" inputId="quantiteSaisie">
      <div class="input-group">
        <input
          [style]="style()"
          autocomplete="off"
          #quantityBox
          [appKeyFilter]="'int'"
          class="form-control"
          id="quantiteSaisie"
          placeholder=" "
          [(ngModel)]="quantite"
          [disabled]="!isValid()"
          (keydown.enter)="onAdd()"
        />
        <app-button (clicked)="handleEnter()" icon="pi pi-check" [disabled]="disabledButton()" severity="primary" />
      </div>
    </app-float-label>
  `,
})
export class QuantiteProdutSaisieComponent {
  hasSelectedProduct = input<boolean>(false);
  isValid = input<boolean>(true);
  disabledButton = input<boolean>(false);
  style = input<{}>();
  addQuantite = output<number>();
  enterPressed = output();
  quantite: number | null = null;
  protected quantityBox = viewChild.required<ElementRef>('quantityBox');

  get enabledButton(): boolean {
    return this.quantite > 0;
  }

  get value(): number {
    return this.quantite;
  }

  focusProduitControl(): void {
    setTimeout(() => {
      const el = this.quantityBox().nativeElement;
      el.focus();
      el.select();
    }, 50);
  }

  reset(value?: number): void {
    this.quantite = value || null;
  }

  protected onAdd(): void {
    const value = this.quantite;
    if (this.isValid() && value != null) {
      this.addQuantite.emit(value);
    }
  }

  protected handleEnter(): void {
    this.onAdd();
  }

  incrementQuantity(amount = 1): void {
    if (this.isValid()) {
      const currentValue = this.quantite || 0;
      this.quantite = currentValue + amount;
      this.focusProduitControl();
    }
  }

  decrementQuantity(amount = 1): void {
    if (this.isValid()) {
      const currentValue = this.quantite || 0;
      const newValue = currentValue - amount;
      this.quantite = newValue > 0 ? newValue : 1;
      this.focusProduitControl();
    }
  }
}
