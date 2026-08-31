import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le bon de réception est le pont entre la commande et le stock : il reprend les lignes
 * commandées et attend qu'on y inscrive ce qui est RÉELLEMENT arrivé. Tant qu'il n'est pas
 * finalisé, rien n'entre en stock — c'est ce qui permet de le préparer pendant le
 * déballage, de le corriger, de le laisser pour le lendemain.
 *
 * Il se crée depuis la commande en attente : on ne réceptionne pas dans le vide, on
 * réceptionne CONTRE un bon de commande, et c'est cette confrontation qui rendra les écarts
 * visibles (ACH-43, ACH-74).
 *
 * Parcours en LECTURE : il ouvre la fenêtre de création sans créer un bon supplémentaire,
 * dont les compteurs de réception des autres parcours ne s'attendent pas à hériter.
 */
scenario('ACH-36', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: 'Commandes fournisseurs' }).click();
    const liste = page.locator('app-commande-requested-home');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').first().dblclick();
    await expect(page.locator('app-commande-requested')).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Créer le bon' }).click();
    // La fenêtre demande ce que le bon du grossiste porte : sa référence et sa date. Ce sont
    // elles qui permettront de rapprocher la facture plus tard (ACH-77).
    await expect(modale).toContainText(/BON DE LIVRAISON/i);
  });
});
