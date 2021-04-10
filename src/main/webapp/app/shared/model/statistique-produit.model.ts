export interface IStatistiqueProduit {
  libelleProduit?: string;
  amount?: number;
  quantity?: number;
}

export class StatistiqueProduit implements IStatistiqueProduit {
  constructor(public libelleProduit?: string, public amount?: number, public quantity?: number) {}
}
