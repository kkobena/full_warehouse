import { expect, test } from '@playwright/test';
import { saisirDate, saisirMontant } from '../../src/actions';
import { lire, rejouer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Une promotion ne change pas le prix de l'article : elle produit une remise, posée par le
 * comptoir entre deux dates. La préparer en brouillon, puis la valider, sépare la saisie de la
 * décision — rien ne s'applique tant que personne n'a validé.
 *
 * Parcours ÉCRIVANT dans la base : la promotion d'essai est supprimée à la fin, et le parcours
 * vérifie qu'il n'en reste rien — laissée validée, elle changerait les prix d'une démonstration
 * (plan des promotions, §7 point 15).
 */
const ARTICLE = 'PANSEMENT ADHESIF B/20';
const LIBELLE = 'ESSAI E2E Pansements -15 %';

test.beforeAll(() => rejouer('scripts/essai-promotion/nettoyer.sql'));
test.afterAll(() => {
  rejouer('scripts/essai-promotion/nettoyer.sql');
  expect(lire("SELECT count(*) FROM promotion WHERE libelle LIKE 'ESSAI E2E%'")).toBe('0');
});

scenario('RFD-22', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');
  const ligne = page.locator('tbody tr', { hasText: LIBELLE });

  await etape(1, async () => {
    await page.goto('/promotions');
    await page.getByRole('button', { name: 'Nouvelle promotion' }).click();
    // Le curseur attend dans le libellé : on tape sans cliquer.
    await expect(modale.locator('#pf-libelle')).toBeFocused();
    await modale.locator('#pf-libelle').fill(LIBELLE);
    await saisirMontant(page, '#pf-valeur', '15');
    const fin = new Date();
    fin.setDate(fin.getDate() + 30);
    await saisirDate(page, 'pf-fin', fin);
    await expect(modale.locator('#pf-libelle')).toHaveValue(LIBELLE);
  });

  await etape(2, async () => {
    // Le nom complet ne désigne qu'un article : la recherche le retient d'elle-même, sans liste
    // à parcourir, l'ajoute aux articles de la promotion puis se vide pour le suivant.
    await modale.locator('#produits').click();
    await modale.locator('#produits').pressSequentially(ARTICLE, { delay: 40 });
    const article = modale.locator('tbody tr', { hasText: ARTICLE });
    await expect(article).toBeVisible();
    // Le prix en promotion se lit avant d'enregistrer : 15 % de moins, arrondi au franc.
    await expect(article.locator('.prix-promo')).not.toBeEmpty();
    await expect(modale.locator('.prix-perte')).toHaveCount(0);
  });

  await etape(3, async () => {
    await modale.getByRole('button', { name: 'Enregistrer en brouillon' }).click();
    await expect(modale).toHaveCount(0);
    // La liste montre d'abord ce qui s'applique : un brouillon se range sous son propre filtre.
    await page.getByRole('button', { name: /^Brouillons/ }).click();
    await expect(ligne).toContainText('Brouillon');
  });

  await etape(4, async () => {
    await ligne.getByRole('button', { name: /^Valider/ }).click();
    const confirmation = page.locator('.modal-content');
    await expect(confirmation).toContainText('Valider la promotion');
    await confirmation.getByRole('button', { name: 'Oui' }).click();
    // Validée, elle quitte les brouillons et rejoint ce qui s'applique au comptoir.
    await expect(ligne).toHaveCount(0);
    await page.getByRole('button', { name: /^En cours et à venir/ }).click();
    await expect(ligne).toContainText('En cours');
  });
});
