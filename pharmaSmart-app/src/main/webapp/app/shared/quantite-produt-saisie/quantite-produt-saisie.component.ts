import {signal, Component, computed, ElementRef, input, output, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonComponent, FloatLabelComponent, KeyFilterDirective } from '../ui';

@Component({
  selector: 'jhi-quantite-produt-saisie',
  imports: [FormsModule, FloatLabelComponent, ButtonComponent, TranslatePipe, KeyFilterDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-float-label label="{{ 'warehouseApp.gestionPerimes.labels.quantiteSaisie' | translate }}" inputId="quantiteSaisie">
      <div class="input-group">
        @if (showSteppers()) {
          <app-button (clicked)="decrementQuantity()" [disabled]="!isValid()" ariaLabel="Diminuer la quantité" icon="pi pi-minus" severity="secondary" />
        }
        <input
          [style]="style()"
          autocomplete="off"
          #quantityBox
          [appKeyFilter]="'int'"
          class="form-control"
          [class.border-warning]="depasseStock()"
          [attr.aria-describedby]="depasseStock() ? 'quantiteSaisie-stock' : null"
          id="quantiteSaisie"
          placeholder=" "
          [ngModel]="quantite()" (ngModelChange)="quantite.set($event)"
          [disabled]="!isValid()"
          (keydown.enter)="onAdd()"
        />
        @if (showSteppers()) {
          <app-button (clicked)="incrementQuantity()" [disabled]="!isValid()" ariaLabel="Augmenter la quantité" icon="pi pi-plus" severity="secondary" />
        }
        <app-button (clicked)="handleEnter()" icon="pi pi-check" ariaLabel="Ajouter au panier" [disabled]="disabledButton()" severity="primary" />
      </div>
    </app-float-label>
    <!-- Avant l'ajout : la quantité tapée dépasse le stock rayon (le forçage reste possible à l'ajout) -->
    @if (depasseStock()) {
      <small class="stock-alerte" id="quantiteSaisie-stock" role="status">
        <i aria-hidden="true" class="pi pi-exclamation-triangle"></i>
        Stock rayon : {{ stockDisponible() }} — la quantité saisie le dépasse
      </small>
    }
  `,
  styles: `
    .stock-alerte {
      display: block;
      margin-top: 0.25rem;
      color: #c77700;
      font-size: 0.8rem;
      font-weight: 500;
    }
  `,
})
export class QuantiteProdutSaisieComponent {
  hasSelectedProduct = input<boolean>(false);
  isValid = input<boolean>(true);
  disabledButton = input<boolean>(false);
  style = input<{}>();
  /** Boutons − et + de part et d'autre du champ (écran tactile du comptoir). Absents par défaut : ce composant sert aussi hors vente. */
  showSteppers = input<boolean>(false);
  /** Stock disponible du produit sélectionné ; renseigné, il déclenche l'avertissement quand la saisie le dépasse. */
  stockDisponible = input<number | null>(null);
  addQuantite = output<number>();
  enterPressed = output();
  readonly quantite = signal<number | null>(null);
  /** Anticipe la rupture : la quantité tapée dépasse le stock, avant l'ajout et non dans le panier. */
  protected readonly depasseStock = computed(() => {
    const stock = this.stockDisponible();
    const saisie = Number(this.quantite() ?? 0);
    return stock != null && saisie > stock;
  });
  protected quantityBox = viewChild.required<ElementRef>('quantityBox');

  get enabledButton(): boolean {
    return this.quantite() > 0;
  }

  get value(): number {
    return this.quantite();
  }

  focusProduitControl(): void {
    setTimeout(() => {
      const el = this.quantityBox().nativeElement;
      el.focus();
      el.select();
    }, 50);
  }

  reset(value?: number): void {
    this.quantite.set(value || null);
  }

  protected onAdd(): void {
    const value = this.quantite();
    if (this.isValid() && value != null) {
      this.addQuantite.emit(value);
    }
  }

  protected handleEnter(): void {
    this.onAdd();
  }

  incrementQuantity(amount = 1): void {
    if (this.isValid()) {
      const currentValue = this.quantite() || 0;
      this.quantite.set(currentValue + amount);
      this.focusProduitControl();
    }
  }

  decrementQuantity(amount = 1): void {
    if (this.isValid()) {
      const currentValue = this.quantite() || 0;
      const newValue = currentValue - amount;
      this.quantite.set(newValue > 0 ? newValue : 1);
      this.focusProduitControl();
    }
  }
}
