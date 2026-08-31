import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * La licence est le seul écran qui doive rester atteignable QUOI QU'IL ARRIVE — abonnement
 * expiré, fichier absent, licence refusée. C'est la porte de sortie de l'officine, et elle ne
 * porte donc aucune garde de licence : la fermer serait enfermer le client dehors.
 *
 * Le parcours se contente de lire : état, échéance, modules couverts. Rien n'y est modifié.
 */
scenario('ADM-20', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/licence');
    await expect(contenu).toContainText('Gérer ma licence');
  });

  await etape(2, async () => {
    // Le statut de l'abonnement se lit d'une pastille et de quatre lignes : ce qu'on a
    // souscrit, pour quelle officine, et jusqu'à quand.
    await expect(contenu).toContainText("Statut de l'abonnement");
    await expect(contenu).toContainText('Type de licence');
    await expect(contenu).toContainText('Officine licenciée');
    await expect(contenu).toContainText('Échéance');
  });

  await etape(3, async () => {
    // Les modules NON souscrits ne sont pas cachés : ils sont montrés en grisé. La question
    // « pourquoi cet écran m'est-il refusé ? » trouve sa réponse ici, et pas au téléphone.
    await expect(contenu).toContainText('Modules souscrits');
    await expect(contenu.locator('.pi-check-circle, .pi-minus-circle').first()).toBeVisible();
  });
});
