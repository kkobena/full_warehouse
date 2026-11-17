import { IPaymentFournisseur } from 'app/shared/model/payment-fournisseur.model';
import { OrderStatut } from 'app/shared/model/enumerations/order-statut.model';
import { IMagasin } from 'app/shared/model/magasin.model';
import { IUser } from 'app/core/user/user.model';
import { IFournisseur } from 'app/shared/model/fournisseur.model';
import { AbstractCommande } from './abstract-commande.model';
import { AbstractOrderItem } from './abstract-order-item.model';

export interface ICommande extends AbstractCommande {
  orderReference?: string;
  /**
   * Renseigné seulement quand une contrainte de conditionnement a modifié la quantité saisie.
   * Absent le reste du temps : le backend ne sérialise pas les nuls.
   */
  ajustementColisage?: IAjustementColisage;
}

/** Ce qu'une contrainte fournisseur a changé à une quantité, et laquelle a décidé. */
export interface IAjustementColisage {
  produitLibelle?: string;
  quantiteDemandee: number;
  quantiteRetenue: number;
  qteColis: number;
  qteMinimaleCommande: number;
  contrainte: ContrainteColisage;
}

/**
 * Laquelle des deux contraintes du fournisseur a décidé de la quantité. Le backend la nomme, la
 * formulation destinée au pharmacien se compose ici.
 */
export type ContrainteColisage = 'COLISAGE' | 'QUANTITE_MINIMALE';

export class Commande implements ICommande {
  constructor(
    public id?: number,
    public fournisseurId?: number,
    public receiptDate?: string,
    public discountAmount?: number,
    public orderAmount?: number,
    public grossAmount?: number,
    public netAmount?: number,
    public taxAmount?: number,
    public createdAt?: string,
    public updatedAt?: string,
    public orderStatus?: OrderStatut,
    public paymentFournisseurs?: IPaymentFournisseur[],
    public orderLines?: AbstractOrderItem[],
    public magasin?: IMagasin,
    public user?: IUser,
    public lastUserEdit?: IUser,
    public fournisseur?: IFournisseur,
    public itemSize?: number,
    public receiptAmount?: number,
  ) {}
}
