import { Banque } from '../../reglement/model/reglement.model';

export class NewReglementDiffere {
  customerId: number;
  expectedAmount: number;
  amount: number;
  saleIds: number[];
  paimentMode: string;
  banqueInfo: Banque;
  paymentDate: string;
}
export class ReglementDiffereResponse {
  idReglement: number;
}
