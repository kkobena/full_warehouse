import { AbstractOrderItem } from './abstract-order-item.model';

export interface IDeliveryItem extends AbstractOrderItem {}

export class DeliveryItem implements IDeliveryItem {
  constructor(public id?: number) {}
}
