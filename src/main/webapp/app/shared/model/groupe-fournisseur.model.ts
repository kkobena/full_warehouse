export interface IGroupeFournisseur {
  id?: number;
  libelle?: string;
  addresspostale?: string;
  numFaxe?: string;
  email?: string;
  tel?: string;
  odre?: number;
  codeRecepteurPharmaMl?: string;
  codeOfficePharmaMl?: string;
  urlPharmaMl?: string;
}

export class GroupeFournisseur implements IGroupeFournisseur {
  constructor(
    public id?: number,
    public libelle?: string,
    public addresspostale?: string,
    public numFaxe?: string,
    public email?: string,
    public tel?: string,
    public odre?: number,
  ) {}
}
