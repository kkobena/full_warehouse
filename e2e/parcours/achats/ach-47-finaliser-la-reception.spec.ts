import { expect } from '@playwright/test';
import { ouvrirBonDeReception, ouvrirOnglet, traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Finaliser, c'est le point de non-retour de la réception : jusque-là le bon se corrigeait
 * librement (ACH-37, ACH-39, ACH-42), après quoi le stock a bougé. C'est pour cela que
 * l'opération n'est pas un simple bouton mais une suite de contrôles, puis de questions.
 *
 * Les contrôles d'abord : CIP obligatoire sur chaque ligne, lots renseignés si le produit en
 * exige, péremption pas trop proche, ligne rattachée à sa commande d'origine. Les questions
 * ensuite, et seules celles qui se posent vraiment : écarts de prix au-delà du seuil (ACH-43),
 * lignes jamais saisies, rangement rayon → réserve (ACH-75), reliquat des articles non servis
 * (ACH-08), étiquettes à imprimer (ACH-76).
 *
 * Une fois passé : le stock est incrémenté, le prix moyen pondéré du produit recalculé, les
 * ruptures ouvertes d'un produit qui repasse en positif sont levées, et le bon est CLÔTURÉ —
 * il ne pourra pas être finalisé une seconde fois.
 *
 * Parcours ÉCRIVANT dans la base, et le plus engageant du module : il fait réellement entrer
 * une livraison en stock. L'instantané de démonstration est restauré avant chaque campagne.
 */
scenario('ACH-47', async ({ etape, page }) => {
  const ecran = page.locator('app-commande-received');
  let reference = '';

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    // Un bon déjà COMPTÉ et servi en entier : c'est la finalisation sans histoire. Les
    // autres cas ont leurs propres parcours — le manquant donne un reliquat (ACH-08), le
    // bon jamais compté se valide en masse d'abord (ACH-40).
    reference = await ouvrirBonDeReception(page, 'complet');
    await expect(ecran.getByRole('button', { name: 'Finaliser' })).toBeVisible();
  });

  await etape(2, async () => {
    // Pas de « Tout valider » ici : les lignes du bon ont été comptées au comptoir. Le
    // bouton existe pour les livraisons conformes qu'on ne veut pas ressaisir (ACH-40), mais
    // il remonte les quantités au niveau de la COMMANDE — et les lots, eux, ne couvrent que
    // ce qui a été reçu : la finalisation serait refusée pour lot manquant.
    await ecran.getByRole('button', { name: 'Finaliser' }).click();
    // Répondre à ce qui se présente — sauf les étiquettes, qui déclencheraient une
    // impression (ACH-76 les traite pour elles-mêmes).
    const questions = await traverserConfirmations(page, { refus: [/[Ii]mpression/, /étiquettes/i] });
    expect(questions.length).toBeGreaterThan(0);
  });

  await etape(3, async () => {
    // La finalisation ramène le bon en CONSULTATION : il n'est plus saisissable, et porte
    // désormais la pastille « Clôturé ». Le stock est passé.
    const retour = page.getByRole('button', { name: 'Retour à la liste' });
    if (await retour.isVisible().catch(() => false)) {
      await retour.click();
    }
    const liste = page.locator('app-list-bons');
    await expect(liste.locator('tbody tr').first()).toBeVisible({ timeout: 20000 });
    await expect(liste.locator('tbody tr').filter({ hasText: reference }).first())
      .toContainText(/Clôturé/i);
  });
});
