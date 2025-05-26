import { Moment } from 'moment';
import { GroupeTiersPayant } from './groupe-tierspayant.model';
import { ICustomer } from './customer.model';

export interface ITiersPayant {
  id?: number;
  name?: string;
  fullName?: string;
  nbreBons?: number;
  montantMaxParFcture?: number;
  codeOrganisme?: string;
  codeRegroupement?: string;
  modelFacture?: string;
  ordreTrisFacture?: string;
  consoMensuelle?: number;
  plafondAbsolu?: boolean;
  adresse?: string;
  telephone?: string;
  telephoneFixe?: string;
  email?: string;
  toBeExclude?: boolean;
  plafondConso?: number;
  statut?: string;
  categorie?: string;
  remiseForfaitaire?: number;
  nbreBordereaux?: number;
  created?: Moment;
  updated?: Moment;
  groupeTiersPayant?: GroupeTiersPayant;
  groupeTiersPayantName?: string;
  groupeTiersPayantId?: number;
  encours?: number;
  customers?: ICustomer[];
  plafondJournalierClient?: number;
  plafondConsoClient?: number;
  plafondAbsoluClient?: boolean;
}

export class TiersPayant implements ITiersPayant {
  constructor(
    public id?: number,
    public name?: string,
    public fullName?: string,
    public nbreBons?: number,
    public montantMaxParFcture?: number,
    public codeOrganisme?: string,
    public codeRegroupement?: string,
    public consoMensuelle?: number,
    public plafondAbsolu?: boolean,
    public adresse?: string,
    public telephone?: string,
    public telephoneFixe?: string,
    public email?: string,
    public toBeExclude?: boolean,
    public plafondConso?: number,
    public statut?: string,
    public categorie?: string,
    public remiseForfaitaire?: number,
    public nbreBordereaux?: number,
    public created?: Moment,
    public updated?: Moment,
    public groupeTiersPayant?: GroupeTiersPayant,
    public groupeTiersPayantName?: string,
    public groupeTiersPayantId?: number,
    public encours?: number,
    public customers?: ICustomer[],
  ) {}
}

export class ModelFacture {
  key?: string;
  value?: string;
}

export class OrdreTrisFacture {
  key?: string;
  value?: string;
}
