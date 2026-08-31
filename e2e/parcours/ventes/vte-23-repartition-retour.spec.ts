import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, ouvrirJournalDuJour, payerEnEspeces } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Sur une vente assurance, le client n'a payé qu'une part : lui rembourser la totalité
 * reviendrait à lui offrir la part de l'organisme. Le retour reprend donc le MÊME partage que
 * la vente d'origine — c'est le taux appliqué ce jour-là qui décide, pas celui d'aujourd'hui.
 *
 * L'écran l'affiche ligne par ligne : le montant remboursable au client, et à côté, en
 * pastille, ce qui revient au tiers payant.
 *
 * Parcours ÉCRIVANT dans la base : il crée une vente assurance puis ferme la fenêtre de
 * retour sans valider.
 */
scenario('VTE-23', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 1G';
  const matricule = 'CIE01-000118';
  const contenu = page.locator('#main-content');
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  // La vente à reprendre est désignée par son MONTANT, et non par sa position : le journal
  // du jour contient aussi les ventes du jeu de démonstration et celles des autres parcours,
  // dont l'ordre ne se prédit pas. Les ventes annulées sont écartées — leur menu n'offre pas
  // de retour, et pour cause.
  const venteRetournable = lignes.filter({ hasText: '25 950' }).filter({ hasNotText: 'Annulée' }).first();

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  // ── Mise en scène : une vente assurance à 70 %, encaissée. ──────────────────────────────
  await page.getByRole('tab', { name: /Assurance/ }).click();
  const recherche = page.getByPlaceholder('Rechercher un client assuré');
  await recherche.fill('ASSI');
  await recherche.press('Enter');
  await modale.locator('tbody tr').filter({ hasText: matricule }).first().dblclick();
  const bon = page.getByPlaceholder('Numéro de bon');
  await bon.click();
  await bon.pressSequentially('BON' + Date.now().toString().slice(-9), { delay: 25 });
  await bon.press('Enter');
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '2');
  await payerEnEspeces(page, '10000');
  await page.getByRole('button', { name: 'Finaliser' }).click();
  await expect(contenu).toContainText(/Panier vide|Sélectionnez un client assuré/i);

  await etape(1, async () => {
    await ouvrirJournalDuJour(page);
    await expect(lignes.first()).toBeVisible();
    await venteRetournable.getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Retour client' }).click();
    await expect(modale).toContainText(produit);
    // La pastille « TP » à côté du prix : la part que l'organisme récupérera. Sans elle, le
    // caissier croirait devoir rendre 25 950 à un client qui n'en a payé que 7 785.
    await expect(modale).toContainText('TP');
  });

  await etape(2, async () => {
    await modale.getByRole('button', { name: 'Augmenter' }).first().click();
    // La vente valait 25 950, dont 18 165 pour l'organisme et 7 785 pour le patient. Au
    // retour, chacun retrouve exactement sa part : 7 785 remboursables au client, 18 165
    // repris à l'organisme. Rembourser 25 950 reviendrait à offrir au client l'argent de son
    // assurance.
    await expect(modale).toContainText('Total remboursable client');
    await expect(modale).toContainText('7 785');
    await expect(modale).toContainText('18 165');
  });

  // ── Remise en état : rien n'est validé. ─────────────────────────────────────────────────
  await modale.getByRole('button', { name: 'Annuler' }).click();
});
