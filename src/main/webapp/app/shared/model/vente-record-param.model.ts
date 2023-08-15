import { StatGroupBy, TypeVente } from './enumerations/type-vente.model';
import { TypeCa } from './enumerations/type-ca.model';
import { CaPeriodeFilter } from './enumerations/ca-periode-filter.model';

export class VenteRecordParam {
  fromDate?: string;
  toDate?: string;
  typeVente?: TypeVente;
  canceled?: boolean;
  differeOnly?: boolean;
  categorieChiffreAffaire?: TypeCa;
  dashboardPeriode?: CaPeriodeFilter;
  venteStatGroupBy?: StatGroupBy;
}
