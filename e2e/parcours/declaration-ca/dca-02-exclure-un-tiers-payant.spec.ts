import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Certains organismes déclarent eux-mêmes le chiffre d'affaires qu'ils couvrent, ou relèvent
 * d'un régime distinct. Le compter une seconde fois côté officine le déclarerait deux fois.
 *
 * L'exclusion d'un tiers payant sort la vente ENTIÈRE — part patient comprise — et c'est ce
 * qui la distingue nettement des deux autres règles : le rayon écarte des lignes, les unités
 * gratuites n'écartent qu'une portion de ligne, l'organisme écarte la vente.
 *
 * Parcours en LECTURE : il montre la règle sans la changer.
 */
scenario('DCA-02', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/declaration-ca');
    await ouvrirOnglet(page, 'Exclusion de tiers-payants');
    await expect(contenu).toContainText('Exclusion de tiers-payants');
  });

  await etape(2, async () => {
    await expect(contenu).toContainText(/\d+ exclus?/);
    // La portée exacte, écrite à l'écran : la vente entière, et non la seule part organisme.
    await expect(contenu).toContainText(/sortent entièrement du chiffre d'affaires à déclarer/);
  });

  await etape(3, async () => {
    // Et la portée dans le temps : la règle vaut pour l'avenir, jamais pour une déclaration
    // déjà transmise.
    await expect(contenu).toContainText(/n'agit que sur les ventes à venir/);
  });
});
