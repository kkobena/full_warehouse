import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une proposition qu'on ne veut pas se supprime — il n'existe pas d'état « rejetée » : ce que
 * le calcul propose n'engage rien, et refuser revient à effacer. Le prochain passage du calcul
 * la reproposera si le besoin est toujours là, ce qui est le comportement voulu : on ne rejette
 * pas un produit, on décline une proposition à un instant donné.
 *
 * Une LIGNE se retire de la même façon, depuis la proposition ouverte, quand seul un produit
 * pose problème.
 *
 * Parcours en LECTURE : supprimer priverait les autres parcours d'achat de leurs propositions.
 */
scenario('ACH-14', async ({ etape, page }) => {
  const liste = page.locator('app-suggestion-fournisseur-list');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Propositions d'achat/ }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').first().getByRole('button', { name: 'Actions' }).click();
    await expect(page.getByRole('button', { name: 'Supprimer' })).toBeVisible();
  });

  etape.horsPortee(
    2,
    'confirmer supprimerait une proposition du jeu de démonstration, dont dépendent ACH-11, ' +
      'ACH-16, ACH-18 et ACH-21.',
  );
});
