import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirEtiquettesProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'application ne connaissait qu'une seule planche, et son calage ne correspondait à aucune
 * référence du commerce : une officine qui achetait un autre consommable ne pouvait rien
 * imprimer de droit. Le catalogue des formats répond à cela — planches A4 de 14 à 65
 * étiquettes, et rouleaux des imprimantes d'étiquettes, groupés par support dans la même liste.
 *
 * Deux conséquences se voient à l'écran, et ce sont elles qui comptent :
 *
 *  * la CAPACITÉ annoncée suit le format, et avec elle la borne de la position de départ.
 *    Passer à une planche plus petite ramène la position à la première case plutôt que de
 *    laisser une valeur qui ne correspond à aucune case ;
 *  * sur un ROULEAU, la position de départ disparaît — elle est masquée, pas grisée. Demander
 *    la case 40 sur un rouleau aurait fait dérouler trente-neuf étiquettes blanches.
 *
 * Le format par défaut, lui, reproduit le calage en place avant le catalogue : ce qui
 * s'imprimait droit hier s'imprime droit aujourd'hui, au millimètre.
 *
 * Parcours en LECTURE : il ne lance aucune impression.
 */
scenario('REF-66', async ({ etape, page }) => {
  const produit = 'ARNICA MONTANA 20DH WELEDA';
  let modale = page.locator('.modal-content');

  await etape(1, async () => {
    modale = await ouvrirEtiquettesProduit(page, produit);
    // Le format par défaut : la planche de 65, celle de l'officine avant le catalogue.
    await expect(modale).toContainText(/65 étiquettes par planche/i);
    await expect(modale).toContainText(/Case de 1 à 65/i);
    await modale.locator('#etiquetteStartAt').fill('40');
    await expect(modale.locator('#etiquetteStartAt')).toHaveValue('40');
  });

  await etape(2, async () => {
    // La planche de 24 : la case 40 n'existe pas sur elle. La position revient à 1 plutôt
    // que de rester hors bornes — un refus à l'impression serait venu trop tard.
    await choisirDansSelect(page, 'etiquetteFormat', '24 étiquettes');
    await expect(modale).toContainText('Avery L7159');
    await expect(modale).toContainText(/24 étiquettes par planche/i);
    await expect(modale).toContainText(/Case de 1 à 24/i);
    await expect(modale.locator('#etiquetteStartAt')).toHaveValue('1');
  });

  await etape(3, async () => {
    // Un rouleau n'est pas une planche : la page EST l'étiquette, l'imprimante déroule. La
    // position de départ n'a plus de sens, et le champ s'en va.
    await choisirDansSelect(page, 'etiquetteFormat', 'Rouleau — 40 × 30 mm');
    await expect(modale).toContainText(/une étiquette par page, 40 × 30 mm/i);
    await expect(modale.locator('#etiquetteStartAt')).toHaveCount(0);
    await expect(modale).not.toContainText(/Position de départ/i);
  });
});
