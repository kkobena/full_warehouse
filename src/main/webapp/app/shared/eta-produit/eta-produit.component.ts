import { Component, input } from '@angular/core';
import { EtatProduit } from '../model/etat-produit.model';
import { MeterGroup } from 'primeng/metergroup';
/*
 template: ` <p-metergroup [value]="values" [max]="120" />`,
 */
@Component({
  selector: 'jhi-eta-produit',
  imports: [MeterGroup],
  template: ` <p-metergroup [value]="values" [max]="120" labelOrientation="horizontal">
    <ng-template #meter let-value let-class="class" let-width="size">
      <span [class]="class" [style]="{ background: value.color, width: width }"></span>
    </ng-template>

    <ng-template #label>
      @if (showLabel()) {
        <ng-container>
          <ol class="p-metergroup-label-list p-component p-metergroup-label-list-horizontal">
            @for (l of values; track $index) {
              <li class="p-metergroup-label">
                <span [style]="{ backgroundColor: l.color }" class="p-metergroup-label-marker"></span>
                <span class="p-metergroup-label-text">{{ l.label }}</span>
              </li>
            }
          </ol>
        </ng-container>
      }
    </ng-template>
  </p-metergroup>`,
  styles: ``,
})
export class EtaProduitComponent {
  readonly etatProduit = input.required<EtatProduit>();
  readonly showLabel = input<boolean>(true);

  private getStockPositif(etat: EtatProduit): number {
    if (etat.stockPositif) {
      return 120 - (etat.enSuggestion ? 20 : 0) - (etat.enCommande ? 20 : 0) - (etat.entree ? 20 : 0);
    }
    return 0;
  }
  private getSockZero(etat: EtatProduit): number {
    if (etat.sockZero) {
      return 120 - (etat.enSuggestion ? 20 : 0) - (etat.enCommande ? 20 : 0) - (etat.entree ? 20 : 0);
    }
    return 0;
  }
  private getStockNegatif(etat: EtatProduit): number {
    if (etat.stockNegatif) {
      return 120 - (etat.enSuggestion ? 20 : 0) - (etat.enCommande ? 20 : 0) - (etat.entree ? 20 : 0);
    }
    return 0;
  }
  private getEnSuggestion(etat: EtatProduit): number {
    if (etat.enSuggestion) {
      return (
        120 -
        (etat.stockPositif ? 20 : 0) -
        (etat.enCommande ? 20 : 0) -
        (etat.entree ? 20 : 0) -
        (etat.stockNegatif ? 20 : 0) -
        (etat.sockZero ? 20 : 0)
      );
    }
    return 0;
  }
  private getEnCommande(etat: EtatProduit): number {
    if (etat.enCommande) {
      return (
        120 -
        (etat.stockPositif ? 20 : 0) -
        (etat.enSuggestion ? 20 : 0) -
        (etat.entree ? 20 : 0) -
        (etat.stockNegatif ? 20 : 0) -
        (etat.sockZero ? 20 : 0)
      );
    }
    return 0;
  }
  private getEntree(etat: EtatProduit): number {
    if (etat.entree) {
      return (
        120 -
        (etat.stockPositif ? 20 : 0) -
        (etat.enSuggestion ? 20 : 0) -
        (etat.enCommande ? 20 : 0) -
        (etat.stockNegatif ? 20 : 0) -
        (etat.sockZero ? 20 : 0)
      );
    }
    return 0;
  }

  protected get values(): any[] {
    const values = [];
    const etat = this.etatProduit();

    if (etat) {
      if (etat.stockPositif) {
        values.push({
          value: this.getStockPositif(etat),
          color: '#34d399',
          label: 'P',
        });
      }
      if (etat.sockZero) {
        values.push({
          value: this.getSockZero(etat),
          color: '#fbbf24',
          label: 'Z',
        });
      }

      if (etat.stockNegatif) {
        values.push({
          value: this.getStockNegatif(etat),
          color: '#f44336',
          label: 'N',
        });
      }
      if (etat.enSuggestion) {
        values.push({
          value: this.getEnSuggestion(etat),
          color: '#60a5fa',
          label: 'S',
        });
      }
      if (etat.enCommande) {
        values.push({
          value: this.getEnCommande(etat),
          color: '#c084fc',
          label: 'C',
        });
        if (etat.entree) {
          values.push({
            value: this.getEntree(etat),
            color: '#9c27b0',
            label: 'E',
          });
        }
      }
    }
    console.log(values);
    return values;
  }
}
