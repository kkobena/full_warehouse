import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * L'historique dit QUAND et COMBIEN ; le détail dit QUOI. C'est lui qu'on ouvre devant un
 * patient qui conteste un produit ou en réclame le nom exact.
 *
 * Les lignes affichées sont celles enregistrées à la vente — pas une reconstitution à partir
 * du catalogue d'aujourd'hui. Un produit renommé depuis, un prix qui a changé : le détail
 * garde ce qui a été vendu ce jour-là.
 *
 * La sélection se fait par un simple clic sur la ligne, sans quitter la fiche : on compare
 * plusieurs visites d'affilée.
 */
scenario('CLI-08', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/customer');
    await expect(lignes.first()).toBeVisible();
    // Un client qui a des achats : sans vente, il n'y a pas de détail à ouvrir.
    await lignes.first().locator('app-button[ngbtooltip="Voir détails"] button').first().click();
    await expect(contenu).toContainText('Achats');
  });

  await etape(2, async () => {
    // Le clic sur une visite déplie ses lignes, sous le tableau des ventes.
    const ventes = page.locator('tbody tr').filter({ visible: true });
    await ventes.first().click();
    await expect(contenu).toContainText(/Montant net/);
  });
});
