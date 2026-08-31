import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un tableau de bord ne sert qu'à ceux qui le regardent. Le reste de la journée se passe dans
 * les écrans de travail — et c'est précisément là qu'une péremption ou une rupture doit se
 * signaler.
 *
 * D'où les pastilles portées par la navigation elle-même, visibles depuis n'importe quel écran :
 *
 *   • Commandes prend le MAXIMUM des ruptures et des urgents SEMOIS, non leur somme : un même
 *     produit en rupture et en urgence ne doit pas être compté deux fois ;
 *   • Péremptions prend son propre compteur ;
 *   • Facturation signale en orange les factures tiers payant échues — une créance en retard
 *     n'est pas une urgence de comptoir, la couleur le dit.
 *
 * Un compteur à zéro n'affiche RIEN. Une pastille grise à « 0 » n'informe pas, elle occupe.
 */
scenario('HOME-22', async ({ etape, page }) => {
  await etape(1, async () => {
    // L'alerte n'est pas fabriquée pour la démonstration : le stock de l'officine porte déjà
    // des lots proches de la péremption.
    await page.goto('/gestion-peremption');
    await expect(page.locator('#main-content')).toBeVisible();
  });

  await etape(2, async () => {
    await page.goto('/');
    // Les compteurs sont chargés une fois pour toute la navigation, par un service partagé.
    await expect(page.locator('.navbar-badge').first()).toBeVisible({ timeout: 20000 });
  });

  await etape(3, async () => {
    const navbar = page.locator('app-navbar');
    await expect(navbar.getByText('Gestion Stock')).toBeVisible();
    // Aucune pastille à zéro : toutes celles qui s'affichent portent un nombre.
    const valeurs = await page.locator('.navbar-badge').allInnerTexts();
    expect(valeurs.length).toBeGreaterThan(0);
    for (const v of valeurs) {
      expect(v.trim()).toMatch(/^([1-9]\d*|99\+)$/);
    }

    // La sidebar affiche les mêmes nombres : c'est le même service qui les fournit, et non
    // deux calculs parallèles qui finiraient par diverger.
    await page.evaluate(() => {
      localStorage.setItem('pharmasmart_layout_mode', 'sidebar');
      localStorage.setItem('pharmasmart_sidebar_collapsed', 'false');
    });
    await page.reload();
    await expect(page.locator('app-sidebar .sidebar-badge').first()).toBeVisible({ timeout: 20000 });
  });

  await etape(4, async () => {
    await page.evaluate(() => localStorage.setItem('pharmasmart_layout_mode', 'navbar'));
    await page.reload();
    await expect(page.locator('app-navbar').getByText('Facturation')).toBeVisible({ timeout: 20000 });
  });
});
