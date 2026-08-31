import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Réceptionner un carton, c'est traiter des lignes l'une après l'autre, les mains prises par
 * les boîtes. Le mode SÉQUENTIEL est fait pour cela : une ligne à l'écran, le curseur déjà
 * dans la quantité reçue, et la touche suivante qui enchaîne. On ne cherche pas sa ligne dans
 * un tableau, elle vient à soi.
 *
 * L'avancement est compté en permanence — traitées sur total, taux de service — de sorte
 * qu'on sait à tout moment ce qui reste, y compris après une interruption au comptoir.
 *
 * Parcours en LECTURE : il saisit une quantité sur la ligne courante sans finaliser la
 * réception, laquelle entrerait tout le bon en stock.
 */
scenario('ACH-66', async ({ etape, page }) => {
  const ecran = page.locator('app-reception-sequential');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    const liste = page.locator('app-list-bons');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    // Un bon EN ATTENTE DE SAISIE : un bon clôturé n'a plus rien à saisir.
    await liste.locator('tbody tr').filter({ hasNotText: 'Clôturé' }).first().dblclick();
    await expect(ecran).toBeVisible();
  });

  await etape(2, async () => {
    // La ligne courante porte sa quantité commandée, son prix, et le champ « Reçu » attend
    // la saisie — c'est le seul geste à faire pour la ligne conforme.
    await expect(page.locator('#rh-qty')).toBeVisible();
    await expect(page.locator('#rh-ug')).toBeVisible();
    await page.locator('#rh-qty').fill('1');
    await expect(page.locator('#rh-qty')).toHaveValue('1');
  });

  etape.horsPortee(
    3,
    'la fenêtre de clôture ne s’ouvre qu’une fois les quatorze lignes traitées, et la clôture ' +
      'ferait entrer tout le bon en stock — ce dont les parcours de stock et de rapports ne ' +
      'peuvent pas hériter en cours de campagne.',
  );
});
