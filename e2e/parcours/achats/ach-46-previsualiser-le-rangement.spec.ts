import { expect } from '@playwright/test';
import { ouvrirBonDeReception, ouvrirOnglet, traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le rayon a une capacité, et elle n'est pas extensible : une livraison qui le remplit au-delà
 * de son maximum laisse des boîtes sur le comptoir. Plutôt que de le découvrir en rangeant,
 * PharmaSmart le dit AVANT de valider l'entrée en stock.
 *
 * À la finalisation, une prévisualisation liste les produits dont le stock rayon dépasserait
 * son maximum : pour chacun, sa classe de rotation, le stock rayon, le maximum, l'excédent et
 * ce que la réserve détient déjà. Rien n'est encore déplacé — c'est une simulation, et elle
 * n'apparaît que si la politique de rangement est en mode manuel.
 *
 * L'utilisateur tranche ensuite : transférer l'excédent tout de suite (ACH-75) ou l'ignorer et
 * garder la suggestion pour plus tard.
 *
 * Parcours ÉCRIVANT dans la base : il finalise une réception après avoir IGNORÉ le transfert.
 */
scenario('ACH-46', async ({ etape, page }) => {
  const ecran = page.locator('app-commande-received');
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    await ouvrirBonDeReception(page, 'complet');
    await expect(ecran.getByRole('button', { name: 'Finaliser' })).toBeVisible();
  });

  await etape(2, async () => {
    await ecran.getByRole('button', { name: 'Finaliser' }).click();
    // Les questions préalables — écarts de prix, lignes non saisies — passent avant la
    // prévisualisation du rangement.
    await traverserConfirmations(page, { sarreterAvant: /Répartition rayon/ });
    await expect(modale).toContainText(/Répartition rayon/i);
    // Ce que la simulation montre, produit par produit : le stock rayon, son maximum,
    // l'excédent qui en découle, et la réserve qui l'accueillerait.
    await expect(modale).toContainText(/Stock rayon/);
    await expect(modale).toContainText(/Stock maxi/);
    await expect(modale).toContainText(/Excédent/);
    await expect(modale).toContainText(/Réserve actuelle/);
  });

  await etape(3, async () => {
    // « Ignorer » laisse la suggestion ouverte : la réception se termine, et la répartition
    // se traitera depuis l'écran de réassort (ACH-70).
    await modale.getByRole('button', { name: 'Ignorer' }).click();
    await traverserConfirmations(page, { refus: [/[Ii]mpression/, /étiquettes/i] });
  });
});
