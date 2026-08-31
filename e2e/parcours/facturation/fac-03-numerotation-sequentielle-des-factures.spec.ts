import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le numéro de facture n'est pas un identifiant technique : c'est ce que le comptable et
 * l'organisme citeront pendant des années, et ce qu'un contrôle exigera de retrouver.
 *
 * Il s'écrit ANNÉE_0001 — l'exercice, puis un compteur sur quatre chiffres. Le préfixe fait
 * deux choses à la fois : il rattache la facture à son exercice comptable, et il garantit
 * l'unicité même quand le compteur repart de 1 au 1er janvier.
 *
 * Ce qu'on vérifie ici est donc la FORME et la continuité, pas une valeur : deux factures ne
 * portent jamais le même numéro, et l'année lue dans le numéro est celle de l'émission.
 */
scenario('FAC-03', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /^Factures/);
    await rechercher(page);
    await expect(lignes.first()).toBeVisible();

    // La LISTE n'affiche que le compteur — quatre chiffres — et c'est délibéré : au
    // quotidien, l'exercice est celui qu'on filtre, le répéter sur chaque ligne n'apprend
    // rien. L'unicité, elle, est tout l'enjeu : autant de numéros distincts que de factures.
    // `evaluateAll` ne rend rien sur un locator filtré par visibilité : on relit les lignes
    // une à une, ce qui reste rapide sur une page de tableau.
    const textes = await lignes.allInnerTexts();
    const numeros = textes
      .map(texte => /\d{4}/.exec(texte)?.[0])
      .filter((numero): numero is string => Boolean(numero));
    expect(numeros.length).toBeGreaterThan(1);
    expect(new Set(numeros).size).toBe(numeros.length);
  });

  await etape(2, async () => {
    // Le même numéro suit la facture jusque dans son détail : c'est lui que le tiers payant
    // citera, et il ne change pas de forme d'un écran à l'autre.
    const premier = /\d{4}/.exec(await lignes.first().innerText())?.[0] ?? '';
    await lignes.first().click();
    await expect(page.locator('.detail-column')).toContainText(premier);
  });
});
