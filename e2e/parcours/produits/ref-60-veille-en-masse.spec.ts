import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une gamme entière retirée de la vente — un laboratoire suspendu, une série rappelée — ne se
 * traite pas fiche par fiche : on en oublie une, et c'est celle-là qui se vendra. La sélection
 * multiple du catalogue ouvre une barre d'actions qui applique le même geste à tout le lot.
 *
 * Les deux actions demandent une confirmation, et celle-ci rappelle le NOMBRE de produits
 * concernés : c'est le dernier moment où l'on peut s'apercevoir qu'on en tient plus qu'on ne
 * croyait. Une fois le traitement passé, la sélection se vide d'elle-même, pour qu'un second
 * clic ne reparte pas sur un lot déjà traité.
 *
 * Parcours ÉCRIVANT dans la base : il réactive les produits qu'il a mis en veille.
 */
scenario('REF-60', async ({ etape, page }) => {
  const recherche = 'NUX VOMICA';
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const barre = page.locator('.bulk-action-bar');
  const confirmation = page.locator('.modal-content');

  const chercherAuCatalogue = async (etat: 'Produits actifs' | 'Produits désactivés'): Promise<void> => {
    await choisirDansSelect(page, 'produitFiltreEtat', etat);
    const champ = page.getByPlaceholder(/Rechercher \(CIP/);
    await champ.fill('');
    await champ.fill(recherche);
    await page.keyboard.press('Enter');
  };

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue('Produits actifs');
    await expect(lignes.first()).toContainText(recherche);
    // Deux lignes suffisent à montrer le lot ; l'action est la même à deux ou à deux cents.
    await lignes.nth(0).getByRole('checkbox').check();
    await lignes.nth(1).getByRole('checkbox').check();
    // La barre d'actions groupées n'apparaît qu'avec une sélection, et compte ce qu'elle tient.
    await expect(barre).toContainText('2 produit(s) sélectionné(s)');
  });

  await etape(2, async () => {
    await barre.getByRole('button', { name: 'Mettre en veille' }).click();
    // La confirmation redit le nombre : le lot traité est celui qu'on a sous les yeux.
    await expect(confirmation).toContainText('2 produit(s)');
  });

  await etape(3, async () => {
    await confirmation.getByRole('button', { name: 'Oui' }).click();
    await expect(confirmation).toBeHidden();
    // La sélection s'est vidée : la barre a disparu avec elle. C'est ce qui évite d'appliquer
    // deux fois le même traitement en croyant travailler sur un nouveau lot.
    await expect(barre).toBeHidden();
    await chercherAuCatalogue('Produits désactivés');
    await expect(lignes.nth(0)).toContainText('Veille');
    await expect(lignes.nth(1)).toContainText('Veille');
  });

  // ── Remise en état : TOUS les produits de la gamme repassent en actif. On prend la
  //    sélection globale plutôt que deux lignes : une exécution précédente interrompue a pu
  //    en laisser d'autres en veille, et le catalogue doit repartir tel qu'il était. ───────
  await page.getByRole('checkbox', { name: 'Tout sélectionner' }).check();
  await barre.getByRole('button', { name: 'Réactiver' }).click();
  await expect(confirmation).toBeVisible();
  await confirmation.getByRole('button', { name: 'Oui' }).click();
  await expect(confirmation).toBeHidden();
  await expect(barre).toBeHidden();
  await chercherAuCatalogue('Produits actifs');
  await expect(lignes.first()).toContainText('Actif');
});
