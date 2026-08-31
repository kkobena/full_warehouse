import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le rayon n'est pas un classement théorique : c'est l'endroit où l'on ira CHERCHER la boîte.
 * Un produit sans emplacement se voit tout de suite — l'onglet Rayons le marque « Sans
 * emplacement » — et il fera perdre du temps à chaque délivrance jusqu'à ce qu'on l'assigne.
 *
 * L'assignation vaut PAR STOCKAGE : le même produit peut occuper l'allée A2 dans l'officine
 * et une étagère différente au dépôt.
 *
 * Parcours en LECTURE : il montre l'emplacement et les deux gestes offerts, sans déplacer un
 * produit du catalogue de démonstration dont d'autres parcours citent le rayon.
 */
scenario('REF-45', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const onglet = page.locator('app-produit-rayons-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Rayons');
    await expect(onglet).toBeVisible();
  });

  await etape(2, async () => {
    // L'emplacement se lit par stockage : le nom du stockage, puis le code et le libellé du
    // rayon qu'il y occupe.
    await expect(onglet.locator('.prt-item').first()).toBeVisible();
    await expect(onglet).toContainText(/ANTALGIQUES|HOMEOPATHIE|Sans emplacement/i);
  });

  await etape(3, async () => {
    // Le geste de déplacement est offert sur chaque ligne : c'est lui qui réassigne le rayon
    // sans passer par le formulaire complet du produit.
    await expect(
      onglet.getByRole('button', { name: /Déplacer vers un autre emplacement|Assigner un emplacement dans ce stockage/ }).first(),
    ).toBeVisible();
  });
});
