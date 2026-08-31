import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un même lot peut être posé à deux endroits : quelques boîtes en rayon, le reste en réserve.
 * Le retirer « du stock » ne veut alors rien dire — il faut dire d'OÙ, faute de quoi le
 * retrait s'imputerait au hasard et l'un des deux emplacements deviendrait faux.
 *
 * L'écran distingue donc les deux cas. Lot présent à un seul endroit : une pastille le nomme,
 * et le retrait y est ciblé d'office. Lot présent à plusieurs : un sélecteur apparaît, marqué
 * « Requis », et tant qu'aucun emplacement n'est choisi ni le retrait ni le retour ne partent.
 *
 * Parcours en LECTURE : il montre le choix d'emplacement sans retirer quoi que ce soit.
 */
scenario('STK-34', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await expect(lignes.first()).toBeVisible();
    // Un lot présent dans PLUSIEURS emplacements : il porte un sélecteur, pas une pastille.
    const multi = lignes.filter({ hasText: 'Choisir emplacement' }).first();
    await expect(multi).toBeVisible();
    // Tant que rien n'est choisi, l'écran le réclame.
    await expect(multi).toContainText('Requis');
  });

  await etape(2, async () => {
    const multi = lignes.filter({ hasText: 'Choisir emplacement' }).first();
    await multi.locator('ng-select').first().click();
    // Chaque emplacement annonce ce qu'il détient : c'est ce qui permet de choisir.
    await expect(page.locator('.ng-option').first()).toBeVisible();
    await page.locator('.ng-option').first().click();
    await expect(multi).not.toContainText('Requis');
  });
});
