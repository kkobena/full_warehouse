import { Injectable } from '@angular/core';
import { IProduit } from '../../../shared/model/produit.model';

@Injectable({ providedIn: 'root' })
export class SaleStockValidator {
  constructor() {}

  validate(
    produit: IProduit,
    quantityRequested: number,
    totalQuantity: number,
    canForceStock: boolean,
    quantityMax: number,
  ): { isValid: boolean; reason?: string } {
    if (quantityRequested <= 0) {
      return { isValid: false, reason: 'invalidQuantity' };
    }

    if (produit.totalQuantity < totalQuantity) {
      if (canForceStock) {
        if (quantityRequested > quantityMax) {
          return { isValid: false, reason: 'forceStockAndQuantityExceedsMax' };
        }
        if (produit.produitId) {
          return { isValid: false, reason: 'deconditionnement' };
        }
        return { isValid: false, reason: 'forceStock' };
      }
      return { isValid: false, reason: 'stockInsuffisant' };
    }

    if (quantityRequested >= quantityMax) {
      if (canForceStock) {
        return { isValid: false, reason: 'forceStockAndQuantityExceedsMax' };
      }
      return { isValid: false, reason: 'quantityExceedsMax' };
    }

    return { isValid: true };
  }
}
