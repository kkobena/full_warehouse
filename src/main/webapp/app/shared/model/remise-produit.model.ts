export interface IRemiseProduit {
  id?: number;
  valeur?: string;
  remiseValue?: number;
}

export class RemiseProduit implements IRemiseProduit {
  constructor(public id?: number, public valeur?: string, public remiseValue?: number) {}
}
