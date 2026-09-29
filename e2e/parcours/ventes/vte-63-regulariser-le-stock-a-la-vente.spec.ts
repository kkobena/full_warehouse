import { expect } from '@playwright/test';
import {
  ajouterAuPanier,
  assurerCaisseOuverte,
  assurerPanierVide,
  chercherProduit,
  creerProduitJetable,
  lireStockProduit,
  payerEnEspeces,
} from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Les boîtes sont en rayon, la machine les ignore : réception mal saisie, retour oublié. Le
 * client repart servi — rien n'est dû à personne, donc ni avoir ni client à identifier. C'est
 * le stock informatique qui a tort, et l'encaissement le corrige par un ajustement d'entrée
 * motivé « Régularisation constatée à la vente ».
 *
 * Un produit neuf, créé à stock nul et sans réserve, rend le parcours indépendant du jeu de
 * démonstration : 2 boîtes vendues, 2 régularisées, stock final à 0.
 *
 * Parcours ÉCRIVANT dans la base : un produit, une vente et un ajustement.
 */
scenario('VTE-63', async ({ etape, page }) => {
  const suffixe = Date.now().toString().slice(-6);
  const produit = `REGULARISATION VENTE ${suffixe}`;
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await creerProduitJetable(page, produit, `79${suffixe}`);
  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');
    // Le serveur refuse, l'écran demande POURQUOI plutôt que « voulez-vous continuer ».
    await expect(modale).toContainText('Stock insuffisant');
  });

  await etape(2, async () => {
    await modale.getByRole('button', { name: /la machine se trompe/ }).click();
    await expect(modale).toBeHidden();
    await expect(lignes.filter({ hasText: produit })).toHaveCount(1);
  });

  await etape(3, async () => {
    // Aucun client demandé : sans avoir, c'est une vente comptant ordinaire.
    const recherche = page.locator('#produitbox');
    await recherche.click();
    await recherche.press('Enter');
    await payerEnEspeces(page, '5000');
    await page.getByRole('button', { name: 'Finaliser' }).click();
    await expect(lignes.filter({ hasText: produit })).toHaveCount(0);
  });

  await etape(4, async () => {
    // L'encaissement a écrit l'ajustement : il figure dans l'historique du jour.
    await page.goto('/features-ajustement');
    await expect(page.locator('app-data-table')).toContainText(/Vente\s+\S+/);
  });

  // 0 en machine + 2 régularisées − 2 vendues : la machine dit à nouveau vrai.
  expect(await lireStockProduit(page, produit)).toBe(0);
});
