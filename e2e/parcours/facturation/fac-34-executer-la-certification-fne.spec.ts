import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * On force la certification quand on ne peut pas attendre la nuit : une facture à remettre
 * le jour même, ou un rattrapage après une coupure du service de la DGI.
 *
 * L'exécution manuelle traite exactement le même lot que le passage automatique — les
 * factures non encore certifiées — et laisse la même trace dans l'historique. Il n'y a donc
 * pas deux comportements à connaître, mais un seul, à deux déclencheurs.
 *
 * Le bouton n'existe que si la planification est active : forcer une automatisation éteinte
 * n'aurait pas de sens.
 *
 * Parcours en LECTURE : certifier engage l'officine auprès de l'administration fiscale, et
 * une certification ne se rétracte pas.
 */
scenario('FAC-34', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Automatisation/);
    await page.getByRole('tab').filter({ hasText: /Certification FNE/ }).first().click();
    await expect(contenu).toContainText(/Heure de déclenchement|planification FNE/i);
  });

  await etape(2, async () => {
    // « Exécuter maintenant » n'apparaît que sur une planification active — sinon c'est
    // l'interrupteur qu'il faut d'abord basculer.
    const executer = page.getByRole('button', { name: 'Exécuter maintenant' });
    const interrupteur = page.locator('app-switch input[role="switch"]').first();
    if ((await executer.count()) > 0) {
      await expect(executer).toBeVisible();
    } else {
      await expect(interrupteur).toBeVisible();
      await expect(contenu).toContainText(/Activer la planification/);
    }
  });
});
