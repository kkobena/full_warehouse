import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le tiers payant est un crédit consenti : la marchandise est partie, l'argent viendra. Savoir
 * combien est parti, combien est revenu et combien est en retard n'est donc pas un confort de
 * gestion — c'est la trésorerie de l'officine.
 *
 * Le bandeau répond en quatre chiffres, sur le périmètre filtré : le TOTAL FACTURÉ, ce qui est
 * effectivement RÉGLÉ, le RESTANT DÛ, et surtout ce qui est EN RETARD — les factures dont
 * l'échéance est passée. C'est ce dernier qui déclenche les relances.
 *
 * Les quatre suivent les filtres de la liste : restreindre à un tiers payant donne son
 * encours à lui, ce qu'on regarde avant de l'appeler.
 *
 * Parcours en LECTURE.
 */
scenario('FAC-45', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /^Factures/);
    await expect(contenu).toContainText('Total facturé');
  });

  await etape(2, async () => {
    // Les quatre chiffres du recouvrement, sur le périmètre filtré.
    await expect(contenu).toContainText('Montant réglé');
    await expect(contenu).toContainText('Restant dû');
    await expect(contenu).toContainText('En retard');
  });
});
