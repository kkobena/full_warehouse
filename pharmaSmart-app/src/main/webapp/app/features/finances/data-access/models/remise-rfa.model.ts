/**
 * Où l'on en est du palier de remise de fin d'année. Miroir de l'énumération backend
 * `EtatPalierRfa` : le serveur nomme l'état, la phrase se compose ici.
 */
export type EtatPalierRfa =
  | 'SANS_ACCORD'
  | 'LOIN_DU_PALIER'
  | 'EN_COURS'
  | 'PROCHE_DU_PALIER'
  | 'ATTEINT';

/**
 * Avancement d'un **groupe** de fournisseurs vers son palier : `fournisseurId` et
 * `fournisseurName` désignent le grossiste principal, pas l'agence qui a reçu les commandes.
 */
export interface IRemiseRfaFournisseur {
  fournisseurId: number;
  fournisseurName: string;
  palierRfa?: number;
  caCommandeN: number;
  pourcentageAtteint: number;
  rfaEstimee: number;
  rfaRecue: number;
  /** Ce qu'il reste à facturer pour franchir le palier ; 0 une fois franchi. */
  resteAvantPalier: number;
  etat: EtatPalierRfa;
}

export interface IAvoirFournisseur {
  id: number;
  fournisseurName: string;
  numAvoir: string;
  dateAvoir: string;
  montant: number;
  statut: string;
}
