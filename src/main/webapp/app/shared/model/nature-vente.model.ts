export interface INatureVente {
  name?: string;
  code?: string;
}

export class NatureVente implements INatureVente {
  constructor(public code?: string, public name?: string) {}
}
