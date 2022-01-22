export interface IRayonProduit {
  id?: number;
  rayonId?: number;
  codeRayon?: string;
  libelleRayon?: string;
  libelleStorage?: string;
  storageType?: string;
  magasin?: string;
  magasinId?: number;
}

export class RayonProduit implements IRayonProduit {
  constructor(
    public id?: number,
    rayonId?: number,
    codeRayon?: string,
    libelleRayon?: string,
    libelleStorage?: string,
    storageType?: string,
    magasin?: string,
    magasinId?: number
  ) {}
}
