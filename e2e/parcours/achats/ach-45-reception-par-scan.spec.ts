import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un carton de quarante références se réceptionne à la douchette, pas au clavier : on scanne
 * chaque boîte, l'application retrouve la ligne, incrémente la quantité et passe à la
 * suivante. Le DataMatrix des boîtes récentes porte en plus le LOT et la PÉREMPTION, qui se
 * remplissent alors tout seuls — c'est là que le scan fait gagner le plus de temps.
 *
 * Trois refus doivent être compris pour travailler vite : un code illisible, un CIP absent de
 * la commande, et un numéro de série déjà scanné — ce dernier étant la protection contre la
 * boîte comptée deux fois.
 *
 * Parcours en LECTURE : il montre le champ de scan et l'aide qui décrit ces cas.
 */
scenario('ACH-45', async ({ etape, page }) => {
  const ecran = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    const liste = page.locator('app-list-bons');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').filter({ hasNotText: 'Clôturé' }).first().dblclick();
    // Le champ de scan est toujours là, au-dessus de la ligne courante : la douchette n'a pas
    // besoin qu'on lui donne le focus, l'écran l'attend.
    await expect(ecran.getByPlaceholder(/Scanner CIP/)).toBeVisible();
  });

  await etape(2, async () => {
    // Un code inconnu de la commande est REFUSÉ, et l'écran dit pourquoi : sans cela, on
    // croirait avoir scanné une boîte qui n'a jamais été comptée.
    const champ = ecran.getByPlaceholder(/Scanner CIP/);
    await champ.fill('9999999');
    await champ.press('Enter');
    await expect(ecran).toContainText(/introuvable|non reconnu|absent|inconnu/i);
  });

  await etape(3, async () => {
    // L'aide récapitule les trois refus et les raccourcis : c'est la page qu'on ouvre la
    // première fois, et qu'on ne rouvre jamais.
    await page.getByRole('button', { name: 'Aide' }).click();
    await expect(page.locator('.modal-content')).toContainText(/scan|DataMatrix|CIP/i);
  });
});
