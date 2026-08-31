import { expect } from '@playwright/test';
import { chercherDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le contrôle d'accès refuse par défaut : sans assignation explicite, un menu n'existe pas
 * pour un rôle — il n'est pas grisé, il est ABSENT. C'est ce qui rend l'interface d'un
 * vendeur plus courte que celle d'un pharmacien, sans que rien ne lui signale ce qu'il ne
 * voit pas.
 *
 * Assigner, c'est cocher : chaque case est enregistrée immédiatement, sans bouton de
 * validation — le changement vaut pour la prochaine connexion des utilisateurs du rôle.
 *
 * Parcours ÉCRIVANT dans la base : il coche puis DÉCOCHE la même case, laissant les droits
 * dans leur état d'origine.
 */
scenario('ADM-10', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  // Une entrée que le caissier n'a pas : la comptabilité ne le regarde pas.
  const entree = 'Rapport TVA';

  await etape(1, async () => {
    await page.goto('/admin/access-management');
    await page.getByRole('tab', { name: /Autorisations/ }).click();
    await chercherDansSelect(page, 'navRole', 'Caiss', 'Caissier');
    await expect(contenu).toContainText('Entrée de menu');
  });

  await etape(2, async () => {
    const ligne = page.locator('tbody tr').filter({ hasText: entree }).first();
    await expect(ligne).toBeVisible();
    // La première case de la ligne, c'est « Afficher » : le menu apparaîtra dans la
    // navigation du rôle. « Accéder » suit — voir un menu ne suffit pas à y entrer.
    const afficher = ligne.locator('app-checkbox input').first();
    await afficher.check();
    await expect(afficher).toBeChecked();
  });

  // ── Remise en état : la case est décochée, les droits du rôle retrouvent leur état. ─────
  const ligne = page.locator('tbody tr').filter({ hasText: entree }).first();
  await ligne.locator('app-checkbox input').first().uncheck();
  await expect(ligne.locator('app-checkbox input').first()).not.toBeChecked();
});
