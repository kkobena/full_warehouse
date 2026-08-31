import { expect } from '@playwright/test';
import { chercherDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Ouvrir un module entier à un rôle, c'est une centaine de cases à cocher — et autant
 * d'occasions d'en oublier une. Les menus qui ont des enfants portent donc deux actions
 * groupées : tout accorder, tout révoquer, sur la branche complète.
 *
 * Chaque ligne reste enregistrée pour elle-même : l'action groupée n'est pas un réglage à
 * part, c'est une suite de coches. L'ajustement fin se fait ensuite, ligne par ligne.
 *
 * Parcours ÉCRIVANT dans la base : il révoque puis réaccorde la même branche, laissant les
 * droits du rôle tels qu'il les a trouvés.
 */
scenario('ADM-19', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  // « Péremptions » : un menu à plusieurs niveaux, dont le caissier n'a pas l'usage.
  const branche = 'Péremptions';

  await etape(1, async () => {
    await page.goto('/admin/access-management');
    await page.getByRole('tab', { name: /Autorisations/ }).click();
    await chercherDansSelect(page, 'navRole', 'Caiss', 'Caissier');
    const ligne = page.locator('tbody tr').filter({ hasText: branche }).first();
    await expect(ligne).toBeVisible();
    // Les deux actions groupées ne s'affichent que sur les menus qui ont une descendance :
    // sur une feuille, elles n'auraient rien à parcourir.
    await expect(ligne.getByRole('button', { name: 'Tout accorder sur cette branche' })).toBeVisible();
    await expect(ligne.getByRole('button', { name: 'Tout révoquer sur cette branche' })).toBeVisible();
  });

  await etape(2, async () => {
    const ligne = page.locator('tbody tr').filter({ hasText: branche }).first();
    await ligne.getByRole('button', { name: 'Tout accorder sur cette branche' }).click();
    // Le menu lui-même est désormais coché — et ses enfants avec lui, chacun enregistré.
    await expect(ligne.locator('app-checkbox input').first()).toBeChecked();
    await expect(contenu).toContainText(branche);
  });

  // ── Remise en état : la branche est révoquée, comme elle l'était. ───────────────────────
  const ligne = page.locator('tbody tr').filter({ hasText: branche }).first();
  await ligne.getByRole('button', { name: 'Tout révoquer sur cette branche' }).click();
  await expect(ligne.locator('app-checkbox input').first()).not.toBeChecked();
});
