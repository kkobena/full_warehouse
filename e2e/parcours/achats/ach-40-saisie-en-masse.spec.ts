import { expect } from '@playwright/test';
import { ouvrirBonDeReception, ouvrirOnglet, traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un bon de vingt-cinq lignes toutes conformes ne mérite pas vingt-cinq saisies. « Tout
 * valider » pose en une fois, sur les lignes non encore saisies, une quantité reçue égale à
 * la quantité commandée : c'est le cas de la livraison sans histoire, où le carton correspond
 * au bon.
 *
 * L'opération est confirmée avant d'être appliquée, et elle le dit sans détour : aucun écart
 * ne sera signalé pour ces lignes. C'est le prix de la vitesse — ce qu'on ne compte pas, on
 * ne le conteste pas.
 *
 * Parcours ÉCRIVANT dans la base : il valide en masse les lignes d'un bon jamais compté —
 * précisément celui que les parcours de finalisation laissent de côté (ACH-47, ACH-08).
 */
scenario('ACH-40', async ({ etape, page }) => {
  const ecran = page.locator('app-commande-received');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    // Un bon dont RIEN n'a encore été compté : taux de service à 0 %. C'est le seul sur
    // lequel « Tout valider » a quelque chose à faire.
    await ouvrirBonDeReception(page, 'aucun');
    await expect(ecran.getByRole('button', { name: 'Tout valider' })).toBeVisible();
  });

  await etape(2, async () => {
    await ecran.getByRole('button', { name: 'Tout valider' }).click();
    // La confirmation annonce le nombre de lignes concernées et la contrepartie : elles
    // seront tenues pour entièrement livrées.
    const question = page.locator('.modal-content').filter({ hasText: /Tout valider/ }).first();
    await expect(question).toContainText(/entièrement livrée/i);
  });

  await etape(3, async () => {
    await traverserConfirmations(page, { limite: 1 });
    // Le taux de service de l'entête décolle : les lignes sont désormais saisies, et
    // servies au niveau commandé. Il n'atteint pas forcément 100 % — une ligne dont les
    // lots ne couvrent pas la quantité reste incomplète tant qu'ils ne sont pas saisis.
    await expect(ecran).not.toContainText(/Taux service\s*:\s*0\s*%/);
  });
});
