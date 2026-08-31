import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Réceptionner, ce n'est pas recopier un bon de livraison : c'est le CONFRONTER à ce qu'on
 * avait commandé et à ce qui est réellement dans les cartons. Le panneau Concordance fait
 * cette confrontation à la place du pharmacien et compte les anomalies par nature — écarts de
 * quantité, de prix, de colisage, lignes non validées.
 *
 * Le triptyque Commandé / Reçu / Écart ferme le contrôle : tant que l'écart n'est pas nul ou
 * assumé, il ne faut pas finaliser — une fois la réception close, les quantités sont entrées
 * en stock et l'écart devient un inventaire à faire.
 *
 * Parcours en LECTURE.
 */
scenario('ACH-74', async ({ etape, page }) => {
  const ecran = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    const liste = page.locator('app-list-bons');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').first().dblclick();
    await expect(ecran).toContainText('Concordance');
  });

  await etape(2, async () => {
    // Les anomalies sont comptées PAR NATURE : on sait d'un coup d'œil s'il s'agit d'un
    // problème de quantité, de prix ou de conditionnement, et l'on traite dans cet ordre.
    await expect(ecran).toContainText('Écarts qté');
    await expect(ecran).toContainText('Écarts prix');
    await expect(ecran).toContainText('Écarts colisage');
    await expect(ecran).toContainText('Non validées');
  });

  await etape(3, async () => {
    // Chaque ligne montre le stock AVANT et APRÈS, avec les prix pratiqués : c'est là que se
    // repère une ligne qui n'a pas la quantité attendue.
    await expect(ecran).toContainText('Stk.Init');
    await expect(ecran).toContainText('Stk.Final');
    await expect(ecran).toContainText('Qté reçue');
  });

  await etape(4, async () => {
    // Et le montant : ce qui était commandé, ce qui a été reçu, et l'écart entre les deux —
    // le chiffre qu'on présente au grossiste si l'on conteste.
    await expect(ecran).toContainText('Commandé');
    await expect(ecran).toContainText('Reçu');
    await expect(ecran).toContainText('Écart');
  });
});
