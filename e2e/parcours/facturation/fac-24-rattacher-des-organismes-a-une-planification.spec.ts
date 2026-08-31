import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * On n'écrit pas une planification, on y rattache des organismes.
 *
 * Le système pose six lignes — une par périodicité (quinzaine, mensuelle, bimensuelle) et par
 * nature de facture (définitive, provisoire) — et ne permet d'en créer aucune autre : la
 * contrainte d'unicité en base l'interdit. Ce qui décide de la facture d'un organisme, c'est
 * donc la PÉRIODICITÉ portée par sa fiche, et l'inclusion dans l'automatisation.
 *
 * D'où les sous-onglets « Groupes » et « Tiers payants », avec leurs actions en lot : inclure,
 * exclure, ou appliquer une périodicité à toute une sélection. Un organisme facturé à la
 * quinzaine et un autre au mois n'ont pas à être traités le même jour.
 */
scenario('FAC-24', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Automatisation/);
    await expect(contenu).toContainText('Factures définitives');
    await expect(contenu).toContainText('Périodicité');
    await expect(contenu).toContainText('Prochain déclenchement');
  });

  await etape(2, async () => {
    // Le détail d'une périodicité s'ouvre à la loupe ; ses sous-onglets disent QUI elle couvre.
    await lignes.first().locator('button:has(.pi-eye)').first().click();
    // Le sous-onglet voisin s'appelle « Groupes Tiers payants » : une correspondance par
    // sous-chaîne ouvre celui-là. On filtre donc sur le libellé ENTIER.
    await page.getByRole('tab').filter({ hasText: /^Tiers payants$/ }).first().click();
    await expect(contenu).toContainText('Tiers payant');
    await expect(contenu).toContainText('Catégorie');
  });

  await etape(3, async () => {
    // La barre d'actions en lot ne se montre qu'une fois une sélection faite : elle n'a rien
    // à proposer sur zéro organisme, et occuperait la place pour rien.
    // Le sous-onglet voisin reste dans le DOM une fois visité : sans porter le sélecteur sur
    // le composant, on coche une ligne de la table des groupes, invisible à l'écran.
    await page.locator('app-planif-subtab-tps tbody input[type="checkbox"]').first().click();
    await expect(page.getByRole('button', { name: 'Inclure' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Exclure' })).toBeVisible();
    // Ces trois gestes évitent d'ouvrir cinquante fiches pour un changement de contrat.
    await expect(page.getByRole('button', { name: 'Appliquer' })).toBeVisible();
  });
});
