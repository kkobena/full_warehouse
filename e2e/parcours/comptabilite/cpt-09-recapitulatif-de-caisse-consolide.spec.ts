import { expect, type Locator } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une officine à plusieurs postes clôture plusieurs caisses. Additionner les tickets Z à la
 * main, poste par poste et jour par jour, est long et se trompe.
 *
 * Le récapitulatif consolide tous les tickets Z de la période, quelle que soit la caisse, et
 * les ventile PAR MODE DE PAIEMENT. C'est cette ventilation qui sert : l'espèce se compte dans
 * le tiroir, le mobile et la carte se rapprochent des relevés.
 *
 * La consolidation doit être exacte dans les deux sens — ne rien omettre, ne rien compter
 * deux fois — puisque c'est elle qui justifie le dépôt en banque. Le parcours l'énonçait sans
 * la vérifier : il se contentait de constater que les titres s'affichaient, ce qui restait vrai
 * d'un écran dont tous les montants auraient été faux.
 *
 * L'écran donne les deux membres de l'égalité : la carte « Récapitulatif Général » et une carte
 * par caissier. Chaque ligne de la première doit totaliser la même ligne dans les secondes.
 *
 * Une exception, et une seule : « Total Mobile ». Ce n'est pas un mode de paiement mais un
 * sous-total des modes mobiles, déjà comptés ligne à ligne — l'additionner reviendrait à
 * compter deux fois. Côté caissier il ne paraît d'ailleurs que pour qui a encaissé sur plus
 * d'un service mobile, et il vit alors dans les lignes en évidence, pas dans la ventilation.
 */
scenario('CPT-09', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const fin = new Date();
  const debut = new Date(fin);
  debut.setDate(debut.getDate() - 60);

  const AGREGAT_REDONDANT = 'Total Mobile';

  const general = page.locator('.recap-card-primary');
  // Les lignes de ventilation d'un caissier portent `recap-item-small` ; ses sous-totaux
  // portent `recap-item-highlight`. Ne retenir que les premières écarte le double compte.
  const lignesCaissiers = page.locator('.recap-card-secondary .recap-item-small');

  /** Le libellé et le montant d'une ligne, quelle que soit la carte qui la porte. */
  const lire = async (ligne: Locator): Promise<{ libelle: string; montant: number }> => {
    const [libelle, valeur] = await Promise.all([
      ligne.locator('.recap-item-label').innerText(),
      ligne.locator('.recap-item-value').innerText(),
    ]);
    return { libelle: libelle.trim(), montant: Number(valeur.replace(/[^\d-]/g, '')) };
  };

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await ouvrirOnglet(page, /Récapitulatif de caisse/);
    await saisirDate(page, 'fromDate', debut);
    await saisirDate(page, 'toDate', fin);
    await rechercher(page);
  });

  await etape(2, async () => {
    // Le total consolidé, puis sa ventilation par mode : c'est elle qu'on rapproche.
    await expect(contenu).toContainText(/RÉCAPITULATIF GÉNÉRAL/i);
    await expect(contenu).toContainText(/VUE D'ENSEMBLE DES PAIEMENTS/i);
    await expect(contenu).toContainText(/ESPECE/i);

    await expect(general).toBeVisible();

    // Ce que chaque caissier a encaissé, mode par mode, tous postes confondus.
    const parLibelle = new Map<string, number>();
    for (const ligne of await lignesCaissiers.all()) {
      const { libelle, montant } = await lire(ligne);
      parLibelle.set(libelle, (parLibelle.get(libelle) ?? 0) + montant);
    }
    expect(parLibelle.size, 'sans aucune ligne de caissier, la consolidation ne prouve rien').toBeGreaterThan(0);

    // Et ce que le récapitulatif général en dit.
    const lignesGenerales = await general.locator('.recap-item').all();
    expect(lignesGenerales.length, 'le récapitulatif général doit porter des lignes').toBeGreaterThan(0);

    let comparees = 0;
    for (const ligne of lignesGenerales) {
      const { libelle, montant } = await lire(ligne);
      if (libelle === AGREGAT_REDONDANT) {
        continue;
      }
      expect(
        parLibelle.has(libelle),
        `« ${libelle} » figure au général sans venir d'aucun caissier`,
      ).toBe(true);
      expect(montant, `consolidation de « ${libelle} »`).toBe(parLibelle.get(libelle));
      comparees++;
    }
    expect(comparees, 'aucune ligne comparée : la vérification serait vide').toBeGreaterThan(0);

    // Dans l'autre sens : rien d'encaissé ne doit manquer au général.
    const librellesGeneraux = new Set(
      await Promise.all(lignesGenerales.map(async ligne => (await lire(ligne)).libelle)),
    );
    for (const libelle of parLibelle.keys()) {
      expect(
        librellesGeneraux.has(libelle),
        `« ${libelle} » a été encaissé mais n'apparaît pas au récapitulatif général`,
      ).toBe(true);
    }
  });
});
