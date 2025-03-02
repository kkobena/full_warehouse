export interface IFournisseur {
  id?: number;
  libelle?: string;
  numFaxe?: string;
  addressePostal?: string;
  phone?: string;
  mobile?: string;
  site?: string;
  code?: string;
  groupeFournisseurId?: number;
  groupeFournisseurLibelle?: string;
}

export class Fournisseur implements IFournisseur {
  constructor(
    public id?: number,
    public libelle?: string,
    public addresspostale?: string,
    public numFaxe?: string,
    public addressePostal?: string,
    public phone?: string,
    public mobile?: string,
    public site?: string,
    public code?: string,
    public groupeFournisseurId?: number,
    public groupeFournisseurLibelle?: string,
  ) {}
}
