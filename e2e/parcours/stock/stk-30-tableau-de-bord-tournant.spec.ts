import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'inventaire tournant remplace l'inventaire annuel par une rotation continue : chaque
 * échéance couvre un rayon, et l'on revient au premier quand tous ont été faits. Le tableau
 * de bord existe pour répondre à une seule question — sommes-nous à jour ? — d'où le taux de
 * couverture et la prochaine échéance mis en avant.
 */
scenario('STK-30', async ({ etape, page }) => {
  const indicateur = (libelle: RegExp) => page.locator('app-kpi-item').filter({ hasText: libelle });

  await etape(1, async () => {
    await page.goto('/inventaire');
    await ouvrirOnglet(page, /Tournant/);
    await expect(indicateur(/Plannings actifs/i)).toContainText(/\d/);
  });

  await etape(2, async () => {
    // Les quatre indicateurs du bandeau, puis le planning qui les produit : sans sa ligne,
    // les compteurs ne seraient rattachables à rien.
    await expect(indicateur(/Inventaires ce mois/i)).toContainText(/\d/);
    await expect(indicateur(/Taux de couverture/i)).toContainText(/%/);
    await expect(indicateur(/Prochain tournant/i)).toContainText(/\d{2}\/\d{2}/);
    await expect(page.locator('tbody tr').filter({ visible: true }).first()).toContainText(/\d{2}\/\d{2}\/\d{4}/);
  });
});
