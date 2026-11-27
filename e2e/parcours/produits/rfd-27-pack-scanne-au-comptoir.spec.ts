import { expect, test } from '@playwright/test';
import { assurerCaisseOuverte, assurerPanierVide } from '../../src/actions';
import { lire, rejouer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Le pack monté par l'officine n'est pas un article : c'est une étiquette collée sur un emballage.
 * Au scan, le comptoir ajoute ses articles, chacun sur sa ligne — chacun sort de son stock, garde sa
 * TVA — et le serveur, reconnaissant le pack complet, répartit la remise entre eux.
 *
 * Point de départ posé en base (`scripts/essai-promotion/preparer-pack.sql`), code PK99999999.
 * Parcours ÉCRIVANT : le panier est abandonné, le pack supprimé, et le parcours vérifie qu'il n'en
 * reste rien.
 */
const CODE = 'PK99999999';
const ARTICLES = ['PANSEMENT ADHESIF B/20', 'SPARADRAP HYPOALLERGENIQUE 5M'];

test.beforeAll(() => rejouer('scripts/essai-promotion/preparer-pack.sql'));
test.afterAll(() => {
  rejouer('scripts/essai-promotion/nettoyer.sql');
  expect(lire("SELECT count(*) FROM promotion WHERE libelle LIKE 'ESSAI E2E%'")).toBe('0');
});

scenario('RFD-27', async ({ etape, page }) => {
  const ligne = (article: string) => page.locator('tbody tr', { hasText: article });
  const montant = async (article: string, classe: string): Promise<number> =>
    Number((await ligne(article).locator(classe).innerText()).replace(/\D/g, ''));

  try {
    await etape(1, async () => {
      await assurerCaisseOuverte(page);
      await assurerPanierVide(page);
      await page.goto('/sales-home');
      // La douchette tape le code en quelques millisecondes, puis Entrée : c'est ce rythme qui
      // distingue un scan d'une frappe au clavier.
      await page.locator('#produitbox').click();
      await page.keyboard.type(CODE, { delay: 5 });
      await page.keyboard.press('Enter');
      for (const article of ARTICLES) {
        await expect(ligne(article)).toBeVisible();
      }
    });

    await etape(2, async () => {
      for (const article of ARTICLES) {
        await expect(ligne(article).locator('.promo-ligne')).toContainText('ESSAI E2E Pack soins');
      }
      // Les deux lignes remisées font, ensemble, le prix du pack : 2 000 F.
      let total = 0;
      for (const article of ARTICLES) {
        total += await montant(article, '.pharma-total');
      }
      expect(total).toBe(2000);
    });
  } finally {
    await assurerPanierVide(page);
  }
});
