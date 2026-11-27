import { expect, test } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { lire, rejouer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * « −10 % dès 2 500 F d'achat » : le caissier n'a rien à surveiller. Tant que les articles de
 * l'opération n'atteignent pas le seuil, le panier reste au prix ; l'article qui le franchit remise
 * toutes les lignes de l'opération d'un coup, et la remise se répartit entre elles au franc près.
 *
 * Point de départ posé en base (`scripts/essai-promotion/preparer-seuil-montant.sql`) : le seuil vaut
 * deux pansements plus 1 F. Parcours ÉCRIVANT : le panier est abandonné, la promotion supprimée, et
 * le parcours vérifie qu'il n'en reste rien.
 */
const PANSEMENT = 'PANSEMENT ADHESIF B/20';
const SPARADRAP = 'SPARADRAP HYPOALLERGENIQUE 5M';
const LIBELLE = 'ESSAI E2E Soins -10 % dès 2 pansements';

test.beforeAll(() => rejouer('scripts/essai-promotion/preparer-seuil-montant.sql'));
test.afterAll(() => {
  rejouer('scripts/essai-promotion/nettoyer.sql');
  expect(lire("SELECT count(*) FROM promotion WHERE libelle LIKE 'ESSAI E2E%'")).toBe('0');
});

scenario('RFD-29', async ({ etape, page }) => {
  const ligne = (article: string) => page.locator('tbody tr', { hasText: article });
  const montant = async (article: string, classe: string): Promise<number> =>
    Number((await ligne(article).locator(classe).innerText()).replace(/\D/g, ''));

  try {
    await etape(1, async () => {
      await assurerCaisseOuverte(page);
      await assurerPanierVide(page);
      await page.goto('/sales-home');
      await chercherProduit(page, PANSEMENT);
      await ajouterAuPanier(page, '2');
      await expect(ligne(PANSEMENT)).toBeVisible();
      // Deux pansements : un franc sous le seuil, rien n'est remisé.
      await expect(ligne(PANSEMENT).locator('.promo-ligne')).toHaveCount(0);
    });

    await etape(2, async () => {
      await chercherProduit(page, SPARADRAP);
      await ajouterAuPanier(page, '1');
      // Le sparadrap franchit le seuil : les deux lignes de l'opération sont remisées ensemble.
      for (const article of [PANSEMENT, SPARADRAP]) {
        await expect(ligne(article).locator('.promo-ligne')).toContainText(LIBELLE);
      }
      // 10 % du montant des deux lignes, arrondi au franc supérieur une seule fois, puis réparti.
      let avant = 0;
      let apres = 0;
      for (const article of [PANSEMENT, SPARADRAP]) {
        avant += await montant(article, '.pharma-total-barre');
        apres += await montant(article, '.pharma-total');
      }
      expect(avant - apres).toBe(Math.ceil(avant / 10));
    });
  } finally {
    await assurerPanierVide(page);
  }
});
