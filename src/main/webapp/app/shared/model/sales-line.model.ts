import { Moment } from 'moment';
import { ISales } from 'app/shared/model/sales.model';
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
  sales?: ISales;
  produit?: IProduit;
  produitLibelle?: string;
  produitId?: number;
  saleId?: number;
  quantityStock?: number;
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
    public sales?: ISales,
    public produit?: IProduit,
    public produitLibelle?: string,
    public produitId?: number,
    public saleId?: number,
    public quantityStock?: number
  ) {}
}
