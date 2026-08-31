import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un taux renégocié, un délai de règlement qui passe de trente à soixante jours, un
 * partenariat qui s'arrête : la fiche d'un organisme bouge plus souvent qu'on ne l'imagine.
 *
 * Désactiver n'est pas supprimer, et c'est toute la différence : l'organisme quitte les
 * listes du comptoir — on ne peut plus lui vendre — mais ses factures, ses règlements et ses
 * bons restent consultables. Supprimer effacerait l'historique comptable de plusieurs années.
 *
 * Parcours en LECTURE : il ouvre l'édition et montre la demande de confirmation, sans
 * désactiver un organisme dont vivent les autres parcours de facturation.
 */
scenario('FAC-37', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/tiers-payant');
    await expect(lignes.first()).toBeVisible();
    await lignes.first().locator('button:has(.pi-pencil)').first().click();
    // L'édition rouvre la même fiche que la création, en modal, pré-remplie.
    const fiche = page.locator('.modal-content:visible');
    await expect(fiche.locator('#field_libelle')).not.toHaveValue('');
  });

  await etape(2, async () => {
    // On referme la fiche sans rien changer : c'est la désactivation qui est le sujet.
    await page.locator('.modal-content:visible').getByRole('button', { name: 'Annuler' }).click();
    await expect(lignes.first()).toBeVisible();
    // Le cadenas, pas la corbeille : la désactivation retire l'organisme des ventes à venir
    // et laisse le passé intact.
    await lignes.first().locator('button:has(.pi-lock)').first().click();
    const question = page.locator('.modal-content:visible');
    await expect(question).toContainText(/DESACTIVATION DE TIERS-PAYANT/);
    await expect(question).toContainText(/désactiver/i);
  });
});
