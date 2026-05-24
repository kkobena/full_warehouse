import { IUser } from '../../core/user/user.model';

export class DossierFacture {
  id?: number;
  assuredCustomer?: IUser;
  createdAt: Date;
  numBon: string;
  montantVente?: number;
  montantBon?: number;
}
