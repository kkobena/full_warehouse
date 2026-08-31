import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un produit ne dépend jamais d'un seul grossiste : celui qui livre en 24 h n'est pas toujours
 * le moins cher, et celui qui est le moins cher est parfois en rupture. L'onglet Fournisseurs
 * garde, pour chaque partenaire, le code CIP qu'IL emploie, son prix d'achat et son prix
 * unitaire — ce sont ces codes qui permettent de commander chez l'un ou chez l'autre sans
 * ressaisie.
 *
 * Parcours en LECTURE : il n'ajoute pas de fournisseur, il montre ce que la fiche en retient.
 */
scenario('REF-51', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const onglet = page.locator('app-produit-fournisseurs-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Fournisseurs');
    await expect(onglet).toBeVisible();
  });

  await etape(2, async () => {
    // Les colonnes qui font la décision d'achat, et le bouton qui ouvre l'ajout d'un
    // partenaire supplémentaire.
    await expect(onglet).toContainText('Principal');
    await expect(onglet).toContainText(/Prix/i);
    await expect(onglet.getByRole('button', { name: 'Ajouter fournisseur' })).toBeVisible();
    await expect(onglet.locator('tbody tr').first()).toBeVisible();
  });
});
