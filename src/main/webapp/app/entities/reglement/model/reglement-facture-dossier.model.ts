import { FactureId } from '../../facturation/facture.model';

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
  invoiceDate?: string;
  parentInvoiceDate?: string;
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
  invoiceDate?: string;
  factureItemId: FactureId;
}

export class LigneSelectionnes {
  id: FactureId;
  montantVerse: number;
  montantAttendu: number;
}
