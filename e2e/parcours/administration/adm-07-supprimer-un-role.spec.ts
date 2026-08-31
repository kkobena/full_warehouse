import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un rôle se supprime quand l'organisation change. Mais les rôles LIVRÉS, eux, ne se
 * suppriment pas : administrateur, pharmacien, caissier, vendeur structurent l'application —
 * leur ligne n'offre tout simplement pas l'action. Seuls les rôles ajoutés par l'officine
 * peuvent l'être, et c'est visible d'un coup d'œil sur la liste.
 *
 * Parcours ÉCRIVANT dans la base : il crée le rôle qu'il supprime.
 */
scenario('ADM-07', async ({ etape, page }) => {
  const suffixe = 'STAGIAIRE';
  const libelle = 'Stagiaire officine';
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  // ── Mise en scène : le rôle à supprimer. ────────────────────────────────────────────────
  await page.goto('/admin/access-management');
  await page.getByRole('button', { name: 'Nouveau rôle' }).click();
  await modale.locator('#field_suffix').pressSequentially(suffixe, { delay: 25 });
  await modale.locator('#field_libelle').fill(libelle);
  await modale.getByRole('button', { name: 'Créer' }).click();
  await expect(modale).toBeHidden();

  await etape(1, async () => {
    // La liste distingue les deux familles : les rôles livrés n'ont pas de corbeille, le
    // rôle ajouté par l'officine en a une.
    const ligne = lignes.filter({ hasText: libelle }).first();
    await expect(ligne).toBeVisible();
    await expect(ligne.getByRole('button', { name: 'Supprimer ce rôle' })).toBeVisible();
    await expect(lignes.filter({ hasText: 'Administrateur' }).first().getByRole('button', { name: 'Supprimer ce rôle' })).toHaveCount(0);
  });

  await etape(2, async () => {
    await lignes.filter({ hasText: libelle }).first().getByRole('button', { name: 'Supprimer ce rôle' }).click();
    const confirmation = page.locator('.modal-content');
    if (await confirmation.count()) {
      await confirmation.getByRole('button', { name: /Oui|Supprimer/ }).first().click();
    }
    await expect(lignes.filter({ hasText: libelle })).toHaveCount(0);
  });
});
