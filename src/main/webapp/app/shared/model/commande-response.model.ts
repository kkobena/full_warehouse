import { IOrderItem } from './order-item.model';

export interface ICommandeResponse {
  items?: IOrderItem[];
  totalItemCount?: number;
  succesCount?: number;
  failureCount?: number;
  reference?: string;
  entity?: any;
}

export class CommandeResponse implements ICommandeResponse {
  constructor(
    public totalItemCount?: number,
    public succesCount?: number,
    public failureCount?: number,
    public reference?: string,
    public entity?: any,
    public items?: IOrderItem[],
  ) {}
}
