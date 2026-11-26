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
  // Le champ par son identifiant, et lui seul : l'ancien sélecteur `#field_note, textarea`
  // retombait sur la première zone de texte du formulaire — l'ADRESSE, placée avant la note —
  // si bien que le parcours écrivait sa note d'accueil dans l'adresse de l'officine.
  const champNote = page.locator('#field_note');
  const notesAffichees = page.locator('app-magasin-detail .magasin-texte', { hasText: 'Notes' }).locator('p');
  const noteParcours = 'Bienvenue — ouverture 8h/20h, dimanche de garde';
  // Lue avant modification, rétablie à l'identique : aucune valeur supposée.
  let noteOrigine = '';

  await etape(1, async () => {
    await page.goto('/magasin');
    // La fiche rassemble ce que les documents imprimeront : nom, contact, mentions
    // administratives.
    await expect(contenu).toContainText('Informations Générales');
    await expect(contenu).toContainText('Informations Administratives');
    await page.getByRole('button', { name: 'Modifier' }).click();
    await expect(page).toHaveURL(/magasin\/\d+\/edit/);
    await expect(page.locator('#field_fullName')).not.toHaveValue('');
    noteOrigine = await champNote.inputValue();
  });

  await etape(2, async () => {
    // La note d'accueil : elle s'affiche au comptoir et sur les documents qui la reprennent.
    await champNote.fill(noteParcours);
    await expect(champNote).toHaveValue(noteParcours);
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: /Enregistrer|Sauvegarder/ }).first().click();
    await expect(page).toHaveURL(/magasin$/);
    // La fiche porte la nouvelle note : les écrans et documents qui la lisent la reprendront
    // telle quelle.
    await expect(notesAffichees).toHaveText(noteParcours);
  });

  // ── Remise en état : la note d'origine est rétablie. ────────────────────────────────────
  await page.getByRole('button', { name: 'Modifier' }).click();
  await champNote.fill(noteOrigine);
  await page.getByRole('button', { name: /Enregistrer|Sauvegarder/ }).first().click();
  await expect(page).toHaveURL(/magasin$/);
  // Une note vide s'affiche « — ».
  await expect(notesAffichees).toHaveText(noteOrigine || '—');
});
