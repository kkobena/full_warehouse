import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La synthèse quotidienne est le rapport qui relie le tableau de bord au journal des ventes :
 * une ligne par jour ET par type de vente, ce qui explique qu'une même date y apparaisse
 * deux fois — une fois en VNO (comptant), une fois en VO (ordonnancée).
 */
scenario('RPT-02', async ({ etape, page }) => {
  const aujourdhui = new Date();
  const debutDuMois = new Date(aujourdhui.getFullYear(), aujourdhui.getMonth(), 1);
  const moisAffiche = new RegExp(`/${String(aujourdhui.getMonth() + 1).padStart(2, '0')}/${aujourdhui.getFullYear()}`);

  // La carte « Répartition par type de vente » précède le tableau et porte elle aussi des
  // lignes : sans cadrer sur `app-data-table`, on lirait sa première ligne — qui n'a pas de
  // date — au lieu de la synthèse quotidienne.
  const lignes = page.locator('app-data-table tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/reports/sales');
    await ouvrirOnglet(page, /Synthèse des Ventes/);
    // « Panier moyen » n'existe qu'ici : le tableau de bord, resté en place mais masqué,
    // n'a pas cette colonne. Elle apparaît DEUX fois depuis que l'écran ventile les types de
    // vente au-dessus du tableau — une fois dans la carte de répartition, une fois dans la
    // synthèse quotidienne. On désigne donc celle du tableau, qui est le sujet du scénario.
    await expect(
      page.locator('app-data-table').getByRole('columnheader', { name: 'Panier moyen' }),
    ).toBeVisible();
  });

  await etape(2, async () => {
    await saisirDate(page, 'ssDateDebut', debutDuMois);
    await saisirDate(page, 'ssDateFin', aujourdhui);
    // L'ancrage porte sur la PREMIÈRE ligne, la plus récente : elle ne peut tomber dans le
    // mois demandé que si le filtre a été appliqué, alors qu'« une ligne du mois existe
    // quelque part » resterait vrai sur n'importe quelle période.
    await expect(lignes.first()).toContainText(moisAffiche);
  });

  await etape(3, async () => {
    // Le libellé vient désormais de `LIBELLES_TYPE_VENTE`, source unique partagée par le
    // filtre, le tableau et le tableau de bord : « Comptant », et non plus le sigle interne.
    await choisirDansSelect(page, 'ssTypeVente', 'Comptant');
    // Ce que le filtre promet, c'est l'ABSENCE des autres types. Le vérifier sur la seule
    // première ligne ne prouverait rien : elle est déjà comptant avant le filtre.
    await expect(page.getByRole('cell', { name: 'VO', exact: true })).toHaveCount(0);
    await expect(lignes.first()).toContainText('Comptant');
  });
});
