export interface ITopProduct {
  mois?: string;
  produitId?: number;
  libelle?: string;
  codeCip?: string;
  nbVentes?: number;
  qteVendue?: number;
  caGenere?: number;
  prixMoyen?: number;
}

export class TopProduct implements ITopProduct {
  constructor(
    public mois?: string,
    public produitId?: number,
    public libelle?: string,
    public codeCip?: string,
    public nbVentes?: number,
    public qteVendue?: number,
    public caGenere?: number,
    public prixMoyen?: number,
  ) {}
}
