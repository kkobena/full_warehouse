import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Une prévision de vente ne sert à rien tant qu'elle ne devient pas une règle de rangement.
 * C'est ce que fait le recalcul : il transforme la VMM en seuils concrets, emplacement par
 * emplacement.
 *
 * Au RAYON, le seuil mini et la quantité de réassort valent environ une semaine de vente
 * moyenne — de quoi tenir jusqu'à la prochaine commande — et le stock maxi borne la
 * surcharge, ce qui déclenche ensuite le rangement en réserve (ACH-71). En RÉSERVE, le seuil
 * mini reprend la marge de sécurité.
 *
 * Le point décisif : seuls les paramètres ENCORE VIDES sont initialisés. Une valeur saisie à
 * la main est conservée — sans quoi le calcul écraserait chaque nuit les décisions du
 * pharmacien, et plus personne ne saisirait rien.
 *
 * Parcours en LECTURE : il montre les seuils là où ils vivent, sans relancer un recalcul qui
 * réécrirait les paramètres de tout le catalogue.
 */
scenario('ACH-83', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  etape.horsPortee(
    1,
    'choisir un produit aux paramètres vides suppose de sonder la base : le manuel montre le ' +
      'résultat du calcul, pas sa sélection.',
  );

  await etape(2, async () => {
    await page.goto('/produits');
    await expect(lignes.first()).toBeVisible();
    await lignes.first().click();
    await expect(contenu).toContainText(/Stock/i);
  });

  await etape(3, async () => {
    // Les trois paramètres du rayon, tels que le calcul les pose : le seuil qui déclenche,
    // le maxi qui borne, et la quantité qu'on remet.
    await page.getByRole('tab', { name: /Stock/ }).first().click();
    await expect(contenu).toContainText('Seuil mini');
    await expect(contenu).toContainText('Stock maxi');
    await expect(contenu).toContainText('Réassort');
  });

  etape.horsPortee(
    4,
    'vérifier qu’une valeur manuelle survit au recalcul demanderait de lancer le recalcul, ' +
      'qui réécrirait les paramètres de tout le catalogue.',
  );
});
