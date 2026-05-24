export interface ITva {
  id?: number;
  taux?: number;
  tva?: string;
}

export class Tva implements ITva {
  constructor(
    public id?: number,
    public taux?: number,
    public tva?: string,
  ) {}
}
