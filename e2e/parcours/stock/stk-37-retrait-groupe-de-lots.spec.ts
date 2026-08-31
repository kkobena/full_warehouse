import { expect } from '@playwright/test';
import { traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le tri des périmés se fait par brassée, pas lot par lot : on vide un rayon, on retrouve
 * quinze lots dépassés, et les retirer un à un serait quinze confirmations pour la même
 * décision.
 *
 * Le retrait groupé les traite ensemble, et le récapitulatif qui précède dit ce qu'on engage :
 * combien de lots, combien d'unités, et surtout QUELLE VALEUR d'achat part à la poubelle. Ce
 * chiffre-là n'est pas décoratif — c'est la perte sèche de l'officine, et le voir avant de
 * confirmer est ce qui fait réfléchir à un retour fournisseur (STK-38) plutôt qu'à une
 * destruction.
 *
 * L'opération est annoncée irréversible, parce qu'elle l'est.
 *
 * Parcours ÉCRIVANT dans la base : il retire plusieurs lots périmés.
 */
scenario('STK-37', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await expect(lignes.first()).toBeVisible();
    // Deux lots mono-emplacement : le retrait groupé n'a pas à choisir pour eux.
    const candidats = lignes.filter({ has: page.locator('.pharma-badge-info .pi-map-marker') });
    await candidats.nth(0).locator('input[type="checkbox"]').first().check();
    await candidats.nth(1).locator('input[type="checkbox"]').first().check();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Tout retirer' }).click();
    const question = page.locator('.modal-content:visible').first();
    // Le récapitulatif : lots, unités, et la valeur d'achat qui part.
    await expect(question).toContainText(/lot\(s\) du stock/);
    await expect(question).toContainText(/Quantité totale/);
    await expect(question).toContainText(/Valeur achat estimée/);
  });

  await etape(3, async () => {
    await expect(page.locator('.modal-content:visible').first()).toContainText(/irréversible/i);
    await traverserConfirmations(page, { limite: 1 });
  });

  await etape(4, async () => {
    // Les lots retirés quittent la liste des périmés : ils sont désormais des lots à
    // détruire (STK-42).
    await expect(page.locator('.modal-content:visible')).toHaveCount(0);
  });
});
