export interface IProduitIndicateurs {
  produitId: number;
  classeCriticite?: string;
  estMedicamentEssentiel: boolean;
  estProduitGarde: boolean;
  cmm?: number;
  rotationAnnuelleQte?: number;
  couvertureStockJours?: number;
  ca30Jours?: number;
  ca12Mois?: number;
  qteVendue12Mois?: number;
  tauxMarge?: number;
  rang?: number;
  caCumulePct?: number;
  frequenceMois?: number;
}
