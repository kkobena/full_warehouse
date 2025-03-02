export interface IMenu {
  id?: number;
  libelle?: string;
  name?: string;
  root?: boolean;
  parentId?: number;
  enable?: boolean;
  items?: IMenu[];
}

export interface IPrivillegesWrapper {
  associes?: IMenu[];
  others?: IMenu[];
}

export class Menu implements IMenu {
  constructor(
    public id?: number,
    public libelle?: string,
    public name?: string,
  ) {}
}

export class PrivillegesWrapper implements IPrivillegesWrapper {
  constructor(
    public associes?: IMenu[],
    public others?: IMenu[],
  ) {}
}
