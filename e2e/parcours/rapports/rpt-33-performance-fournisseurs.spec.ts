import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le score de performance agrège volume, délai et conformité : c'est un chiffre qu'on ne sait
 * pas lire sans savoir d'où il vient, et l'écran range précisément ces trois composantes dans
 * les colonnes qui le précèdent.
 */
scenario('RPT-33', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const indicateur = (libelle: string) => page.locator('app-kpi-item').filter({ hasText: libelle });

  /** Score de la n-ième ligne — dernière colonne du tableau. */
  const score = async (rang: number): Promise<number> =>
    Number((await lignes.nth(rang).locator('td').last().innerText()).replace(/\s/g, '').replace(',', '.'));

  await etape(1, async () => {
    await page.goto('/reports/partners');
    await ouvrirOnglet(page, /Performance Fournisseurs/);
    await expect(page.getByRole('columnheader', { name: 'Score' })).toBeVisible();
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    await choisirDansSelect(page, 'perfFiltre', 'Performance excellente');
    // Le segment promet un plancher, pas un fournisseur en particulier : on le vérifie sur la
    // DERNIÈRE ligne, la moins bien classée — celle qui tomberait la première si le filtre
    // n'avait pas été appliqué.
    await expect(lignes.first()).toBeVisible();
    expect(await score((await lignes.count()) - 1)).toBeGreaterThanOrEqual(70);
  });

  await etape(3, async () => {
    // Les trois composantes du score, telles que l'écran les résume en tête.
    await expect(indicateur('Achats 12 mois')).toContainText(/\d/);
    await expect(indicateur('Délai moyen')).toContainText(/\d/);
    await expect(indicateur('Conformité moy.')).toContainText(/\d/);
  });
});
