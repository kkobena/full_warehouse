export interface IFournisseur {
  id?: number;
  libelle?: string;
  numFaxe?: string;
  addressePostal?: string;
  phone?: string;
  mobile?: string;
  site?: string;
  code?: string;
  email?: string;
  odre?: number;
  parentId?: number;
  parentLibelle?: string;
  identifiantRepartiteur?: string;
  delaiLivraisonJours?: number;
  frequenceCommandeJours?: number;
  joursCredit?: number;
  joursCritique?: number;
  palierRfa?: number;
  tauxRfa?: number;
  urlPharmaMl?: string;
  codeOfficePharmaMl?: string;
  codeRecepteurPharmaMl?: string;
  idRecepteurPharmaMl?: string;
}

export class Fournisseur implements IFournisseur {
  constructor(
    public id?: number,
    public libelle?: string,
    public numFaxe?: string,
    public addressePostal?: string,
    public phone?: string,
    public mobile?: string,
    public site?: string,
    public code?: string,
    public email?: string,
    public odre?: number,
    public parentId?: number,
    public parentLibelle?: string,
  ) {}
}
