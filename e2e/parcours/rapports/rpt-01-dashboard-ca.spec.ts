import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le tableau de bord du chiffre d'affaires est l'écran d'entrée de la famille « Chiffre
 * d'affaires » : `/reports` redirige sur `/reports/sales`, dont le premier onglet est celui-ci.
 *
 * La période par défaut est « 7 derniers jours ». Le manuel l'illustre sur trente jours : sur
 * une semaine, les répartitions par mode de paiement et par famille reposent sur trop peu de
 * ventes pour que les graphiques disent quoi que ce soit.
 */
scenario('RPT-01', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/reports/sales');
    // Un indicateur PORTANT UN MONTANT, et non le seul titre de l'écran : le tableau de bord
    // s'affiche avant que les chiffres n'arrivent, et une capture prise entre les deux
    // montrerait une bande d'indicateurs vides.
    await expect(page.locator('app-kpi-item').filter({ hasText: 'CA Semaine' })).toContainText(/\d/);
  });

  await etape(2, async () => {
    await choisirDansSelect(page, 'dcaPeriode', '30 derniers jours');
  });

  await etape(3, async () => {
    // Les deux répartitions annoncées par la légende. Ce sont des graphiques Chart.js — donc
    // des canevas, sans texte interrogeable : l'ancrage porte sur le titre de chaque carte,
    // qui n'existe que si la carte est rendue.
    await expect(page.getByRole('heading', { name: 'Répartition par Mode de Paiement' })).toBeVisible();
    await expect(page.getByRole('heading', { name: /CA par Famille de Produits/ })).toBeVisible();

    // Les deux cartes sont sous la ligne de flottaison : sans ce défilement, la capture
    // montrerait le haut du tableau de bord, pas ce que la légende annonce. C'est le seul
    // geste de mise en scène que s'autorise un parcours — il ne change rien à l'état de
    // l'application.
    // `scrollIntoViewIfNeeded` ne suffit pas : le titre affleure déjà le bas de la fenêtre,
    // donc Playwright le juge visible et ne défile pas — alors que les graphiques, eux,
    // restent hors champ. D'où le défilement explicite, qui amène la carte en haut de l'image.
    await page
      .getByRole('heading', { name: 'Répartition par Mode de Paiement' })
      .evaluate(titre => titre.scrollIntoView({ block: 'start' }));
  });
});
