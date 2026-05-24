import { Injectable } from '@angular/core';
import { ProduitAuditingParam } from '../../../shared/model/produit-record.model';

/**
 * Service léger de partage de paramètres entre StatSalesComponent / StatDeliveryComponent
 * et leurs composants enfants (daily / yearly).
 * Fourni au niveau du composant parent (providers: [ProduitAuditingParamService]).
 */
@Injectable()
export class ProduitAuditingParamService {
  produitAuditingParam: ProduitAuditingParam = {};

  setParameter(params: ProduitAuditingParam): void {
    this.produitAuditingParam = params;
  }
}

