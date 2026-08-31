import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'envers de RFD-03 : ce qui n'est porté par aucune fiche s'efface sans détour.
 *
 * Le cas se présente après une reprise de données — le logiciel précédent avait ses propres
 * classements, dont la moitié ne sert plus — ou après une famille créée par erreur. La laisser
 * traîner encombre chaque liste déroulante de chaque fiche produit, indéfiniment.
 *
 * Parcours ÉCRIVANT, et propre : il crée une famille de test, la supprime aussitôt, et rend
 * le référentiel exactement dans l'état où il l'a trouvé. C'est la seule façon honnête de
 * montrer une suppression qui aboutit — toutes les familles du jeu de démonstration portent
 * des produits.
 */
scenario('RFD-04', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modal = page.locator('.modal-content:visible');
  const libelle = 'FAMILLE PROVISOIRE A SUPPRIMER';

  await etape(1, async () => {
    await page.goto('/famille-produit');
    await page.getByRole('button', { name: 'Nouveau' }).click();
    await modal.locator('#libelle').fill(libelle);
    await modal.locator('#code').fill('9990');
    await choisirDansSelect(page, 'categorieId', 'Parfumerie');
    await modal.getByRole('button', { name: 'Enregistrer' }).click();
    // La modal se referme sur la réponse du serveur : agir avant laisse son fond capter les
    // clics, et le geste suivant expire sans que rien ne le dise.
    await expect(page.locator('.modal-content')).toHaveCount(0, { timeout: 15000 });

    // Créée, donc listée — et sans le moindre produit rattaché.
    // La recherche se déclenche à ENTRÉE, pas à la frappe : sans elle, la liste reste
    // affichée telle quelle et l'on croit la création perdue.
    const recherche = page.getByPlaceholder('Taper pour rechercher');
    await recherche.fill('PROVISOIRE');
    await recherche.press('Enter');
    await expect(lignes.first()).toContainText(libelle, { timeout: 15000 });
  });

  await etape(2, async () => {
    await lignes.first().getByRole('button', { name: 'Supprimer' }).click();
    await page.locator('.modal-content:visible').getByRole('button', { name: 'Oui' }).click();

    // Rien ne s'y opposait : la famille disparaît de la liste.
    await expect(page.locator('#main-content')).not.toContainText(libelle, { timeout: 15000 });
  });
});
