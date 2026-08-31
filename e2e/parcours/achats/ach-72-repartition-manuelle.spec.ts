import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le transfert manuel est la soupape : quand le calcul ne propose rien et qu'on a pourtant
 * besoin de déplacer une quantité — un carton descendu à la réserve, un rayon regarni avant
 * une garde — on choisit le produit, le stock SOURCE, la quantité, et le mouvement est tracé
 * comme les autres (ACH-69).
 *
 * Le stock total de l'officine ne bouge pas : c'est un déplacement, pas une entrée.
 *
 * Parcours en LECTURE : il montre la saisie sans exécuter le transfert.
 */
scenario('ACH-72', async ({ etape, page }) => {
  const ecran = page.locator('app-repartition-stock');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Répartition & Transferts/);
    await ecran.getByRole('tab', { name: /Transfert manuel/ }).click();
    await expect(page.locator('app-manual-repartition')).toBeVisible();
  });

  await etape(2, async () => {
    // On désigne le produit, puis l'emplacement d'où part la marchandise : sans source, il
    // n'y a rien à déplacer.
    await expect(page.locator('app-manual-repartition')).toContainText(/[Pp]roduit/);
  });

  etape.horsPortee(3, 'la quantité se saisit produit par produit, une fois la source choisie.');
  etape.horsPortee(4, 'la création de l’emplacement de destination ne concerne qu’un produit qui n’en a pas.');
  etape.horsPortee(
    5,
    'enregistrer déplacerait réellement du stock entre rayon et réserve ; la saisie est ' +
      'illustrée, l’enregistrement ne l’est pas.',
  );
});
