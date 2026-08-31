import { expect } from '@playwright/test';
import { assurerCaisseFermee } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une caisse comptée deux fois n'est plus une caisse comptée : le second passage effacerait
 * le premier, et avec lui la trace de l'écart. La clôture est donc DÉFINITIVE — l'écran ne
 * propose plus de fermer ce qui est déjà fermé.
 *
 * Là encore, l'application ne se contente pas d'un message : elle retire le geste. Ce qui
 * reste offert, c'est l'ouverture de la caisse SUIVANTE, qui repartira de son propre fonds.
 *
 * Parcours en LECTURE : il constate l'état d'une caisse close.
 */
scenario('VTE-40', async ({ etape, page }) => {
  // La caisse doit être FERMÉE pour que le scénario ait un objet.
  await assurerCaisseFermee(page);

  await etape(1, async () => {
    await page.goto('/my-cash-register');
    await expect(page.getByText('Ouverture de caisse')).toBeVisible();
    // Aucun bouton de fermeture, aucun accès au billetage : le comptage de la journée close
    // ne peut pas être rejoué.
    await expect(page.getByRole('button', { name: /Fermer la caisse/i })).toHaveCount(0);
    await expect(page.getByText('Billetage de caisse')).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Ouvrir la caisse' })).toBeVisible();
  });
});
