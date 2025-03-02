import { Moment } from 'moment';

export interface IRayon {
  id?: number;
  createdAt?: Moment;
  updatedAt?: Moment;
  code?: string;
  libelle?: string;
  storageLibelle?: string;
  storageId?: number;
  exclude?: boolean;
}

export class Rayon implements IRayon {
  constructor(
    public id?: number,
    public createdAt?: Moment,
    public updatedAt?: Moment,
    public code?: string,
    public libelle?: string,
    public storageLibelle?: string,
    public storageId?: number,
    public exclude?: boolean,
  ) {
    this.exclude = this.exclude || false;
  }
}
