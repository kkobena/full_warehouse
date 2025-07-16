import { Component, computed, ElementRef, inject, viewChild } from '@angular/core';
import { Panel } from 'primeng/panel';
import { AutoComplete } from 'primeng/autocomplete';
import { DecimalPipe } from '@angular/common';
import {
  APPEND_TO,
  PRODUIT_COMBO_MIN_LENGTH,
  PRODUIT_NOT_FOUND
} from '../../../shared/constants/pagination.constants';
import { FloatLabel } from 'primeng/floatlabel';
import { TranslatePipe } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';
import { IProduit } from '../../../shared/model/produit.model';
import { Button } from 'primeng/button';
import { InputGroup } from 'primeng/inputgroup';
import { InputGroupAddon } from 'primeng/inputgroupaddon';
import { InputText } from 'primeng/inputtext';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { ProduitService } from '../../produit/produit.service';
import { KeyFilter } from 'primeng/keyfilter';
import { ProductToDestroyService } from '../product-to-destroy.service';
import { ProductToDestroyPayload } from '../model/product-to-destroy';

@Component({
  selector: 'jhi-ajout-perimes',
  imports: [
    Panel,
    AutoComplete,
    DecimalPipe,
    FloatLabel,
    TranslatePipe,
    FormsModule,
    Button,
    InputGroup,
    InputGroupAddon,
    InputText,
    NgxSpinnerModule,
    KeyFilter,
  ],
  templateUrl: './ajout-perimes.component.html',
  styleUrl: './ajout-perimes.component.scss',
})
export class AjoutPerimesComponent {
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly APPEND_TO = APPEND_TO;
  protected quantiteSaisie: number = 0;
  protected produits: IProduit[] = [];
  protected produitSelected?: IProduit | null = null;
  protected searchTerm?: string;
  protected quantityBox = viewChild.required<ElementRef>('quantityBox');
  protected numLot = viewChild.required<ElementRef>('numLot');
  protected produitbox = viewChild.required<any>('produitbox');
  private spinner = inject(NgxSpinnerService);
  private readonly produitService = inject(ProduitService);
  protected quantite = computed(() => {
    return this.quantityBox()?.nativeElement.value as number ;
  });
  private readonly productToDestroyService = inject(ProductToDestroyService);

  protected searchFn(event: any): void {
    this.searchTerm = event.query;
    this.loadProduits();
  }

  protected previousState(): void {
    window.history.back();
  }

  protected onQuantityBoxAction(event: any): void {
    const qytMvt = Number(event.target.value);
    if (qytMvt <= 0) {
      return;
    }
    this.addQuantity(qytMvt);
  }

  protected onQuantity(): void {
    const qytMvt = Number(this.quantityBox().nativeElement.value);
    if (qytMvt <= 0) {
      return;
    }
    this.addQuantity(qytMvt);
  }

  protected onSelect(): void {
    setTimeout(() => {
      const el = this.quantityBox().nativeElement;
      el.focus();
      el.select();
    }, 50);
  }

  private loadProduits(): void {
    this.produitService
      .queryLite({
        page: 0,
        size: 5,
        withdetail: true,
        search: this.searchTerm,
      })
      .subscribe(res => this.onSuccess());
  }

  private onSuccess(): void {}

  private addQuantity(qytMvt: number): void {
    if (this.produitSelected) {
    }
  }

  private buildItem(): ProductToDestroyPayload {
    return {
      produitId: this.produitSelected?.id,
      quantity: this.quantite(),
      numLot: this.numLot()?.nativeElement.value,

    };
  }
}
