import { ReceiptStatut } from './enumerations/receipt-statut';
import { CaPeriodeFilter } from './enumerations/ca-periode-filter.model';

export class AchatRecordParam {
  fromDate?: string;
  toDate?: string;
  receiptStatuts?: ReceiptStatut[];
  fournisseurId?: number;
  dashboardPeriode?: CaPeriodeFilter;
}
