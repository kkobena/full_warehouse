export interface IModeReglment {
  id?: number;
  libelle?: string;
  code?: string;
}

export class ModeReglment implements IModeReglment {
  constructor(public id?: number, public libelle?: string, public code?: string) {}
}
