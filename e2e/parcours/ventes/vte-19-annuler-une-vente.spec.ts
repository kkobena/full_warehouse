import { expect } from '@playwright/test';
import { ouvrirJournalDuJour } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'annulation ne supprime rien : elle marque la vente, réintègre le stock et conserve le
 * motif et l'auteur. C'est ce que le contrôle de caisse exige — un ticket Z doit pouvoir
 * expliquer chaque écart, et une vente disparue serait inexplicable.
 *
 * Deux confirmations s'enchaînent, et c'est délibéré : la première demande si l'on veut
 * vraiment annuler, la seconde réclame le MOTIF, obligatoire. Le bouton « Enregistrer »
 * reste désactivé tant qu'il est vide — on ne peut donc pas annuler une vente sans dire
 * pourquoi.
 *
 * Parcours ÉCRIVANT dans la base : il annule une vente réelle et remet son stock.
 */
scenario('VTE-19', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');
  let reference = '';

  await etape(1, async () => {
    await ouvrirJournalDuJour(page);
    // La première ligne non encore annulée : le journal s'ouvre sur la journée du jour, et
    // les ventes des parcours précédents s'y trouvent.
    const ligne = page.locator('tbody tr').filter({ visible: true }).filter({ hasNotText: 'Annulée' }).first();
    await expect(ligne).toBeVisible();
    // La RÉFÉRENCE, prise sur la cellule qui en a la forme — un long nombre. La première
    // colonne n'est pas toujours celle de la référence, et se rabattre dessus faisait porter
    // la vérification finale sur une autre vente que celle qu'on vient d'annuler.
    const cellules = await ligne.locator('td').allInnerTexts();
    reference = (cellules.map(cellule => cellule.trim()).find(texte => /^\d{8,}$/.test(texte)) ?? '').trim();
    expect(reference, 'référence de la vente à annuler').not.toBe('');

    await ligne.getByRole('button', { name: 'Actions' }).click();
    // Le menu est ouvert par ngbDropdown avec `container="body"` : il vit hors de la ligne,
    // d'où la recherche à l'échelle de la page.
    await expect(page.getByRole('button', { name: 'Annuler la vente' })).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Annuler la vente' }).click();
    await expect(modale).toContainText('Voulez-vous vraiment annuler cette vente ?');
    await modale.getByRole('button', { name: 'Oui' }).click();

    // Seconde modale : le motif. Sans lui, « Enregistrer » reste désactivé — c'est la
    // traçabilité exigée par le contrôle de caisse qui l'impose.
    await expect(modale).toContainText("Raison d'annulation de la vente");
    await page.locator('#field_name').fill('Erreur de saisie du caissier');
  });

  await etape(3, async () => {
    await modale.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(modale).toBeHidden();

    // La vente est TOUJOURS dans le journal, marquée « Annulée » : elle n'a pas disparu,
    // c'est tout l'intérêt de l'opération.
    const ligne = page.locator('tbody tr').filter({ hasText: reference }).first();
    await expect(ligne).toContainText('Annulée');
  });
});
