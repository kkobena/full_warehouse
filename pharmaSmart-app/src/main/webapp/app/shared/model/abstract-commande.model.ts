import { OrderStatut } from './enumerations/order-statut.model';
import { IPaymentFournisseur } from './payment-fournisseur.model';
import { IMagasin } from './magasin.model';
import { IUser } from '../../core/user/user.model';
import { IFournisseur } from './fournisseur.model';
import { AbstractOrderItem } from './abstract-order-item.model';

/**
 * Par quel moyen la commande est partie. Miroir de l'énumération backend `CanalEnvoi`.
 */
export type CanalEnvoi = 'EDI' | 'MAIL' | 'PDF' | 'TELEPHONE' | 'AUTRE';

export interface AbstractCommande {
  id?: number;
  commandeId?: CommandeId;
  orderDate?: string;
  fournisseurId?: number;
  receiptDate?: string;
  discountAmount?: number;
  orderAmount?: number;
  grossAmount?: number;
  netAmount?: number;
  taxAmount?: number;
  receiptAmount?: number;
  createdAt?: string;
  updatedAt?: string;
  orderStatus?: OrderStatut;
  paymentFournisseurs?: IPaymentFournisseur[];
  orderLines?: AbstractOrderItem[];
  magasin?: IMagasin;
  user?: IUser;
  fournisseur?: IFournisseur;
  numberTransaction?: string;
  receiptReference?: string;
  reliquatDeCommandeId?: number;
  fournisseurLibelle?: string;
  orderReference?: string;
  itemSize?: number;
  statut?: string;
  doTransfer?: boolean;
  conformeSansLot?: boolean;
  hasBeenSubmittedToPharmaML?: boolean;
  /**
   * Horodatage de la transmission au grossiste. Absent = brouillon, jamais parti — le backend ne
   * sérialise pas les nuls.
   */
  dateEnvoi?: string;
  canalEnvoi?: CanalEnvoi;
  /** Dérivé de `dateEnvoi` côté serveur, pour ne pas tester une date dans les gabarits. */
  envoyee?: boolean;
  /** Jours au-delà du délai annoncé par le grossiste ; 0 si dans les temps ou déjà reçue. */
  joursDeRetard?: number;
  reconciliationStatut?: 'EN_ATTENTE' | 'RECONCILIEE' | 'ECART' | 'LITIGE' | null;
}

export class CommandeId {
  id: number;
  orderDate: string;
}
