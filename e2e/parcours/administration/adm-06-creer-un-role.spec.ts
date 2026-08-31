import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Les rôles livrés — administrateur, pharmacien, caissier, vendeur — couvrent l'officine
 * type. Une officine qui ne l'est pas en ajoute : un préparateur, un stagiaire, un
 * responsable achats. Le rôle ne vaut rien tant qu'on ne lui a pas donné de droits (ADM-08 et
 * ADM-10) ; il est d'abord une ÉTIQUETTE que l'on posera sur des comptes.
 *
 * Deux champs, et la distinction compte : le NOM TECHNIQUE identifie le rôle dans les
 * autorisations, le LIBELLÉ est ce que verront les écrans.
 *
 * Parcours ÉCRIVANT dans la base : il supprime le rôle créé après la dernière capture.
 */
scenario('ADM-06', async ({ etape, page }) => {
  const suffixe = 'PREPARATEUR';
  const libelle = 'Préparateur en pharmacie';
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/admin/access-management');
    await expect(page.locator('#main-content')).toContainText('Rôles');
    await page.getByRole('button', { name: 'Nouveau rôle' }).click();
    await expect(modale).toContainText('Nouveau rôle');

    // Le nom technique n'accepte que des lettres (`appKeyFilter="alpha"`) : c'est lui qui
    // deviendra `ROLE_PREPARATEUR` côté autorisations.
    await modale.locator('#field_suffix').pressSequentially(suffixe, { delay: 25 });
    await modale.locator('#field_libelle').fill(libelle);
  });

  await etape(2, async () => {
    await modale.getByRole('button', { name: 'Créer' }).click();
    await expect(modale).toBeHidden();
    // Le rôle rejoint la liste : il est désormais assignable à un compte, et ses
    // autorisations restent à définir.
    await expect(lignes.filter({ hasText: libelle })).toBeVisible();
  });

  // ── Remise en état : le rôle créé est supprimé. ─────────────────────────────────────────
  await lignes.filter({ hasText: libelle }).first().getByRole('button', { name: 'Supprimer ce rôle' }).click();
  const confirmation = page.locator('.modal-content');
  if (await confirmation.count()) {
    await confirmation.getByRole('button', { name: /Oui|Supprimer/ }).first().click();
  }
});
