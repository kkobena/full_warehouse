import { expect, test } from '@playwright/test';
import { choisirDansSelect, saisirDate, saisirMontant } from '../../src/actions';
import { lire, rejouer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * « −10 % sur tout le rayon accessoires » : on ne recherche pas quarante articles un par un. Le
 * rayon remplit la liste d'un geste, en signalant les médicaments à prix administré. C'est la
 * liste qui s'enregistre, pas le rayon : chaque article y passe les contrôles ordinaires, et un
 * article rangé plus tard dans le rayon n'entre pas dans une promotion déjà validée.
 *
 * Parcours ÉCRIVANT : un brouillon est enregistré puis supprimé, et le parcours vérifie qu'il n'en
 * reste rien. Un brouillon ne s'applique jamais au comptoir.
 */
const RAYON = 'ACCESSOIRES ET DISPOSITIFS';
const LIBELLE = 'ESSAI E2E Accessoires -10 %';

/** Les articles promouvables du rayon : actifs, à prix libre — ce que le formulaire doit ajouter. */
const ATTENDUS = `
  SELECT count(DISTINCT p.id)
    FROM rayon_produit rp
    JOIN rayon r ON r.id = rp.rayon_id
    JOIN storage s ON s.id = r.storage_id
    JOIN app_user u ON u.magasin_id = s.magasin_id AND u.login = 'admin'
    JOIN produit p ON p.id = rp.produit_id
    JOIN tva t ON t.id = p.tva_id
   WHERE r.libelle = '${RAYON}' AND p.status = 'ENABLE' AND t.taux <> 0`;

test.beforeAll(() => rejouer('scripts/essai-promotion/nettoyer.sql'));
test.afterAll(() => {
  rejouer('scripts/essai-promotion/nettoyer.sql');
  expect(lire("SELECT count(*) FROM promotion WHERE libelle LIKE 'ESSAI E2E%'")).toBe('0');
});

scenario('RFD-28', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');
  const lignes = modale.locator('tbody tr');
  const attendus = Number(lire(ATTENDUS));
  expect(attendus, 'le jeu de démo range des articles à prix libre dans le rayon').toBeGreaterThan(1);

  await etape(1, async () => {
    await page.goto('/promotions');
    await page.getByRole('button', { name: 'Nouvelle promotion' }).click();
    await expect(modale.locator('#pf-libelle')).toBeFocused();
    await modale.locator('#pf-libelle').fill(LIBELLE);
    await saisirMontant(page, '#pf-valeur', '10');
    const fin = new Date();
    fin.setDate(fin.getDate() + 30);
    await saisirDate(page, 'pf-fin', fin);
  });

  await etape(2, async () => {
    // Le rayon, avec son nombre d'articles actifs entre parenthèses.
    await choisirDansSelect(page, 'pf-perimetre', RAYON);
    await modale.getByRole('button', { name: 'Ajouter ses articles' }).click();
    await expect(lignes).toHaveCount(attendus);
    await expect(page.getByText(/article\(s\) ajouté\(s\)/)).toBeVisible();
  });

  await etape(3, async () => {
    // À −10 %, un article à faible marge peut tomber sous son prix d'achat : le formulaire le
    // signale avant le serveur, et propose de le retirer d'un clic.
    const alerte = modale.getByRole('alert');
    if (await alerte.isVisible()) {
      await alerte.getByRole('button', { name: 'Les retirer' }).click();
    }
    await expect(modale.locator('.prix-perte')).toHaveCount(0);
    const retenus = await lignes.count();
    await modale.getByRole('button', { name: 'Enregistrer en brouillon' }).click();
    await expect(modale).toHaveCount(0);
    expect(
      lire(`SELECT count(*) FROM promotion_produit pp JOIN promotion p ON p.id = pp.promotion_id WHERE p.libelle = '${LIBELLE}'`),
    ).toBe(String(retenus));
  });
});
