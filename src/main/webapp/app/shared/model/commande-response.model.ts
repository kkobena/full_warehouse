import { IOrderItem } from './order-item.model';

export interface ICommandeResponse {
  items?: IOrderItem[];
  totalItemCount?: number;
  succesCount?: number;
  failureCount?: number;
  reference?: string;
}

export class CommandeResponse implements ICommandeResponse {
  constructor(
    public totalItemCount?: number,
    public succesCount?: number,
    public failureCount?: number,
    public reference?: string,
    public items?: IOrderItem[]
  ) {}
}
