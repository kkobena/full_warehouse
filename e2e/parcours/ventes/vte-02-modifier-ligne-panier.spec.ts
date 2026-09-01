import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerPanierVide } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Corriger une quantité au comptoir doit coûter un geste, pas une reprise de la vente. Les
 * cellules de quantité du panier sont éditables AU CLIC : rien ne l'annonce à l'écran, ce qui
 * en fait exactement le genre de détail qu'un manuel doit montrer. Et la correction n'est
 * prise en compte qu'à la touche ENTRÉE — sans elle, la valeur reste dans un champ, pas dans
 * la vente. La troisième étape montre l'autre correction possible : retirer la ligne.
 *
 * Parcours ÉCRIVANT dans la base : la vente en cours disparaît avec sa dernière ligne.
 */
scenario('VTE-02', async ({ etape, page }) => {
  const produit = 'PARACETAMOL 1G';
  const ligne = page.locator('tbody tr').filter({ visible: true }).first();

  // Mise en place hors étapes : le scénario commence panier servi, ce que VTE-01 couvre déjà.
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await page.locator('#produitbox').fill(produit);
  // Attendre la suggestion avant de cliquer : le champ quantité reste DÉSACTIVÉ tant
  // qu'aucun produit n'est retenu, et un clic prématuré sur une liste vide ne le débloque pas.
  await expect(page.locator('.ng-option').first()).toContainText(produit);
  await page.locator('.ng-option').first().click();
  await ajouterAuPanier(page, '2');
  await expect(ligne).toContainText(produit);

  await etape(1, async () => {
    // « Sélectionner la ligne » se traduit ici par cliquer la cellule à corriger : c'est
    // elle, et non la ligne entière, qui bascule en saisie.
    // C'est la CELLULE éditable qui porte le clic, pas la cellule de tableau qui la
    // contient : `app-editable-cell` bascule sur son propre hôte.
    await ligne.locator('app-editable-cell').first().click();
    await expect(ligne.locator('input[type="number"]').first()).toBeVisible();
  });

  await etape(2, async () => {
    const champ = ligne.locator('input[type="number"]').first();
    await champ.fill('5');
    await champ.press('Enter');
    // Le total de la ligne suit la quantité — 5 × 1 085. C'est lui qui prouve la prise en
    // compte, la quantité seule pouvant n'être qu'affichée dans un champ non validé.
    await expect(ligne).toContainText('5');
    await expect(ligne).toContainText('5 425');
  });

  await etape(3, async () => {
    // L'autre moitié de la fiche : retirer la ligne. Le bouton est porté par la ligne
    // elle-même, et la confirmation est systématique — c'est elle qui distingue le geste
    // d'une fausse manœuvre. La demande d'autorisation superviseur, qui s'intercalerait
    // ici pour un compte dépourvu du privilège, fait l'objet de VTE-46 : le compte joué
    // ici le détient, la ligne part donc sans un mot de plus.
    await ligne.getByRole('button', { name: 'Supprimer' }).click();
    const confirmation = page.locator('.modal-content');
    await expect(confirmation).toContainText(/Voulez-vous supprimer/i);
    await confirmation.getByRole('button', { name: 'Oui' }).click();
    // La suppression de la dernière ligne rend l'écran au client suivant : le parcours
    // n'a donc rien à nettoyer derrière lui.
    await expect(page.locator('#main-content')).toContainText(/Aucun produit dans la vente|Panier vide|Ajoutez des produits/i);
  });
});
