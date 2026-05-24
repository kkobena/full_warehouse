import { IProduit } from './produit.model';

export type TypeSubstitut = 'GENERIQUE' | 'THERAPEUTIQUE';

export interface ISubstitut {
  produit: IProduit;
  typeSubstitut: TypeSubstitut;
  typeSubstitutLibelle: string;
}
