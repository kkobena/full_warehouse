import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, ouvrirJournalDuJour, payerEnEspeces } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un retour ne part jamais de rien : il part d'une VENTE. C'est elle qui dit ce qui a été
 * servi, à quel prix, et donc ce qui peut revenir — pas plus, ni autre chose.
 *
 * Le chemin est celui du journal : la ligne de la vente, son menu « Actions », « Retour
 * client ». L'onglet « Retours clients » ne sert qu'à consulter ceux déjà enregistrés.
 *
 * Parcours ÉCRIVANT dans la base : il crée sa vente d'origine puis abandonne le retour avant
 * validation — la reprise en stock est l'affaire de VTE-25.
 */
scenario('VTE-20', async ({ etape, page }) => {
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

  await etape(1, async () => {
    await ouvrirJournalDuJour(page);
    await expect(lignes.first()).toBeVisible();
    await venteRetournable.getByRole('button', { name: 'Actions' }).click();
    await expect(page.getByRole('button', { name: 'Retour client' })).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Retour client' }).click();
    // La fenêtre rappelle la vente : sa référence, son client, son ancienneté en jours — et
    // les lignes réellement servies, avec leur quantité vendue.
    await expect(modale).toContainText('Retour client');
    await expect(modale).toContainText(produit);
    // En-têtes mis en capitales par la feuille de style : c'est le texte du DOM qu'on vise.
    await expect(modale).toContainText('Qté vendue');
  });

  await etape(3, async () => {
    // La quantité à reprendre se saisit ligne par ligne, bornée par ce qui a été vendu : on
    // ne rend pas trois boîtes quand deux ont été servies.
    // Le compteur de la ligne : un clic sur « Augmenter » vaut mieux qu'une frappe dans un
    // champ que le composant reformate à chaque touche.
    await modale.getByRole('button', { name: 'Augmenter' }).first().click();

    // La quantité retenue s'inscrit dans le compteur de la ligne, et le montant remboursable
    // se recalcule aussitôt : c'est ce total que le client verra, et lui seul fait foi.
    await expect(modale.locator('app-input-number input').first()).not.toHaveValue('');
    await expect(modale).toContainText('Total remboursable client');
    // Le détail de l'état du produit n'apparaît qu'à partir du moment où une quantité est
    // retenue : sans reprise, il n'y a rien à qualifier.
    await expect(modale).toContainText('Emballage intact');

    // Le motif est obligatoire — un retour sans raison ne se justifie pas au contrôle.
    const motif = modale.locator('.form-group').filter({ hasText: 'Motif' }).locator('ng-select').first();
    await motif.click();
    await page.locator('.ng-option').filter({ hasText: 'Erreur de dispensation' }).first().click();
    await expect(motif).toContainText('Erreur de dispensation');
  });

  // ── Remise en état : le retour n'est pas validé. ────────────────────────────────────────
  await modale.getByRole('button', { name: 'Annuler' }).click();
});
