import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * « Où est passé mon stock ? » — la question se pose devant chaque écart d'inventaire, et
 * l'onglet Mouvements y répond en une lecture : une ligne par jour, le stock au matin, tout
 * ce qui est SORTI (ventes, retours fournisseur, pertes, déconditionnements), tout ce qui est
 * ENTRÉ (réceptions, retours clients, ajustements), et le stock du soir.
 *
 * Le filtre par types ne masque pas des lignes : il masque des COLONNES. On isole ainsi une
 * nature de mouvement sans perdre le fil chronologique, ce qui est exactement ce qu'on veut
 * quand on cherche d'où vient un écart.
 *
 * Parcours en LECTURE.
 */
scenario('REF-08', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const onglet = page.locator('app-produit-mouvements-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Mouvements');
    await expect(onglet).toBeVisible();
  });

  await etape(2, async () => {
    // Un an : la période qui montre l'année entière du produit, réceptions comprises.
    await onglet.getByRole('button', { name: '1 an' }).click();
    await onglet.getByRole('button', { name: 'Actualiser' }).click();
    await expect(onglet.locator('tbody tr').first()).toBeVisible();
    // Les deux familles de colonnes sont bien là, c'est ce qui rend la lecture possible.
    await expect(onglet).toContainText('Sorties');
    await expect(onglet).toContainText('Entrées');
  });

  await etape(3, async () => {
    // La colonne de droite ferme le raisonnement : stock du matin ± mouvements = stock du
    // soir. C'est cette égalité, ligne à ligne, qui permet de dater un écart.
    await expect(onglet).toContainText('Stock');
    await expect(onglet.locator('tbody tr').first()).toBeVisible();
  });
});
