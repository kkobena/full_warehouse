export type ReconciliationStatut = 'EN_ATTENTE' | 'RECONCILIEE' | 'ECART' | 'LITIGE';

export interface IReconciliationFactureFournisseur {
  id?: number;
  factureReference?: string;
  factureDate?: string;
  factureMontantHT?: number;
  factureTVA?: number;
  blMontantHT?: number;
  blTVA?: number;
  ecartHT?: number;
  ecartTVA?: number;
  statut?: ReconciliationStatut;
  createdAt?: string;
  updatedAt?: string;
}

export interface IReconciliationCommand {
  factureReference: string;
  factureDate: string | null;
  factureMontantHT: number;
  factureTVA: number;
}
