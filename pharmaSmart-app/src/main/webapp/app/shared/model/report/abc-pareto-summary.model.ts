export interface IABCParetoSummary {
  totalProduits?: number;
  caGlobal?: number;
  // Classe A_PLUS (≤ 60% du CA cumulé)
  nbProduitsAPlus?: number;
  caClasseAPlus?: number;
  pctCaClasseAPlus?: number;
  // Classe A (60-80%)
  nbProduitsA?: number;
  caClasseA?: number;
  pctCaClasseA?: number;
  // Classe B (80-95%)
  nbProduitsB?: number;
  caClasseB?: number;
  pctCaClasseB?: number;
  // Classe C (95-99%)
  nbProduitsC?: number;
  caClasseC?: number;
  pctCaClasseC?: number;
  // Classe D (> 99% ou sans ventes)
  nbProduitsD?: number;
  caClasseD?: number;
  pctCaClasseD?: number;
}

export class ABCParetoSummary implements IABCParetoSummary {
  constructor(
    public totalProduits?: number,
    public caGlobal?: number,
    public nbProduitsAPlus?: number,
    public caClasseAPlus?: number,
    public pctCaClasseAPlus?: number,
    public nbProduitsA?: number,
    public caClasseA?: number,
    public pctCaClasseA?: number,
    public nbProduitsB?: number,
    public caClasseB?: number,
    public pctCaClasseB?: number,
    public nbProduitsC?: number,
    public caClasseC?: number,
    public pctCaClasseC?: number,
    public nbProduitsD?: number,
    public caClasseD?: number,
    public pctCaClasseD?: number,
  ) {}
}
