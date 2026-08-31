import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * L'identité de l'officine n'est pas décorative : c'est elle qui s'imprime en tête des
 * tickets, des factures et des états. Un numéro de téléphone changé ici suit sur tous les
 * documents, sans redémarrage — c'est tout l'intérêt de la centraliser.
 *
 * L'écran distingue quatre blocs : identité, contact, administratif (registre de commerce,
 * compte contribuable, identifiants FNE) et messages affichés au comptoir.
 *
 * Parcours ÉCRIVANT dans la base : il modifie une note d'accueil, puis lui rend sa valeur.
 */
scenario('ADM-13', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const noteOrigine = 'Bienvenue !';
  const noteParcours = 'Bienvenue — ouverture 8h/20h, dimanche de garde';

  await etape(1, async () => {
    await page.goto('/magasin');
    // La fiche rassemble ce que les documents imprimeront : nom, contact, mentions
    // administratives.
    await expect(contenu).toContainText('Informations Générales');
    await expect(contenu).toContainText('Informations Administratives');
    await page.getByRole('button', { name: 'Modifier' }).click();
    await expect(page).toHaveURL(/magasin\/\d+\/edit/);
  });

  await etape(2, async () => {
    // La note d'accueil : elle s'affiche au comptoir et sur les documents qui la reprennent.
    const note = page.locator('#field_note, textarea').first();
    await note.fill(noteParcours);
    await expect(note).toHaveValue(noteParcours);
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: /Enregistrer|Sauvegarder/ }).first().click();
    await expect(page).toHaveURL(/magasin$/);
    // La fiche porte la nouvelle note : les écrans et documents qui la lisent la reprendront
    // telle quelle.
    await expect(contenu).toContainText(noteParcours);
  });

  // ── Remise en état : la note d'origine est rétablie. ────────────────────────────────────
  await page.getByRole('button', { name: 'Modifier' }).click();
  await page.locator('#field_note, textarea').first().fill(noteOrigine);
  await page.getByRole('button', { name: /Enregistrer|Sauvegarder/ }).first().click();
  await expect(contenu).toContainText(noteOrigine);
});
