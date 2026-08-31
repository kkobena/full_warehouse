import { expect } from '@playwright/test';
import { chercherDansSelect, choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le même produit vit à deux endroits : le RAYON, où l'on sert, et la RÉSERVE, où l'on
 * stocke. Un ajustement qui ne dirait pas lequel des deux est concerné déplacerait le
 * problème plutôt que de le corriger — le total resterait juste, et l'étagère toujours fausse.
 *
 * L'emplacement se choisit donc avant le produit, et il commande tout le reste : la recherche
 * ne propose que ce que CE stockage détient, le stock affiché est le sien, et le libellé
 * change avec lui (« Rayon » ou « Réserve »).
 *
 * Le stock APRÈS mouvement s'affiche avant que la ligne soit ajoutée. Un résultat négatif est
 * signalé « Stock insuffisant » : on ne sort pas ce qu'on n'a pas.
 *
 * Parcours ÉCRIVANT dans la base : il prépare un ajustement sans le clôturer.
 */
scenario('STK-02', async ({ etape, page }) => {
  // Un produit RÉELLEMENT présent en réserve : la recherche ne propose que ce que le
  // stockage choisi détient, ce qui est tout l'intérêt de l'écran.
  const produit = 'ARNICA MONTANA 9CH';
  const ecran = page.locator('app-ajustement-form');

  await etape(1, async () => {
    await page.goto('/features-ajustement');
    await page.getByRole('button', { name: 'Nouvel ajustement' }).click();
    await expect(ecran).toBeVisible();
  });

  await etape(2, async () => {
    // L'emplacement : c'est lui qui définit le stock à corriger.
    await choisirDansSelect(page, 'ajustement-emplacement', 'Stock réserve');
  });

  await etape(3, async () => {
    await choisirDansSelect(page, 'ajustement-motif', 'Erreur de comptage');
    await chercherDansSelect(page, 'produitbox', produit, produit);
    // Le stock affiché est celui de CET emplacement, pas le total de l'officine.
    await expect(ecran).toContainText(/Réserve\s*:/);
  });

  await etape(4, async () => {
    const qte = ecran.locator('input[placeholder="Qté (±)"]');
    await qte.click();
    await qte.fill('');
    await qte.pressSequentially('4', { delay: 40 });
    // Le stock qui résultera du mouvement, avant de l'engager.
    await expect(ecran).toContainText(/Après\s*:/);
  });

  await etape(5, async () => {
    await ecran.getByRole('button', { name: /Ajouter|^$/ }).last().click();
    await expect(ecran.locator('tbody tr').first()).toContainText(produit);
  });
});
