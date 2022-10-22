export interface ITypeTransaction {
  value?: number;
  name?: string;
}

export class TypeTransaction implements ITypeTransaction {
  constructor(public value?: number, public name?: string) {}
}
