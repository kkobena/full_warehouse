import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Deux rapports en un : les mêmes filtres alimentent la liste des produits vendus et celle des
 * invendus, qui n'ont ni les mêmes colonnes ni les mêmes indicateurs. C'est l'écran le plus
 * dense de la famille Stock, et celui dont les filtres avancés — rangés dans un tiroir — se
 * devinent le moins.
 */
scenario('RPT-19', async ({ etape, page }) => {
  const indicateur = (libelle: string) => page.locator('app-kpi-item').filter({ hasText: libelle });
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/reports/stock');
    await ouvrirOnglet(page, /Récap Produits/);
    await expect(indicateur('Quantité vendue')).toContainText(/\d/);
  });

  await etape(2, async () => {
    await ouvrirOnglet(page, 'Produits Invendus');
    // « Nb produits » n'existe que du côté invendus : l'onglet des vendus compte des
    // quantités, pas des références. C'est donc la preuve que la bascule a eu lieu.
    await expect(indicateur('Nb produits')).toContainText(/\d/);
    await expect(page.getByRole('columnheader', { name: 'Seuil mini' })).toBeVisible();
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: 'Filtres avancés' }).click();
    // Le tiroir est ce que l'étape montre : période fine, utilisateur, rayon, fournisseur,
    // seuils. Rien de tout cela n'est visible depuis la barre d'outils.
    const tiroir = page.locator('app-offcanvas');
    await expect(tiroir.getByText('Filtres de sélection')).toBeVisible();
    await expect(tiroir.getByText('Filtres de seuil')).toBeVisible();
    // Le tiroir reste OUVERT jusqu'à la capture : c'est lui que la légende désigne. Le
    // refermer ici donnerait deux images identiques pour deux étapes distinctes.
  });

  await etape(4, async () => {
    // Le « Rechercher » du tiroir le referme et relance la recherche. Il est homonyme de
    // celui de la barre d'outils : sans le préfixe `tiroir`, le sélecteur en trouverait deux.
    const tiroir = page.locator('app-offcanvas');
    await tiroir.getByRole('button', { name: 'Rechercher' }).click();
    await expect(tiroir.getByText('Filtres de sélection')).toBeHidden();

    // Le résumé et le détail annoncés : les indicateurs valorisent l'ensemble, le tableau
    // donne le stock dormant produit par produit.
    await expect(indicateur('Valeur vente')).toContainText(/\d/);
    await expect(lignes.first()).toBeVisible();
  });
});
