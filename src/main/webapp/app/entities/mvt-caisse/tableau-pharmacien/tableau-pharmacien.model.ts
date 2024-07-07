export class TableauPharmacien {
  montantAvoirFournisseur: number;
  montantComptant: number;
  montantTtc: number;
  montantCredit: number;
  montantRemise: number;
  montantNet: number;
  montantAchat: number;
  montantAchatNet: number;
  montantTaxe: number;
  nombreVente: number;
  montantHt: number;
  amountToBePaid: number;
  amountToBeTakenIntoAccount: number;
  montantNetUg: number;
  montantTtcUg: number;
  montantHtUg: number;
  partAssure: number;
  groupAchats: Map<number, Achat[]>;
  mvtDate: string;
  ratioAchatVente: number;
  ratioVenteAchat: number;
}

export class Achat {
  montantNet: number;
  montantTtc: number;
  montantHt: number;
  montantTaxe: number;
  groupeGrossisteId: number;
  groupeGrossiste: string;
  montantRemise: number;
  ordreAffichage: number;
  mvtDate: string;
}

export class TableauPharmacienWrapper {
  tableauPharmaciens: TableauPharmacien[];
  ratioAchatVente: number;
  ratioVenteAchat: number;
  montantAchat: number;
  montantVenteTtc: number;
  montantVenteHt: number;
  montantVenteNet: number;
  montantVenteRemise: number;
  montantVenteTaxe: number;
  numberCount: number;
  montantVenteCredit: number;
  montantVenteComptant: number;
  montantAchatNet: number;
  montantAchatTaxe: number;
  montantAchatRemise: number;
  montantAchatTtc: number;
  montantAchatHt: number;
  montantAvoirFournisseur: number;
}
