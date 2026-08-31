import { expect } from '@playwright/test';
import { chercherDansSelect, ouvrirJournalDuJour, seConnecterEnTantQue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Retirer un privilège ne grise pas un bouton : il le fait DISPARAÎTRE. C'est le principe du
 * refus par défaut — l'utilisateur ne voit pas ce qu'il ne peut pas faire, et n'a donc pas à
 * se demander pourquoi il ne peut pas.
 *
 * Le parcours montre la chaîne complète, des deux côtés : l'administrateur retire le droit,
 * le caissier ouvre le même écran et l'action a disparu de son menu. Et le contrôle ne
 * s'arrête pas à l'affichage : l'appel serveur correspondant est refusé lui aussi, ce qui
 * rend le contournement de l'interface sans effet.
 *
 * Parcours ÉCRIVANT dans la base : il rend le droit au rôle en fin de parcours.
 */
scenario('ADM-09', async ({ etape, page }) => {
  const privilege = 'Créer un retour client';
  const contenu = page.locator('#main-content');

  const caseDuPrivilege = () =>
    page.locator('tbody tr').filter({ hasText: privilege }).first().locator('app-checkbox input').first();

  // ── Mise en scène : le droit doit être ACCORDÉ au départ, sans quoi il n'y aurait rien à
  //    retirer. Un parcours précédent a pu le laisser dans l'autre état. ──────────────────
  await page.goto('/admin/access-management');
  await page.getByRole('tab', { name: /Autorisations/ }).click();
  await chercherDansSelect(page, 'navRole', 'Caiss', 'Caissier');
  await caseDuPrivilege().check();
  await expect(caseDuPrivilege()).toBeChecked();

  await etape(1, async () => {
    // Le privilège est là, coché : le caissier peut aujourd'hui enregistrer un retour. On le
    // lui retire — l'enregistrement est immédiat, il n'y a pas de bouton de validation.
    await caseDuPrivilege().uncheck();
    await expect(caseDuPrivilege()).not.toBeChecked();
  });

  await etape(2, async () => {
    // On passe de l'autre côté : le caissier lui-même, sur le journal des ventes.
    await seConnecterEnTantQue(page, 'kkone', 'admin');
    await ouvrirJournalDuJour(page);
    await expect(contenu).toContainText('Journal des ventes');
  });

  await etape(3, async () => {
    const ligne = page.locator('tbody tr').filter({ visible: true }).filter({ hasNotText: 'Annulée' }).first();
    await ligne.getByRole('button', { name: 'Actions' }).click();
    // Le menu s'ouvre sans « Retour client » : l'action n'est pas refusée, elle n'est pas
    // proposée. Comparer avec VTE-20, où elle figure en bonne place.
    await expect(page.getByRole('button', { name: 'Imprimer ticket' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Retour client' })).toHaveCount(0);
  });

  // ── Remise en état : le droit est rendu au rôle. ────────────────────────────────────────
  await seConnecterEnTantQue(page, 'admin', 'admin');
  await page.goto('/admin/access-management');
  await page.getByRole('tab', { name: /Autorisations/ }).click();
  await chercherDansSelect(page, 'navRole', 'Caiss', 'Caissier');
  await caseDuPrivilege().check();
  await expect(caseDuPrivilege()).toBeChecked();
});
