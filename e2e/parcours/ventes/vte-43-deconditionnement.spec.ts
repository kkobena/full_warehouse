import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le client veut deux gélules, pas la boîte. L'officine vend à l'unité — mais le
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
  // La gélule se vend à l'unité (DÉTAIL) et se stocke en boîtes (33 en rayon). L'unité n'a pas de stock
  // propre tant qu'une boîte n'a pas été ouverte — et STK-20 en ouvre une avant ce parcours.
  const produit = 'ITRACONAZOLE TEVA 100 mg, gélule - UNITE';
  const modale = page.locator('.modal-content');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await chercherProduit(page, 'ITRACONAZOLE TEVA', produit);
    // On en demande deux de plus que ce que le détail porte : c'est le conditionnement parent
    // qui fournit le reste, après déconditionnement.
    await expect(page.locator('#main-content')).toContainText(/Rayon\s*:\s*\d+/);
    const enRayon = Number((await page.locator('#main-content').innerText()).match(/Rayon\s*:\s*(\d+)/)?.[1] ?? '0');
    await ajouterAuPanier(page, String(enRayon + 2));
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
