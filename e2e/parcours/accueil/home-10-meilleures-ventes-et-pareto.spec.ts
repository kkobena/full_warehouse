import { expect } from '@playwright/test';
import { carte } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Les deux classements et l'analyse 20/80 répondent à la même question par deux bouts : ce qui
 * fait le chiffre, et ce qui fait le volume. Le basculement Quantité / Montant de la carte
 * Pareto est le geste qui les réconcilie — et il n'est signalé par rien d'autre que deux
 * petites pastilles, ce qui justifie de l'illustrer.
 */
scenario('HOME-10', async ({ etape, page }) => {
  const pareto = carte(page, 'Analyse Pareto 20/80');

  // « Auj. » est la période d'ouverture : selon l'heure et le jour, elle peut ne porter aucune
  // vente, et les classements s'affichent alors vides à juste titre. Le mois en cours est la
  // période qui montre l'écran vivant — même choix qu'en HOME-07.
  const passerAuMois = async () => {
    const mois = page.getByRole('button', { name: /Mois$/ });
    await mois.click();
    await expect(mois).toHaveClass(/active/);
  };

  await etape(1, async () => {
    await page.goto('/');
    await expect(page.locator('.kpi-strip-item').filter({ hasText: 'CA Net' })).toBeVisible();
    await passerAuMois();
  });

  await etape(2, async () => {
    const valeur = carte(page, 'Meilleures ventes en valeur');
    const quantite = carte(page, 'Meilleures ventes en quantité');
    await expect(valeur).toContainText(/#1/);
    await expect(quantite).toContainText(/#1/);
    await valeur.scrollIntoViewIfNeeded();
  });

  await etape(3, async () => {
    // L'onglet ouvert est « Quantité » : la colonne de pourcentage le dit.
    await expect(pareto.getByText('%Qté')).toBeVisible();
    // Ancrage en fin de chaîne : l'icône du bouton laisse une espace en tête du nom
    // accessible, que `exact` ne pardonnerait pas. Voir HOME-07.
    await pareto.getByRole('button', { name: /Montant$/ }).click();
    // Le basculement change la grandeur cumulée, pas seulement l'ordre des lignes : c'est
    // l'en-tête qui le prouve, et lui seul distingue les deux vues.
    await expect(pareto.getByText('%Montant')).toBeVisible();
    await pareto.scrollIntoViewIfNeeded();
  });
});
