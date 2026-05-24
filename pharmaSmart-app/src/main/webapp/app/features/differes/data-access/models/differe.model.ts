export interface IClientDiffere {
  id: number;
  firstName?: string;
  lastName?: string;
  fullName?: string;
}

export interface IDiffereItem {
  user?: string;
  lastName?: string;
  firstName?: string;
  reference?: string;
  amount?: number;
  paidAmount?: number;
  restAmount?: number;
  mvtDate?: Date;
  saleId?: number;
  customerId?: number;
}

export interface IDiffere {
  customerId?: number;
  firstName?: string;
  lastName?: string;
  saleAmount?: number;
  paidAmount?: number;
  rest?: number;
  differeItems?: IDiffereItem[];
}

export interface IDiffereSummary {
  saleAmount?: number;
  paidAmount?: number;
  rest?: number;
}

export interface IDiffereSearchParams {
  customerId?: number;
  paymentStatuses?: string[];
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export interface IPaymentIdDiffere {
  id: number;
  transactionDate: string;
}

export interface IBanqueDiffere {
  nom?: string;
  adresse?: string;
  code?: string;
  beneficiaire?: string;
}

export interface INewReglementDiffere {
  customerId: number;
  expectedAmount: number;
  amount: number;
  saleIds: number[];
  paimentMode: string;
  banqueInfo?: IBanqueDiffere;
  paymentDate?: string;
}

export interface IReglementDiffereResponse {
  idReglement: IPaymentIdDiffere;
}

export interface IReglementDiffereItem {
  id?: number;
  user?: string;
  libelleMode?: string;
  expectedAmount?: number;
  paidAmount?: number;
  mvtDate?: Date;
  montantVerse?: number;
}

export interface IReglementDiffere {
  id?: number;
  user?: string;
  lastName?: string;
  paidAmount?: number;
  solde?: number;
  items?: IReglementDiffereItem[];
}

export interface IReglementDiffereSummary {
  paidAmount?: number;
  solde?: number;
}
