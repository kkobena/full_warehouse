import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * On n'importe pas des fournisseurs tous les jours : c'est le geste d'une reprise de données,
 * ou de l'entrée dans un nouveau groupement d'achat qui arrive avec sa liste.
 *
 * L'import CRÉE ou MET À JOUR — il ne double pas les fiches existantes — ce qui permet de
 * rejouer le même fichier après correction sans nettoyer derrière soi.
 *
 * Parcours en LECTURE : il ouvre la boîte de dépôt sans envoyer de fichier. Un import réel
 * modifierait le référentiel dont dépendent toutes les commandes du manuel.
 */
scenario('RFD-20', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/fournisseur');
    await expect(lignes.first()).toBeVisible();
    await page.getByRole('button', { name: 'Importer' }).click();
    await expect(modal).toBeVisible();
  });

  await etape(2, async () => {
    // Le dépôt de fichier, seul geste demandé : le format est celui de l'export, ce qui rend
    // l'aller-retour possible.
    await expect(modal.locator('input[type="file"]')).toHaveCount(1);
  });

  await etape(3, async () => {
    // Le compte rendu suit l'envoi ; la liste se recharge sur les fiches créées ou mises à
    // jour. On s'arrête avant, faute de fichier à soumettre.
    await expect(modal).toBeVisible();
  });
});
