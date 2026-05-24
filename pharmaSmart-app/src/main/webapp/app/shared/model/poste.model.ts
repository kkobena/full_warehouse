export interface IPoste {
  id?: number;
  name?: string;
  posteNumber?: string;
  address?: string;
}

export class Poste implements IPoste {
  constructor(
    public id?: number,
    public name?: string,
    public posteNumber?: string,
    public address?: string,
  ) {}
}
