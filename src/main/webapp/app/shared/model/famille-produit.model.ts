export interface IFamilleProduit {
  id?: number;
  code?: string;
  libelle?: string;
  categorieLibelle?: string;
  categorieId?: number;
}

export class FamilleProduit implements IFamilleProduit {
  constructor(
    public id?: number,
    public code?: string,
    public libelle?: string,
    public categorieLibelle?: string,
    public categorieId?: number
  ) {}
}
