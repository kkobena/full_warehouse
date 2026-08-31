import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';

/**
 * Ouvre un sélecteur par l'identifiant de son champ et retient la première option.
 *
 * Les emplacements et les rayons portent des libellés propres à chaque officine : les nommer
 * ici rendrait le parcours dépendant du jeu de données. Ce qu'il démontre, c'est que le
 * formulaire réclame ces précisions — pas laquelle on retient.
 */
async function choisirPremiereOption(page: import('@playwright/test').Page, inputId: string): Promise<void> {
  const champ = page.locator(`#${inputId}`);
  await champ.focus();
  await champ.press('ArrowDown');
  await page.locator('.ng-option').first().click();
}
import { scenario } from '../../src/scenario';

/**
 * Un inventaire général ferme l'officine une journée. Un inventaire de rayon se compte en une
 * heure, entre deux clients. C'est pourquoi le périmètre se choisit à la création, et qu'il
 * commande tout le reste : les produits à compter, la durée, et le moment où on le fait.
 *
 * Les périmètres offerts vont du magasin entier à une famille thérapeutique, en passant par
 * un emplacement, un rayon, ou les produits proches de la péremption — ce dernier étant moins
 * un inventaire qu'une chasse aux dates.
 *
 * La description est obligatoire : six mois plus tard, « Inventaire du 30/08 » ne dit rien,
 * quand « Rayon dermato — après la casse du 28 » dit tout.
 *
 * Parcours ÉCRIVANT dans la base : il crée un inventaire, que la restauration de l'instantané
 * efface avant la campagne suivante.
 */
scenario('STK-04', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');
  const description = `Parcours STK-04 — rayon ${Date.now().toString().slice(-5)}`;

  await etape(1, async () => {
    await page.goto('/inventaire');
    await page.getByRole('button', { name: 'Nouveau' }).click();
    await expect(modale).toBeVisible();
    await expect(modale).toContainText(/Sélectionner un type/);
  });

  await etape(2, async () => {
    // Le périmètre : ici un rayon, l'inventaire qui se compte sans fermer l'officine.
    await choisirDansSelect(page, 'inventoryCategory', 'rayon');
    await expect(modale).toContainText(/Inventaire d.un rayon/);
    // Le choix du rayon n'apparaît qu'après celui du type : un formulaire qui ne demande que
    // ce que le périmètre retenu exige.
    await expect(modale.getByText('Rayon', { exact: false }).first()).toBeVisible();
  });

  await etape(3, async () => {
    // Ce périmètre demande DEUX précisions : dans quel emplacement, et quel rayon —
    // le même rayon existe des deux côtés, en rayon et en réserve.
    await choisirPremiereOption(page, 'storage');
    await choisirPremiereOption(page, 'rayon');
    await modale.locator('#description').fill(description);
    await modale.getByRole('button', { name: 'Créer' }).click();
    // L'inventaire créé enchaîne aussitôt sur l'impression de la feuille de comptage : on
    // compte sur papier, dans les rayons, avant de saisir. Le parcours s'arrête là — la
    // feuille se télécharge, et l'inventaire attend dans l'onglet « En cours ».
    await expect(page.locator('.modal-content').last()).toContainText(/[Ee]xport|[Ii]mprimer|[Ff]euille/);
  });
});
