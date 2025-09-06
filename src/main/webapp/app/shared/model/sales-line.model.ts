import { Moment } from 'moment';
import { ISales, SaleId } from 'app/shared/model/sales.model';
import { IProduit } from 'app/shared/model/produit.model';

export interface ISalesLine {
  id?: number;
  quantitySold?: number;
  regularUnitPrice?: number;
  discountUnitPrice?: number;
  netUnitPrice?: number;
  discountAmount?: number;
  salesAmount?: number;
  grossAmount?: number;
  netAmount?: number;
  taxAmount?: number;
  costAmount?: number;
  createdAt?: Moment;
  updatedAt?: Moment;
  sales?: ISales | null;
  produit?: IProduit;
  produitLibelle?: string;
  produitId?: number;
  saleId?: number;
  quantityStock?: number;
  quantityRequested?: number;
  calculationBasePrice?: number;
  code?: string;
  forceStock?: boolean;
  saleLineId?: SaleLineId;
  saleCompositeId?: SaleId;
}

export class SalesLine implements ISalesLine {
  constructor(
    public id?: number,
    public quantitySold?: number,
    public regularUnitPrice?: number,
    public discountUnitPrice?: number,
    public netUnitPrice?: number,
    public discountAmount?: number,
    public salesAmount?: number,
    public grossAmount?: number,
    public netAmount?: number,
    public taxAmount?: number,
    public costAmount?: number,
    public createdAt?: Moment,
    public updatedAt?: Moment,
    public sales?: ISales | null,
    public produit?: IProduit,
    public produitLibelle?: string,
    public produitId?: number,
    public saleId?: number,
    public quantityStock?: number,
    public quantityRequested?: number,
    public code?: string,
    public forceStock?: boolean
  ) {
    this.forceStock = this.forceStock || false;
  }
}

export class SaleLineId {
  id: number;
  saleDate: string;
}

