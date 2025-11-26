export interface IABCParetoSummary {
  totalProduits?: number;
  caGlobal?: number;
  nbProduitsA?: number;
  caClasseA?: number;
  pctCaClasseA?: number;
  nbProduitsB?: number;
  caClasseB?: number;
  pctCaClasseB?: number;
  nbProduitsC?: number;
  caClasseC?: number;
  pctCaClasseC?: number;
}

export class ABCParetoSummary implements IABCParetoSummary {
  constructor(
    public totalProduits?: number,
    public caGlobal?: number,
    public nbProduitsA?: number,
    public caClasseA?: number,
    public pctCaClasseA?: number,
    public nbProduitsB?: number,
    public caClasseB?: number,
    public pctCaClasseB?: number,
    public nbProduitsC?: number,
    public caClasseC?: number,
    public pctCaClasseC?: number
  ) {}
}
