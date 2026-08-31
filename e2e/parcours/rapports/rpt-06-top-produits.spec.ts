import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le classement se lit sur un mois, jamais sur une période libre : le rapport compare chaque
 * produit à son rang du mois précédent (colonne « Δ Rang M-1 »), ce qui suppose deux mois
 * comparables. D'où le sélecteur de mois, et non deux dates.
 */
scenario('RPT-06', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  /** Quantité vendue de la n-ième ligne : 6e colonne, débarrassée de ses séparateurs. */
  const quantite = async (rang: number): Promise<number> =>
    Number((await lignes.nth(rang).locator('td').nth(5).innerText()).replace(/\D/g, ''));

  await etape(1, async () => {
    await page.goto('/reports/sales');
    await ouvrirOnglet(page, /Top Produits/);
    // « Δ Rang M-1 » n'appartient qu'à ce rapport — le Top 10 du tableau de bord, masqué mais
    // toujours dans la page, n'a pas de colonne d'évolution.
    await expect(page.getByRole('columnheader', { name: /Δ Rang/ })).toBeVisible();
  });

  await etape(2, async () => {
    await choisirDansSelect(page, 'tpLimite', 'Top 10');
    // La taille du classement est vérifiable telle quelle : dix lignes, pas les vingt du
    // réglage par défaut.
    await expect(lignes).toHaveCount(10);
  });

  await etape(3, async () => {
    await ouvrirOnglet(page, 'Par Quantité Vendue');
    await expect(lignes).toHaveCount(10);
    // Ce qui distingue ce classement de celui par chiffre d'affaires, c'est son ORDRE : les
    // deux tableaux ont les mêmes colonnes et souvent les mêmes produits. On vérifie donc
    // que les quantités décroissent, seule chose que la légende promet ici.
    expect(await quantite(0)).toBeGreaterThanOrEqual(await quantite(9));
  });
});
