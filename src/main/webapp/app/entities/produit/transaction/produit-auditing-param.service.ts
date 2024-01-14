import { Injectable, signal, WritableSignal } from '@angular/core';
import { ProduitAuditingParam } from '../../../shared/model/produit-record.model';

@Injectable({
  providedIn: 'root',
})
export class ProduitAuditingParamService {
  private produitAuditingParamWritableSignal: WritableSignal<ProduitAuditingParam> = signal({});

  constructor() {}

  get produitAuditingParam(): ProduitAuditingParam {
    return this.produitAuditingParamWritableSignal();
  }

  setParameter(produitAuditing: ProduitAuditingParam): void {
    this.produitAuditingParamWritableSignal.set(produitAuditing);
  }
}
