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
  // Un nom commercial, non une molécule : la recherche porte aussi sur la DCI, donc
  // « PARACETAMOL » remonte d'abord DOLIPRANE et EFFERALGAN, qui l'ont pour DCI. Voir REF-11.
  const produit = 'AUGMENTIN';
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

    // L'ordre ne dit rien des quantités. Les lots ne peuvent pas porter plus que le stock du
    // produit : s'ils le dépassaient, FEFO allouerait des unités qui n'existent pas. L'égalité
    // n'est pas exigible — tout le stock n'est pas tracé par lot — mais le dépassement, si.
    const quantites = (await onglet.locator('.fefo-row:not(.fefo-head) .fefo-qty').allInnerTexts())
      .map(q => Number(q.replace(/[^\d-]/g, '')));
    const sommeDesLots = quantites.reduce((a, b) => a + b, 0);
    // Le total n'a plus de zone propre dans l'onglet : on le lit dans l'en-tête du produit
    // (« Stock 19 u »), qui reste affiché au-dessus des onglets.
    const entete = await page.locator('#main-content').innerText();
    const stockTotal = Number(/Stock\s+([\d\s]+)\s*u\b/.exec(entete)?.[1]?.replace(/\s/g, '') ?? NaN);

    expect(sommeDesLots, 'des lots sans quantité ne seraient pas des lots').toBeGreaterThan(0);
    expect(sommeDesLots, 'les lots ne peuvent excéder le stock du produit')
      .toBeLessThanOrEqual(stockTotal);
  });
});
