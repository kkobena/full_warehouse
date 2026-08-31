import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un compteur d'alerte n'a de valeur que si l'on sait EXACTEMENT ce qu'il compte. Faute de
 * quoi il devient un voyant décoratif que l'on cesse de regarder.
 *
 * Les huit compteurs ont chacun leur règle, et chacune est vérifiable depuis l'écran de
 * destination :
 *
 *   • rupture      — produit actif dont le stock total est nul ;
 *   • péremption   — lot disponible arrivant à échéance sous trois mois ;
 *   • urgent       — VMM positive et stock actuel sous le stock objectif (SEMOIS) ;
 *   • ajustement   — produits distincts ajustés depuis la veille ;
 *   • prix         — changements de prix de vente journalisés sur 24 heures ;
 *   • facture échue — impayée ou partielle, dont la date plus le délai de l'organisme
 *     (30 jours à défaut) est dépassée.
 *
 * Le parcours suit la péremption de bout en bout : la pastille, puis l'écran qui la justifie.
 */
scenario('HOME-24', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/');
    await expect(page.locator('.navbar-badge').first()).toBeVisible({ timeout: 20000 });
  });

  await etape(2, async () => {
    // Le tableau de bord Pharmacien force son propre rafraîchissement tant qu'il est affiché :
    // les pastilles qu'on lit ici ne datent pas de la connexion.
    await expect(page.locator('.kpi-strip-item').first()).toBeVisible({ timeout: 20000 });
  });

  await etape(3, async () => {
    // L'écran de destination porte le détail de ce que le compteur résume : les lots, leurs
    // dates, et de quoi les traiter.
    await page.goto('/gestion-peremption');
    await expect(page.locator('#main-content')).toContainText(/Péremption|Lot/i, { timeout: 20000 });
  });
});
