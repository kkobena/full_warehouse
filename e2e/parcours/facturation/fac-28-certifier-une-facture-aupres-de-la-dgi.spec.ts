import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La facture normalisée électronique n'est pas une option : la DGI exige que chaque facture
 * lui soit transmise et lui revienne signée, avec un numéro de certification qui l'authentifie.
 *
 * La certification se lance depuis la facture elle-même. L'écran demande confirmation en la
 * nommant, et pour une bonne raison : une facture certifiée ne se décertifie pas. C'est un
 * acte fiscal, pas un enregistrement de plus.
 *
 * Seules les factures DÉFINITIVES et non encore certifiées l'offrent — une facture provisoire
 * n'a aucune valeur fiscale, et une facture déjà certifiée propose son certificat à la place.
 *
 * Parcours en LECTURE, et c'est délibéré : confirmer enverrait pour de bon une facture de
 * démonstration à l'administration fiscale.
 */
scenario('FAC-28', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /^Factures/);
    await rechercher(page);
    await expect(lignes.first()).toBeVisible();
    // Le bouclier ne s'affiche que sur une facture certifiable : définitive, impayée, et
    // pas encore transmise.
    const certifiable = lignes.filter({ has: page.locator('button:has(.pi-shield)') }).first();
    await expect(certifiable).toBeVisible();
  });

  await etape(2, async () => {
    const certifiable = lignes.filter({ has: page.locator('button:has(.pi-shield)') }).first();
    await certifiable.locator('button:has(.pi-shield)').first().click();
    const question = page.locator('.modal-content:visible');
    await expect(question).toContainText(/Confirmer la certification/);
    await expect(question).toContainText(/FNE/);
  });
});
