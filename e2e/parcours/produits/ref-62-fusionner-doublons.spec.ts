import { expect } from '@playwright/test';
import { chercherAuCatalogue, creerProduitJetable } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le doublon est l'accident de catalogue le plus courant : deux fiches pour le même produit,
 * créées à quelques jours d'écart par deux personnes différentes. Les supprimer est exclu —
 * chacune porte des ventes, des lots, des tarifs. La fusion les réunit sur une fiche CIBLE et
 * archive les autres, sans rien perdre.
 *
 * Trois précautions structurent l'écran, et c'est ce que le parcours doit montrer :
 *  1. un TABLEAU COMPARATIF (stock, prix, ventes des douze mois, dernières dates) : la cible
 *     se choisit sur des faits, pas sur la fiche qu'on avait sous les yeux ;
 *  2. une ANALYSE PRÉALABLE qui énumère ce qui sera réaffecté, et signale ce qui ne peut pas
 *     l'être — un produit rattaché à deux déconditionnés est exclu, il faut trancher avant ;
 *  3. les conflits d'un même numéro de lot, tranchés un par un — fusionner les quantités ou
 *     supprimer le doublon. Un conflit de stock sur le MÊME emplacement, lui, n'est jamais
 *     résolu automatiquement : l'écran le dit, et laisse un ajustement manuel à faire.
 *
 * Parcours ÉCRIVANT dans la base : il crée ses deux doublons, les fusionne, puis supprime ce
 * qu'il a créé — la cible comme la source archivée.
 */
scenario('REF-62', async ({ etape, page }) => {
  const suffixe = Date.now().toString().slice(-6);
  const cible = `SIROP DOUBLON ${suffixe} A`;
  const source = `SIROP DOUBLON ${suffixe} B`;
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  // Deux fiches jetables : fusionner deux produits du catalogue de démonstration en
  // archiverait un dont d'autres parcours dépendent.
  await creerProduitJetable(page, cible, `81${suffixe}`);
  await creerProduitJetable(page, source, `82${suffixe}`);

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, `SIROP DOUBLON ${suffixe}`, cible);
    await expect(lignes).toHaveCount(2);
    await lignes.nth(0).getByRole('checkbox').check();
    await lignes.nth(1).getByRole('checkbox').check();
    await expect(page.locator('.bulk-action-bar')).toContainText('2 produit(s) sélectionné(s)');
  });

  await etape(2, async () => {
    await page.locator('.bulk-action-bar').getByRole('button', { name: 'Fusionner' }).click();
    // La confirmation prévient de ce qui est irréversible : les doublons seront ARCHIVÉS,
    // au profit d'une cible qu'on n'a pas encore choisie.
    const confirmation = page.locator('.modal-content');
    await expect(confirmation).toContainText(/irréversible/i);
    await confirmation.getByRole('button', { name: 'Oui' }).click();
  });

  await etape(3, async () => {
    await expect(modale).toContainText('Fusionner 2 produits en doublon');
    // Le tableau comparatif : c'est lui qui désigne la fiche à conserver.
    await expect(modale).toContainText('Choisir le produit à conserver');
    await expect(modale).toContainText('Ventes (12 derniers mois)');
    await modale.getByRole('radio', { name: new RegExp(cible) }).check();
  });

  await etape(4, async () => {
    // L'analyse énumère ce qui sera réaffecté. Sur deux fiches neuves il n'y a ni lot ni
    // vente à déplacer : aucun conflit n'est proposé, et c'est le cas le plus simple —
    // celui qu'on veut voir avant d'en rencontrer un compliqué.
    await expect(modale.getByRole('button', { name: 'Confirmer la fusion' })).toBeEnabled();
  });

  await etape(5, async () => {
    await modale.getByRole('button', { name: 'Confirmer la fusion' }).click();
    await expect(modale).toBeHidden();
    // Le doublon est ARCHIVÉ, pas supprimé : il passe en veille, sa fiche et son historique
    // restent consultables, et la cible poursuit seule sa carrière au catalogue actif.
    await chercherAuCatalogue(page, `SIROP DOUBLON ${suffixe}`, cible);
    await expect(lignes.filter({ hasText: cible }).first()).toContainText('Actif');
    await expect(lignes.filter({ hasText: source }).first()).toContainText('Veille');
  });

  // ── Remise en état : les deux fiches créées sont supprimées. ────────────────────────────
  for (const libelle of [cible, source]) {
    await page.goto('/produits');
    await page.getByPlaceholder(/Rechercher \(CIP/).fill(libelle);
    await page.keyboard.press('Enter');
    const ligne = lignes.filter({ hasText: libelle }).first();
    if (await ligne.isVisible().catch(() => false)) {
      await ligne.getByRole('button', { name: 'Actions' }).click();
      await page.getByRole('button', { name: 'Supprimer' }).click();
      const confirmation = page.locator('.modal-content');
      await expect(confirmation).toBeVisible();
      await confirmation.getByRole('button', { name: 'Oui' }).click();
      await expect(confirmation).toBeHidden();
    }
  }
});
