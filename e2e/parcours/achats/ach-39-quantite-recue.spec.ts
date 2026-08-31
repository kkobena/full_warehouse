import { expect } from '@playwright/test';
import { ouvrirBonDeReception, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La quantité reçue est le seul chiffre que le grossiste ne peut pas décider à notre place :
 * c'est ce qu'on a compté dans le carton. La saisir, c'est engager le stock — et c'est aussi
 * ce qui fera apparaître un écart si le bon annonçait autre chose (ACH-74).
 *
 * L'écran distingue la quantité REÇUE des UNITÉS GRATUITES : les deux entrent en stock, mais
 * les secondes ne se paient pas et ne doivent donc pas gonfler le montant du bon (ACH-41).
 *
 * Parcours ÉCRIVANT dans la base : il saisit une quantité sur une ligne d'un bon en cours,
 * que la restauration de l'instantané efface avant la campagne suivante.
 */
scenario('ACH-39', async ({ etape, page }) => {
  const ecran = page.locator('app-reception-sequential');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    // Un bon dont rien n'a encore été compté : c'est là que la saisie commence.
    await ouvrirBonDeReception(page, 'aucun');
    await expect(ecran).toBeVisible();
    // La ligne courante annonce ce qui était commandé : c'est à cela qu'on compare le carton.
    await expect(page.locator('#rh-qty')).toBeVisible();
  });

  await etape(2, async () => {
    await page.locator('#rh-qty').fill('2');
    await expect(page.locator('#rh-qty')).toHaveValue('2');
    // La saisie validée fait avancer d'une ligne : c'est le rythme du mode séquentiel, une
    // ligne traitée, la suivante à l'écran.
    await expect(ecran).toContainText('UG');
  });
});
