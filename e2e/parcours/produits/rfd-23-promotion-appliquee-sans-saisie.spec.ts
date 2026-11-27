import { expect, test } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { lire, rejouer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Le caissier ne fait rien : il ajoute l'article comme d'habitude, et la ligne arrive remisée.
 * C'est le cœur de la fonctionnalité — une promotion qui dépend de la mémoire du comptoir finit
 * appliquée à une cliente sur deux.
 *
 * Point de départ posé en base (`scripts/essai-promotion/preparer.sql`) : une promotion validée
 * de -20 % sur l'article, en cours. Parcours ÉCRIVANT : le panier est abandonné, la promotion
 * supprimée, et le parcours vérifie qu'il n'en reste rien.
 */
const ARTICLE = 'PANSEMENT ADHESIF B/20';

test.beforeAll(() => rejouer('scripts/essai-promotion/preparer.sql'));
test.afterAll(() => {
  rejouer('scripts/essai-promotion/nettoyer.sql');
  expect(lire("SELECT count(*) FROM promotion WHERE libelle LIKE 'ESSAI E2E%'")).toBe('0');
});

scenario('RFD-23', async ({ etape, page }) => {
  const ligne = page.locator('tbody tr', { hasText: ARTICLE });

  try {
    await etape(1, async () => {
      await assurerCaisseOuverte(page);
      await assurerPanierVide(page);
      await page.goto('/sales-home');
      await chercherProduit(page, ARTICLE);
      await ajouterAuPanier(page, '2');
      await expect(ligne).toBeVisible();
    });

    await etape(2, async () => {
      // Aucune remise saisie : le montant barré, le montant remisé et le nom de l'opération
      // viennent du serveur, qui a appliqué la promotion à l'ajout.
      await expect(ligne.locator('.pharma-total-barre')).toBeVisible();
      await expect(ligne.locator('.promo-ligne')).toContainText('ESSAI E2E Pansements -20 %');
      const barre = Number((await ligne.locator('.pharma-total-barre').innerText()).replace(/\D/g, ''));
      const remise = Number((await ligne.locator('.pharma-total').innerText()).replace(/\D/g, ''));
      // -20 % du montant de la ligne, arrondi au franc supérieur côté remise.
      expect(remise).toBe(barre - Math.ceil(barre * 0.2));
    });
  } finally {
    await assurerPanierVide(page);
  }
});
