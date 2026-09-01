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
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modal = page.locator('.modal-content:visible');
  const planificationActive = lignes
    .filter({ hasText: 'Facturation mensuelle' })
    .filter({ has: page.locator('button:has(.pi-play)') })
    .first();

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Automatisation/);
    // « Exécuter maintenant » n'existe que sur une planification active : déclencher une
    // planification éteinte n'aurait pas de sens. Le jeu de démonstration garantit que la
    // mensuelle définitive est active ; attendre cette ligne évite de cliquer un interrupteur
    // pendant que le tableau est encore en cours de chargement.
    await expect(planificationActive).toBeVisible();
  });

  await etape(2, async () => {
    await planificationActive.locator('button:has(.pi-play)').first().click();
    await expect(modal).toBeVisible();
    await expect(modal).toContainText(/Exécution manuelle/);
    await expect(modal).toContainText(/maintenant/);
  });
});
