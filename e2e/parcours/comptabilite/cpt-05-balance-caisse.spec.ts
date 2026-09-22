import { expect, type Locator } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

scenario('CPT-05', async ({ etape, page }) => {
  // Période calculée et non écrite en dur : le jeu de démonstration est daté par rapport au
  // jour du chargement, une date figée cesserait de ramener des mouvements.
  const fin = new Date();
  const debut = new Date(fin);
  debut.setDate(debut.getDate() - 60);

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await ouvrirOnglet(page, /Balance/);
    await expect(page.getByText('Balance de caisse')).toBeVisible();
  });

  await etape(2, async () => {
    await saisirDate(page, 'dateDebut', debut);
    await saisirDate(page, 'dateFin', fin);
    await rechercher(page);

    // Sans période, l'écran affiche des totaux à zéro — état parfaitement valide, mais qui
    // n'illustrerait pas « consulter la balance ». L'assertion exige donc un total NON nul :
    // c'est ce qui distingue l'écran renseigné de l'écran vide, et elle attend au passage la
    // fin du calcul avant que la capture ne soit prise.
    await expect(page.getByText(/Total TTC/)).toBeVisible();
    await expect(page.locator('#main-content')).not.toContainText('Total TTC 0 FCFA');
  });

  // Une balance dont les colonnes ne se répondent pas n'est pas une balance. Deux identités
  // la tiennent, et l'écran donne tout ce qu'il faut pour les éprouver — constater qu'un
  // « Total TTC » non nul s'affiche ne disait rien de leur justesse.
  const table = page.locator('table').filter({ has: page.getByText('Brut(TTC)') }).first();
  const nombre = async (ligne: Locator, colonne: number): Promise<number> =>
    Number((await ligne.locator('td').nth(colonne).innerText()).replace(/[^\d-]/g, ''));

  const lignes = table.locator('tbody tr').filter({ visible: true });
  const nombreDeTypes = await lignes.count();
  expect(nombreDeTypes, 'une balance sans ligne ne prouve rien').toBeGreaterThan(0);

  // 1. Ligne à ligne : le net est le brut diminué de la remise.
  let sommeBrut = 0;
  let sommeRemise = 0;
  let sommeNet = 0;
  for (let rang = 0; rang < nombreDeTypes; rang++) {
    const brut = await nombre(lignes.nth(rang), 2);
    const remise = await nombre(lignes.nth(rang), 3);
    const net = await nombre(lignes.nth(rang), 4);
    expect(net, `net du type n°${rang + 1}`).toBe(brut - remise);
    sommeBrut += brut;
    sommeRemise += remise;
    sommeNet += net;
  }

  // 2. Le pied de table totalise les lignes, sans en oublier ni en inventer.
  const total = table.locator('tfoot tr').first();
  expect(await nombre(total, 2), 'total brut').toBe(sommeBrut);
  expect(await nombre(total, 3), 'total remise').toBe(sommeRemise);
  expect(await nombre(total, 4), 'total net').toBe(sommeNet);
});
