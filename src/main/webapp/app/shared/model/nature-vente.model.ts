export interface INatureVente {
  name?: string;
  code?: string;
  disabled?: boolean;
}

export class NatureVente implements INatureVente {
  constructor(
    public code?: string,
    public name?: string,
    disabled?: boolean,
  ) {}
}
