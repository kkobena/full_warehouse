import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La certification des factures auprès de la DGI est une obligation, pas un choix — mais la
 * transmettre facture par facture, à la main, ne tient pas dans une journée d'officine.
 *
 * Une planification nocturne s'en charge : elle reprend chaque nuit les factures non encore
 * certifiées et les envoie. L'interrupteur sert aux périodes où l'on ne veut pas qu'elle
 * tourne — service DGI indisponible, coordonnées d'organismes en cours de correction — sans
 * rien perdre : les factures restent en attente et partiront à la reprise.
 *
 * Parcours en LECTURE : il montre le réglage sans le basculer, une certification étant un
 * acte fiscal irréversible.
 */
scenario('FAC-33', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Automatisation/);
    await page.getByRole('tab').filter({ hasText: /Certification FNE/ }).first().click();
    await expect(contenu).toContainText(/Certification FNE|planification FNE/i);
  });

  await etape(2, async () => {
    // Deux informations décident de tout : l'heure de passage, et la prochaine échéance —
    // qui disparaît dès que la planification est éteinte.
    await expect(contenu).toContainText(/Heure de déclenchement|Prochain déclenchement/);
    await expect(page.locator('app-switch input[role="switch"]').first()).toBeVisible();
  });
});
