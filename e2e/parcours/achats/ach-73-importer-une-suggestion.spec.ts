import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le chemin inverse de « commander une proposition » : la commande existe déjà — on l'a
 * commencée à la main sur deux ruptures — et l'on veut y verser ce que le calcul a proposé
 * pour ce grossiste, sans la reconstruire.
 *
 * La fenêtre permet les deux gestes : importer la proposition ENTIÈRE, ou déplier ses lignes
 * et n'en retenir que certaines. Les lignes se chargent à la demande, proposition par
 * proposition, pour ne pas tout ramener d'un coup.
 *
 * Parcours en LECTURE : importer verserait des lignes dans une commande du jeu de
 * démonstration et consommerait la proposition.
 */
scenario('ACH-73', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: 'Commandes fournisseurs' }).click();
    const liste = page.locator('app-commande-requested-home');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').first().dblclick();
    await expect(page.locator('app-commande-requested')).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Depuis suggestion' }).click();
    await expect(modale).toBeVisible();
    await expect(modale.locator('tbody tr').first()).toBeVisible();
  });

  await etape(3, async () => {
    // Le filtre par fournisseur : sur une officine qui travaille avec six grossistes, c'est
    // ce qui évite d'importer la proposition du mauvais.
    await expect(modale).toContainText(/[Ff]ournisseur/);
  });

  await etape(4, async () => {
    // Déplier une proposition charge SES lignes : on peut alors n'en cocher que certaines,
    // au lieu de tout verser.
    await modale.locator('tbody tr').first().locator('button').first().click();
    await expect(modale.locator('.suggestion-lines-panel')).toBeVisible();
    await expect(modale.locator('.suggestion-lines-panel')).toContainText(/ligne\(s\)|CIP/);
  });
});
