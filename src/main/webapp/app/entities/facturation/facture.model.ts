export class Facture {
  numFacture?: string;
  tiersPayantName?: string;
  factureId?: number;
  groupeFactureId?: number;
  groupeNumFacture?: string;
  montantRegle?: number;
  montant?: number;
  remiseForfetaire?: number;
  montantVente?: number;
  montantRemiseVente?: number;
  montantNetVente?: number;
  montantNet?: number;
  created?: Date;
  itemsCount?: number;
  montantAttendu?: number;
  itemMontantRegle?: number;
  montantRestant?: number;
  debutPeriode?: Date;
  finPeriode?: Date;
  factureProvisoire?: boolean;
  periode?: string;
  statut?: string;
}
