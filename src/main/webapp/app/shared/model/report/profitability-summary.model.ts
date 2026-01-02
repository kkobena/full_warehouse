export interface IProfitabilitySummary {
  totalProduits?: number;
  caTotalGlobal?: number;
  coutAchatGlobal?: number;
  margeBruteGlobale?: number;
  tauxMargeMoyen?: number;
  nbStars?: number;
  nbCashCows?: number;
  nbQuestionMarks?: number;
  nbDogs?: number;
  caStars?: number;
  caCashCows?: number;
  caQuestionMarks?: number;
  caDogs?: number;
  margeStars?: number;
  margeCashCows?: number;
  margeQuestionMarks?: number;
  margeDogs?: number;
}

export class ProfitabilitySummary implements IProfitabilitySummary {
  constructor(
    public totalProduits?: number,
    public caTotalGlobal?: number,
    public coutAchatGlobal?: number,
    public margeBruteGlobale?: number,
    public tauxMargeMoyen?: number,
    public nbStars?: number,
    public nbCashCows?: number,
    public nbQuestionMarks?: number,
    public nbDogs?: number,
    public caStars?: number,
    public caCashCows?: number,
    public caQuestionMarks?: number,
    public caDogs?: number,
    public margeStars?: number,
    public margeCashCows?: number,
    public margeQuestionMarks?: number,
    public margeDogs?: number,
  ) {}
}
