import { AbstractCommande } from './abstract-commande.model';

export interface IDelivery extends AbstractCommande {}

export class Delivery implements IDelivery {
  constructor(
    public id?: number,
    public numberTransaction?: string,
    public receiptDate?: string,
    public discountAmount?: number,
    public receiptAmount?: number,
    public fournisseurId?: number,
    public fournisseurLibelle?: string,
    public netAmount?: number,
    public taxAmount?: number,
    public itemSize?: number,
    public statut?: string,
  ) {}
}
