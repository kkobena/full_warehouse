import { expect, test } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { lire, rejouer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * « 2 achetés, 1 offert » : le caissier saisit trois unités, la troisième ne se paie pas. Rien à
 * retenir, rien à saisir — et surtout pas une ligne gratuite à part, qui puiserait dans les unités
 * gratuites du fournisseur : l'offre est une remise, posée sur la ligne.
 *
 * Point de départ posé en base (`scripts/essai-promotion/preparer-offre-nm.sql`). Parcours
 * ÉCRIVANT : le panier est abandonné, l'offre supprimée, et le parcours vérifie qu'il n'en reste rien.
 */
const ARTICLE = 'PANSEMENT ADHESIF B/20';

test.beforeAll(() => rejouer('scripts/essai-promotion/preparer-offre-nm.sql'));
test.afterAll(() => {
  rejouer('scripts/essai-promotion/nettoyer.sql');
  expect(lire("SELECT count(*) FROM promotion WHERE libelle LIKE 'ESSAI E2E%'")).toBe('0');
});

scenario('RFD-26', async ({ etape, page }) => {
  const ligne = page.locator('tbody tr', { hasText: ARTICLE });
  const montant = async (classe: string): Promise<number> => Number((await ligne.locator(classe).innerText()).replace(/\D/g, ''));

  try {
    await etape(1, async () => {
      await assurerCaisseOuverte(page);
      await assurerPanierVide(page);
      await page.goto('/sales-home');
      await chercherProduit(page, ARTICLE);
      await ajouterAuPanier(page, '2');
      await expect(ligne).toBeVisible();
      // Deux unités : le groupe de trois n'est pas complet, rien n'est offert.
      await expect(ligne.locator('.promo-ligne')).toHaveCount(0);
    });

    await etape(2, async () => {
      await chercherProduit(page, ARTICLE);
      await ajouterAuPanier(page, '1');
      await expect(ligne.locator('.promo-ligne')).toContainText('ESSAI E2E Pansements 2+1');
      // Trois unités, une offerte : on paie deux fois le prix de l'unité.
      const barre = await montant('.pharma-total-barre');
      expect(await montant('.pharma-total')).toBe((barre / 3) * 2);
    });
  } finally {
    await assurerPanierVide(page);
  }
});
