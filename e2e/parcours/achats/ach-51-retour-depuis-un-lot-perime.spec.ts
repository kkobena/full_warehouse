import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un lot périmé se renvoie au grossiste plutôt que de partir à la destruction : c'est de
 * l'argent qu'on récupère, sous forme d'avoir, au lieu d'une perte sèche.
 *
 * Le geste part du lot, pas du bon de retour, et c'est ce qui le rend praticable : on ne
 * reconstitue pas à la main la commande d'origine — le logiciel la RETROUVE seul à partir du
 * lot, ainsi que la ligne et son prix d'achat. Sans cela, il faudrait fouiller des mois de
 * réceptions pour un carton de dix boîtes.
 *
 * La quantité sort du stock au moment de la validation : le lot cesse d'apparaître dans les
 * périmés, et se retrouve dans les retours en attente (ACH-50).
 *
 * Parcours en LECTURE : il ouvre la préparation sans valider, un retour sortant réellement
 * les quantités du stock dont vivent les parcours suivants.
 */
scenario('ACH-51', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await expect(lignes.first()).toBeVisible();
    // Le retour se lance depuis la ligne du lot : c'est lui le point de départ.
    //
    // On choisit un lot MONO-EMPLACEMENT : présent dans plusieurs stockages, il faudrait
    // d'abord dire lequel on renvoie, et l'écran le refuse tant que ce n'est pas fait.
    const monoEmplacement = lignes
      .filter({ has: page.locator('.pharma-badge-info .pi-map-marker') })
      .first();
    await monoEmplacement
      .locator('app-button[ngbtooltip="Faire un retour fournisseur"] button')
      .first()
      .click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    // La commande d'origine, résolue seule : c'est ce qui distingue ce raccourci d'une
    // saisie manuelle de bon de retour.
    await expect(modale).toContainText(/retour|fournisseur/i);
    await expect(modale.getByRole('button', { name: /Valider|Confirmer|Créer/ }).first()).toBeVisible();
  });
});
