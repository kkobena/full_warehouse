import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, ouvrirJournalDuJour, payerEnEspeces } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le cas le plus fréquent du comptoir : le client ne veut pas être remboursé, il veut
 * ÉCHANGER — la mauvaise dose, le mauvais conditionnement. Cocher « avec échange » évite le
 * détour par la caisse : l'avoir est généré et immédiatement disponible pour la vente de
 * remplacement, sans avoir à le rechercher.
 *
 * Deux conséquences, visibles à l'écran : le mode de règlement n'a plus à être choisi — c'est
 * forcément un avoir — et le récapitulatif annonce le montant crédité, avec sa référence.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre un retour et l'avoir qui l'accompagne.
 */
scenario('VTE-26', async ({ etape, page }) => {
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

  // ── Mise en scène : la vente à échanger. ────────────────────────────────────────────────
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '2');
  await payerEnEspeces(page, '40000');
  await page.getByRole('button', { name: 'Finaliser' }).click();
  await expect(contenu).toContainText(/Panier vide|Ajoutez des produits/i);

  await ouvrirJournalDuJour(page);
  await expect(lignes.first()).toBeVisible();
  await venteRetournable.getByRole('button', { name: 'Actions' }).click();
  await page.getByRole('button', { name: 'Retour client' }).click();
  await expect(modale).toContainText(produit);
  await modale.getByRole('button', { name: 'Augmenter' }).first().click();

  await etape(1, async () => {
    // La case change la nature de l'opération : plus de choix de règlement à faire, l'avoir
    // s'impose. Le formulaire s'allège d'autant.
    await modale.locator('#avecEchangeCheck').check();
    await expect(modale).toContainText('Retour avec échange');

    const motif = modale.locator('.form-group').filter({ hasText: 'Motif' }).locator('ng-select').first();
    await motif.click();
    await page.locator('.ng-option').filter({ hasText: 'Erreur de dispensation' }).first().click();
  });

  await etape(2, async () => {
    await modale.getByRole('button', { name: 'Valider le retour' }).click();

    // L'avoir est là, avec son montant et sa référence : c'est ce crédit que la vente de
    // remplacement consommera, sans que personne ait à le chercher.
    await expect(modale).toContainText('Retour enregistré');
    await expect(modale).toContainText('Avoir généré pour échange');
    await expect(modale).toContainText('Montant disponible');
    await modale.getByRole('button', { name: 'Fermer' }).click();
  });
});
