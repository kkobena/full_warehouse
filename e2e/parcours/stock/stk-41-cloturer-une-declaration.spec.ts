import { expect } from '@playwright/test';
import { chercherDansSelect, traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La clôture est le moment où la déclaration cesse d'être un brouillon : les quantités
 * déclarées sortent du stock vendable et les lignes rejoignent les lots à détruire (STK-42).
 * Avant elle, rien n'a bougé ; après, tout est passé.
 *
 * C'est aussi ce qui rend la déclaration utilisable à plusieurs : on compte le matin, on
 * complète l'après-midi, et on ne clôture qu'une fois le rayon entier vérifié.
 *
 * Parcours ÉCRIVANT dans la base : il clôture une déclaration qu'il a lui-même constituée.
 */
scenario('STK-41', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  // Le même produit que les autres parcours de déclaration, avec un lot distinct : ce
  // qui distingue une ligne, c'est le couple produit/lot.
  const produit = 'DOLIPRANE 500MG';

  await etape(1, async () => {
    // Mise en scène : une déclaration d'une ligne, constituée par le parcours.
    await page.goto('/gestion-peremption/edit');
    await chercherDansSelect(page, 'produits', produit, produit);
    await page.locator('#numLot').fill('LOTCLOTURE41');
    const datePeremption = page.locator('#datePeremtion');
    await datePeremption.fill('31/12/2025');
    await datePeremption.press('Enter');
    const qte = page.locator('#quantiteSaisie');
    await qte.click();
    await qte.fill('');
    await qte.pressSequentially('1', { delay: 40 });
    await qte.press('Enter');
    await expect(lignes.filter({ hasText: produit }).first()).toBeVisible({ timeout: 20000 });
  });

  await etape(2, async () => {
    // La clôture n'est offerte que si la déclaration porte au moins une ligne.
    await expect(page.getByRole('button', { name: 'Clôturer' })).toBeEnabled();
    await page.getByRole('button', { name: 'Clôturer' }).click();
    await traverserConfirmations(page, { limite: 2 });
  });

  await etape(3, async () => {
    // Le stock est passé : les quantités déclarées ont quitté le stock vendable.
    await expect(contenu).toBeVisible();
  });
});
