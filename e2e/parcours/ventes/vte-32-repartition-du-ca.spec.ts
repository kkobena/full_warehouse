import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un chiffre d'affaires global ne dit rien de ce qu'il faut corriger. Les mêmes 800 000 F
 * appellent des décisions opposées selon qu'ils viennent du comptant ou du tiers payant — le
 * second se recouvre à trois mois —, et selon qu'ils entrent en espèces ou par carte.
 *
 * Le CA se décline donc sur trois axes indépendants : dans le TEMPS, par sous-période, ce qui
 * fait apparaître les creux et les pics ; par TYPE DE VENTE, ce qui distingue l'encaissé de
 * l'à-recouvrer ; par MODE DE PAIEMENT, ce qui prépare le rapprochement de caisse.
 *
 * Parcours en LECTURE.
 */
scenario('VTE-32', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/reports/sales');
    // L'axe du TEMPS : l'évolution du chiffre d'affaires sur la période choisie.
    await expect(contenu).toContainText(/Évolution du Chiffre d'Affaires/);
  });

  await etape(2, async () => {
    // L'axe des ENCAISSEMENTS : ce qui est entré en espèces, en carte, en mobile.
    await expect(contenu).toContainText(/Répartition par Mode de Paiement/);
  });

  await etape(3, async () => {
    // L'axe du TYPE DE VENTE : la synthèse le donne ligne à ligne, et le filtre permet de
    // s'y restreindre — comptant, tiers payant, différé, dépôt.
    await ouvrirOnglet(page, /Synthèse des Ventes/);
    await expect(contenu).toContainText(/Type de vente/);
    await expect(contenu).toContainText(/CA Net/);
  });
});
