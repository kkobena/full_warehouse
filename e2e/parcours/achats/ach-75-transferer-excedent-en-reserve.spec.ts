import { expect } from '@playwright/test';
import { ouvrirBonDeReception, ouvrirOnglet, traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le pendant de la prévisualisation (ACH-46) : ce qu'on en fait. Quand la livraison porte le
 * rayon au-delà de sa capacité, l'excédent n'a rien à faire sur l'étagère — il encombre, se
 * périme au fond, et masque les produits qui se vendent.
 *
 * Accepter le transfert range l'excédent en réserve dans le même mouvement que la réception :
 * SEUL l'excédent part, le rayon reste à son maximum. C'est la différence avec un transfert
 * manuel (ACH-72), où l'on décide de la quantité.
 *
 * Refuser ne perd rien : la suggestion de réassort reste ouverte et se traitera plus tard
 * (ACH-70). Le transfert n'est jamais imposé.
 *
 * Parcours ÉCRIVANT dans la base : il finalise une réception EN ACCEPTANT le transfert.
 */
scenario('ACH-75', async ({ etape, page }) => {
  const ecran = page.locator('app-commande-received');
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    await ouvrirBonDeReception(page, 'complet');
    await ecran.getByRole('button', { name: 'Finaliser' }).click();
    await traverserConfirmations(page, { sarreterAvant: /Répartition rayon/ });
    await expect(modale).toContainText(/Répartition rayon/i);
  });

  await etape(2, async () => {
    // La ligne dit tout ce qu'il faut pour trancher : la classe de rotation du produit — un
    // A se garde à portée de main — le stock rayon, son maximum, et l'excédent qui en sort.
    await expect(modale).toContainText(/Excédent/);
    await expect(modale).toContainText(/Classe/);
  });

  await etape(3, async () => {
    // Transférer : l'excédent rejoint la réserve, le rayon redescend à son maximum, et la
    // réception se termine dans la foulée.
    await modale.getByRole('button', { name: /Transférer vers réserve/ }).click();
    await traverserConfirmations(page, { refus: [/[Ii]mpression/, /étiquettes/i] });
    const liste = page.locator('app-list-bons');
    await expect(liste.locator('tbody tr').first()).toBeVisible({ timeout: 20000 });
  });
});
