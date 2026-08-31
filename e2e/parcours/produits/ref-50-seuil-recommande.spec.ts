import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un seuil mini posé à la création et jamais revu ne veut plus rien dire trois ans après :
 * les ventes ont changé, le délai du fournisseur aussi. L'application calcule donc un seuil
 * RECOMMANDÉ à partir de la consommation mensuelle moyenne et du délai de livraison, et le
 * met en regard du seuil paramétré.
 *
 * L'écart entre les deux est la seule information qui compte : un seuil paramétré très
 * au-dessus du recommandé immobilise de la trésorerie ; très en dessous, il fait manquer des
 * ventes. La couverture en jours dit lequel des deux cas on a sous les yeux.
 *
 * Parcours en LECTURE.
 */
scenario('REF-50', async ({ etape, page }) => {
  const produit = 'ARNICA MONTANA 9CH';
  const onglet = page.locator('app-produit-stock-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Stock');
    await expect(onglet).toContainText('Analyse CMM');
  });

  await etape(2, async () => {
    // Les quatre chiffres du raisonnement, et le seuil paramétré sur la carte du rayon.
    await expect(onglet).toContainText('CMM');
    await expect(onglet).toContainText('Délai fournisseur');
    await expect(onglet).toContainText('Couverture actuelle');
    await expect(onglet).toContainText('Seuil recommandé');
    await expect(onglet.getByText('Seuil mini').first()).toBeVisible();
  });
});
