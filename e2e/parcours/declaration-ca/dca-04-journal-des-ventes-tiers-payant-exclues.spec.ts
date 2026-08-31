import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un écart entre encaissé et déclaré ne se défend pas par une règle : il se défend par la
 * liste de ce qu'elle a écarté.
 *
 * Ce journal donne, pour la période, les ventes sorties de l'assiette au titre d'un organisme
 * exclu — leur nombre, leur valeur TTC, et le montant retiré du CA déclaré. Les deux derniers
 * chiffres sont ÉGAUX ici, et c'est le propre de cette règle : la vente sort entièrement.
 *
 * La marge brute figure à côté, parce que la question suivante vient toujours : ce qu'on
 * écarte du déclaré reste du chiffre d'affaires réel, et il rapporte.
 */
scenario('DCA-04', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/declaration-ca');
    await ouvrirOnglet(page, 'Ventes tiers-payant exclues');
    await expect(contenu).toContainText('Ventes tiers-payant exclues');
  });

  await etape(2, async () => {
    // Les quatre indicateurs qui font la justification : combien de ventes, pour quelle
    // valeur, ce qui a quitté le déclaré, et ce que cela représentait en marge.
    await expect(contenu).toContainText(/ventes exclues/i);
    await expect(contenu).toContainText(/valeur ttc/i);
    await expect(contenu).toContainText(/retiré du ca déclaré/i);
    await expect(contenu).toContainText(/marge brute/i);
  });
});
