import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un chèque n'est pas encaissé au moment où on le reçoit : il peut revenir impayé des jours
 * plus tard, et il faut alors savoir de quelle banque il venait et sous quel numéro. Le même
 * raisonnement vaut, en plus souple, pour un virement.
 *
 * L'écran l'impose plutôt qu'il ne le suggère : choisir chèque ou virement fait apparaître le
 * bloc bancaire, le nom de la banque est exigé dans les deux cas, et le numéro en plus pour un
 * chèque — sans quoi la validation reste fermée.
 *
 * Parcours en LECTURE : il montre l'exigence sans encaisser.
 */
scenario('CLI-19', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const panneau = page.locator('.detail-column');
  const formulaire = page.locator('app-reglement-differe-form');

  await etape(1, async () => {
    await page.goto('/differes');
    await lignes.first().click();
    await panneau.getByRole('tab', { name: /Régler/ }).click();
    await expect(formulaire).toBeVisible();
  });

  await etape(2, async () => {
    // Le sélecteur de mode n'a pas d'identifiant : on le désigne par le formulaire qui le
    // porte, seul ng-select de ce panneau.
    await formulaire.locator('ng-select').click();
    await page.getByRole('option', { name: /Ch[èe]que/i }).first().click();
    await expect(formulaire).toContainText('Informations bancaires');
  });

  await etape(3, async () => {
    // Deux champs obligatoires pour un chèque, contre un seul pour un virement : le numéro
    // est ce qui permet de retrouver la coupure en cas de rejet.
    await expect(formulaire.getByPlaceholder('Nom de la banque')).toBeVisible();
    await expect(formulaire.getByPlaceholder('Numéro de chèque')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Valider' })).toBeDisabled();
  });

  await etape(4, async () => {
    await formulaire.getByPlaceholder('Nom de la banque').fill('SGBCI');
    await formulaire.getByPlaceholder('Numéro de chèque').fill('4417820');
    await formulaire.getByPlaceholder('Bénéficiaire').fill('PHARMACIE DU CENTRE');
    // Renseignées, les informations bancaires rouvrent la validation.
    await expect(page.getByRole('button', { name: 'Valider' })).toBeEnabled();
  });
});
