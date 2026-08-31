import { expect } from '@playwright/test';
import { ouvrirBonDeReception, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un bon de réception se corrige tant qu'il n'est pas finalisé, et seulement jusque-là : une
 * fois l'entrée en stock faite (ACH-47), il est clôturé et ne s'ouvre plus qu'en consultation.
 *
 * Entre les deux, tout se reprend : la quantité comptée, les unités gratuites, le lot et sa
 * péremption, le code CIP resté provisoire. Rien de cela ne touche le stock — c'est le propre
 * d'un bon en cours : il enregistre ce qu'on a constaté, sans encore l'engager.
 *
 * C'est aussi ce qui permet de réceptionner à deux : commencer le comptage, s'interrompre, et
 * reprendre le bon plus tard là où il en était.
 *
 * Parcours ÉCRIVANT dans la base : il corrige une quantité sur un bon en cours.
 */
scenario('ACH-37', async ({ etape, page }) => {
  const ecran = page.locator('app-reception-sequential');
  let reference = '';

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    reference = await ouvrirBonDeReception(page, 'aucun');
    await expect(ecran).toBeVisible();
    await expect(page.locator('#rh-qty')).toBeVisible();
  });

  await etape(2, async () => {
    // La quantité comptée sur la ligne courante : c'est la correction la plus fréquente,
    // et elle s'enregistre à la validation de la ligne.
    await page.locator('#rh-qty').fill('3');
    await page.locator('#rh-qty').press('Enter');
    // Le taux de service de l'entête bouge : la ligne est désormais comptée.
    await expect(ecran).not.toContainText(/Taux service\s*:\s*0\s*%/);
  });

  await etape(3, async () => {
    // Le bon revient à la liste sans être clôturé : rien n'est entré en stock, et il
    // s'ouvrira de nouveau pour la suite du comptage.
    await page.getByRole('button', { name: 'Retour à la liste' }).click();
    const liste = page.locator('app-list-bons');
    await expect(liste.locator('tbody tr').filter({ hasText: reference }).first())
      .toContainText(/En attente de saisie/i);
  });
});
