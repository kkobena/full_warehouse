import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un paiement par carte, chèque ou virement laisse une trace ailleurs qu'en caisse : sur un
 * relevé, un bordereau, un talon. La vente doit porter la RÉFÉRENCE qui permettra de les
 * rapprocher — sinon, en cas de réclamation ou de contrôle, plus rien ne relie l'encaissement
 * à la vente.
 *
 * Le mode se change sur la ligne de règlement elle-même : un bouton dédié remplace le mode en
 * place, sans défaire le panier.
 *
 * Le refus sans référence a dû être RÉTABLI : le contrôle existait, mais seulement sur le
 * chemin clavier ; cliquer « Finaliser » l'évitait, et la vente passait sans preuve. Le
 * message d'erreur, lui, n'était affiché sur aucun des deux écrans qui le déclenchaient.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre une vente réglée par carte.
 */
scenario('VTE-56', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '2');
  await expect(lignes.first()).toContainText(produit);

  await etape(1, async () => {
    // Le bouton de REMPLACEMENT, et non d'ajout : sur une ligne unique, on ne descend jamais
    // à zéro mode de règlement — on échange celui qui est là.
    await page.getByRole('button', { name: 'Changer de mode de règlement' }).click();
    // La liste reste OUVERTE à la fin de l'étape : changer de mode, c'est d'abord VOIR ce
    // qu'on peut choisir. L'image doit donc montrer les modes offerts, pas leur résultat.
    const menu = page.locator('.payment-mode-menu').locator('visible=true');
    await expect(menu).toBeVisible();
    await expect(menu).toContainText('CARTE BANCAIRE');
    await expect(menu).toContainText('VIREMENT');
    await expect(menu).toContainText('CHEQUE');
  });

  await etape(2, async () => {
    await page.locator('.payment-mode-menu').locator('visible=true').getByRole('button', { name: 'CARTE BANCAIRE' }).click();

    // Le choix d'un mode bancaire ouvre le bloc des informations complémentaires : c'est là
    // que se consigne la preuve du paiement.
    await expect(contenu).toContainText('CARTE BANCAIRE');
    await expect(contenu).toContainText('Informations complémentaires');
    await expect(page.getByPlaceholder('Référence bancaire')).toBeVisible();

    // Valider sans référence : refusé, et le panier reste entier. Rien n'est perdu, rien
    // n'est enregistré.
    await page.getByRole('button', { name: 'Finaliser' }).click();
    await expect(page.locator('[role="alert"], .toast').filter({ hasText: /Référence bancaire requise/i }).first()).toBeVisible();
    await expect(lignes.first()).toContainText(produit);
  });

  await etape(3, async () => {
    // La référence est obligatoire ; la banque et le lieu ne le sont pas, mais ce sont eux
    // qui rendront le rapprochement rapide six mois plus tard.
    await page.getByPlaceholder('Référence bancaire').fill('TPE-' + Date.now().toString().slice(-6));
    await page.getByPlaceholder('Nom de la banque').fill('SGBCI');
    await page.getByPlaceholder('Lieu').fill('Agence Plateau');
  });

  await etape(4, async () => {
    await page.getByRole('button', { name: 'Finaliser' }).click();
    await expect(contenu).toContainText(/Panier vide|Ajoutez des produits/i);
  });
});
