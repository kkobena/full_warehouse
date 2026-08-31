import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une unité gratuite accordée par le grossiste et passée au client n'a rien rapporté. La
 * déclarer reviendrait à payer l'impôt sur un chiffre d'affaires qui n'existe pas.
 *
 * Le réglage est un simple interrupteur, et c'est cohérent : contrairement aux rayons et aux
 * organismes, il n'y a rien à choisir — soit l'officine déduit ses unités gratuites, soit elle
 * ne les déduit pas.
 *
 * Le retrait est PARTIEL, et c'est le point que le manuel doit faire comprendre : seule la
 * valeur des unités gratuites sort de l'assiette, le reste de la ligne y demeure.
 *
 * Parcours en LECTURE : basculer l'interrupteur changerait l'assiette déclarée de toutes les
 * ventes à venir.
 */
scenario('DCA-03', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/declaration-ca');
    await ouvrirOnglet(page, 'Unités gratuites');
    await expect(contenu).toContainText("Exclure les unités gratuites du chiffre d'affaires à déclarer");
    // Un interrupteur, pas une liste : il n'y a rien à sélectionner.
    await expect(page.locator('#exclude-free-unit')).toBeVisible();
  });

  etape.horsPortee(
    2,
    'basculer l’interrupteur changerait l’assiette déclarée de toutes les ventes à venir, ' +
      'et avec elle les montants de tous les états du module.',
  );
});
