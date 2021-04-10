import { IProduit } from 'app/shared/model/produit.model';

export interface ICategorie {
  id?: number;
  libelle?: string;
  produits?: IProduit[];
}

export class Categorie implements ICategorie {
  constructor(public id?: number, public libelle?: string, public produits?: IProduit[]) {}
}
