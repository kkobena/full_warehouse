import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { IProduit } from 'app/shared/model/produit.model';

@Component({
  selector: 'app-produit-deconditions-tab',
  templateUrl: './produit-deconditions-tab.component.html',
  styleUrls: ['./produit-deconditions-tab.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, TableModule],
})
export class ProduitDeconditionsTabComponent {
  readonly produit = input.required<IProduit>();

  protected get details(): IProduit[] {
    return this.produit().produits ?? [];
  }
}
