import { expect } from '@playwright/test';
import { ouvrirBonDeReception, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Ce qui se corrige sur une ligne à la réception, c'est ce que le CARTON dit et que la
 * commande ne savait pas : la quantité réellement servie (ACH-39), les unités offertes
 * (ACH-41), le CIP quand le produit n'avait qu'un code provisoire, et le LOT livré — son
 * numéro, sa péremption, la part de la quantité qu'il porte.
 *
 * Les prix, eux, ne s'éditent pas ici, et c'est délibéré : le prix d'achat et le prix de
 * vente d'une ligne se fixent sur la commande avant qu'elle parte (ACH-03). À la réception,
 * un prix qui a bougé est SIGNALÉ — la colonne P.A porte un avertissement avec le tarif
 * catalogue en regard (ACH-43) — pour être arbitré, pas réécrit à la volée.
 *
 * La péremption saisie est contrôlée à mesure : sous trois mois, l'écran prévient, et la
 * finalisation refusera un lot trop proche de sa date (ACH-47).
 *
 * Parcours ÉCRIVANT dans la base : il ajoute un lot sur une ligne d'un bon en cours.
 */
scenario('ACH-42', async ({ etape, page }) => {
  const ecran = page.locator('app-reception-sequential');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    // Un bon jamais compté : ses lignes attendent leur quantité et leur lot.
    await ouvrirBonDeReception(page, 'aucun');
    await expect(ecran).toBeVisible();
    await expect(page.locator('#rh-qty')).toBeVisible();
  });

  await etape(2, async () => {
    // Le grossiste a livré en deux lots, et le second n'était pas annoncé : la quantité
    // reçue dépasse ce que les lots couvrent, et la saisie du lot manquant s'ouvre d'elle-
    // même. Un lot ne peut pas porter plus que ce qui est reçu — c'est la quantité qui
    // commande, pas l'inverse.
    const commande = Number((await ecran.innerText()).match(/Commandé\s+(\d+)/)?.[1] ?? '0');
    expect(commande).toBeGreaterThan(0);
    await page.locator('#rh-qty').fill(String(commande + 3));
    // La quantité se valide au clavier : le bouton reprend le même geste, mais la valeur
    // doit d'abord être confirmée dans le champ.
    await page.locator('#rh-qty').press('Enter');
    await expect(page.locator('#rh-numlot')).toBeVisible();
  });

  await etape(3, async () => {
    // Numéro et péremption : ce que porte l'étiquette du carton, et que la commande ne
    // pouvait pas connaître. La date se saisit en MM/AAAA, et l'écran prévient si elle est
    // trop proche — sous trois mois, la finalisation la refusera (ACH-47).
    await page.locator('#rh-numlot').fill('LOT-PARCOURS-42');
    await page.locator('#rh-expiry').fill('09/2028');
    const qteLot = page.locator('#rh-lotqty');
    if (await qteLot.isVisible().catch(() => false)) {
      await qteLot.fill('3');
    }
    await page.getByRole('button', { name: /Ajouter ce lot/ }).first().click();
    // Le lot accepté referme la couverture de la ligne : il ne reste rien à ventiler, le
    // formulaire de saisie disparaît et le mode séquentiel rend la main.
    await expect(page.locator('#rh-numlot')).toBeHidden();
    await expect(ecran).toContainText(/lot\(s\) saisi\(s\)|Tous les lots sont couverts/);
  });
});
