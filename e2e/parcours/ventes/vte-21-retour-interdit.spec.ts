import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, ouvrirJournalDuJour, payerEnEspeces } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Certains produits ne reviennent jamais en rayon, quel qu'en soit le motif : les stupéfiants
 * et les psychotropes. La règle n'est pas commerciale mais réglementaire — une fois sortie de
 * l'officine, la boîte suit le circuit de destruction, pas celui du stock.
 *
 * L'écran le dit avant toute saisie : la ligne porte son statut légal, le compteur de
 * quantité disparaît, et la mention « Interdit » prend sa place. Il n'y a donc rien à
 * refuser plus tard — le geste est bloqué dès l'ouverture.
 *
 * Parcours ÉCRIVANT dans la base : il vend un stupéfiant pour pouvoir montrer le refus, puis
 * ferme la fenêtre sans rien retourner.
 */
scenario('VTE-21', async ({ etape, page }) => {
  // TRAMADOL est classé STUPÉFIANT dans le jeu de démonstration, comme en officine.
  const produit = 'TRAMADOL 500MG';
  const contenu = page.locator('#main-content');
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  // La vente à reprendre est désignée par son MONTANT, et non par sa position : le journal
  // du jour contient aussi les ventes du jeu de démonstration et celles des autres parcours,
  // dont l'ordre ne se prédit pas. Les ventes annulées sont écartées — leur menu n'offre pas
  // de retour, et pour cause.
  const venteRetournable = lignes.filter({ hasText: '5 270' }).filter({ hasNotText: 'Annulée' }).first();

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  // ── Mise en scène : la vente du produit soumis à restriction. ───────────────────────────
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '1');
  await payerEnEspeces(page, '50000');
  await page.getByRole('button', { name: 'Finaliser' }).click();
  await expect(contenu).toContainText(/Panier vide|Ajoutez des produits/i);

  await etape(1, async () => {
    await ouvrirJournalDuJour(page);
    await expect(lignes.first()).toBeVisible();
    await venteRetournable.getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Retour client' }).click();

    // La ligne est là, mais barrée d'un statut : STUPEFIANTS. Pas de compteur, pas de
    // montant — « Interdit » à la place. Le caissier sait tout de suite quoi répondre au
    // client, et vers quel circuit l'orienter.
    await expect(modale).toContainText(produit);
    await expect(modale).toContainText('STUPEFIANTS');
    await expect(modale).toContainText('Interdit');
    await expect(modale.getByRole('button', { name: 'Augmenter' })).toHaveCount(0);
  });

  // ── Remise en état : rien n'est validé. ─────────────────────────────────────────────────
  await modale.getByRole('button', { name: 'Annuler' }).click();
});
