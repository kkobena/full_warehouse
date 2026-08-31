import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le tableau des mouvements dit ce qui s'est passé ; la COURBE dit la forme que ça a prise —
 * une décroissance régulière, une rupture, un réassort tardif. C'est la même donnée, lue
 * autrement, et c'est celle qu'on emporte en réunion : d'où les deux exports, PDF pour
 * transmettre, Excel pour retravailler.
 *
 * Parcours en LECTURE : il produit des fichiers, il ne modifie rien.
 */
scenario('REF-61', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const onglet = page.locator('app-produit-mouvements-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Mouvements');
    await onglet.getByRole('button', { name: '1 an' }).click();
    await onglet.getByRole('button', { name: 'Actualiser' }).click();
    await expect(onglet.locator('tbody tr').first()).toBeVisible();
  });

  await etape(2, async () => {
    // Le graphique n'est proposé qu'une fois des données chargées : sans mouvement, il n'y
    // aurait rien à tracer, et le bouton n'apparaît pas.
    await onglet.getByRole('button', { name: 'Graphique' }).click();
    await expect(onglet.locator('canvas')).toBeVisible();
  });

  await etape(3, async () => {
    // Excel plutôt que PDF pour montrer l'export retravaillable — le PDF est illustré par
    // REF-58 et REF-59. On attend le FICHIER : c'est la seule preuve que l'export a abouti.
    const telechargement = page.waitForEvent('download');
    await onglet.getByRole('button', { name: 'Excel' }).click();
    const fichier = await telechargement;
    expect(fichier.suggestedFilename()).toMatch(/\.(xlsx|pdf)$/);
  });
});
