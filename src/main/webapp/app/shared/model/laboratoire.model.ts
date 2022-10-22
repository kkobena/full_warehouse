export interface ILaboratoire {
  id?: number;
  libelle?: string;
}

export class Laboratoire implements ILaboratoire {
  constructor(public id?: number, public libelle?: string) {}
}
