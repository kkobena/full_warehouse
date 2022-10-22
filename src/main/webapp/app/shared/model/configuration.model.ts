export interface IConfiguration {
  name?: string;
  value?: string;
  description?: string;
}

export class Configuration implements IConfiguration {
  constructor(public name?: string, public value?: string, public description?: string) {}
}
