export interface ICategorie {
  id?: number;
  libelle?: string;
  code?: string;
}

export class Categorie implements ICategorie {
  constructor(public id?: number, public libelle?: string, public code?: string) {}
}
