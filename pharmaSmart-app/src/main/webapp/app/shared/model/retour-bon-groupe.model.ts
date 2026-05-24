import { IRetourBon } from './retour-bon.model';

export interface IRetourBonGroupe {
  fournisseurId?: number;
  fournisseurLibelle?: string;
  nbRetours?: number;
  montantTotal?: number;
  retourBons?: IRetourBon[];
}
