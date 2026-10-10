import { convertToParamMap } from '@angular/router';

import { lireReglage, preparerRequete, REGLAGE_PAR_DEFAUT, versParametresUrl } from './reglage-analyse';

describe("Réglage de l'onglet Analyser dans l'URL", () => {
  it('sans paramètre : CA TTC par famille, top 20, en barres', () => {
    expect(lireReglage(convertToParamMap({}))).toEqual(REGLAGE_PAR_DEFAUT);
  });

  it("aller-retour, chemin de descente compris, même avec un libellé contenant « : »", () => {
    const reglage = {
      indicateurs: ['CA_TTC', 'MARGE_BRUTE'],
      axe: 'PRODUIT' as const,
      axe2: 'NATURE_VENTE' as const,
      top: 0,
      tri: 'ECART_BAISSE' as const,
      affichage: 'TABLEAU' as const,
      chemin: [{ axe: 'FAMILLE' as const, cle: '12', libelle: 'Antalgiques : adultes' }],
    };

    expect(lireReglage(convertToParamMap(versParametresUrl(reglage)))).toEqual(reglage);
  });

  it('un chemin vide efface le paramètre de l’URL', () => {
    expect(versParametresUrl(REGLAGE_PAR_DEFAUT).f).toBeNull();
  });

  it('la courbe suit les éléments dans le temps, huit séries au plus', () => {
    const courbe = preparerRequete({ ...REGLAGE_PAR_DEFAUT, affichage: 'COURBE', top: 50, axe2: 'NATURE_VENTE' });

    expect(courbe.axe2).toBe('PERIODE');
    expect(courbe.top).toBe(8);
  });

  it('les autres affichages partent tels quels', () => {
    const tableau = { ...REGLAGE_PAR_DEFAUT, affichage: 'TABLEAU' as const };
    expect(preparerRequete(tableau)).toBe(tableau);
  });
});
