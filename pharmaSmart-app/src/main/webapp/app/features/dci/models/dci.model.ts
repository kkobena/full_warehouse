/**
 * Dénomination Commune Internationale : la substance active d'un médicament.
 *
 * Le libellé est obligatoire et unique ; le code l'est aussi mais peut être omis
 * à la saisie — le serveur le dérive alors du libellé.
 */
export interface IDci {
  id?: number;
  code?: string;
  libelle?: string;
}

export class Dci implements IDci {
  constructor(
    public id?: number,
    public code?: string,
    public libelle?: string,
  ) {}
}

/** Modèle du formulaire de saisie, distinct du DTO : le code y est toujours une chaîne. */
export interface DciFormModel {
  id: number | null;
  code: string;
  libelle: string;
}

export const EMPTY_DCI_FORM: DciFormModel = { id: null, code: '', libelle: '' };

/**
 * Produit rattaché à une DCI, tel qu'affiché dans le panneau de détail.
 *
 * Projection étroite côté serveur : l'écran ne répond qu'à « quels produits portent cette
 * molécule ? ». Le code CIP peut être absent — il vit sur `fournisseur_produit`, et un produit
 * sans fournisseur principal désigné n'en a donc pas.
 */
export interface IDciProduit {
  id: number;
  libelle: string;
  codeCip: string | null;
  famille: string | null;
  prixVente: number | null;
  statut: string | null;
}
