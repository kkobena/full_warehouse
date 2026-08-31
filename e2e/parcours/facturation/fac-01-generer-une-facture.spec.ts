import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une facture de tiers payant ne se saisit pas : elle se GÉNÈRE. Les bons de prise en charge
 * s'accumulent au fil des ventes, et la facture les rassemble pour une période et un payeur
 * donnés — c'est le document qui déclenchera le règlement, souvent trois mois plus tard.
 *
 * D'où l'importance des deux bornes de dates : facturer trop tôt laisse des bons dehors, qui
 * partiront sur la facture suivante ; facturer deux fois la même période produit un doublon
 * que l'assureur rejettera.
 *
 * L'édition part du mode retenu — par tiers-payant, par type, par groupe, ou par sélection de
 * bons (FAC-04 à FAC-06) — et la même période peut donner une facture provisoire, réutilisable,
 * ou définitive, qui consomme ses lignes (FAC-02).
 *
 * Parcours en LECTURE : il prépare l'édition et montre ce qu'elle demande, sans l'émettre —
 * une facture définitive rend ses lignes indisponibles.
 */
scenario('FAC-01', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Édition de factures/);
    await expect(contenu).toContainText(/Date début/);
    await expect(contenu).toContainText(/Date fin/);
  });

  await etape(2, async () => {
    // Le mode d'édition commande le reste du formulaire : ici, un payeur précis.
    await choisirDansSelect(page, 'edMode', 'Par tiers-payant');
    await expect(contenu).toContainText(/Tiers-payants/);
  });

  await etape(3, async () => {
    // Le tiers payant à facturer, puis la recherche des bons éligibles de la période.
    await page.locator('#edTp').focus();
    await page.locator('#edTp').press('ArrowDown');
    await page.locator('.ng-option').first().click();
    // Le panneau du sélecteur reste ouvert et recouvre la barre d'outils : le refermer avant
    // de chercher, sinon le clic tombe dessus.
    await page.keyboard.press('Escape');
    await rechercher(page);
    // Le bouton d'édition attend : c'est lui qui produira la facture.
    await expect(page.getByRole('button', { name: 'Éditer' }).first()).toBeVisible();
  });
});
