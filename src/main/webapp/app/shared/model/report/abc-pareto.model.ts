import { ClassePareto } from './classe-pareto.enum';

export interface IABCPareto {
  produitId?: number;
  libelle?: string;
  codeCip?: string;
  categorie?: string;
  caTotal?: number;
  qteVendue?: number;
  nbVentes?: number;
  caGlobal?: number;
  caCumule?: number;
  contributionPct?: number;
  caCumulePct?: number;
  classePareto?: ClassePareto;
  rang?: number;
}

export class ABCPareto implements IABCPareto {
  constructor(
    public produitId?: number,
    public libelle?: string,
    public codeCip?: string,
    public categorie?: string,
    public caTotal?: number,
    public qteVendue?: number,
    public nbVentes?: number,
    public caGlobal?: number,
    public caCumule?: number,
    public contributionPct?: number,
    public caCumulePct?: number,
    public classePareto?: ClassePareto,
    public rang?: number
  ) {}
}
