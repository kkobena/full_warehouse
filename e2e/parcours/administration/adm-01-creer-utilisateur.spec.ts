import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Scénario CORRIGÉ dans le modèle en écrivant ce parcours : il annonçait un compte créé
 * « sans accès tant qu'aucun rôle ne lui est affecté ». Le formulaire ne le permet pas — le
 * champ « Droits » est obligatoire. C'est un meilleur choix que ce que décrivait le cahier :
 * on ne crée pas de compte dont personne ne saura dire ce qu'il autorise.
 *
 * Parcours ÉCRIVANT dans la base : il supprime le compte créé une fois la dernière capture
 * prise, pour rester rejouable sans restauration préalable.
 */
scenario('ADM-01', async ({ etape, page }) => {
  const login = 'fndiaye';
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    // On passe par la liste plutôt que d'ouvrir le formulaire directement : « Enregistrer »
    // revient à l'écran PRÉCÉDENT (`history.back()`), et une arrivée en lien direct ferait
    // donc sortir de l'application.
    await page.goto('/admin/user-management');
    await page.getByRole('button', { name: 'Nouvel utilisateur' }).click();
    await page.locator('#field_login').fill(login);
    await page.locator('#field_lastName').fill('N’DIAYE');
    await page.locator('#field_firstName').fill('FATOU');
    await expect(page.locator('#field_login')).toHaveValue(login);
  });

  await etape(2, async () => {
    // Le rôle n'est pas un réglage optionnel : sans lui, « Enregistrer » refuse la fiche.
    // Le libellé du rôle s'écrit « Vendeur » dans la liste déroulante ; la colonne du
    // tableau l'affiche en capitales, mais c'est une mise en forme, pas le texte.
    await choisirDansSelect(page, 'field_authority', 'Vendeur');
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    // Le retour à la liste ET la présence du compte : l'un sans l'autre laisserait croire à
    // un enregistrement qui n'a pas eu lieu.
    await expect(page).toHaveURL(/user-management$/);
    const nouveau = lignes.filter({ hasText: login });
    await expect(nouveau).toContainText(/vendeur/i);
    await expect(nouveau).toContainText(/actif/i);
  });

  // ── Remise en état, hors étapes : la capture est déjà prise. ──────────────────────────
  // On recharge la liste d'abord : le bandeau de confirmation affiché après l'enregistrement
  // recouvre la ligne et intercepte le clic sur son bouton de suppression.
  await page.goto('/admin/user-management');
  await lignes.filter({ hasText: login }).getByRole('button', { name: 'Supprimer' }).click();
  // La confirmation est une modale ng-bootstrap : attendre qu'elle soit posée avant de
  // cliquer, sinon le clic part sur l'écran qu'elle recouvre encore.
  const confirmation = page.locator('.modal-content');
  await expect(confirmation).toBeVisible();
  await confirmation.getByRole('button', { name: 'Oui' }).click();
  await expect(confirmation).toBeHidden();
  await expect(lignes.filter({ hasText: login })).toHaveCount(0);
});
