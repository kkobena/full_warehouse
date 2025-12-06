import { BCGCategory } from './bcg-category.enum';

export interface IProductProfitability {
  produitId?: number;
  libelle?: string;
  codeCip?: string;
  categorie?: string;
  nbVentes?: number;
  qteVendue?: number;
  caTotal?: number;
  coutAchatTotal?: number;
  margeBrute?: number;
  tauxMargePct?: number;
  prixVenteMoyen?: number;
  prixAchatMoyen?: number;
  stockQuantity?: number;
  prixAchatUnitaire?: number;
  prixVenteUnitaire?: number;
  tauxRotationAnnuel?: number;
  bcgCategory?: BCGCategory;
}

export class ProductProfitability implements IProductProfitability {
  constructor(
    public produitId?: number,
    public libelle?: string,
    public codeCip?: string,
    public categorie?: string,
    public nbVentes?: number,
    public qteVendue?: number,
    public caTotal?: number,
    public coutAchatTotal?: number,
    public margeBrute?: number,
    public tauxMargePct?: number,
    public prixVenteMoyen?: number,
    public prixAchatMoyen?: number,
    public stockQuantity?: number,
    public prixAchatUnitaire?: number,
    public prixVenteUnitaire?: number,
    public tauxRotationAnnuel?: number,
    public bcgCategory?: BCGCategory
  ) {}
}
