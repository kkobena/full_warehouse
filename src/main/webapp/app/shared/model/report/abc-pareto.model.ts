import { ClassePareto } from './classe-pareto.enum';

export interface IABCPareto {
  produitId?: number;
  libelle?: string;
  codeCip?: string;
  famille?: string;
  classeActuelle?: string;
  caTotal?: number;
  qteVendue?: number;
  nbVentes?: number;
  frequenceMois?: number;
  caGlobal?: number;
  caCumule?: number;
  contributionPct?: number;
  caCumulePct?: number;
  rang?: number;
  classePareto?: ClassePareto;
}

export class ABCPareto implements IABCPareto {
  constructor(
    public produitId?: number,
    public libelle?: string,
    public codeCip?: string,
    public famille?: string,
    public classeActuelle?: string,
    public caTotal?: number,
    public qteVendue?: number,
    public nbVentes?: number,
    public frequenceMois?: number,
    public caGlobal?: number,
    public caCumule?: number,
    public contributionPct?: number,
    public caCumulePct?: number,
    public rang?: number,
    public classePareto?: ClassePareto,
  ) {}
}
