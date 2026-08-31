import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Prévoir n'est pas deviner, mais ce n'est pas non plus savoir. Le rapport le dit de trois
 * façons.
 *
 * D'abord il ne cache pas sa MÉTHODE : régression linéaire pour une tendance régulière,
 * moyenne mobile pour lisser un historique heurté, modèle saisonnier quand l'activité suit le
 * calendrier — grippe, rentrée, saison des pluies. Le résultat change avec la méthode, et
 * c'est normal.
 *
 * Ensuite il affiche un INTERVALLE DE CONFIANCE à 95 %, et non un chiffre isolé : la largeur
 * de la fourchette est l'information la plus honnête de l'écran.
 *
 * Enfin il avertit quand l'historique est trop court pour qu'on s'y fie.
 */
scenario('RPT-04', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'sales', 'Prévisions de Ventes');
    await expect(page.getByRole('heading', { name: 'Prévisions de Ventes' })).toBeVisible();
  });

  await etape(2, async () => {
    // La moyenne mobile sur six mois : le couple que l'on choisit quand l'historique est
    // court et irrégulier — ce qui est le cas d'une officine récemment informatisée.
    await choisirDansSelect(page, 'prevMethode', 'Moyenne Mobile');
    await choisirDansSelect(page, 'prevHorizon', '6 mois');
  });

  await etape(3, async () => {
    await expect(contenu).toContainText(/PRÉVISION 3 MOIS/i);
    // La fourchette, pas seulement la courbe.
    await expect(page.getByRole('heading', { name: /Intervalles de Confiance/ })).toBeVisible();
  });
});
