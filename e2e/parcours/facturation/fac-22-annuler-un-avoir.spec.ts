import { expect } from '@playwright/test';
import { ouvrirMenuActions, ouvrirOnglet, rechercher, trouverLigneAvecAction } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * On renonce à un avoir quand le rejet qui l'a motivé se révèle infondé, ou quand il a été
 * saisi deux fois. L'annulation est ouverte au brouillon comme à l'émis — mais jamais après
 * imputation : un avoir imputé a déjà réduit un solde, l'annuler rouvrirait une créance que
 * le tiers payant croit soldée. Le serveur le refuse, pas seulement l'écran.
 *
 * Le motif est obligatoire, et le bouton de confirmation reste inerte tant qu'il est vide :
 * un avoir annulé sans raison est un trou dans la piste d'audit.
 *
 * Parcours en LECTURE jusqu'à la confirmation : il montre l'exigence du motif sans consommer
 * l'un des avoirs annulables du jeu de démonstration.
 */
scenario('FAC-22', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Avoirs/);
    await rechercher(page);
    // « Annuler l'avoir » n'est proposé, dans le menu d'actions de la ligne, que sur un brouillon ou
    // un avoir émis : on cherche donc la première ligne dont le menu l'offre.
    const annulable = await trouverLigneAvecAction(page, lignes, "Annuler l'avoir");
    await expect(annulable).toBeVisible();
  });

  await etape(2, async () => {
    const annulable = await trouverLigneAvecAction(page, lignes, "Annuler l'avoir");
    const menu = await ouvrirMenuActions(page, annulable);
    await menu.getByRole('button', { name: "Annuler l'avoir" }).click();
    await expect(modal).toBeVisible();

    const confirmer = modal.getByRole('button', { name: 'Confirmer' });
    await expect(confirmer).toBeDisabled();

    await modal.locator('textarea').first().fill("Rejet infondé : l'organisme a finalement pris la ligne en charge");
    await expect(confirmer).toBeEnabled();
  });
});
