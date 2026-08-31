import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Le réapprovisionnement automatique ne se pilote pas ligne à ligne mais par masses : combien
 * de références sont EN RUPTURE, combien passent SOUS LEUR SEUIL, combien vont bien. Ces trois
 * nombres disent en une seconde si la commande de la semaine sera légère ou lourde.
 *
 * La FRAÎCHEUR du calcul est la donnée qu'on regarde d'abord : un tableau de bord bâti sur des
 * ventes moyennes vieilles de trois semaines conseille le stock d'il y a trois semaines.
 *
 * Parcours en LECTURE.
 */
scenario('ACH-26', async ({ etape, page }) => {
  const ecran = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/semois/dashboard');
    await expect(page.getByRole('heading', { name: /SEMOIS/ })).toBeVisible();
  });

  await etape(2, async () => {
    // La fraîcheur du calcul, annoncée sur la barre d'outils : c'est elle qui dit si les
    // chiffres du dessous valent quelque chose.
    await expect(ecran).toContainText(/VMM|calcul|Actualiser/i);
  });

  await etape(3, async () => {
    // Les trois masses, et le détail par classe : un produit A en rupture n'a pas le même
    // poids qu'un produit D.
    await expect(ecran).toContainText('En rupture');
    await expect(ecran).toContainText('Sous seuil');
    await expect(ecran).toContainText('Stock OK');
  });
});
