import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'historique donne la lecture d'ensemble que les écrans de saisie ne peuvent pas donner : le
 * chiffre d'affaires RÉEL cumulé face au chiffre d'affaires DÉCLARÉ cumulé, et l'écart entre
 * les deux, exprimé en montant ponctionné et en taux effectif moyen.
 *
 * Chaque ponction y figure avec son sort. Une ponction ANNULÉE reste visible — elle n'est pas
 * effacée — mais ne pèse plus sur le déclaré : c'est ce qui permet de revenir en arrière sans
 * faire disparaître la trace de la décision initiale.
 *
 * Le détail d'une ponction s'ouvre en cliquant sa ligne : on y retrouve les ventes touchées.
 */
scenario('DCA-10', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/declaration-ca');
    await ouvrirOnglet(page, 'Historique des ponctions');
    await expect(contenu).toContainText('Historique des ponctions');
  });

  await etape(2, async () => {
    // Le cumul qui donne l'écart d'ensemble, et le sort de chaque ponction.
    await expect(contenu).toContainText(/CA réel cumulé/i);
    await expect(contenu).toContainText(/CA déclaré cumulé/i);
    await expect(contenu).toContainText(/validée/i);
    await expect(contenu).toContainText(/annulée/i);
  });

  await etape(3, async () => {
    // Le taux effectif moyen : ce que l'écart représente rapporté à l'assiette.
    await expect(contenu).toContainText(/Taux effectif moyen/i);
    await expect(lignes.first()).toBeVisible();
  });
});
