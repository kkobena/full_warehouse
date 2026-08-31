import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Le ticket Z est le document que le pharmacien lit le soir : ce qui est entré en caisse,
 * par mode de paiement et par caissier, et ce qui n'y est PAS entré — le crédit, différé ou
 * tiers payant. Confondre les deux, c'est croire encaissé ce qui reste à recouvrer.
 *
 * Scénario CORRIGÉ dans le modèle : il le disait produit « depuis la caisse clôturée ». Il
 * vit en réalité dans la comptabilité, onglet « Récapitulatif de caisse », et porte sur une
 * PÉRIODE — pas sur une session de caisse. On peut donc le rééditer pour une journée passée.
 *
 * Parcours en LECTURE.
 */
scenario('VTE-39', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await page.getByRole('tab', { name: /Récapitulatif de caisse/ }).first().click();
    // L'onglet charge son écran après coup : attendre le titre du récapitulatif, et non le
    // libellé de l'onglet — qui est présent dès l'arrivée sur la page, onglet actif ou non.
    await expect(contenu).toContainText('Récapitulatif Général', { timeout: 20000 });

    // La ventilation par mode de paiement, puis le détail par caissier : les deux lectures
    // dont le contrôle a besoin.
    await expect(contenu).toContainText("Vue d'ensemble des paiements");
    await expect(contenu).toContainText('ESPECE');
    await expect(contenu).toContainText(/Crédit/);
    await expect(contenu).toContainText(/CAISSIER/i);

    // Et l'impression, sous « Autres actions » : c'est elle qui produit le ticket Z papier.
    await expect(page.getByRole('button', { name: 'Exporter' })).toBeVisible();
  });
});
