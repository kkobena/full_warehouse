export interface IMargeDTO {
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
}

export interface IMargeSummary {
  totalProduits?: number;
  caTotalGlobal?: number;
  coutAchatGlobal?: number;
  margeBruteGlobale?: number;
  tauxMargeMoyen?: number;
  nbProduitsMargeInsuffisante?: number;
  caProduitsFaibleMarge?: number;
  nbProduitsMargeConfortable?: number;
  caProduitsBonneMarge?: number;
}

