import { Tuple } from '../../../shared/model/tuple.model';

export class BalanceCaisse {
  count?: number;
  montantTtc?: number;
  montantHt?: number;
  montantDiscount?: number;
  montantPartAssure?: number;
  montantPartAssureur?: number;
  montantNet?: number;
  montantCash?: number;
  montantPaye?: number;
  montantCard?: number;
  montantMobileMoney?: number;
  montantCheck?: number;
  montantCredit?: number;
  montantDiffere?: number;
  typeSalePercent?: number;
  typeSale?: string;
  panierMoyen?: number;
  montantVirement?: number;
  modePaiement?: string;
  libelleModePaiement?: string;
  typeVente?: string;
  montantDepot?: number;
  montantAchat?: number;
  montantMarge?: number;
  amountToBePaid?: number;
  amountToBeTakenIntoAccount?: number;
  montantNetUg?: number;
  montantTtcUg?: number;
  montantHtUg?: number;
  montantTaxe?: number;
  partAssure?: number;
  partTiersPayant?: number;
  typeVeTypeAffichage?: string;
}

export class BalanceCaisseSum {
  libelleTypeMvt?: string;
  typeMvt?: string;
  montantCash?: number;
  montantCard?: number;
  montantMobileMoney?: number;
  montantCheck?: number;
  montantVirement?: number;
  total?: number;
}

export class BalanceCaisseWrapperSum {
  montantCash?: number;
  montantCard?: number;
  montantMobileMoney?: number;
  montantCheck?: number;
  montantVirement?: number;
  totalVente?: number;
}

export class BalanceCaisseWrapper {
  mvtCaissesByModes: Tuple[] = [];
  mvtCaisses: Tuple[] = [];
  balanceCaisses: BalanceCaisse[] = [];
  count?: number;
  montantTtc?: number;
  montantHt?: number;
  montantDiscount?: number;
  montantPartAssure?: number;
  montantPartAssureur?: number;
  montantNet?: number;
  montantCash?: number;
  montantCard?: number;
  montantMobileMoney?: number;
  montantCheck?: number;
  montantCredit?: number;
  montantDiffere?: number;
  panierMoyen?: number;
  montantVirement?: number;
  montantDepot?: number;
  montantTaxe?: number;
  montantAchat?: number;
  montantMarge?: number;
  amountToBePaid?: number;
  amountToBeTakenIntoAccount?: number;
  montantNetUg?: number;
  montantTtcUg?: number;
  montantHtUg?: number;
  partAssure?: number;
  partTiersPayant?: number;
  montantPaye?: number;
  periode?: string;
  balanceCaisseSums: BalanceCaisseSum[] = [];
  balanceCaisseWrapperSum?: BalanceCaisseWrapperSum;
  ratioVenteAchat?: number;
  ratioAchatVente?: number;
  typeSalePercent?: number;
}
