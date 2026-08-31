import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un rayon vide dont la réserve est pleine n'est pas une rupture : c'est un rangement en
 * retard. L'application le voit avant le comptoir — elle compare le stock du rayon à son
 * seuil, regarde ce que la réserve peut donner, et propose le mouvement.
 *
 * La quantité suggérée reste MODIFIABLE : c'est une proposition, pas un ordre, et celui qui
 * range sait parfois ce que le calcul ignore (une promotion, un carton déjà ouvert).
 *
 * Parcours en LECTURE : valider déplacerait du stock que les parcours de vente comptent.
 */
scenario('ACH-70', async ({ etape, page }) => {
  const ecran = page.locator('app-repartition-stock');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Répartition & Transferts/);
    await expect(ecran).toBeVisible();
    // Quatre vues : la traçabilité de ce qui a bougé, les deux sens de réassort suggéré, et
    // le transfert manuel.
    await expect(ecran).toContainText('Traçabilité');
    await expect(ecran).toContainText('Réassort suggéré – Rayon');
    await ecran.getByRole('tab', { name: /Réassort suggéré – Rayon/ }).click();
    await expect(page.locator('app-suggestion-reassort')).toBeVisible();
  });

  await etape(2, async () => {
    // La suggestion dit d'où vient la marchandise et combien en déplacer ; la quantité est
    // saisissable, précisément parce qu'elle se discute.
    await expect(page.locator('app-suggestion-reassort')).toBeVisible();
  });

  etape.horsPortee(
    3,
    'valider déplacerait réellement du stock entre réserve et rayon ; la suggestion est ' +
      'illustrée, le déplacement ne l’est pas.',
  );
});
