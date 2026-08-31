import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le modèle parle du « tableau de bord des commandes » : l'écran s'appelle « Tableau de bord
 * Approvisionnement » et c'est le premier onglet de `/commande`. Il ne se limite pas aux
 * commandes — il réunit les indicateurs d'achat ET l'état du stock qui les justifie, ce qui en
 * fait l'écran d'entrée du module.
 */
scenario('ACH-10', async ({ etape, page }) => {
  const indicateur = (libelle: string) => page.locator('app-kpi-item').filter({ hasText: libelle });

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Tableau de bord/);
    // Le premier indicateur DOIT porter un nombre : les cartes se dessinent avant que l'API
    // ne réponde, et une capture prise entre les deux montrerait un bandeau muet.
    await expect(indicateur('En cours')).toContainText(/\d/);
  });

  await etape(2, async () => {
    // Les indicateurs à vérifier couvrent les deux versants de l'écran : ce qui est engagé
    // (commandes, réceptions attendues) et ce qui le motive (stock).
    await expect(indicateur('À réceptionner')).toContainText(/\d/);
    await expect(indicateur('Péremptions')).toContainText(/\d/);
    // Et les tableaux qui détaillent ces compteurs, faute de quoi la capture ne montrerait
    // que la bande d'indicateurs.
    await expect(page.getByRole('heading', { name: 'Commandes en cours' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'En attente de réception' })).toBeVisible();
    // Et au moins une commande listée : les deux cartes existent même vides, et une capture
    // de deux tableaux sans ligne n'illustrerait pas « le tableau de bord des commandes ».
    await expect(page.locator('tbody tr').filter({ visible: true }).first()).toBeVisible();
  });
});
