import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Les référentiels produit — familles, formes, gammes, laboratoires, DCI — ne servent pas à
 * décorer la fiche : ce sont eux qui rendent le catalogue exploitable. Une famille absente,
 * et le produit sort des rapports par famille, des filtres, et des inventaires ciblés.
 *
 * La FAMILLE se distingue des autres : elle se rattache à une catégorie parente, laquelle
 * porte des règles de TVA et de marge. Créer une famille sans catégorie revient donc à créer
 * un produit hors de toute politique commerciale.
 *
 * Une valeur créée ici est proposée immédiatement dans les fiches produit — sans rechargement
 * ni délai — parce que le geste réel est enchaîné : on butte sur une famille manquante en
 * saisissant une fiche, on la crée, on revient.
 *
 * Parcours en LECTURE : il remplit sans enregistrer, un référentiel de test s'invitant
 * ensuite dans toutes les listes déroulantes du catalogue.
 */
scenario('RFD-01', async ({ etape, page }) => {
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/famille-produit');
    await page.getByRole('button', { name: 'Nouveau' }).click();
    await expect(modal).toBeVisible();
  });

  await etape(2, async () => {
    await modal.locator('#libelle').fill('DERMOCOSMETIQUE SOLAIRE');
    await modal.locator('#code').fill('9100');
    // La catégorie parente : c'est elle qui portera les règles appliquées à la famille.
    await choisirDansSelect(page, 'categorieId', 'Parfumerie');
  });

  await etape(3, async () => {
    await expect(modal.getByRole('button', { name: 'Enregistrer' })).toBeEnabled();
  });
});
