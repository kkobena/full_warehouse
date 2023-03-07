export interface ITableau {
  id?: number;
  value?: number;

  code?: string;


}

export class Tableau implements ITableau {
  constructor(
    public id?: number,
    public value?: number,
    public code?: string
  ) {

  }
}
