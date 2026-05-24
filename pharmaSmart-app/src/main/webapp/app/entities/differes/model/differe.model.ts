import { DiffereItem } from './differe-item.model';

export class Differe {
  customerId: number;
  firstName: string;
  lastName: string;
  saleAmount: number;
  paidAmount: number;
  rest: number;
  differeItems: DiffereItem[];
}
