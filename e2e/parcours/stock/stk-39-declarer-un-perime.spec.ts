import { expect } from '@playwright/test';
import { chercherDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Tous les périmés ne sont pas connus de l'application : une boîte oubliée au fond d'un
 * tiroir, un produit dont le lot n'a jamais été saisi, une remise en rayon qui traîne. Le
 * comptage physique les fait apparaître, et il faut pouvoir les déclarer sans qu'un lot
 * existe déjà.
 *
 * La déclaration se construit ligne à ligne — produit, numéro de lot, date de péremption,
 * quantité constatée — et reste modifiable tant qu'elle n'est pas clôturée (STK-40). Ce n'est
 * qu'à la clôture que le stock bouge et que les lignes rejoignent les lots à détruire.
 *
 * Parcours ÉCRIVANT dans la base : il ajoute une ligne à une déclaration en cours.
 */
scenario('STK-39', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const produit = 'DOLIPRANE 500MG';

  await etape(1, async () => {
    await page.goto('/gestion-peremption/edit');
    await expect(contenu).toContainText('Ajout de lots périmés');
    await chercherDansSelect(page, 'produits', produit, produit);
  });

  await etape(2, async () => {
    // Le lot et sa péremption : ce que porte l'étiquette de la boîte retrouvée. Le champ
    // n'accepte que des caractères alphanumériques — un tiret ferait rejeter la saisie
    // entière, sans message.
    await page.locator('#numLot').fill('LOTCONSTATE39');
    await expect(page.locator('#numLot')).toHaveValue('LOTCONSTATE39');
    // La date de péremption commande la suite : tant qu'elle manque, la quantité reste
    // fermée — on ne déclare pas un périmé sans dire depuis quand il l'est.
    const datePeremption = page.locator('#datePeremtion');
    await datePeremption.fill('31/12/2025');
    await datePeremption.press('Enter');
  });

  await etape(3, async () => {
    // La quantité constatée — celle qu'on a en main, pas celle que le stock annonce.
    const qte = page.locator('#quantiteSaisie');
    await qte.click();
    await qte.fill('');
    await qte.pressSequentially('2', { delay: 40 });
    await expect(qte).toHaveValue('2');
  });

  await etape(4, async () => {
    await qteValider(page);
    // La ligne rejoint la déclaration en cours ; le stock, lui, n'a pas encore bougé.
    await expect(contenu).toContainText(produit);
  });
});

/** Valide la quantité saisie — la touche Entrée fait office de bouton d'ajout. */
async function qteValider(page: import('@playwright/test').Page): Promise<void> {
  await page.locator('#quantiteSaisie').press('Enter');
}
