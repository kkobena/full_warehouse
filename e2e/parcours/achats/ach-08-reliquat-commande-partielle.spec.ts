import { expect } from '@playwright/test';
import { ouvrirBonDeReception, ouvrirOnglet, traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le grossiste sert rarement tout : il manque deux boîtes ici, une là. Sans reliquat, ce
 * manquant disparaît à la finalisation — la commande est clôturée sur ce qui est arrivé, et
 * ce qui n'est pas arrivé n'existe plus nulle part. On le redécouvre en rupture, trois
 * semaines plus tard.
 *
 * PharmaSmart pose donc la question au moment où la réponse est encore connue : à la
 * finalisation, si des lignes sont partiellement servies, il propose de créer un RELIQUAT.
 * Celui-ci est une nouvelle commande, en préparation, qui ne porte que les quantités
 * manquantes — et qui garde le lien vers la commande d'origine, visible par la pastille
 * RELIQUAT dans la liste des commandes.
 *
 * Parcours ÉCRIVANT dans la base : il finalise une réception partielle et crée le reliquat
 * qu'elle appelle. L'instantané de démonstration est restauré avant chaque campagne.
 */
scenario('ACH-08', async ({ etape, page }) => {
  const ecran = page.locator('app-commande-received');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    // Un bon COMPTÉ dont le grossiste n'a pas tout servi : son taux de service est
    // entre 1 et 99 %, et c'est lui — et lui seul — qui appellera un reliquat.
    await ouvrirBonDeReception(page, 'partiel');
    await expect(ecran).toContainText(/Taux service/);
  });

  await etape(2, async () => {
    await ecran.getByRole('button', { name: 'Finaliser' }).click();
    // La question du reliquat ne se pose que si des articles manquent — c'est elle qu'on
    // vient chercher ici, et on l'accepte.
    const questions = await traverserConfirmations(page, { refus: [/[Ii]mpression/, /étiquettes/i] });
    expect(questions.join(' ')).toMatch(/[Aa]rticles manquants/);
  });

  await etape(3, async () => {
    // Le reliquat est une commande neuve, en préparation : elle attend d'être renvoyée au
    // grossiste, et porte la pastille RELIQUAT qui rappelle d'où elle vient.
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: 'Commandes fournisseurs' }).click();
    const commandes = page.locator('app-commande-requested-home');
    await expect(commandes.locator('tbody tr').first()).toBeVisible();
    await expect(commandes.locator('tbody tr').filter({ hasText: 'RELIQUAT' }).first()).toBeVisible();
  });
});
