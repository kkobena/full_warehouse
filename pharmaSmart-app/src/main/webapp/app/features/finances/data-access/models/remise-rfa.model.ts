export interface IRemiseRfaFournisseur {
  fournisseurId: number;
  fournisseurName: string;
  palierRfa?: number;
  caCommandeN: number;
  pourcentageAtteint: number;
  rfaEstimee: number;
  rfaRecue: number;
  alerte?: string;
}

export interface IAvoirFournisseur {
  id: number;
  fournisseurName: string;
  numAvoir: string;
  dateAvoir: string;
  montant: number;
  statut: string;
}
