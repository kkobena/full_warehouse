export interface IGroupeTiersPayant {
  id?: number;
  name?: string;
  adresse?: string;
  telephone?: string;
  telephoneFixe?: string;
  ordreTrisFacture?: string;
}

export class GroupeTiersPayant implements IGroupeTiersPayant {
  constructor(
    public id?: number,
    public name?: string,
    public adresse?: string,
    public telephone?: string,
    public telephoneFixe?: string,
  ) {}
}
