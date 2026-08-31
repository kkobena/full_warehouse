import { expect } from '@playwright/test';
import { chercherAuCatalogue, choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Vendre à l'unité ce qui s'achète en boîte demande deux choses, et l'application les
 * distingue : marquer la boîte DÉCONDITIONNABLE, puis lui associer son produit DÉTAIL —
 * l'unité qu'on délivrera, avec son propre prix. Le facteur de conversion est le nombre
 * d'unités par boîte : c'est lui qui fera passer une boîte en autant d'unités au moment du
 * déconditionnement (STK-20).
 *
 * L'unité n'est pas un produit comme un autre : elle porte un `parent`, ne se commande jamais
 * — on commande la boîte — et c'est ce lien qui la distingue d'un produit simplement vendu à
 * l'unité.
 *
 * Parcours en LECTURE : la configuration est illustrée sur une boîte qui l'a déjà, sans créer
 * un déconditionné dont les parcours de stock ne s'attendent pas à hériter.
 */
scenario('REF-55', async ({ etape, page }) => {
  const boite = 'AMOXICILLINE SIROP';
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/produits');
    // Le filtre « Déconditionnables » isole les boîtes qui se délivrent à l'unité : ce sont
    // les seules à porter la configuration.
    await choisirDansSelect(page, 'produitFiltreEtat', 'Déconditionnables');
    await chercherAuCatalogue(page, boite, boite);
    await lignes.first().getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Configurer le détail' }).click();
    await expect(modale).toContainText(/détail/i);
  });

  await etape(2, async () => {
    // La fenêtre rappelle les prix de la BOÎTE et demande l'unité : combien d'unités elle
    // contient, et à quel prix l'unité se vend.
    await expect(modale).toContainText('Prix achat boîte');
    await expect(modale).toContainText('Prix vente boîte');
    await expect(modale).toContainText('Unités par conditionnement');
  });

  etape.horsPortee(
    3,
    'enregistrer créerait ou modifierait un produit détail rattaché à cette boîte ; la ' +
      'configuration est illustrée, l’enregistrement ne l’est pas.',
  );
});
