import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Tous les rapports ne s'exportent pas dans tous les formats, et c'est délibéré.
 *
 * Le tableau de bord CA sort en PDF, Excel ou CSV parce qu'il porte à la fois une présentation
 * et une série de chiffres. Les alertes de stock, la valorisation ou la segmentation ne sortent
 * qu'en PDF : ce sont des documents, pas des jeux de données. Le récapitulatif des produits
 * vendus et invendus, lui, sort en PDF ou en Excel, parce qu'on s'en sert aussi comme base de
 * travail.
 *
 * Promettre un format absent de l'écran ferait perdre plus de temps qu'il n'en fait gagner :
 * ce parcours montre donc les formats RÉELLEMENT proposés, écran par écran.
 */
scenario('RPT-34', async ({ etape, page }) => {
  await etape(1, async () => {
    await ouvrirRapport(page, 'stock', 'Valorisation du Stock');
    await expect(page.getByRole('heading', { name: 'Valorisation du Stock' }).first()).toBeVisible();
  });

  await etape(2, async () => {
    // Un document : le PDF, et lui seul.
    await expect(page.getByRole('button', { name: 'Exporter PDF' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Excel', exact: true })).toHaveCount(0);
  });

  await etape(3, async () => {
    // Le tableau de bord CA, lui, propose les trois : le document et les données.
    await ouvrirRapport(page, 'sales', 'Dashboard CA');
    await expect(page.getByRole('button', { name: 'PDF', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Excel', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'CSV', exact: true })).toBeVisible();
  });
});
