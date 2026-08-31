import { expect } from '@playwright/test';
import { seConnecterEnTantQue } from '../../src/actions';
import { IDENTIFIANTS } from '../../src/config';
import { scenario } from '../../src/scenario';

/**
 * Demander à un caissier d'ouvrir sa caisse en saisissant lui-même son fonds suppose qu'il le
 * détienne. Ce n'est pas l'organisation la plus répandue : dans la plupart des officines, le
 * fonds appartient à la pharmacie, un responsable l'alloue, et le caissier le reçoit.
 *
 * L'option d'ALLOCATION AUTOMATIQUE traduit cette organisation. Une fois activée avec son
 * montant, le caissier n'a plus rien à ouvrir : sa caisse se crée à sa première vente, avec le
 * fonds alloué pour son compte, et l'opération est tracée au nom du responsable qui l'a rendue
 * possible — le caissier ne s'est pas servi lui-même.
 *
 * AMINATA TRAORÉ n'a pas de caisse ouverte dans le jeu de démonstration : c'est la situation
 * du matin, celle que le scénario doit montrer.
 */
scenario('VTE-35', async ({ etape, page }) => {
  await etape(1, async () => {
    await seConnecterEnTantQue(page, 'atraore', IDENTIFIANTS.motDePasse);
    // Elle n'ouvre rien : elle va vendre.
    await page.goto('/sales-home/prevente');
    await expect(page.locator('#main-content')).toBeVisible({ timeout: 20000 });
  });

  await etape(2, async () => {
    // La caisse existe désormais, avec le fonds alloué — sans que le montant ait été saisi
    // par la caissière.
    await page.goto('/');
    await expect(page.locator('.caisse-banner')).toContainText(/Caisse Ouverte/, { timeout: 20000 });
    await expect(page.locator('.caisse-banner')).toContainText(/Fond d'ouverture/);
  });
});
