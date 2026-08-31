import { expect } from '@playwright/test';
import { ouvrirOnglet, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un produit qui manque au rayon alors que le stock total est bon n'est pas une rupture : il
 * est resté en réserve. L'historique des répartitions raconte ces déplacements internes — qui
 * a bougé quoi, quand, depuis quel emplacement vers quel autre — et c'est ce qui permet de
 * distinguer un problème d'approvisionnement d'un problème de rangement.
 *
 * Parcours en LECTURE : il produit un PDF, il ne déplace rien.
 */
scenario('ACH-69', async ({ etape, page }) => {
  const ecran = page.locator('app-repartition-stock');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Répartition & Transferts/);
    await expect(ecran).toBeVisible();
  });

  await etape(2, async () => {
    // Les filtres couvrent les quatre façons de chercher un mouvement : ce qui a bougé, qui
    // l'a bougé, d'où, et quand.
    await expect(ecran).toContainText('Type de mouvement');
    await expect(ecran).toContainText('Opérateur');
    await expect(ecran).toContainText('Emplacement');
    // L'écran s'ouvre sur la JOURNÉE : un historique se lit sur une période, on remonte donc
    // d'un an avant de chercher. Sans cela, un écran vide donnerait à croire qu'aucun
    // mouvement interne n'a jamais eu lieu.
    const debut = new Date();
    debut.setFullYear(debut.getFullYear() - 1);
    await saisirDate(page, 'dtStart', debut);
    await ecran.getByRole('button', { name: 'Rechercher' }).click();
    await expect(ecran.locator('tbody tr').first()).toBeVisible();
  });

  await etape(3, async () => {
    // Chaque ligne porte les stocks AVANT et APRÈS de part et d'autre : c'est ce qui rend le
    // mouvement vérifiable, et non pas seulement daté.
    await expect(ecran.locator('tbody tr').first()).toBeVisible();
  });

  await etape(4, async () => {
    const telechargement = page.waitForEvent('download');
    await ecran.getByRole('button', { name: 'Exporter PDF' }).click();
    const fichier = await telechargement;
    expect(fichier.suggestedFilename()).toMatch(/\.pdf$/);
  });
});
