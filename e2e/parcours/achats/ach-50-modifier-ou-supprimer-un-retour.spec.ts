import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un retour se corrige tant qu'il n'est pas parti : une quantité de trop, un motif choisi à la
 * hâte, une ligne qui n'aurait pas dû y figurer.
 *
 * La fenêtre se referme au moment où le fournisseur en prend connaissance — envoi PharmaML ou
 * clôture manuelle. Après, le bon fait foi : le modifier créerait un écart entre ce que
 * l'officine croit avoir renvoyé et ce que le grossiste a reçu. Les deux boutons disparaissent
 * alors de la ligne, plutôt que de refuser après coup.
 *
 * Parcours en LECTURE : il ouvre la modification sans enregistrer, et montre la suppression
 * sans supprimer.
 */
scenario('ACH-50', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Retours fournisseurs/);
    await expect(lignes.first()).toBeVisible();
    // Un retour encore modifiable se reconnaît à ce qu'il porte le crayon : les autres l'ont
    // perdu.
    const modifiable = lignes
      .filter({ has: page.locator('app-button[ngbtooltip="Modifier le retour"]') })
      .first();
    await expect(modifiable).toBeVisible();
  });

  await etape(2, async () => {
    const modifiable = lignes
      .filter({ has: page.locator('app-button[ngbtooltip="Modifier le retour"]') })
      .first();
    // La suppression est offerte au même moment et pour la même raison : rien n'est parti.
    await expect(
      modifiable.locator('app-button[ngbtooltip="Supprimer le retour"] button'),
    ).toBeVisible();
    await modifiable.locator('app-button[ngbtooltip="Modifier le retour"] button').click();
    // L'édition ouvre le même plan de travail que la création : chaque ligne, sa quantité
    // renvoyée et son motif — celui-ci obligatoire, comme l'astérisque le dit.
    await expect(page.locator('#main-content')).toContainText('Qté à Retourner', { timeout: 15000 });
    await expect(page.locator('#main-content')).toContainText('Motif de retour');
    // La commande d'origine et le fournisseur sont rappelés : on corrige un bon précis, pas
    // un formulaire vierge.
    await expect(page.locator('#main-content')).toContainText('Fournisseur:');
  });
});
