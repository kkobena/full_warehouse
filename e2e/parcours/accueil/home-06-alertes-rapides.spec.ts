import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Le premier écran que voit un pharmacien : `/` résout le tableau de bord de son rôle. Sa
 * particularité est qu'il ne montre QUE les alertes non nulles — un compteur à zéro n'a pas
 * de pastille du tout. C'est ce qui rend la bande lisible d'un coup d'œil, et c'est aussi ce
 * qui surprend : l'absence d'une pastille est une bonne nouvelle, pas un écran incomplet.
 */
scenario('HOME-06', async ({ etape, page }) => {
  // Les pastilles sont des `<button>` sans rôle particulier ; on les cible par leur classe,
  // faute d'un libellé accessible distinct du compteur qu'elles portent.
  const pastille = (libelle: string) => page.locator('.quick-alert-card').filter({ hasText: libelle });

  await etape(1, async () => {
    await page.goto('/');
    await expect(page.locator('.kpi-strip-item').filter({ hasText: 'CA Net' })).toContainText(/\d/);
  });

  await etape(2, async () => {
    const badges = page.locator('.quick-alert-badge');

    // Le résultat attendu du modèle, mot pour mot : aucune pastille à zéro n'encombre
    // l'accueil.
    await expect(badges.filter({ hasText: /^0$/ })).toHaveCount(0);

    // Et son corollaire : tout compteur affiché est strictement positif. C'est la règle
    // entière. Exiger en plus qu'une pastille NOMMÉE soit présente reviendrait à exiger que
    // la base porte ce type d'alerte — or le modèle dit l'inverse : « chaque pastille
    // n'apparaît que si son compteur est supérieur à zéro ». Le parcours tombait sur
    // « Ajustements » les jours où il n'y en avait aucun, c'est-à-dire sur une bonne
    // nouvelle.
    for (const compteur of await badges.allInnerTexts()) {
      expect(compteur.trim()).toMatch(/^[1-9]\d*$/);
    }

    // Ce qui se vérifie sans rien supposer des données : les pastilles présentes sont
    // prises parmi les cinq que le modèle énumère, et aucune autre.
    const attendus = ['Péremptions', 'Ruptures', 'À commander', 'Ajustements', 'Modif. Prix'];
    for (const libelle of await page.locator('.quick-alert-label').allInnerTexts()) {
      expect(attendus).toContain(libelle.trim());
    }
  });

  await etape(3, async () => {
    // Le modèle laisse le choix entre trois raccourcis ; celui des péremptions est le
    // premier qu'il cite, et c'est lui que montre la capture. Il suppose, lui, qu'il y a
    // des péremptions — assumé et vérifié ici plutôt que subi trois lignes plus bas.
    await expect(
      pastille('Péremptions'),
      'aucune péremption dans le jeu de démonstration : le raccourci ne peut pas être illustré',
    ).toBeVisible();
    await pastille('Péremptions').click();
    // Le raccourci n'ouvre pas une liste filtrée « maison » : il envoie sur l'écran de
    // traitement des péremptions, celui-là même que couvre STK-18.
    await expect(page).toHaveURL(/gestion-peremption/);
    await expect(page.getByRole('tab', { name: /Produits périmés/ })).toBeVisible();
  });
});
