export interface IConfiguration {
  name?: string;
  value?: string;
  otherValue?: string;
  description?: string;
}

export class Configuration implements IConfiguration {
  constructor(
    public name?: string,
    public value?: string,
    public description?: string,
  ) {}
}

export class Pair {
  constructor(
    public key?: any,
    public value?: any,
  ) {}
}
