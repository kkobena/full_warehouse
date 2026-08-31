import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Premier parcours réel de la campagne. Volontairement en lecture seule : il valide la chaîne
 * complète — connexion rejouée, navigation, capture, légende issue du modèle — sans modifier
 * la base de démonstration.
 *
 * Sélecteurs par libellé et par rôle, aucun `data-testid` : l'écran client en offre déjà
 * d'assez stables. C'est la règle posée dans e2e/parcours/README.md — on n'annote un gabarit
 * que lorsque le libellé est ambigu.
 */
scenario('CLI-05', async ({ etape, page }) => {
  const recherche = page.getByPlaceholder('Taper pour rechercher');

  await etape(1, async () => {
    await page.goto('/customer');
    await expect(recherche).toBeVisible();
  });

  await etape(2, async () => {
    // KOUASSI est l'un des patronymes générés par 04_clients.sql : la recherche
    // ramène donc toujours des lignes sur le jeu de démonstration.
    await recherche.fill('KOUASSI');
    await page.getByRole('button', { name: 'Rechercher' }).click();

    // Assertion PLACÉE DANS L'ÉTAPE, et portant sur la PREMIÈRE ligne.
    //
    // Les deux points comptent, et le premier jet se trompait sur les deux. Un
    // `getByText(/KOUASSI/i).first()` passait sur la liste NON filtrée — il suffisait
    // qu'une ligne KOUASSI existe plus bas — et la capture, prise juste après le clic,
    // montrait la liste d'avant le rafraîchissement. Le manuel aurait illustré « rechercher
    // un client » avec un écran non filtré.
    //
    // Ancrée sur la première ligne, l'assertion ne peut plus passer sur une liste non
    // filtrée ; et comme `expect` réessaie, elle attend le rafraîchissement avant que
    // `etape()` ne prenne l'image.
    await expect(page.locator('tbody tr').first()).toContainText(/KOUASSI/i);
  });
});
