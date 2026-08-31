import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Suspendre une facturation automatique arrive plus souvent qu'on ne croit : un contrat
 * renégocié, un organisme qui demande un autre découpage, un mois où l'on veut relire avant
 * d'envoyer.
 *
 * L'interrupteur suffit, et rien ne se perd : la planification garde son périmètre et ses
 * exécutions passées. Sa prochaine échéance disparaît le temps de la suspension — l'écran
 * affiche « — désactivé » plutôt qu'une date qui ne viendra pas — et se recalcule à la
 * réactivation.
 *
 * C'est aussi le seul geste d'extinction offert : la suppression a été retirée de cet écran,
 * les six lignes étant posées par le système et impossibles à recréer.
 */
scenario('FAC-25', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Automatisation/);
    await expect(lignes.first()).toBeVisible();
    await expect(contenu).toContainText('Actif');
  });

  await etape(2, async () => {
    // On agit sur la première ligne, quel que soit son état : la bascule se lit dans les deux
    // sens, et le parcours ne doit pas dépendre de l'ordre dans lequel il a été joué.
    const ligne = lignes.filter({ has: page.locator('app-switch') }).first();
    const etaitDesactivee = (await ligne.innerText()).includes('désactivé');

    await ligne.locator('app-switch input[role="switch"]').first().click();

    // Basculer une facturation automatique se confirme : l'écran nomme la planification
    // avant d'agir, car l'effet vaut jusqu'à ce que quelqu'un y repense.
    const confirmation = page.locator('.modal-content:visible');
    await expect(confirmation).toContainText(etaitDesactivee ? /Activer/ : /Désactiver/);
    await confirmation.getByRole('button', { name: 'Oui' }).click();

    // La colonne « Prochain déclenchement » porte la conséquence : une échéance quand la
    // planification veille, la mention « désactivé » quand elle dort.
    if (etaitDesactivee) {
      await expect(ligne).not.toContainText(/désactivé/, { timeout: 15000 });
    } else {
      await expect(ligne).toContainText(/désactivé/, { timeout: 15000 });
    }

    // On repose l'interrupteur comme on l'a trouvé : le jeu de démonstration doit rester stable.
    await ligne.locator('app-switch input[role="switch"]').first().click();
    await confirmation.getByRole('button', { name: 'Oui' }).click();
    await expect(ligne).toContainText(
      etaitDesactivee ? /désactivé/ : /\d{2}\/\d{2}\/\d{4}|à calculer/,
      { timeout: 15000 },
    );
  });
});
