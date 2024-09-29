import { Moment } from 'moment';
import { ISales } from 'app/shared/model/sales.model';
import { IProduit } from 'app/shared/model/produit.model';
import { IPayment } from 'app/shared/model/payment.model';
import { IClientTiersPayant } from 'app/shared/model/client-tiers-payant.model';
import { ITiersPayant } from './tierspayant.model';

export interface ICustomer {
  id?: number;
  assureId?: number;
  firstName?: string;
  lastName?: string;
  phone?: string;
  email?: string;
  createdAt?: Moment;
  updatedAt?: Moment;
  produits?: IProduit[];
  sales?: ISales[];
  encours?: number;
  payments?: IPayment[];
  fullName?: string;
  type?: string;
  code?: string;
  num?: string;
  categorie?: string;
  datNaiss?: string;
  numAyantDroit?: string;
  sexe?: string;
  remiseId?: number;
  plafondConso?: number;
  plafondJournalier?: number;
  plafondAbsolu?: boolean;
  priorite?: number;
  taux?: number;
  ayantDroits?: ICustomer[];
  tiersPayants?: IClientTiersPayant[];
  tiersPayantId?: number;
  tiersPayant?: ITiersPayant;
  typeTiersPayant?: string;
}

export class Customer implements ICustomer {
  constructor(
    public id?: number,
    public assureId?: number,
    public firstName?: string,
    public lastName?: string,
    public phone?: string,
    public email?: string,
    public createdAt?: Moment,
    public updatedAt?: Moment,
    public sales?: ISales[],
    public produits?: IProduit[],
    public payments?: IPayment[],
    public encours?: number,
    public fullName?: string,
    public type?: string,
    public code?: string,
    public num?: string,
    public numAyantDroit?: string,
    public categorie?: string,
    public datNaiss?: string,
    public sexe?: string,
    public remiseId?: number,
    public plafondConso?: number,
    public plafondJournalier?: number,
    public plafondAbsolu?: boolean,
    public priorite?: number,
    public taux?: number,
    public ayantDroits?: ICustomer[],
    public tiersPayants?: IClientTiersPayant[],
    public tiersPayantId?: number,
  ) {}
}
