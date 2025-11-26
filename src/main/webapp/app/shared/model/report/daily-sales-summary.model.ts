export interface IDailySalesSummary {
  saleDate?: string;
  typeVente?: string;
  nbVentes?: number;
  caTotal?: number;
  caNet?: number;
  panierMoyen?: number;
  totalRemises?: number;
}

export class DailySalesSummary implements IDailySalesSummary {
  constructor(
    public saleDate?: string,
    public typeVente?: string,
    public nbVentes?: number,
    public caTotal?: number,
    public caNet?: number,
    public panierMoyen?: number,
    public totalRemises?: number,
  ) {}
}
