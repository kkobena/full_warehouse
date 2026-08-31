import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un compte se supprime quand la personne part — mais l'historique, lui, reste : les ventes
 * qu'elle a encaissées ne disparaissent pas avec elle. C'est pourquoi la suppression demande
 * confirmation : elle retire l'accès, elle ne réécrit pas le passé.
 *
 * Pour un départ temporaire, la liste offre mieux : la DÉSACTIVATION, qui coupe l'accès sans
 * effacer le compte.
 *
 * Parcours ÉCRIVANT dans la base : il crée le compte qu'il supprime.
 */
scenario('ADM-04', async ({ etape, page }) => {
  // Le login porte l'horodatage : il est UNIQUE par base, et le formulaire refuse un doublon.
  // Un identifiant figé ne rendrait le parcours jouable qu'une fois — jusqu'à la prochaine
  // restauration de l'instantané.
  const login = 'ptraore' + Date.now().toString().slice(-6);
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  // ── Mise en scène : le compte à supprimer. ──────────────────────────────────────────────
  await page.goto('/admin/user-management');
  await page.getByRole('button', { name: 'Nouvel utilisateur' }).click();
  await page.locator('#field_login').fill(login);
  await page.locator('#field_lastName').fill('TRAORE');
  await page.locator('#field_firstName').fill('Paul');
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
    // La ligne porte les deux gestes : désactiver (réversible) et supprimer (définitif).
    await expect(lignes.filter({ hasText: login })).toBeVisible();
    await lignes.filter({ hasText: login }).first().getByRole('button', { name: 'Supprimer' }).click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    await modale.getByRole('button', { name: /Supprimer|Oui/ }).first().click();
    await expect(modale).toBeHidden();
    // Le compte a disparu de la liste ; les ventes qu'il a enregistrées, elles, sont
    // toujours au journal.
    await expect(lignes.filter({ hasText: login })).toHaveCount(0);
  });
});
