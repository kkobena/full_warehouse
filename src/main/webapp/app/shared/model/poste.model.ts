export interface IPoste {
  id?: number;
  name?: string;
  posteNumber?: string;
  address?: string;
  customerDisplay?: boolean;
  customerDisplayPort?: string;
}

export class Poste implements IPoste {
  constructor(
    public id?: number,
    public name?: string,
    public posteNumber?: string,
    public address?: string,
    public customerDisplay?: boolean,
    public customerDisplayPort?: string,
  ) {
    this.customerDisplay = this.customerDisplay || false;
  }
}
