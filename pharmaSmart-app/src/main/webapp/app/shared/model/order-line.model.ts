import { AbstractOrderItem } from './abstract-order-item.model';

export interface IOrderLine extends AbstractOrderItem {}

export class OrderLine implements IOrderLine {
  constructor(public id?: number) {}
}

export class OrderLineLot {
  quantity?: number;
  freeQuantity?: number;
  lotNumber?: string;
  expirationDate?: Date;
  manufacturingDate?: Date;
}
