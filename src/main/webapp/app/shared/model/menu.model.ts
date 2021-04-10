export interface IMenu {
  id?: number;
  libelle?: string;
  name?: string;
}

export class Menu implements IMenu {
  constructor(public id?: number, public libelle?: string, public name?: string) {}
}
