import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un inventaire se compte à plusieurs et sur plusieurs heures : savoir ce qu'il reste à
 * compter est la question qu'on se pose vingt fois. L'écran y répond par un pourcentage et
 * un décompte de lignes saisies — et cette information ne vit PAS dans la liste des
 * inventaires, mais dans l'inventaire lui-même, ce qu'un manuel se doit de dire.
 */
scenario('STK-07', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/inventaire');
    // L'onglet « En cours » est celui d'arrivée : on vérifie qu'il liste bien un inventaire
    // ouvert, faute de quoi l'étape suivante n'aurait rien à ouvrir.
    // L'onglet « En cours » réunit les inventaires ouverts : ceux qui viennent d'être créés
    // comme ceux dont le comptage a commencé.
    await expect(lignes.first()).toContainText(/en cours|créé/i);
  });

  await etape(2, async () => {
    await lignes.first().getByRole('button', { name: 'Ouvrir' }).click();
    // La progression : un pourcentage ET le décompte qui l'explique. Exiger les deux évite
    // de photographier une barre à 0 % pendant le chargement des lignes.
    await expect(page.getByText(/\d+\s*\/\s*\d+ lignes saisies/)).toBeVisible();
    await expect(page.locator('#main-content')).toContainText(/\d+%/);
  });
});
