export class Suggestion {
  id: number;
  suggessionReference: string;
  createdAt: Date;
  updatedAt: Date;
  typeSuggession: string;
  fournisseurId: number;
  fournisseurLibelle: string;
  statut: string;
  statutLibelle: string;
  suggestionAggregator: SuggestionAggregator;
}

export class SuggestionAggregator {
  itemsCount: number;
  montantAchat: number;
  montantVente: number;
}
