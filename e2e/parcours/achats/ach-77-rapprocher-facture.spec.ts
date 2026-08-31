import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le bon de livraison dit ce qui est arrivé, la facture dit ce qui est réclamé — et les deux
 * ne coïncident pas toujours. Le rapprochement met les deux montants côte à côte et calcule
 * l'ÉCART : c'est ce chiffre, et lui seul, qui déclenche un appel au grossiste ou une demande
 * d'avoir.
 *
 * Rapprocher n'est pas payer : le règlement suit son propre chemin (ACH-62), et une facture
 * rapprochée avec écart peut être enregistrée telle quelle pour être reprise plus tard
 * (ACH-78).
 *
 * Parcours en LECTURE : il saisit la référence de la facture pour montrer le calcul de
 * l'écart, mais n'enregistre pas le rapprochement.
 */
scenario('ACH-77', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    const liste = page.locator('app-list-bons');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').filter({ hasNotText: 'Clôturé' }).first().dblclick();
    await page.getByRole('button', { name: 'Rapprocher facture' }).click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    // Trois colonnes, et c'est toute la méthode : ce que le BL totalise, ce que la facture
    // réclame, et la différence.
    await expect(modale).toContainText('BL calculé');
    await expect(modale).toContainText('Facture fournisseur');
    await expect(modale).toContainText('Écart');
  });

  await etape(3, async () => {
    // Ce qu'on saisit vient du PAPIER : la référence de la facture, sa date, son montant HT
    // et sa TVA. Rien n'est repris du bon — sinon il n'y aurait rien à comparer.
    await expect(modale).toContainText('Référence facture');
    await expect(modale).toContainText('Date facture');
    await modale.locator('input[type="text"]').first().fill('FA-2026-0912');
  });

  await etape(4, async () => {
    // L'écart se recalcule à la saisie, ligne à ligne : montant HT, TVA. C'est lui qu'on
    // porte au grossiste, et lui qui justifiera un avoir (ACH-58).
    await expect(modale).toContainText('TVA');
    await expect(modale).toContainText(/Écart/);
  });
});
