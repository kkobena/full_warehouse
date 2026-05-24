export interface IAjustement {
  id?: number;
  qtyMvt?: number;
  dateMtv?: string;
  produitId?: number;
  ajustId?: number;
  produitLibelle?: string;
  codeCip?: string;
  userFullName?: string;
  stockBefore?: number;
  stockAfter?: number;
  commentaire?: string;
  motifAjustementId?: number;
  motifAjustementLibelle?: string;
  /** Emplacement de stockage cible (PRINCIPAL ou SAFETY_STOCK). Null = emplacement par défaut de l'utilisateur. */
  storageId?: number;
  /** Lot explicitement sélectionné pour AJUSTEMENT_IN (gestion_lot=true). Null = heuristique "dernier reçu". */
  lotId?: number;
}

export class Ajustement implements IAjustement {
  constructor(
    public id?: number,
    public qtyMvt?: number,
    public dateMtv?: string,
    public produitId?: number,
    public produitLibelle?: string,
    public commentaire?: string,
    public ajustId?: number,
    public userFullName?: string,
    public stockBefore?: number,
    public stockAfter?: number,
    public codeCip?: string,
    public motifAjustementId?: number,
    public motifAjustementLibelle?: string,
  ) {}
}
