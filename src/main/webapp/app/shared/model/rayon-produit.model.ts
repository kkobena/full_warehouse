export interface IRayonProduit {
  id?: number;
  rayonId?: number;
  codeRayon?: string;
  libelleRayon?: string;
  libelleStorage?: string;
  storageType?: string;
  magasin?: string;
  magasinId?: number;
  produitId?: number;
}

export class RayonProduit implements IRayonProduit {
  constructor(
    public id?: number,
    public rayonId?: number,
    public codeRayon?: string,
    public libelleRayon?: string,
    public libelleStorage?: string,
    public storageType?: string,
    public magasin?: string,
    public magasinId?: number,
    public produitId?: number
  ) {}
}
