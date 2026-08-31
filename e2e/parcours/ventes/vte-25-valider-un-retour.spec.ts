import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, ouvrirJournalDuJour, payerEnEspeces } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le mode de règlement d'un retour n'est pas un détail de caisse : il décide de ce que le
 * client emporte. Remboursé en espèces, il repart avec son argent et le dossier est clos.
 * Réglé « en avoir », il repart avec un droit — un avoir par ligne rendue, à faire valoir sur
 * un prochain achat.
 *
 * Parcours ÉCRIVANT dans la base : il crée sa vente d'origine, la retourne, et laisse donc un
 * retour et son avoir — que VTE-27 et VTE-28 iront justement consulter.
 */
scenario('VTE-25', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const contenu = page.locator('#main-content');
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  // La vente à reprendre est désignée par son MONTANT, et non par sa position : le journal
  // du jour contient aussi les ventes du jeu de démonstration et celles des autres parcours,
  // dont l'ordre ne se prédit pas. Les ventes annulées sont écartées — leur menu n'offre pas
  // de retour, et pour cause.
  const venteRetournable = lignes.filter({ hasText: '38 620' }).filter({ hasNotText: 'Annulée' }).first();

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  // ── Mise en scène : la vente qui sera retournée. ────────────────────────────────────────
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '2');
  await payerEnEspeces(page, '40000');
  await page.getByRole('button', { name: 'Finaliser' }).click();
  await expect(contenu).toContainText(/Panier vide|Ajoutez des produits/i);

  await ouvrirJournalDuJour(page);
  await expect(lignes.first()).toBeVisible();
  await venteRetournable.getByRole('button', { name: 'Actions' }).click();
  await page.getByRole('button', { name: 'Retour client' }).click();
  await expect(modale).toContainText('Retour client');
  await modale.getByRole('button', { name: 'Augmenter' }).first().click();

  await etape(1, async () => {
    // Le motif d'abord — il justifie la reprise — puis le mode de règlement, qui décide de
    // la suite.
    const motif = modale.locator('.form-group').filter({ hasText: 'Motif' }).locator('ng-select').first();
    await motif.click();
    await page.locator('.ng-option').filter({ hasText: 'Produit défectueux' }).first().click();

    const mode = modale.locator('.form-group').filter({ hasText: 'Mode de règlement' }).locator('ng-select').first();
    await mode.click();
    // Les deux voies sont là, côte à côte : c'est le choix que le caissier doit comprendre.
    await expect(page.locator('.ng-option').first()).toBeVisible();
    await page.locator('.ng-option').filter({ hasText: /Avoir/i }).first().click();
    await expect(mode).toContainText(/Avoir/i);
  });

  await etape(2, async () => {
    await modale.getByRole('button', { name: 'Valider le retour' }).click();
    await expect(modale).toBeHidden();

    // Le retour est enregistré et figure dans son onglet, avec son motif et son montant.
    await page.getByRole('tab', { name: /Retours client/ }).click();
    await expect(lignes.first()).toBeVisible();
    await expect(contenu).toContainText('Retours clients');
  });
});
