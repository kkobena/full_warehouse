import { expect } from '@playwright/test';
import { chercherDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une déclaration de périmés se constitue au fil du comptage, souvent à deux : on ajoute, on
 * se trompe de quantité, on retrouve la boîte qu'on croyait perdue. Tant qu'elle n'est pas
 * clôturée, elle se corrige — c'est ce qui la distingue d'un mouvement de stock.
 *
 * Retirer une ligne n'annule rien puisque rien n'a encore été appliqué : le stock ne bougera
 * qu'à la clôture (STK-41). D'ici là, la liste n'est qu'un brouillon partagé.
 *
 * Parcours ÉCRIVANT dans la base : il ajoute une ligne puis la retire.
 */
scenario('STK-40', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  // Le même produit que les autres parcours de déclaration, avec un lot distinct : ce
  // qui distingue une ligne, c'est le couple produit/lot.
  const produit = 'DOLIPRANE 500MG';

  await etape(1, async () => {
    // Mise en scène : la ligne qu'on corrigera, ajoutée par le parcours.
    await page.goto('/gestion-peremption/edit');
    await chercherDansSelect(page, 'produits', produit, produit);
    await page.locator('#numLot').fill('LOTCORRECTION40');
    const datePeremption = page.locator('#datePeremtion');
    await datePeremption.fill('31/12/2025');
    await datePeremption.press('Enter');
    const qte = page.locator('#quantiteSaisie');
    await qte.click();
    await qte.fill('');
    await qte.pressSequentially('3', { delay: 40 });
    await qte.press('Enter');
    await expect(lignes.filter({ hasText: produit }).first()).toBeVisible({ timeout: 20000 });
  });

  await etape(2, async () => {
    // Retirer la ligne : la déclaration reprend sa forme d'avant, sans trace ni mouvement.
    const ligne = lignes.filter({ hasText: produit }).first();
    await ligne.getByRole('button').last().click();
    await expect(contenu).toContainText(/Ajout de lots périmés/);
  });
});
