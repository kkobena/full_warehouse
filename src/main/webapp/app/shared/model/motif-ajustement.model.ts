export interface IMotifAjustement {
  id?: number;
  libelle?: string;
}

export class MotifAjustement implements IMotifAjustement {
  constructor(public id?: number, public libelle?: string) {}
}
