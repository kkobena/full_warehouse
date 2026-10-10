import { IOrderItem } from './order-item.model';

export interface ICommandeResponse {
  items?: IOrderItem[];
  totalItemCount?: number;
  succesCount?: number;
  failureCount?: number;
  reference?: string;
  entity?: any;
  /** CSV des lignes non prises en compte (base64) ; absent s'il n'y en a pas. */
  ruptureCsv?: string;
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
