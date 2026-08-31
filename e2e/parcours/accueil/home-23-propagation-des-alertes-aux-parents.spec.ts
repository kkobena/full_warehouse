import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Une alerte cachée dans une rubrique repliée n'alerte personne. La navigation regroupe les
 * écrans par thème — « Gestion Stock » contient les commandes et les péremptions — et rien ne
 * garantit que l'utilisateur ouvre la rubrique au bon moment.
 *
 * La règle est donc simple : chaque parent affiche la SOMME de ses enfants, récursivement. Le
 * total attire l'attention sur la rubrique, et le détail attend à l'intérieur.
 *
 * Somme au parent, maximum sur Commandes : les deux règles ne se contredisent pas. Le maximum
 * évite de compter deux fois un même produit dans un même compteur ; la somme additionne des
 * compteurs qui portent sur des choses différentes.
 */
scenario('HOME-23', async ({ etape, page }) => {
  const rubrique = () => page.locator('app-navbar li', { hasText: 'Gestion Stock' }).first();

  await etape(1, async () => {
    await page.goto('/');
    // Rubrique repliée : c'est l'état d'ouverture de l'application.
    await expect(page.locator('.navbar-badge').first()).toBeVisible({ timeout: 20000 });
  });

  await etape(2, async () => {
    // Le total est lisible sans avoir rien déplié.
    const badge = rubrique().locator('.navbar-badge').first();
    await expect(badge).toBeVisible();
    await expect(badge).toContainText(/[1-9]|99\+/);
  });

  await etape(3, async () => {
    await page.getByText('Gestion Stock', { exact: true }).click();
    // Le panneau s'ouvre en surcouche, hors de la barre : les enfants gardent leur propre
    // compteur, le parent résume sans le remplacer.
    await expect(page.locator('app-nav-flyout')).toBeVisible();
  });
});
