import { expect } from '@playwright/test';
import { ouvrirOnglet, saisirDate, saisirMontant } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La validation est le seul geste du module qui ÉCRIT. Tout ce qui précède mesure ou simule ;
 * ici, le chiffre d'affaires déclaré de la période change durablement, et l'opération entre à
 * l'historique avec son auteur et sa date.
 *
 * Le bouton n'apparaît qu'après une simulation : on ne valide pas un montant qu'on n'a pas vu
 * se répartir. Et si la période chevauche une ponction déjà validée, l'écran le dit avant —
 * ponctionner deux fois les mêmes ventes fausserait le déclaré sans laisser de trace lisible.
 *
 * Parcours en LECTURE, et le motif est simple : valider réduirait le chiffre d'affaires
 * déclaré du jeu de démonstration, donc les montants de tous les journaux et états que les
 * autres parcours illustrent. L'historique (DCA-10) montre ce qu'une ponction validée devient.
 */
scenario('DCA-09', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const debut = new Date();
  debut.setMonth(debut.getMonth() - 2, 1);
  const fin = new Date(debut.getFullYear(), debut.getMonth() + 1, 0);

  await etape(1, async () => {
    await page.goto('/declaration-ca');
    await ouvrirOnglet(page, 'Ponction');
    await saisirDate(page, 'ponction-du', debut);
    await saisirDate(page, 'ponction-au', fin);
    await saisirMontant(page, '#valeur', '2');
    await page.getByRole('button', { name: 'Simuler' }).click();
    await expect(contenu).toContainText(/Ponctionné/i, { timeout: 30000 });
  });

  await etape(2, async () => {
    // Le bouton n'existe qu'une fois la simulation faite, et le garde-fou du chevauchement
    // s'affiche à côté quand la période a déjà été ponctionnée.
    await expect(page.getByRole('button', { name: 'Valider la ponction' })).toBeVisible();
    await expect(contenu).toContainText(/chevauche une ponction déjà validée|Résultat de la simulation/i);
  });
});
