import { expect, test } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { lire, rejouer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Une promotion cumulable ne s'efface pas quand le caissier choisit la remise produit de la vente :
 * la remise du caissier passe d'abord, la promotion s'applique ensuite sur le prix déjà remisé. Sans
 * le drapeau, la ligne garderait la seule remise du caissier — c'est le défaut, et il le reste.
 *
 * Point de départ posé en base (`scripts/essai-promotion/preparer-cumul.sql`). Parcours ÉCRIVANT : le
 * panier est abandonné, la promotion supprimée, et le parcours vérifie qu'il n'en reste rien.
 */
const ARTICLE = 'PANSEMENT ADHESIF B/20';
const LIBELLE = 'ESSAI E2E Pansements -10 % cumulable';

test.beforeAll(() => rejouer('scripts/essai-promotion/preparer-cumul.sql'));
test.afterAll(() => {
  rejouer('scripts/essai-promotion/nettoyer.sql');
  expect(lire("SELECT count(*) FROM promotion WHERE libelle LIKE 'ESSAI E2E%'")).toBe('0');
});

scenario('RFD-30', async ({ etape, page }) => {
  const ligne = page.locator('tbody tr', { hasText: ARTICLE });
  const montant = async (classe: string): Promise<number> => Number((await ligne.locator(classe).innerText()).replace(/\D/g, ''));
  const remiseDeLaLigne = async (): Promise<number> => (await montant('.pharma-total-barre')) - (await montant('.pharma-total'));

  try {
    let promotionSeule = 0;
    await etape(1, async () => {
      await assurerCaisseOuverte(page);
      await assurerPanierVide(page);
      await page.goto('/sales-home');
      await chercherProduit(page, ARTICLE);
      await ajouterAuPanier(page, '2');
      await expect(ligne.locator('.promo-ligne')).toContainText(LIBELLE);
      promotionSeule = await remiseDeLaLigne();
      // -10 % de deux unités, arrondi au franc supérieur.
      expect(promotionSeule).toBe(Math.ceil((await montant('.pharma-total-barre')) / 10));
    });

    await etape(2, async () => {
      await page.getByRole('button', { name: 'Remise' }).click();
      const choix = page.locator('ngb-popover-window').first();
      await expect(choix).toContainText('Choisir une remise');
      await choix.getByText('Grille de remise officine').first().click();
      await expect(page.locator('#main-content')).toContainText(/Remise\s*:\s*\d+\s*%/);
      // La promotion est restée sur la ligne, par-dessus la remise du caissier : la remise de la
      // ligne ne baisse jamais, elle s'ajoute à celle de la grille quand la grille vise l'article.
      await expect(ligne.locator('.promo-ligne')).toContainText(LIBELLE);
      expect(await remiseDeLaLigne()).toBeGreaterThanOrEqual(promotionSeule);
    });
  } finally {
    await assurerPanierVide(page);
  }
});
