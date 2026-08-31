import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Une licence est liée à un poste, et le poste se nomme par son EMPREINTE. C'est elle que le
 * revendeur attend ; la recopier à la main est le meilleur moyen d'y perdre un caractère et
 * de recevoir une licence qui ne s'activera jamais.
 *
 * Parcours en LECTURE : il copie dans le presse-papiers, il n'écrit rien dans la base.
 */
scenario('ADM-21', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/licence');
    await expect(contenu).toContainText('Gérer ma licence');
  });

  await etape(2, async () => {
    // L'empreinte est calculée pour CETTE installation ; elle s'affiche telle qu'elle sera
    // transmise, sans mise en forme qui pourrait tromper à la relecture.
    await expect(contenu).toContainText('Identification du poste');
    await expect(contenu.locator('code').first()).toBeVisible();
  });

  await etape(3, async () => {
    // « Copier la demande » met dans le presse-papiers l'officine ET l'empreinte : le message
    // au revendeur est complet du premier coup, sans aller-retour pour réclamer le reste.
    await page.getByRole('button', { name: 'Copier la demande' }).click();
    await expect(contenu).toContainText("Copier l'empreinte");
  });
});
