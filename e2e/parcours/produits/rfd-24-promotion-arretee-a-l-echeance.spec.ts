import { expect, test } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { executer, lire, rejouer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Personne n'arrête une promotion : elle s'arrête seule. L'état « en cours / terminée » n'est pas
 * enregistré, il se lit sur les dates à chaque vente — aucun travail planifié, donc rien à rater
 * si le serveur est éteint à minuit.
 *
 * On ne peut pas attendre un mois : le parcours pose une promotion validée, puis recule ses dates
 * dans le passé, comme si son échéance était tombée hier. C'est exactement ce que voit
 * l'application le lendemain de la date de fin. Parcours ÉCRIVANT : panier abandonné, promotion
 * supprimée, et vérifié.
 */
const ARTICLE = 'PANSEMENT ADHESIF B/20';
const LIBELLE = 'ESSAI E2E Pansements -20 %';

test.beforeAll(() => {
  rejouer('scripts/essai-promotion/preparer.sql');
  executer(`UPDATE promotion SET date_debut = CURRENT_DATE - 30, date_fin = CURRENT_DATE - 1 WHERE libelle = '${LIBELLE}';`);
});
test.afterAll(() => {
  rejouer('scripts/essai-promotion/nettoyer.sql');
  expect(lire("SELECT count(*) FROM promotion WHERE libelle LIKE 'ESSAI E2E%'")).toBe('0');
});

scenario('RFD-24', async ({ etape, page }) => {
  try {
    await etape(1, async () => {
      await page.goto('/promotions');
      // Le filtre par défaut ne montre que les opérations actives : « Toutes » pour voir l'échue.
      await page.getByRole('button', { name: /^Toutes/ }).click();
      const ligne = page.locator('tbody tr', { hasText: LIBELLE });
      // Toujours « validée » : personne ne l'a touchée. Seule la date a passé.
      await expect(ligne).toContainText('Terminée');
      await expect(ligne.getByRole('button', { name: /^Suspendre/ })).toHaveCount(0);
    });

    await etape(2, async () => {
      await assurerCaisseOuverte(page);
      await assurerPanierVide(page);
      await page.goto('/sales-home');
      await chercherProduit(page, ARTICLE);
      await ajouterAuPanier(page, '2');
      const ligne = page.locator('tbody tr', { hasText: ARTICLE });
      await expect(ligne).toBeVisible();
      await expect(ligne.locator('.promo-ligne')).toHaveCount(0);
      await expect(ligne.locator('.pharma-total-barre')).toHaveCount(0);
    });
  } finally {
    await assurerPanierVide(page);
  }
});
