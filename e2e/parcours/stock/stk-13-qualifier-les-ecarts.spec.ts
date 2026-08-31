import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un écart d'inventaire sans cause est un chiffre orphelin : on sait qu'il manque douze
 * boîtes, on ne sait pas pourquoi, et l'année suivante on n'en saura pas plus. La
 * qualification transforme la démarque en information exploitable — vol, casse, erreur de
 * saisie, périmé non déclaré — et c'est elle qui alimente le résumé par cause (STK-14).
 *
 * L'analyse porte sur l'inventaire CLÔTURÉ : tant qu'il est ouvert, l'écart n'est pas figé et
 * peut encore venir d'un comptage inachevé. Chaque ligne rappelle le théorique, le compté et
 * l'écart, à côté de la cause à choisir et d'un commentaire libre.
 *
 * Parcours en LECTURE : il ouvre l'analyse et montre ce qu'elle demande, sans l'enregistrer.
 */
scenario('STK-13', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/inventaire');
    // L'onglet des inventaires clôturés : ce sont les seuls dont les écarts sont définitifs.
    await page.getByRole('tab', { name: /Clôturés/ }).click();
    await expect(lignes.first()).toBeVisible();
    await lignes.first().getByRole('button', { name: 'Analyser les écarts' }).click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    // Chaque ligne en écart : le théorique, le compté, l'écart — et la cause à qualifier.
    await expect(modale).toContainText('Théorique');
    await expect(modale).toContainText('Compté');
    await expect(modale).toContainText('Cause');
    await expect(modale).toContainText('Commentaire');
  });

  await etape(3, async () => {
    // « Passer » existe pour ne pas bloquer la clôture sur une analyse qu'on fera plus tard ;
    // « Enregistrer » fige les causes retenues.
    await expect(modale.getByRole('button', { name: 'Enregistrer' })).toBeVisible();
  });
});
