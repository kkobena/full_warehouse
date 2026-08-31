import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * FEFO — *first expired, first out* — n'est pas un tri d'affichage, c'est la règle de sortie
 * de l'officine : on délivre d'abord ce qui périme le plus tôt. L'écran l'applique à la
 * lecture, du plus proche au plus lointain, et signale à part le lot qui passe sous les trois
 * mois : celui-là ne se range pas, il se traite.
 *
 * Le modèle annonçait un onglet « Lots / péremption » : il s'appelle « Stock ». Corrigé dans
 * cahier-recette.model.ts en écrivant REF-11.
 *
 * Parcours en LECTURE.
 */
scenario('REF-43', async ({ etape, page }) => {
  const produit = 'PARACETAMOL';
  const onglet = page.locator('app-produit-stock-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Stock');
    await expect(onglet).toContainText('Premier expirant, premier sorti');
  });

  await etape(2, async () => {
    // L'ordre se VÉRIFIE, il ne se suppose pas : les dates lues à l'écran doivent être
    // croissantes. Une liste FEFO dans le désordre ferait sortir le mauvais lot.
    // Les dates s'affichent au format du pays (jj/mm/aaaa) : on les retourne en aaaa-mm-jj
    // pour les comparer, un tri sur la chaîne affichée classerait par jour du mois.
    const dates = (await onglet.locator('.fefo-row:not(.fefo-head)').allInnerTexts())
      .map(l => l.match(/(\d{2})\/(\d{2})\/(\d{4})/))
      .filter((m): m is RegExpMatchArray => Boolean(m))
      .map(m => `${m[3]}-${m[2]}-${m[1]}`);
    expect(dates.length).toBeGreaterThan(1);
    expect([...dates].sort()).toEqual(dates);
  });
});
