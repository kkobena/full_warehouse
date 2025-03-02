export interface IMotifRetourProduit {
  id?: number;
  libelle?: string;
}

export class MotifRetourProduit implements IMotifRetourProduit {
  constructor(
    public id?: number,
    public libelle?: string,
  ) {}
}
