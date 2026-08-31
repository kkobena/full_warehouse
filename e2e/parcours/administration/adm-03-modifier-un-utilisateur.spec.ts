import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Une fiche utilisateur vieillit : un nom mal orthographié à l'embauche, un e-mail qui change,
 * un numéro à corriger. La modification ne touche ni le rôle ni le mot de passe — elle porte
 * sur l'identité, ce que le compte affichera partout ailleurs (ventes, journaux, tickets).
 *
 * Parcours ÉCRIVANT dans la base : il crée son propre compte, le modifie, puis le supprime —
 * il ne laisse donc rien derrière lui et ne touche à aucun compte du jeu de démonstration.
 */
scenario('ADM-03', async ({ etape, page }) => {
  // Le login porte l'horodatage : il est UNIQUE par base, et le formulaire refuse un doublon.
  // Un identifiant figé ne rendrait le parcours jouable qu'une fois — jusqu'à la prochaine
  // restauration de l'instantané.
  const login = 'jkouame' + Date.now().toString().slice(-6);
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  // ── Mise en scène : le compte à modifier. ───────────────────────────────────────────────
  await page.goto('/admin/user-management');
  await page.getByRole('button', { name: 'Nouvel utilisateur' }).click();
  await page.locator('#field_login').fill(login);
  await page.locator('#field_lastName').fill('KOUAME');
  await page.locator('#field_firstName').fill('Jean');
  const droits = page.locator('#field_authority');
  await droits.focus();
  await droits.press('ArrowDown');
  await page.locator('.ng-option').filter({ hasText: 'Vendeur' }).first().click();
  await page.getByRole('button', { name: 'Enregistrer' }).click();
  await expect(page).toHaveURL(/user-management$/);
  // La liste est paginée et triée : le compte tout juste créé n'est pas forcément sur la
  // première page. On le retrouve par la recherche, comme le ferait n'importe qui.
  await page.getByPlaceholder('Rechercher…').fill(login);
  await expect(lignes.filter({ hasText: login })).toBeVisible();

  await etape(1, async () => {
    // La fiche s'ouvre depuis la ligne : « Modifier », et non « Voir » — la vue est en
    // lecture seule.
    // `app-button` rend un <button>, même lorsqu'il porte un `routerLink` : c'est un bouton
    // qu'on cherche, pas un lien.
    await lignes.filter({ hasText: login }).first().getByRole('button', { name: 'Modifier' }).click();
    await expect(page.locator('#field_login')).toHaveValue(login);
  });

  await etape(2, async () => {
    // Le login, lui, ne se retouche pas à la légère : c'est la clé sous laquelle toutes les
    // opérations de ce compte ont été enregistrées. Ce sont l'identité et le contact qu'on
    // corrige ici.
    await page.locator('#field_firstName').fill('Jean-Baptiste');
    await page.locator('#field_email').fill(login + '@pharma-smart.example');
    await expect(page.locator('#field_firstName')).toHaveValue('Jean-Baptiste');
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page).toHaveURL(/user-management$/);
    // La liste porte la correction : c'est elle que verront les autres écrans.
    await expect(lignes.filter({ hasText: login })).toContainText('Jean-Baptiste');
  });

  // ── Remise en état : le compte créé pour le parcours est supprimé. ──────────────────────
  await lignes.filter({ hasText: login }).first().getByRole('button', { name: 'Supprimer' }).click();
  await expect(modale).toBeVisible();
  await modale.getByRole('button', { name: /Supprimer|Oui/ }).first().click();
});
