import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Tout ce qui se vend en officine n'entre pas dans la même assiette déclarée. La
 * parapharmacie, la diététique, certains dispositifs relèvent d'un autre régime — et le
 * déclarer avec le reste fausse la déclaration dans un sens comme dans l'autre.
 *
 * L'exclusion porte sur le RAYON, jamais sur des produits pris un à un. C'est ce qui la rend
 * tenable : un article qui entre au catalogue hérite du sort de son rayon, sans qu'on ait à y
 * penser. Et un produit déplacé change de sort du même coup.
 *
 * La règle ne réécrit PAS le passé — l'écran le dit lui-même — et c'est la seule lecture
 * défendable : retraiter après coup des ventes déjà déclarées reviendrait à modifier une
 * déclaration transmise.
 *
 * Parcours en LECTURE : il montre les rayons exclus sans changer la règle, dont dépendent les
 * journaux et les états de tout le module.
 */
scenario('DCA-01', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/declaration-ca');
    await ouvrirOnglet(page, 'Exclusion de rayons');
    await expect(contenu).toContainText('Exclusion de rayons');
  });

  await etape(2, async () => {
    // Le compteur dit d'emblée l'ampleur de la règle, et les trois filtres permettent de
    // vérifier ce qui reste dans l'assiette autant que ce qui en sort.
    await expect(contenu).toContainText(/\d+ exclus?/);
    await expect(contenu).toContainText('Exclus');
    await expect(contenu).toContainText('Non exclus');
  });

  await etape(3, async () => {
    // La portée de la règle, écrite à l'écran : elle vaut pour l'avenir, pas pour ce qui est
    // déjà déclaré.
    await expect(contenu).toContainText(/n'agit que sur les ventes à venir/);
  });
});
