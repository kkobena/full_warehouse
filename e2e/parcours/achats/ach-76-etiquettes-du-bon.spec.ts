import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une livraison rangée est une livraison étiquetée. Plutôt que de rechercher chaque article
 * et d'imprimer son étiquette une à une, l'impression part DU BON : elle reprend les produits
 * réceptionnés et leurs quantités, dans l'ordre du bon.
 *
 * Un détail qui n'en est pas un : la POSITION DE DÉPART sur la planche. Les planches sont
 * rarement neuves — on en garde une entamée dans le tiroir — et pouvoir commencer à la case 12
 * évite d'en gâcher une par livraison. La position se saisit entre 1 et 64.
 *
 * Parcours en LECTURE : il ouvre la demande d'impression sans la lancer, l'édition du document
 * n'ayant rien à montrer de plus qu'un fichier qui se télécharge.
 */
scenario('ACH-76', async ({ etape, page }) => {
  const liste = page.locator('app-list-bons');
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    // Un bon CLÔTURÉ : les étiquettes s'impriment sur ce qui est entré en stock, pas sur
    // ce qui reste à compter.
    const clos = liste.locator('tbody tr').filter({ hasText: 'Clôturé' }).first();
    await clos.getByRole('button', { name: 'Actions' }).click();
    await expect(page.getByRole('button', { name: 'Étiquettes' })).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Étiquettes' }).click();
    await expect(modale).toContainText(/ETIQUETTES DU BON DE LIVRAISON/i);
    await expect(modale).toContainText(/Position de départ/i);
  });

  await etape(3, async () => {
    // Reprendre une planche entamée : la première étiquette ira dans la case 12.
    await modale.locator('#etiquette-start').fill('12');
    await expect(modale.locator('#etiquette-start')).toHaveValue('12');
    // Hors de 1–64, la planche n'a pas de case correspondante et l'impression est refusée.
    await modale.locator('#etiquette-start').fill('70');
    await expect(modale).toContainText(/comprise entre 1 et 64/i);
    await modale.locator('#etiquette-start').fill('12');
  });
});
