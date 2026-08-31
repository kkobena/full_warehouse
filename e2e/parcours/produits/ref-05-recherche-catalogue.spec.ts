import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le catalogue d'une officine se compte en milliers de références : le retrouver « en
 * parcourant » n'est pas une méthode. Trois leviers se combinent, et c'est leur COMBINAISON
 * qui fait le travail — un terme libre (CIP, libellé ou EAN), les deux classements du
 * produit (famille thérapeutique, rayon), et l'état du catalogue.
 *
 * Ce dernier filtre est le moins évident et le plus utile : par défaut la liste ne montre que
 * les produits ACTIFS. Un produit mis en veille n'a pas disparu — il est derrière « Produits
 * désactivés », ce dont dépendent REF-39 et REF-40.
 *
 * Parcours en LECTURE.
 */
scenario('REF-05', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/produits');
    await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    // La recherche accepte indifféremment le libellé, le CIP ou l'EAN : c'est le même champ,
    // et c'est ce qui permet de taper ce qu'on a sous les yeux sans se demander ce que c'est.
    await page.getByPlaceholder(/Rechercher \(CIP/).fill('AMOXICILLINE');
    await page.keyboard.press('Enter');
    await expect(lignes.first()).toContainText(/AMOXICILLINE/i);
  });

  await etape(3, async () => {
    // Le filtre d'état se combine à la recherche au lieu de la remplacer : « Tous » ramène
    // les produits en veille en plus des actifs, sur le même terme.
    await choisirDansSelect(page, 'produitFiltreEtat', 'Tous');
    await expect(lignes.first()).toContainText(/AMOXICILLINE/i);
  });
});
