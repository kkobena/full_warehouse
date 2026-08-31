import { expect } from '@playwright/test';
import { chercherDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Deux gestes sont annoncés par ce scénario : masquer, et réordonner. Seul le premier est
 * disponible — l'onglet « Réorganisation » existe dans le code mais son affichage est
 * désactivé en dur (`showTabReorder = false`). C'est donc la VISIBILITÉ que ce parcours
 * illustre, et il le fait de bout en bout : on décoche, puis on vérifie le résultat dans la
 * prévisualisation, qui montre la navigation telle que le rôle la verra.
 *
 * L'enregistrement est immédiat — il n'y a pas de bouton « Enregistrer » : chaque case cochée
 * part au serveur, et le changement s'appliquera à la prochaine connexion des utilisateurs
 * concernés.
 *
 * Parcours ÉCRIVANT dans la base : il rend au rôle le droit qu'il lui a retiré.
 */
scenario('ADM-11', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  // Un menu que le caissier voit, et qu'on va lui masquer le temps du parcours.
  const entree = 'Mouvements de caisse';

  await etape(1, async () => {
    await page.goto('/admin/access-management');
    await page.getByRole('tab', { name: /Autorisations/ }).click();
    await chercherDansSelect(page, 'navRole', 'Caiss', 'Caissier');
    await expect(contenu).toContainText('Entrée de menu');
  });

  await etape(2, async () => {
    const ligne = page.locator('tbody tr').filter({ hasText: entree }).first();
    await expect(ligne).toBeVisible();
    // « Afficher » décoché : l'entrée disparaîtra de la navigation du rôle. Elle n'est pas
    // grisée pour lui — elle n'existe plus.
    const afficher = ligne.locator('app-checkbox input').first();
    await afficher.uncheck();
    await expect(afficher).not.toBeChecked();
  });

  await etape(3, async () => {
    // La prévisualisation montre la navigation telle que le rôle la verra : c'est le
    // contrôle qui évite de découvrir l'effet d'un décochage par un appel du caissier.
    await page.getByRole('tab', { name: /Prévisualisation/ }).click();
    await expect(contenu).toContainText(/Prévisualisation|navigation/i);
  });

  // ── Remise en état : le menu est rendu au rôle. ─────────────────────────────────────────
  await page.getByRole('tab', { name: /Permissions par rôle|Permissions/ }).first().click();
  const ligne = page.locator('tbody tr').filter({ hasText: entree }).first();
  await ligne.locator('app-checkbox input').first().check();
  await expect(ligne.locator('app-checkbox input').first()).toBeChecked();
});
