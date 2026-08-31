import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * La bande d'indicateurs agrège six modules en une ligne : ventes, marge, comptant, créances
 * tiers payant, achats, stock et différés. Chacun est le résumé d'un écran complet — d'où
 * l'intérêt de l'illustrer, et la nécessité de choisir une période qui les alimente tous.
 */
scenario('HOME-07', async ({ etape, page }) => {
  const indicateur = (libelle: string) => page.locator('.kpi-strip-item').filter({ hasText: libelle });

  await etape(1, async () => {
    await page.goto('/');
    await expect(indicateur('CA Net')).toBeVisible();
  });

  await etape(2, async () => {
    // « Auj. » à l'ouverture : une seule journée de ventes, et la moitié des indicateurs
    // tombent à zéro. Le mois en cours est la période qui montre l'écran vivant.
    // Le nom accessible du bouton est « ␣Mois » : l'élément d'icône qui précède le libellé
    // laisse une espace que Playwright ne rogne pas, et `exact` échoue donc. On ancre en fin
    // de chaîne ; la casse suffit à écarter « 12 mois », le sélecteur du bloc fournisseurs.
    const mois = page.getByRole('button', { name: /Mois$/ });
    await mois.click();
    await expect(mois).toHaveClass(/active/);
  });

  await etape(3, async () => {
    // Un montant non nul sur les indicateurs issus des trois modules les plus éloignés les
    // uns des autres : les ventes, le stock et les différés.
    await expect(indicateur('CA Net')).toContainText(/[1-9]/);
    await expect(indicateur('Marge brute')).toContainText(/[1-9]/);
    await expect(indicateur('Stock valorisé')).toContainText(/[1-9]/);
    await expect(indicateur('Différés clients')).toContainText(/[1-9]/);
  });
});
