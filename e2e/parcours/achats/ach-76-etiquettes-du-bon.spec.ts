import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une livraison rangée est une livraison étiquetée. Plutôt que de rechercher chaque article
 * et d'imprimer son étiquette une à une, l'impression part DU BON : elle reprend les produits
 * réceptionnés et leurs quantités, dans l'ordre du bon.
 *
 * Trois choix s'y font, et chacun répond à une question différente :
 *
 *  * le TYPE — l'étiquette standard porte le code article seul ; l'étiquette « lot » y ajoute
 *    le numéro de lot et la péremption dans un GS1 DataMatrix, et sort une étiquette par lot
 *    reçu. C'est la réponse au marquage des fabricants que les grossistes ne reprennent pas ;
 *  * le FORMAT — une planche du commerce a sa propre grille, et le format choisi décide de la
 *    capacité annoncée ;
 *  * la POSITION DE DÉPART — les planches sont rarement neuves, on en garde une entamée dans
 *    le tiroir. Commencer à la case 12 évite d'en gâcher une par livraison. La borne vient du
 *    format : elle n'est plus la même sur une planche de 24 que sur une planche de 65.
 *
 * Parcours en LECTURE : il ouvre la demande d'impression sans la lancer, l'édition du document
 * n'ayant rien à montrer de plus qu'un fichier qui se télécharge.
 */
scenario('ACH-76', async ({ etape, page }) => {
  const liste = page.locator('app-list-bons');
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('tab', { name: /^Réceptions/ }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    // Un bon CLÔTURÉ : les étiquettes s'impriment sur ce qui est entré en stock, pas sur
    // ce qui reste à compter.
    const clos = liste.locator('tbody tr').filter({ hasText: 'Clôturé' }).first();
    await clos.getByRole('button', { name: 'Actions' }).click();
    await expect(page.getByRole('button', { name: 'Étiquettes' })).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Étiquettes' }).click();
    await expect(modale).toContainText(/ETIQUETTES DU BON DE LIVRAISON/i);
    await expect(modale).toContainText(/Position de départ/i);
  });

  await etape(3, async () => {
    // Le type « Lot » n'est pas une variante de l'étiquette standard mais une seconde
    // étiquette : le texte sous le sélecteur dit ce que chacune encode, parce que la
    // différence ne se voit pas avant d'avoir imprimé.
    await modale.getByRole('button', { name: 'Lot (DataMatrix)' }).click();
    await expect(modale).toContainText(/une étiquette par lot reçu/i);
    await expect(modale).toContainText(/DataMatrix portant lot et péremption/i);
  });

  await etape(4, async () => {
    // Une planche de 24 : le format ne change pas seulement la grille imprimée, il change
    // la capacité annoncée — donc la borne de l'étape suivante.
    await choisirDansSelect(page, 'etiquette-format', '24 étiquettes');
    await expect(modale).toContainText('Avery L7159');
    await expect(modale).toContainText(/24 étiquettes par planche/i);
    await expect(modale).toContainText(/de 1 à 24/i);
  });

  await etape(5, async () => {
    // Reprendre une planche entamée : la première étiquette ira dans la case 12.
    await modale.locator('#etiquette-start').fill('12');
    await expect(modale.locator('#etiquette-start')).toHaveValue('12');
    // Hors des bornes de la planche CHOISIE, l'impression est refusée — et c'est bien 24,
    // non 65 : la borne suit le format, elle n'est plus écrite en dur.
    await modale.locator('#etiquette-start').fill('30');
    await expect(modale).toContainText(/comprise entre 1 et 24/i);
    await expect(modale.getByRole('button', { name: 'Enregistrer' })).toBeDisabled();
    await modale.locator('#etiquette-start').fill('12');
  });
});
