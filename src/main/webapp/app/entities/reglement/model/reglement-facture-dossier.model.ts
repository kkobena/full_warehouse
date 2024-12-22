export class ReglementFactureDossier {
  id: number;
  montantPaye?: number;
  parentId?: number;
  montantTotal?: number;
  numFacture?: string;
  organismeName?: string;
  itemsCount?: number;
  debutPeriode?: Date;
  finPeriode?: Date;
  montantDetailRegle?: number;
  facturationDate?: Date;
  saleDate?: Date;
  matricule?: string;
  customerFullName?: string;
  bonNumber?: string;
  groupe?: boolean;
  montantVerse?: number;
}

export class DossierFactureProjection {
  montantPaye?: number;
  montantTotal?: number;
  numFacture?: string;
  categorie?: string;
  name?: string;
  itemCount?: number;
  montantDetailRegle?: number;
  facturationDate?: Date;
  id?: number;
  montantVerse?: number;
}

export class LigneSelectionnes {
  id: number;
  montantVerse: number;
  montantAttendu: number;
}
