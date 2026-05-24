export type Periodicite = 'MENSUEL' | 'QUINZAINE' | 'BIMENSUEL';

export interface IGroupeTiersPayant {
  id?: number;
  name?: string;
  adresse?: string;
  telephone?: string;
  telephoneFixe?: string;
  email?: string;
  ordreTrisFacture?: string;
  delaiReglement?: number;
  periodiciteFactureDefinitive?: Periodicite | null;
  periodiciteFactureProvisoire?: Periodicite | null;
  inclureFacturationAutoDefinitive?: boolean;
  inclureFacturationAutoProvisoire?: boolean;
}

export class GroupeTiersPayant implements IGroupeTiersPayant {
  constructor(
    public id?: number,
    public name?: string,
    public adresse?: string,
    public telephone?: string,
    public telephoneFixe?: string,
    public email?: string,
    public delaiReglement?: number,
    public periodiciteFactureDefinitive?: Periodicite | null,
    public periodiciteFactureProvisoire?: Periodicite | null,
    public inclureFacturationAutoDefinitive?: boolean,
    public inclureFacturationAutoProvisoire?: boolean,
  ) {}
}
