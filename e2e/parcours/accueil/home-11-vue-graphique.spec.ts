import { expect } from '@playwright/test';
import { carte } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un seul interrupteur commute TOUT le tableau de bord entre tableaux et graphiques — les
 * mêmes données, la même période, une autre lecture. C'est la fonction la moins devinable de
 * l'accueil : rien n'indique, devant un tableau, qu'une courbe existe pour les mêmes chiffres.
 */
scenario('HOME-11', async ({ etape, page }) => {
  const pareto = carte(page, 'Analyse Pareto 20/80');

  await etape(1, async () => {
    await page.goto('/');
    // En vue tabulaire, les cartes portent des tableaux : l'en-tête de pourcentage cumulé le
    // prouve, et c'est ce qui doit disparaître à l'étape suivante.
    await expect(pareto.getByText('%Qté')).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: /Vue Graphique$/ }).click();
    // Le tableau cède la place à un canevas Chart.js. Vérifier la présence du graphique NE
    // SUFFIT PAS — certaines cartes en portent un dans les deux vues ; c'est la disparition
    // de l'en-tête de tableau qui distingue l'état.
    await expect(pareto.getByText('%Qté')).toBeHidden();
    await expect(pareto.locator('canvas')).toBeVisible();
    await pareto.scrollIntoViewIfNeeded();
  });
});
