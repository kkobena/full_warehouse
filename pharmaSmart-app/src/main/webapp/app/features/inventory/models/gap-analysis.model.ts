export type CauseEcart = 'CASSE' | 'VOL' | 'ERREUR_RECEPTION' | 'ERREUR_SAISIE' | 'PEREMPTION' | 'INCONNU';

export interface IGapLine {
  lineId: number;
  produitLibelle: string;
  quantityInit: number;
  quantityOnHand: number;
  gap: number;
  valeurEcart: number;
  existingCause?: CauseEcart;
  existingComment?: string;
}

export interface IGapEntry {
  lineId: number;
  cause: CauseEcart;
  commentaire?: string;
}

export interface IGapSummary {
  cause: CauseEcart;
  causeLabel: string;
  nbProduits: number;
  quantiteTotale: number;
}

export const CAUSE_ECART_OPTIONS: { value: CauseEcart; label: string; icon: string }[] = [
  { value: 'CASSE',            label: 'Casse / dommage',     icon: 'pi pi-times-circle' },
  { value: 'VOL',              label: 'Vol',                  icon: 'pi pi-exclamation-triangle' },
  { value: 'ERREUR_RECEPTION', label: 'Erreur de réception',  icon: 'pi pi-inbox' },
  { value: 'ERREUR_SAISIE',    label: 'Erreur de saisie',     icon: 'pi pi-pencil' },
  { value: 'PEREMPTION',       label: 'Péremption',           icon: 'pi pi-calendar-times' },
  { value: 'INCONNU',          label: 'Cause inconnue',       icon: 'pi pi-question-circle' },
];
