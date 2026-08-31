import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Supprimer et mettre en veille répondent à deux situations différentes. Une rupture longue,
 * un litige avec le laboratoire, un produit qu'on ne veut plus voir proposé au comptoir : on
 * le SUSPEND. La fiche reste, son historique aussi, et le geste se défait.
 *
 * Ce qui rend le parcours utile à montrer, c'est l'effet côté vente : le produit suspendu ne
 * se cherche plus au comptoir. Ce n'est pas un bouton grisé, c'est une absence — et c'est
 * exactement ce que le pharmacien veut obtenir.
 *
 * Parcours ÉCRIVANT dans la base : il réactive le produit qu'il a suspendu.
 */
scenario('REF-39', async ({ etape, page }) => {
  const recherche = 'ARNICA';
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const ligne = () => lignes.first();
  let produit = '';

  const chercherAuCatalogue = async (etat: 'Produits actifs' | 'Produits désactivés' | 'Tous'): Promise<void> => {
    await choisirDansSelect(page, 'produitFiltreEtat', etat);
    const champ = page.getByPlaceholder(/Rechercher \(CIP/);
    await champ.fill('');
    await champ.fill(recherche);
    await page.keyboard.press('Enter');
  };

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue('Produits actifs');
    // Le libellé est LU dans la liste plutôt que codé ici : le catalogue de démonstration est
    // construit par combinaison (souche, dilution, conditionnement), et figer un libellé
    // exact reviendrait à parier sur cette combinatoire.
    await expect(ligne()).toContainText(recherche);
    produit = ((await ligne().locator('td').nth(2).innerText()) || '').trim();
    await expect(ligne()).toContainText('Actif');
  });

  await etape(2, async () => {
    await ligne().getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Mettre en veille' }).click();
    // La confirmation dit ce que la veille produit : le produit sort des ventes ET des
    // commandes. C'est le seul endroit où l'application l'énonce.
    const confirmation = page.locator('.modal-content');
    await expect(confirmation).toContainText(/masqué des ventes et des commandes/i);
    await confirmation.getByRole('button', { name: /Oui|Confirmer/ }).click();
    await expect(confirmation).toBeHidden();
  });

  await etape(3, async () => {
    // Côté comptoir : le produit ne se trouve plus. Il n'est pas proposé puis refusé, il
    // n'est pas proposé du tout — la recherche de vente ne voit que les produits actifs.
    await page.goto('/sales-home');
    const champ = page.locator('#produitbox');
    await champ.click();
    await champ.pressSequentially(produit.slice(0, 12), { delay: 60 });
    await expect(page.locator('.ng-option', { hasText: produit })).toHaveCount(0);
  });

  await etape(4, async () => {
    await page.goto('/produits');
    // Il faut le filtre « désactivés » pour le retrouver : c'est là que vivent les produits
    // en veille, et c'est ce qui les rend récupérables.
    await chercherAuCatalogue('Produits désactivés');
    await expect(ligne()).toContainText('Veille');
    await ligne().getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Réactiver' }).click();
    const confirmation = page.locator('.modal-content');
    await expect(confirmation).toBeVisible();
    await confirmation.getByRole('button', { name: /Oui|Confirmer/ }).click();
    await expect(confirmation).toBeHidden();
    await chercherAuCatalogue('Produits actifs');
    await expect(ligne()).toContainText('Actif');
  });
});
