import { ReglementDiffereItem } from './reglement-differe-item.model';

export class ReglementDiffere {
  id: number;
  user: string;
  lastName: string;
  paidAmount: number;
  solde: number;
  items: ReglementDiffereItem[];
}
