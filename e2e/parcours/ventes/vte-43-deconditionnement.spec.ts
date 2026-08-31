import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le client veut deux sachets, pas la boîte de trente. L'officine vend à l'unité — mais le
 * stock, lui, est en boîtes tant que personne ne l'a ouverte.
 *
 * Plutôt que de refuser, l'écran propose le DÉCONDITIONNEMENT : une boîte est ouverte, ses
 * unités entrent en stock, et la vente reprend là où elle s'était arrêtée. Le mouvement est
 * enregistré des deux côtés — une boîte en moins, N unités en plus — pour que l'inventaire
 * reste juste.
 *
 * Parcours en LECTURE : il décline, aucune boîte n'est ouverte.
 */
scenario('VTE-43', async ({ etape, page }) => {
  // Le sirop se vend à l'unité (DÉTAIL, stock 0) et se stocke en boîtes (BOÎTE, 23 en rayon).
  const produit = 'PARACETAMOL SIROP 30 UNITES DETAIL';
  const modale = page.locator('.modal-content');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await chercherProduit(page, produit);
    // Le détail est à zéro : c'est le conditionnement parent qui porte le stock.
    await expect(page.locator('#main-content')).toContainText(/Rayon\s*:\s*0/);
    await ajouterAuPanier(page, '2');
  });

  await etape(2, async () => {
    // La question est posée en une phrase, sans jargon : ouvrir une boîte, ou pas.
    await expect(modale).toContainText('Déconditionnement nécessaire');
    await expect(modale).toContainText('Voulez-vous déconditionner');
    await expect(modale.getByRole('button', { name: 'Oui' })).toBeVisible();
  });

  // ── Remise en état : on décline, aucune boîte n'est ouverte. ────────────────────────────
  await modale.getByRole('button', { name: 'Non' }).click();
  await expect(modale).toBeHidden();
});
