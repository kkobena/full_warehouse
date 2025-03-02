import { IResponseCommandeItem } from './response-commande-item.model';

export interface IResponseCommande {
  items?: IResponseCommandeItem[];
  extraItems?: IResponseCommandeItem[];
}

export class ResponseCommande implements IResponseCommande {
  constructor(
    public items?: IResponseCommandeItem[],
    public extraItems?: IResponseCommandeItem[],
  ) {}
}
