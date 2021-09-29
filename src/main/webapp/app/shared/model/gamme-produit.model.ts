export interface IGammeProduit {
  id?: number;
  code?: string;
  libelle?: string;
}

export class GammeProduit implements IGammeProduit {
  constructor(public id?: number, public code?: string, public libelle?: string) {}
}
