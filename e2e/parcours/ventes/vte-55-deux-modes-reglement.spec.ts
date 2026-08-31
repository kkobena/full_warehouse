import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, payerEnEspeces } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le pavé de règlement fait deux choses que le caissier n'a plus à calculer :
 *
 *  1. le SOLDE. Dès qu'un montant partiel est saisi, un second mode peut être ajouté ; il
 *     prend automatiquement ce qui reste. Modifier ensuite l'un des deux montants recale
 *     l'autre — les deux lignes couvrent toujours exactement le dû, jamais plus ;
 *  2. la MONNAIE. Sur un règlement en espèces seul, le montant REMIS par le client est
 *     conservé à part du montant encaissé, et la différence s'affiche.
 *
 * Les deux mécanismes s'excluent, et c'est logique : en paiement mixte les montants sont
 * ajustés au dû, il n'y a donc rien à rendre. C'est pourquoi le parcours revient au tout
 * espèces pour montrer la monnaie.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre une vente comptant.
 */
scenario('VTE-55', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const contenu = page.locator('#main-content');
  const montants = page.locator('.payment-amount-field input');
  const cash = page.locator('#CASH');

  // Saisir un montant dans un champ DÉJÀ renseigné : `fill()` ne vide pas ces champs
  // formatés — la valeur se concatène à l'ancienne et l'écran affiche des centaines de
  // millions. On efface donc explicitement avant de frapper.
  const saisirMontant = async (montant: string): Promise<void> => {
    await payerEnEspeces(page, montant);
  };

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');
    await expect(contenu).toContainText('38 620');
    await expect(contenu).toContainText('À ENCAISSER');
  });

  await etape(2, async () => {
    // 20 000 sur 38 620 : le règlement est incomplet, et c'est cet écart qui fait apparaître
    // le bouton d'ajout d'un second mode.
    await saisirMontant('20000');
    await expect(page.getByRole('button', { name: 'Ajouter un mode de règlement' })).toBeVisible();
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: 'Ajouter un mode de règlement' }).click();
    // La LISTE des modes reste OUVERTE à la fin de l'étape : c'est le choix offert au
    // caissier qu'il faut photographier — mobile money, carte, virement, chèque. La refermer
    // avant la capture donnerait une image d'où le choix aurait disparu.
    //
    // Deux menus coexistent dans le DOM — celui d'ajout et celui de remplacement ; seul
    // l'ouvert est visible.
    const menu = page.locator('.payment-mode-menu').locator('visible=true');
    await expect(menu).toBeVisible();
    await expect(menu).toContainText('ORANGE');
    await expect(menu).toContainText('CARTE BANCAIRE');
    await expect(menu).toContainText('CHEQUE');
  });

  await etape(4, async () => {
    // Le second mode prend le solde sans qu'on l'ait calculé : 38 620 − 20 000 = 18 620.
    await page.locator('.payment-mode-menu').locator('visible=true').getByRole('button', { name: 'ORANGE' }).click();
    await expect(montants).toHaveCount(2);
    await expect(montants.nth(1)).toHaveValue(/18\s*620/);

    // Retour au tout espèces pour la monnaie : le client remet 40 000 pour 38 620.
    // Chaque ligne porte son bouton de suppression ; c'est celui d'ORANGE — la seconde —
    // qu'il faut viser pour revenir au tout espèces.
    await page.getByRole('button', { name: 'Supprimer ce mode de règlement' }).nth(1).click();
    await expect(montants).toHaveCount(1);
    await saisirMontant('40000');

    // « Monnaie » est mis en capitales par la feuille de style : viser le texte du DOM.
    await expect(contenu).toContainText('Monnaie');
    await expect(contenu).toContainText('1 380');

    await page.getByRole('button', { name: 'Finaliser' }).click();
    await expect(contenu).toContainText(/Panier vide|Aucun produit dans la vente/i);
  });
});
