export interface IClientTiersPayant {
  id?: number;
  tiersPayantName?: string;
  tiersPayantFullName?: string;
  num?: string;
  tiersPayantId?: number;
  plafondConso?: number;
  plafondJournalier?: number;
  plafondAbsolu?: boolean;
  priorite?: number;
  taux?: number;
  statut?: string;
  tiersPayant?: any;
  categorie?: number;
  numBon?: string;
}

export class ClientTiersPayant implements IClientTiersPayant {
  constructor(
    public id?: number,
    public tiersPayantName?: string,
    public tiersPayantFullName?: string,
    public num?: string,
    public tiersPayantId?: number,
    public plafondConso?: number,
    public plafondJournalier?: number,
    public plafondAbsolu?: boolean,
    public priorite?: number,
    public taux?: number,
    public statut?: string,
    public tiersPayant?: any,
    public categorie?: number,
    public numBon?: string
  ) {}
}
