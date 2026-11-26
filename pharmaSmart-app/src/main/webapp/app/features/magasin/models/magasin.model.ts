import { IMagasin, TypeMagasin } from 'app/shared/model/magasin.model';

/**
 * Seule la racine — l'officine — certifie : elle porte l'identité FNE, et tout ce que facturent
 * ses dépôts et unités de gestion part sous son nom. Le backend refuse un point de vente FNE
 * ailleurs ; le formulaire n'en propose donc pas la saisie.
 */
export function porteIdentiteFne(magasin: IMagasin): boolean {
  return (magasin.typeMagasin ?? TypeMagasin.OFFICINE) === TypeMagasin.OFFICINE;
}

/**
 * Une unité de gestion partage le lieu de son officine (une parapharmacie, par exemple) : son
 * téléphone et son adresse en sont repris, à corriger au besoin. Le rattachement est explicite,
 * pour ne pas dépendre de la règle « une seule officine » appliquée côté serveur.
 */
export function nouvelleUniteDeGestion(officine: IMagasin): IMagasin {
  return {
    typeMagasin: TypeMagasin.UNITE_GESTION,
    parentId: officine.id,
    phone: officine.phone,
    address: officine.address,
  };
}

/** Champs saisis dans le formulaire ; le reste du magasin (type, rattachement…) est conservé tel quel. */
export interface MagasinFormValue {
  fullName: string;
  name: string;
  phone: string;
  email: string | null;
  address: string | null;
  registre: string | null;
  compteContribuable: string | null;
  numComptable: string | null;
  registreImposition: string | null;
  fnePointOfSale: string | null;
  fneSecretKey: string | null;
  welcomeMessage: string | null;
  note: string | null;
}
