import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Parfois on ne facture pas tout : un bon litigieux qu'on veut vérifier, une prise en charge
 * dont l'attestation manque, une période à couper en deux pour ne pas dépasser le plafond de
 * bons par facture négocié avec l'assureur.
 *
 * Le mode « par sélection de bons » laisse alors cocher un par un ce qui part. Les bons non
 * cochés restent facturables : ils partiront sur une facture ultérieure, et ne sont pas
 * perdus.
 *
 * Parcours en LECTURE.
 */
scenario('FAC-06', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Édition de factures/);
    await choisirDansSelect(page, 'edMode', 'Par sélection de bons');
  });

  await etape(2, async () => {
    // Les bons éligibles de la période s'affichent : c'est parmi eux qu'on choisit.
    await rechercher(page);
    await expect(contenu).toBeVisible();
  });

  await etape(3, async () => {
    // Ce qui n'est pas coché reste facturable : rien n'est perdu, tout est reporté.
    await expect(page.getByRole('button', { name: 'Éditer' }).first()).toBeVisible();
  });
});
