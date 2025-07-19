import {Component, ElementRef, input, output, viewChild} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {FloatLabelModule} from 'primeng/floatlabel';
import {InputGroupModule} from 'primeng/inputgroup';
import {InputGroupAddonModule} from 'primeng/inputgroupaddon';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {TranslatePipe} from '@ngx-translate/core';
import {KeyFilterModule} from "primeng/keyfilter";

@Component({
  selector: 'jhi-quantite-produt-saisie',
  imports: [FormsModule, FloatLabelModule, InputGroupModule, InputGroupAddonModule, InputText, Button, TranslatePipe, KeyFilterModule],
  template: `
    <p-floatlabel variant="on">
      <p-inputgroup>
        <input
          [style]="style()"
          autocomplete="off"
          #quantityBox
          pInputText
          pKeyFilter="int"
          id="quantiteSaisie"
          [(ngModel)]="quantite"
          [disabled]="!isValid()"
          (keydown.enter)="onAdd()"
        />
        <label for="quantiteSaisie">
          {{ 'warehouseApp.gestionPerimes.labels.quantiteSaisie' | translate }}
        </label>
        <p-inputgroup-addon>
          <p-button
            (click)="handleEnter()"
            icon="pi pi-plus"
            [disabled]="!enabledButton"
            severity="primary"
          ></p-button>
        </p-inputgroup-addon>
      </p-inputgroup>
    </p-floatlabel>
  `
})
export class QuantiteProdutSaisieComponent {
  hasSelectedProduct = input<boolean>(false);
  isValid = input<boolean>(true);
  style = input<{}>();
  addQuantite = output<number>();
  enterPressed = output<void>();
  protected quantite: number | null = null;
  protected quantityBox = viewChild.required<ElementRef>('quantityBox');

  focusProduitControl(): void {
    setTimeout(() => {
      const el = this.quantityBox().nativeElement;
      el.focus();
      el.select();
    }, 50);
  }

  get enabledButton(): boolean {
    return this.quantite > 0;
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

reset(): void {
  this.quantite = null;

}
}
