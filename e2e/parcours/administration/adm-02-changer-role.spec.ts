import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Scénario CORRIGÉ dans le modèle : il parlait d'affecter « un ou plusieurs rôles ». L'écran
 * n'en accepte qu'UN — c'est ce rôle unique qui ouvre les menus et autorise les actions.
 *
 * Parcours ÉCRIVANT dans la base : il rend au compte son rôle d'origine une fois la dernière
 * capture prise.
 */
scenario('ADM-02', async ({ etape, page }) => {
  const login = 'ybrou';
  const roleOrigine = 'Vendeur';
  const nouveauRole = 'Caissier';
  const ligne = page.locator('tbody tr').filter({ visible: true }).filter({ hasText: login });

  await etape(1, async () => {
    await page.goto('/admin/user-management');
    await expect(ligne).toContainText(new RegExp(roleOrigine, 'i'));
    // « Modifier » ouvre la fiche ; « Voir » l'affiche en lecture seule. Les deux boutons se
    // suivent dans la ligne, d'où l'ancrage sur leur nom accessible.
    await ligne.getByRole('button', { name: 'Modifier' }).click();
    // Le login n'est plus modifiable une fois le compte créé : c'est la clé de traçabilité
    // de toutes ses opérations.
    await expect(page.locator('#field_login')).toHaveValue(login);
  });

  await etape(2, async () => {
    await choisirDansSelect(page, 'field_authority', nouveauRole);
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page).toHaveURL(/user-management$/);
    // Le rôle affiché dans la liste est le seul témoin visible du changement.
    await expect(ligne).toContainText(new RegExp(nouveauRole, 'i'));
    await expect(ligne).not.toContainText(new RegExp(roleOrigine, 'i'));
  });

  // ── Remise en état, hors étapes. ──────────────────────────────────────────────────────
  await page.goto('/admin/user-management');
  await ligne.getByRole('button', { name: 'Modifier' }).click();
  await choisirDansSelect(page, 'field_authority', roleOrigine);
  await page.getByRole('button', { name: 'Enregistrer' }).click();
  await expect(ligne).toContainText(new RegExp(roleOrigine, 'i'));
});
