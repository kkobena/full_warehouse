export interface IMagasin {
  id?: number;
  name?: string;
  phone?: string;
  address?: string;
  note?: string;
  registre?: string;
  welcomeMessage?: string;
}

export class Magasin implements IMagasin {
  constructor(
    public id?: number,
    public name?: string,
    public phone?: string,
    public address?: string,
    public note?: string,
    public registre?: string,
    public welcomeMessage?: string
  ) {}
}
