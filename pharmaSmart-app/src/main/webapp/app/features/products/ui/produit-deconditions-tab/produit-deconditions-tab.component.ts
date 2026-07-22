import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataTableComponent } from 'app/shared/ui';
import { IProduit } from 'app/shared/model/produit.model';

@Component({
  selector: 'app-produit-deconditions-tab',
  templateUrl: './produit-deconditions-tab.component.html',
  styleUrls: ['./produit-deconditions-tab.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, DataTableComponent],
})
export class ProduitDeconditionsTabComponent {
  readonly produit = input.required<IProduit>();

  protected get details(): IProduit[] {
    return this.produit().produits ?? [];
  }
}
