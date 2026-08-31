import { expect } from '@playwright/test';
import { ouvrirOnglet, traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un planning ne vaut que par ce qu'il produit : à chaque échéance, il crée l'inventaire du
 * périmètre qui vient dans la rotation, et celui-ci apparaît dans l'onglet « En cours » comme
 * n'importe quel autre comptage.
 *
 * L'échéance est normalement automatique — c'est tout l'intérêt : personne n'a à y penser.
 * Mais on peut la provoquer, et c'est ce que fait « Exécuter maintenant » : la démonstration
 * n'attend pas une semaine, et l'officine qui rattrape un retard non plus.
 *
 * Le tableau du planning tient le compte des exécutions et la date de la dernière : c'est ce
 * qui permet de voir qu'un tournant a cessé de tourner.
 *
 * Parcours ÉCRIVANT dans la base : il déclenche une exécution, qui crée un inventaire.
 */
scenario('STK-10', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/inventaire');
    await ouvrirOnglet(page, /Tournant/);
    await expect(lignes.first()).toBeVisible();
    // Le suivi du planning : ses exécutions passées, et la prochaine échéance.
    await expect(page.locator('#main-content')).toContainText(/Prochain tournant|Plannings actifs/);
  });

  await etape(2, async () => {
    // Provoquer l'échéance plutôt que l'attendre.
    await lignes.first().getByRole('button', { name: 'Exécuter maintenant' }).click();
    await traverserConfirmations(page, { limite: 1 });
  });

  await etape(3, async () => {
    // L'inventaire généré rejoint les comptages en cours : le planning n'est qu'un
    // déclencheur, l'inventaire qu'il produit se compte comme les autres.
    await ouvrirOnglet(page, /En cours/);
    await expect(lignes.first()).toBeVisible();
  });
});
