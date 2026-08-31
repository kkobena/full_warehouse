import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La facture du grossiste ne tombe pas toujours juste sur le bon de livraison : une remise
 * oubliée, un prix révisé entre la commande et la livraison, une ligne facturée deux fois.
 * L'écart se discute — mais la discussion prend des jours, et le contrôle déjà fait ne doit
 * pas être perdu pour autant.
 *
 * Le rapprochement s'enregistre donc MÊME EN ÉCART : le bouton change de libellé et de
 * couleur (« Enregistrer avec écart » plutôt que « Valider — Réconcilié »), et le statut
 * conservé distingue une facture réconciliée d'une facture en litige. À la réouverture, les
 * montants saisis sont rechargés : on reprend là où on s'était arrêté, sans ressaisie.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre un rapprochement en écart, puis le rouvre.
 */
scenario('ACH-78', async ({ etape, page }) => {
  const liste = page.locator('app-list-bons');
  const espace = page.locator('app-reconciliation-workspace');
  // Sans tiret : le champ de référence n'accepte que des caractères alphanumériques, et
  // rejette la saisie entière — le champ reprend alors sa valeur d'avant, sans rien dire.
  const reference = `FAECART${Date.now().toString().slice(-5)}`;
  let bon = '';

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    const clos = liste.locator('tbody tr').filter({ hasText: 'Clôturé' }).first();
    bon = (await clos.innerText()).match(/BL\d+/)?.[0] ?? '';
    await clos.getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: /Rapprocher la facture|Modifier la réconciliation/ }).click();
    await expect(espace).toBeVisible();

    // Une facture dont le montant HT ne correspond pas au bon : c'est l'écart qu'on veut
    // conserver, pas masquer.
    await espace.locator('input[placeholder="N° facture fournisseur"]').fill(reference);
    // Le montant HT facturé, dans la ligne « Montant HT » du tableau de comparaison —
    // et non le premier champ venu : la date de facture en occupe un aussi.
    const montant = espace.locator('.rw-compare__row').filter({ hasText: 'Montant HT' })
      .locator('input').first();
    await montant.click();
    await montant.fill('');
    await montant.pressSequentially('123456', { delay: 30 });
    await expect(espace).toContainText(/Écart/);
  });

  await etape(2, async () => {
    // Le bouton dit ce qu'il fera : enregistrer AVEC l'écart, et non valider un
    // rapprochement qui n'en est pas un.
    await expect(espace.getByRole('button', { name: /Enregistrer avec écart/ })).toBeVisible();
    await espace.getByRole('button', { name: /Enregistrer avec écart/ }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible({ timeout: 20000 });
  });

  await etape(3, async () => {
    // Le bon rouvre son rapprochement sous un autre libellé : il n'est plus à faire, il est
    // à MODIFIER — le contrôle est historisé.
    // La liste est rechargée : c'est elle qui porte le statut de réconciliation, et le
    // libellé de l'action en dépend.
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    const ligne = liste.locator('tbody tr').filter({ hasText: bon }).first();
    await ligne.getByRole('button', { name: 'Actions' }).click();
    await expect(page.getByRole('button', { name: 'Modifier la réconciliation' })).toBeVisible();
  });

  await etape(4, async () => {
    await page.getByRole('button', { name: 'Modifier la réconciliation' }).click();
    await expect(espace).toBeVisible();
    // Les valeurs saisies sont rechargées : la référence de facture est là, et l'écart avec
    // elle. On peut corriger sans tout ressaisir.
    await expect(espace.locator('input[placeholder="N° facture fournisseur"]')).toHaveValue(reference);
  });
});
