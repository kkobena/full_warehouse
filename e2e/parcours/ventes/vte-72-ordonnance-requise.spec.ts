import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { lire, rejouer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Un médicament listé ne part pas sans qu'on sache qui l'a prescrit. À la finalisation, la
 * fenêtre « Ordonnance requise » nomme le produit : on associe une ordonnance en cours du client,
 * ou à défaut on indique le prescripteur — créé à la volée s'il manque. On peut aussi revenir à
 * la vente : rien n'est clôturé.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre une vente réelle ; le prescripteur d'essai est
 * retiré à la fin (scripts/essai-ordonnance/nettoyer.sql).
 */
scenario('VTE-72', async ({ etape, page }) => {
  const produit = 'AMOXICILLINE TEVA 1 g';
  const modale = page.locator('.modal-content');

  rejouer('scripts/essai-ordonnance/nettoyer.sql');
  expect(lire(`SELECT statut_legal <> 'SANS_LISTE' FROM produit WHERE libelle LIKE '${produit}%' ORDER BY id LIMIT 1`), `${produit} doit être sur ordonnance`).toBe('t');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '1');
  const recherche = page.locator('#produitbox');
  await recherche.click();
  await recherche.press('Enter');
  await expect(page.locator('#CASH')).toBeVisible();
  await page.locator('#CASH').fill('100000');

  await etape(1, async () => {
    await page.getByRole('button', { name: 'Finaliser' }).click();
    await expect(modale).toContainText('Ordonnance requise');
    await expect(modale).toContainText(produit);
  });

  await etape(2, async () => {
    // Revenir à la vente : la fenêtre se ferme, le panier est intact, rien n'est clôturé.
    await modale.getByRole('button', { name: 'Revenir à la vente' }).click();
    await expect(modale).toBeHidden();
    await expect(page.locator('tbody tr').filter({ visible: true }).first()).toContainText(produit);
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: 'Finaliser' }).click();
    await expect(modale).toContainText('Ordonnance requise');
    await modale.locator('input[placeholder*="nouveau prescripteur"]').fill('ESSAI E2E Requis');
    await modale.getByRole('button', { name: 'Créer' }).click();
    await modale.getByRole('button', { name: 'Valider' }).click();
    await expect(modale).toBeHidden();
    await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);
  });

  rejouer('scripts/essai-ordonnance/nettoyer.sql');
});
