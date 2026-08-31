import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Composer un rayon depuis la fiche du rayon, et non depuis chaque fiche produit : c'est le
 * sens du geste réel — on a le rayon sous les yeux, on y range ce qui manque.
 *
 * Un champ de recherche unique suffit : on tape, on retient, le produit entre dans le rayon.
 * Rien à valider derrière, parce que l'affectation d'un produit à un rayon ne se négocie pas.
 *
 * L'écran signale au passage les produits présents dans PLUSIEURS stockages — un même article
 * peut vivre en rayon et en réserve — ce qui évite de croire à un doublon.
 */
scenario('RFD-12', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/rayon');
    await expect(lignes.first()).toBeVisible();
    await lignes.first().click();
    await expect(page.locator('app-rayon-produits-tab')).toBeVisible();
  });

  await etape(2, async () => {
    // La recherche interroge le catalogue entier : c'est un ajout, pas un filtre sur le
    // contenu du rayon — lequel a son propre champ, plus bas.
    // Le champ est un ng-select : c'est son `<input>` interne qui reçoit la frappe, et la
    // recherche part au serveur — d'où l'attente d'une suggestion plutôt que d'un délai.
    const recherche = page.locator('app-rayon-produits-tab ng-select input[type="text"]').first();
    await recherche.click();
    await recherche.fill('DOLIPRANE');
    await expect(page.locator('.ng-option').first()).toBeVisible({ timeout: 15000 });
  });

  await etape(3, async () => {
    // Retenir une suggestion suffit à l'affecter : le rayon se recharge aussitôt.
    await expect(page.locator('.ng-option').first()).toContainText(/DOLIPRANE/);
  });
});
