import { Statut } from './enumerations/statut.model';

export interface IProduitCriteria {
  search?: string;
  codeCip?: string;
  libelle?: string;
  status?: Statut;
  id?: number;
  codeEan?: string;
  dateperemption?: boolean;
  deconditionnable?: boolean;
  qtySeuilMini?: number;
  qtyAppro?: number;
  parentId?: number;
  prixPaf?: number;
  prixUni?: number;
  formeId?: number;
  familleId?: number;
  gammeId?: number;
  laboratoireId?: number;
  tvaId?: number;
  storageId?: number;
  rayonId?: number;
  deconditionne?: boolean;
  remiseId?: number;
}

export class ProduitCriteria implements IProduitCriteria {
  constructor(
    public search?: string,
    public codeCip?: string,
    public libelle?: string,
    public status?: Statut,
    public id?: number,
    public codeEan?: string,
    public dateperemption?: boolean,
    public deconditionnable?: boolean,
    public qtySeuilMini?: number,
    public qtyAppro?: number,
    public parentId?: number,
    public prixPaf?: number,
    public prixUni?: number,
    public formeId?: number,
    public familleId?: number,
    public gammeId?: number,
    public laboratoireId?: number,
    public tvaId?: number,
    public storageId?: number,
    public rayonId?: number,
    public deconditionne?: boolean,
    public remiseId?: number
  ) {}
}
