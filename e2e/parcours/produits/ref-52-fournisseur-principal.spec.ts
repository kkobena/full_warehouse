import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le fournisseur PRINCIPAL n'est pas une préférence : c'est celui que l'application retiendra
 * d'office pour une commande rapide (REF-57), pour une suggestion de réapprovisionnement et
 * pour le prix d'achat affiché sur la fiche. Le désigner est donc un geste de gestion, pas un
 * marquage décoratif — d'où l'interrupteur, qui s'enregistre sur-le-champ.
 *
 * Parcours en LECTURE : le produit de démonstration n'a qu'un fournisseur rattaché, et le
 * basculer n'aurait rien à montrer ; l'écran est illustré tel qu'il se présente.
 */
scenario('REF-52', async ({ etape, page }) => {
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
    // La pastille dit lequel fait foi, l'interrupteur permet d'en désigner un autre. Le
    // fournisseur principal ne peut pas être retiré tant qu'il est le seul — l'action de
    // suppression reste inerte sur cette ligne.
    await expect(onglet.getByText('Principal', { exact: true }).first()).toBeVisible();
    await expect(onglet.getByRole('switch', { name: 'Définir comme fournisseur principal' }).first()).toBeVisible();
  });
});
