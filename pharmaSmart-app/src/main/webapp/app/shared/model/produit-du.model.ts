/** Un produit promis à des clients qu'aucune commande partie chez le grossiste ne couvre. */
export interface IProduitDuNonCommande {
  produitId: number;
  produitLibelle: string;
  codeEan?: string;
  /** Clients identifiés distincts. Les avoirs au porteur n'y figurent pas. */
  nbClients: number;
  /** Avoirs ouverts. Dépasse nbClients dès qu'un avoir est au porteur ou qu'un client a demandé deux fois. */
  nbDemandes: number;
  quantiteDue: number;
  attenteDepuis: string;
  /** Peut être négatif : une vente forcée sort la quantité demandée même sans stock. */
  stockDisponible: number;
}

/** Clients en attente d'un produit, pour l'afficher à côté d'une ligne de commande ou de suggestion. */
export interface IClientsEnAttente {
  produitId: number;
  nbClients: number;
  nbDemandes: number;
  quantiteDue: number;
}
