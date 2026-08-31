import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Clôturer le mois deux jours plus tôt, rattraper une échéance manquée pour cause de serveur
 * éteint : l'exécution manuelle sert à ne pas attendre neuf heures du matin.
 *
 * Elle applique exactement la logique du déclenchement automatique — les groupes éligibles
 * d'abord, puis les tiers payants qu'aucun groupe ne couvre — et laisse la même trace dans
 * l'historique. Il n'y a donc pas deux chemins de facturation à éprouver, mais un seul, à
 * deux déclencheurs.
 *
 * La confirmation nomme la planification avant d'agir : une exécution manuelle génère de
 * vraies factures définitives, qui rendront leurs lignes de vente indisponibles.
 *
 * Parcours en LECTURE : il s'arrête sur la demande de confirmation, une exécution consommant
 * pour de bon les ventes facturables du jeu de démonstration.
 */
scenario('FAC-26', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Automatisation/);
    // « Exécuter maintenant » n'existe que sur une planification active : déclencher une
    // planification éteinte n'aurait pas de sens.
    const active = lignes.filter({ has: page.locator('button:has(.pi-play)') }).first();
    if ((await active.count()) === 0) {
      await lignes.first().locator('app-switch input[role="switch"]').first().click();
    }
    await expect(lignes.filter({ has: page.locator('button:has(.pi-play)') }).first()).toBeVisible();
  });

  await etape(2, async () => {
    const active = lignes.filter({ has: page.locator('button:has(.pi-play)') }).first();
    await active.locator('button:has(.pi-play)').first().click();
    await expect(modal).toBeVisible();
    await expect(modal).toContainText(/Exécution manuelle/);
    await expect(modal).toContainText(/maintenant/);
  });
});
