import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Scénario AJOUTÉ au cahier de recette en écrivant ce parcours : l'onglet existait et
 * fonctionnait, mais aucun scénario ne décrivait sa consultation — seulement l'export
 * (CLI-18) et la saisie d'un règlement (CLI-15). Le cahier n'est pas exhaustif ; parcourir
 * l'application est aussi ce qui révèle ce qu'il passe sous silence.
 */
scenario('CLI-21', async ({ etape, page }) => {
  const fin = new Date();
  const debut = new Date(fin);
  debut.setDate(debut.getDate() - 60);

  const lignes = page.locator('tbody tr');

  await etape(1, async () => {
    await page.goto('/differes');
    await ouvrirOnglet(page, /Historique/);
    await expect(page.getByRole('columnheader', { name: 'Total encaissé' })).toBeVisible();
  });

  await etape(2, async () => {
    await saisirDate(page, 'hrDu', debut);
    await saisirDate(page, 'hrAu', fin);
    await rechercher(page);
    // Au moins un règlement sur la période : 14b_reglements.sql en place délibérément dans
    // les trente derniers jours, faute de quoi cet écran s'ouvrirait vide.
    await expect(lignes.first()).toBeVisible();
  });

  await etape(3, async () => {
    // Le dépliage révèle les versements ligne à ligne. L'ancrage porte sur l'en-tête du
    // sous-tableau et non sur un mode de paiement : l'écran affiche le LIBELLÉ du mode
    // (« ORANGE », « ESPECE »), pas le code stocké en base (« OM », « CASH »).
    await page.getByRole('button', { name: 'Déplier le détail' }).first().click();
    await expect(page.getByRole('columnheader', { name: 'Mode de paiement' })).toBeVisible();
  });
});
