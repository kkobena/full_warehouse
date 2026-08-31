import { expect } from '@playwright/test';
import { chercherAuCatalogue, chercherDansSelect, choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un tiers payant ne rembourse pas toujours au prix de l'officine : il applique son propre
 * tarif, ou son pourcentage, ou les deux. Le TARIF ASSURANCE fixe cette règle pour un couple
 * produit / organisme, et c'est lui que la vente appliquera au moment de ventiler la part
 * assurance et la part patient (VTE-04, VTE-52).
 *
 * Trois options, et il faut savoir laquelle choisir :
 *  • un PRIX DE RÉFÉRENCE — l'assureur rembourse sur cette base, quel que soit le prix vendu ;
 *  • un POURCENTAGE appliqué au prix de vente ;
 *  • un pourcentage appliqué au prix de référence, quand l'organisme combine les deux.
 *
 * Parcours ÉCRIVANT dans la base : il supprime le tarif qu'il a créé.
 */
scenario('REF-53', async ({ etape, page }) => {
  const produit = 'ARNICA MONTANA 5CH';
  // Le select affiche le NOM COMPLET de l'organisme, pas son sigle : on cherche donc sur ce
  // qui est affiché — ng-select refiltre la réponse du serveur sur le libellé visible.
  const recherche = 'CAISSE NATIONALE';
  const organisme = 'CAISSE NATIONALE';
  // Deux fenêtres se superposent : la LISTE des tarifs du produit, puis le FORMULAIRE
  // d'ajout par-dessus. On les distingue, sinon toute assertion porte sur les deux.
  const liste = page.locator('.modal-content').first();
  const formulaire = page.locator('.modal-content').last();

  const ouvrirTarifs = async (): Promise<void> => {
    // Attendre que plus aucune fenêtre ne soit ouverte : tant que la précédente se referme,
    // son voile intercepte les clics et le menu d'actions reste inatteignable.
    await expect(page.locator('.modal-content')).toHaveCount(0);
    await page.locator('app-produit-list tbody tr').first()
      .getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Tarifs assurance' }).click();
    await expect(liste).toContainText(/Tiers payant/i);
  };

  const supprimerTarif = async (): Promise<void> => {
    const ligne = liste.locator('tbody tr').filter({ hasText: organisme }).first();
    if (!(await ligne.isVisible().catch(() => false))) {
      return;
    }
    await ligne.getByRole('button', { name: 'Supprimer le tarif' }).click();
    const confirmation = page.locator('.modal-content').last();
    await expect(confirmation).toBeVisible();
    await confirmation.getByRole('button', { name: /Oui|Confirmer/ }).click();
    await expect(ligne).toBeHidden();
  };

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await ouvrirTarifs();
    // Partir d'un produit SANS tarif pour cet organisme : le couple (produit, tiers payant)
    // n'en porte qu'un, et une exécution précédente a pu le laisser en place.
    await supprimerTarif();
  });

  await etape(2, async () => {
    await liste.getByRole('button', { name: 'Nouveau' }).click();
    // La fenêtre rappelle le produit, son prix d'achat et son prix de vente : le tarif se
    // décide en regard de ce que l'officine paie et facture.
    await expect(formulaire).toContainText('un tarif assurance');
    await chercherDansSelect(page, 'tiersPayantId', recherche, organisme);
  });

  await etape(3, async () => {
    // L'option choisie commande le champ à renseigner : un montant pour le prix de
    // référence, un taux pour le pourcentage.
    await choisirDansSelect(page, 'type', "Pourcentage appliqué par l'assureur");
    await page.locator('#rate').fill('80');
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    // L'enregistrement referme le formulaire ; la liste, elle, reste parfois ouverte derrière.
    // On la rouvre seulement si elle a disparu — c'est là qu'on vérifie le résultat, comme le
    // ferait l'utilisateur qui veut s'assurer de son geste.
    await expect(page.locator('.modal-content')).toHaveCount(1);
    if (!(await liste.getByText(organisme).first().isVisible().catch(() => false))) {
      await liste.getByRole('button', { name: 'Fermer' }).last().click();
      await ouvrirTarifs();
    }
    await expect(liste).toContainText(organisme);
    await expect(liste).toContainText('80');
  });

  // ── Remise en état : le tarif créé est retiré. ──────────────────────────────────────────
  await supprimerTarif();
});
