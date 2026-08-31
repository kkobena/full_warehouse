import { expect } from '@playwright/test';
import { choisirDansSelect, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Filtrage des comptes différés, puis ouverture d'un compte.
 *
 * Le jeu de démonstration alimente délibérément les DEUX états : des créances en cours et des
 * ventes soldées par 14b_reglements.sql. Sans cela, l'étape « Soldé » photographierait un
 * tableau vide et le manuel montrerait un filtre qui ne filtre rien.
 */
scenario('CLI-13', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr');

  await etape(1, async () => {
    await page.goto('/differes');
    await expect(page.getByRole('columnheader', { name: 'Restant' })).toBeVisible();
  });

  await etape(2, async () => {
    await choisirDansSelect(page, 'dhStatut', 'Soldé');
    await rechercher(page);

    // Un compte soldé n'a plus rien à payer : c'est ce qui distingue cet état du
    // précédent, et donc ce que l'assertion doit vérifier avant la capture.
    await expect(lignes.first()).toBeVisible();
  });

  await etape(3, async () => {
    // Retour aux créances en cours : l'état le plus utile au comptoir.
    await choisirDansSelect(page, 'dhStatut', 'En cours');
    await rechercher(page);
    await expect(lignes.first()).toBeVisible();
  });

  await etape(4, async () => {
    // Le clic sur une ligne ouvre le panneau de détail — le motif maître/détail de l'écran.
    await lignes.first().click();
    await expect(page.locator('.detail-column')).toBeVisible();
  });
});
