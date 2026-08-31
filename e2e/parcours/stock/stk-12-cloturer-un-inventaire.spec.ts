import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * La clôture est le moment où le comptage devient la vérité : les écarts constatés se
 * transforment en mouvements de stock, et l'inventaire devient un document qu'on ne modifie
 * plus. Elle est irréversible — d'où le récapitulatif qui la précède.
 *
 * Ce récapitulatif dit tout ce qu'il faut pour décider : combien de lignes ont été comptées,
 * combien RESTENT à compter, combien présentent un écart, et surtout ce que ces écarts pèsent
 * — valorisés au prix d'achat et au prix de vente. Clôturer avec des lignes non comptées
 * revient à décréter qu'elles valent zéro : le chiffre est là pour qu'on ne le fasse pas sans
 * le savoir.
 *
 * Parcours en LECTURE : il ouvre le récapitulatif de clôture sans clôturer — l'opération est
 * sans retour, et le jeu de démonstration a besoin de ses inventaires ouverts.
 */
scenario('STK-12', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/inventaire');
    await expect(lignes.first()).toContainText(/en cours|créé/i);
    await lignes.first().getByRole('button', { name: 'Clôturer' }).click();
    await expect(modale).toContainText(/Clôturer l'inventaire/);
  });

  await etape(2, async () => {
    // Le récapitulatif : ce qui a été compté, ce qui ne l'a pas été, et le poids des écarts.
    await expect(modale).toContainText('Lignes comptées');
    await expect(modale).toContainText('Lignes restantes');
    await expect(modale).toContainText('Lignes avec écart');
    await expect(modale).toContainText(/Écart valorisé \(achat\)/);
    // Le bouton existe, mais le parcours s'arrête là : la clôture ne se rejoue pas.
    await expect(modale.getByRole('button', { name: 'Clôturer' })).toBeVisible();
  });
});
