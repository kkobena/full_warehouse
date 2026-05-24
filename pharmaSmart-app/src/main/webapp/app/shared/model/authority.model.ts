export interface IAuthority {
  name?: string;
  libelle?: string;
  privilleges?: string[];
}

export class Privilege implements IAuthority {
  constructor(
    public name?: string,
    public libelle?: string,
    public privilleges?: string[],
  ) {}
}
