import { expect } from '@playwright/test';
import { choisirTaillePage } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une partie des règles de l'officine ne se code pas : elle se règle. Combien de jours pour
 * accepter un retour, quel budget mensuel de commande, quel écart de prix déclenche une
 * alerte à la réception. Ces valeurs vivent dans les paramètres, et les modules les relisent
 * en direct — sans redémarrage.
 *
 * L'écran distingue deux familles : les interrupteurs (activer / désactiver) et les valeurs
 * chiffrées, qui s'ouvrent dans un formulaire.
 *
 * Parcours ÉCRIVANT dans la base : il modifie un délai puis lui rend sa valeur d'origine.
 */
scenario('ADM-14', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const modale = page.locator('.modal-content');
  // Le délai de retour client : c'est lui que VTE-24 met en scène — au-delà, l'écran de
  // retour affiche « Hors délai ». Trente jours par défaut.
  const parametre = 'Délai maximum en jours entre la date de vente et la date du retour client';

  await etape(1, async () => {
    await page.goto('/parametre');
    await expect(contenu).toContainText('Paramètres de configuration');
    // La liste est longue : on l'ouvre en grand plutôt que de la parcourir page à page.
    await choisirTaillePage(page, '50');
    await expect(page.locator('tbody tr').filter({ hasText: parametre })).toBeVisible();
  });

  await etape(2, async () => {
    const ligne = page.locator('tbody tr').filter({ hasText: parametre }).first();
    await ligne.getByRole('button', { name: 'Modifier' }).click();
    await expect(modale).toBeVisible();
    // Quinze jours au lieu de trente : l'officine resserre sa politique de reprise. Le champ
    // porte `appKeyFilter="int"` — il filtre les TOUCHES, et une valeur posée d'un bloc ne
    // l'atteint pas.
    const valeur = modale.locator('#field_value');
    await valeur.click();
    await valeur.press('Control+a');
    await valeur.press('Delete');
    await valeur.pressSequentially('15', { delay: 40 });
    await expect(valeur).toHaveValue('15');
  });

  await etape(3, async () => {
    await modale.getByRole('button', { name: /Enregistrer|Valider/ }).first().click();
    await expect(modale).toBeHidden();
    // La nouvelle valeur s'affiche dans la liste : les écrans de retour la reliront telle
    // quelle, sans redémarrage.
    await expect(page.locator('tbody tr').filter({ hasText: parametre })).toContainText('15');
  });

  // ── Remise en état : le délai retrouve ses trente jours. ────────────────────────────────
  const ligne = page.locator('tbody tr').filter({ hasText: parametre }).first();
  await ligne.getByRole('button', { name: 'Modifier' }).click();
  const valeurInitiale = modale.locator('#field_value');
  await valeurInitiale.click();
  await valeurInitiale.press('Control+a');
  await valeurInitiale.press('Delete');
  await valeurInitiale.pressSequentially('30', { delay: 40 });
  await modale.getByRole('button', { name: /Enregistrer|Valider/ }).first().click();
  await expect(modale).toBeHidden();
});
