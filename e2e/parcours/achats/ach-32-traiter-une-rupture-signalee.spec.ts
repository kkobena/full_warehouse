import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le grossiste répond rarement « oui à tout ». Il signale ce qu'il ne peut pas livrer, et
 * c'est cette réponse qu'il faut lire avant de compter sur la livraison.
 *
 * L'écran de réponse sépare deux choses que la commande confond : ce qui a été SERVI, avec la
 * quantité réellement retenue, et ce qui est en RUPTURE. La différence entre commandé et livré
 * est ce qui décide de la suite — commander ailleurs, attendre le reliquat, ou prévenir le
 * patient qui attend.
 *
 * Un reliquat peut être créé automatiquement pour ce qui manque ; le bandeau de la commande
 * l'indique alors.
 *
 * Parcours en LECTURE : il consulte la réponse sans agir dessus.
 */
scenario('ACH-32', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Commandes fournisseurs/ }).click();
    await expect(lignes.first()).toBeVisible();
    await lignes.first().dblclick();
    await expect(page.locator('#main-content')).toContainText('Retour à la liste');
  });

  await etape(2, async () => {
    // La réponse du grossiste : ce qu'il sert, et ce qu'il ne peut pas.
    const reponse = page.getByRole('button', { name: /Voir réponse|Importer réponse/ }).first();
    await expect(reponse).toBeVisible();
  });
});
