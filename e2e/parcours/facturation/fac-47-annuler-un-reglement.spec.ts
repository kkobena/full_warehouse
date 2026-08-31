import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un règlement imputé sur la mauvaise facture fausse deux dossiers à la fois : celui qui
 * paraît soldé sans l'être, et celui qu'on relance à tort. L'annulation remet les deux
 * d'aplomb.
 *
 * Elle se fait là où l'erreur se voit — dans le détail des règlements d'une facture, au
 * rapprochement — et non depuis un écran d'administration : celui qui constate est celui qui
 * corrige.
 *
 * Parcours en LECTURE : il montre l'action sans annuler, un règlement supprimé faussant le
 * recouvrement du jeu de démonstration.
 */
scenario('FAC-47', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Rapprochement/);
    await rechercher(page);
    await lignes.first().locator('button:has(.pi-eye)').first().click();
    await expect(contenu).toContainText('N° Facture');
  });

  await etape(2, async () => {
    // Le règlement erroné se repère dans le détail de la facture : date, référence, mode,
    // montant — de quoi le reconnaître parmi plusieurs.
    const avecReglements = lignes.filter({ has: page.locator('button:has(.pi-chevron-right)') }).first();
    await avecReglements.locator('button:has(.pi-chevron-right)').first().click();
    await expect(contenu).toContainText(/Référence/);
    await expect(contenu).toContainText(/Mode/);
  });

  await etape(3, async () => {
    // L'annulation est offerte sur chaque versement, là où l'erreur se constate.
    await expect(page.locator('button:has(.pi-trash)').first()).toBeVisible();
  });
});
