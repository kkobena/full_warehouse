import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * L'officine n'est pas qu'un en-tête d'ordonnancier : c'est l'entité qui PORTE UN STOCK.
 * Un dépôt distinct a le sien, séparé, et c'est ce qui donne un sens aux ventes dépôt
 * (VTE-56 et suivants), à la répartition de stock et aux inventaires par magasin.
 *
 * Sa fiche porte aussi ce qui figure sur les documents légaux — raison sociale, registre du
 * commerce, compte contribuable — repris tels quels sur les factures et les tickets. Une
 * coquille ici se retrouve sur chaque pièce émise.
 *
 * Parcours en LECTURE : il ouvre la fiche en modification sans enregistrer, l'identité de
 * l'officine se retrouvant sur toutes les captures du manuel.
 */
scenario('RFD-17', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/magasin');
    await expect(page.getByRole('button', { name: 'Modifier' })).toBeVisible();
    await page.getByRole('button', { name: 'Modifier' }).click();
  });

  await etape(2, async () => {
    // La raison sociale et les mentions légales : ce que porteront les documents émis.
    await expect(page.locator('#field_fullName')).not.toHaveValue('');
    await expect(page.locator('#field_registre')).toBeVisible();
    await expect(page.locator('#compteContribuable')).toBeVisible();
  });
});
