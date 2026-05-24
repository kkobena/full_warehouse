export interface IThirdPartySaleLine {
  id?: number;
  montant?: number;
  clientTiersPayantId?: number;
  customerId?: number;
  tiersPayantId?: number;
  taux?: number;
  statut?: string;
  customerFullName?: string;
  invoiceStatut?: string;
  numBon?: string;
  num?: string;
  tiersPayantFullName?: string;
  name?: string;
}

export class ThirdPartySaleLine implements IThirdPartySaleLine {
  constructor(
    public id?: number,
    public montant?: number,
    public clientTiersPayantId?: number,
    public customerId?: number,
    public tiersPayantId?: number,
    public taux?: number,
    public statut?: string,
    public customerFullName?: string,
    public invoiceStatut?: string,
    public numBon?: string,
    public tiersPayantFullName?: string,
  ) {}
}
