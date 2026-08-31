import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, ouvrirJournalDuJour, payerEnEspeces } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Rembourser un client ne veut pas dire remettre la boîte en rayon. Deux raisons l'interdisent,
 * et l'écran les traite séparément :
 *
 *   * le produit est THERMOSENSIBLE — une insuline sortie de l'officine a rompu sa chaîne du
 *     froid, quel que soit son aspect. Elle part en destruction, sans discussion ;
 *   * son ÉTAT déclaré est défaillant : emballage ouvert, numéro de lot illisible, date de
 *     péremption douteuse. La ligne part alors en quarantaine.
 *
 * Dans les deux cas le client est remboursé : c'est le STOCK qui ne bouge pas. Le caissier
 * coche ce qu'il constate, et l'application en tire les conséquences.
 *
 * Parcours ÉCRIVANT dans la base : il vend une insuline puis la reprend sans la restocker.
 */
scenario('VTE-22', async ({ etape, page }) => {
  const produit = 'INSULINE HUMAINE 100MG';
  const contenu = page.locator('#main-content');
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  // La vente à reprendre est désignée par son MONTANT, et non par sa position : le journal
  // du jour contient aussi les ventes du jeu de démonstration et celles des autres parcours,
  // dont l'ordre ne se prédit pas. Les ventes annulées sont écartées — leur menu n'offre pas
  // de retour, et pour cause.
  const venteRetournable = lignes.filter({ hasText: '3 880' }).filter({ hasNotText: 'Annulée' }).first();

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  // ── Mise en scène : la vente du produit thermosensible. ─────────────────────────────────
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '1');
  await payerEnEspeces(page, '60000');
  await page.getByRole('button', { name: 'Finaliser' }).click();
  await expect(contenu).toContainText(/Panier vide|Ajoutez des produits/i);

  await ouvrirJournalDuJour(page);
  await expect(lignes.first()).toBeVisible();
  await venteRetournable.getByRole('button', { name: 'Actions' }).click();
  await page.getByRole('button', { name: 'Retour client' }).click();
  await expect(modale).toContainText(produit);

  await etape(1, async () => {
    // La ligne est signalée thermosensible AVANT toute saisie : le caissier sait déjà que la
    // boîte ne retournera pas en rayon.
    await modale.getByRole('button', { name: 'Augmenter' }).first().click();
    await expect(modale).toContainText('Emballage intact');
    await expect(modale).toContainText('N° lot lisible');
    await expect(modale).toContainText('Péremption OK');
  });

  await etape(2, async () => {
    // On décoche ce qu'on ne peut pas certifier : l'emballage n'est plus intact. Trois
    // cases, trois constats — et c'est ce constat, non le motif commercial, qui décide du
    // sort de la marchandise.
    await modale.getByText('Emballage intact').click();

    const motif = modale.locator('.form-group').filter({ hasText: 'Motif' }).locator('ng-select').first();
    await motif.click();
    await page.locator('.ng-option').filter({ hasText: 'Produit défectueux' }).first().click();
    const mode = modale.locator('.form-group').filter({ hasText: 'Mode de règlement' }).locator('ng-select').first();
    await mode.click();
    await page.locator('.ng-option').filter({ hasText: 'Remboursement espèces' }).first().click();

    await modale.getByRole('button', { name: 'Valider le retour' }).click();

    // La fenêtre ne se referme pas : elle rend COMPTE. Le client est remboursé — 3 880 F —
    // et la ligne est nommément écartée du stock, avec sa raison. C'est ce récapitulatif que
    // le pharmacien doit lire avant de ranger la boîte ailleurs qu'en rayon.
    await expect(modale).toContainText('Retour enregistré');
    await expect(modale).toContainText('non remises en stock');
    await expect(modale).toContainText('thermosensible');
    await modale.getByRole('button', { name: 'Fermer' }).click();
  });
});
