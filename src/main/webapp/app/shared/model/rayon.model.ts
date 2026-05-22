export interface IRayon {
  id?: number;
  createdAt?: string;
  updatedAt?: string;
  code?: string;
  libelle?: string;
  storageLibelle?: string;
  storageType?: string;
  storageId?: number;
  exclude?: boolean;
}

export class Rayon implements IRayon {
  constructor(
    public id?: number,
    public createdAt?: string,
    public updatedAt?: string,
    public code?: string,
    public libelle?: string,
    public storageLibelle?: string,
    public storageId?: number,
    public exclude?: boolean,
  ) {
    this.exclude = this.exclude || false;
  }
}
