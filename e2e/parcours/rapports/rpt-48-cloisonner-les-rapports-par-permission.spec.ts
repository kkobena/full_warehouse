import { expect } from '@playwright/test';
import { seConnecterEnTantQue } from '../../src/actions';
import { scenario } from '../../src/scenario';
import { IDENTIFIANTS } from '../../src/config';

/**
 * Les rapports sont l'endroit où l'officine se met à nu : marges, créances, performance de
 * chaque vendeur. Tout le monde n'a pas à les lire.
 *
 * La protection joue à DEUX niveaux, et c'est ce qui la rend utile. À l'entrée, chaque famille
 * — ventes, stock, partenaires, finance — a son sujet d'autorisation : sans lui, l'adresse
 * elle-même est refusée. À l'intérieur, chaque onglet vérifie encore sa propre permission
 * d'affichage : on peut donner accès aux rapports de stock sans donner la valorisation.
 *
 * Le parcours compare l'administrateur, qui voit tout, à MARIAM DIALLO, vendeuse, à qui la
 * famille est refusée jusque dans son URL.
 */
scenario('RPT-48', async ({ etape, page }) => {
  const sections = page.locator('.pharma-nav-vertical-link');

  await etape(1, async () => {
    // Le cloisonnement est porté par les droits des rôles sur les entrées de navigation :
    // c'est la configuration existante que le parcours observe, il n'en fabrique pas une.
    await page.goto('/reports/finance');
    await expect(sections.first()).toBeVisible({ timeout: 20000 });
  });

  await etape(2, async () => {
    // Cinq sections de finance sur les dix déclarées : les autres ont été désactivées et
    // n'apparaissent pour personne. Le nombre visible dépend donc bien des droits, pas du
    // gabarit.
    await expect(sections).toHaveCount(5);
  });

  await etape(3, async () => {
    await seConnecterEnTantQue(page, 'mdiallo', IDENTIFIANTS.motDePasse);
    await page.goto('/reports/finance');
    await expect(page.locator('#main-content')).toContainText('403', { timeout: 20000 });
  });

  await etape(4, async () => {
    // Une autre famille, saisie directement : le refus ne tient pas au menu absent.
    await page.goto('/reports/sales');
    await expect(page.locator('#main-content')).toContainText('403', { timeout: 20000 });
    await expect(sections).toHaveCount(0);
  });
});
