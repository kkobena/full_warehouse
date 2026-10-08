import { expect } from '@playwright/test';
import { assurerCaisseOuverte, assurerPanierVide } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un clic sur la tuile d'un favori ajoute le produit au panier pour une unité : ni recherche, ni
 * quantité à saisir. C'est le geste du comptoir pour la vaseline ou les pansements.
 *
 * Parcours en LECTURE : la vente commencée est abandonnée.
 */
scenario('VTE-65', async ({ etape, page }) => {
  const produit = 'VASELINE PURE 250ML';
  const grille = page.getByRole('region', { name: 'Produits favoris du comptoir' });
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    // Chaque tuile porte le libellé et le prix ; une rupture y est signalée.
    await expect(grille.getByRole('button', { name: new RegExp(`^${produit}`) })).toBeVisible();
    await expect(grille).toContainText('Rupture');
  });

  await etape(2, async () => {
    await grille.getByRole('button', { name: new RegExp(`^${produit}`) }).click();
  });

  await etape(3, async () => {
    await expect(lignes.first()).toContainText(produit);
    await expect(lignes).toHaveCount(1);
    // Une unité, au prix de la tuile.
    await expect(lignes.first()).toContainText(/\b1\b/);
    await expect(lignes.first()).toContainText('2 060');
  });

  await assurerPanierVide(page);
});
