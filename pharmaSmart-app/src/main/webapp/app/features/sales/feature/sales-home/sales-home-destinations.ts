/** Où l'on peut aller depuis le bandeau de l'espace de vente : comptoir, pré-vente, proforma. */
export type DestinationVente = 'comptoir' | 'prevente' | 'proforma';

export interface Destination {
  id: DestinationVente;
  libelle: string;
  icone: string;
  url: string;
}

export interface HabilitationsDestinations {
  comptoir: boolean;
  prevente: boolean;
  proforma: boolean;
}

const DESTINATIONS: Destination[] = [
  { id: 'comptoir', libelle: 'Comptoir', icone: 'pi pi-shopping-cart', url: '/sales-home' },
  { id: 'prevente', libelle: 'Pré-vente', icone: 'pi pi-bookmark', url: '/sales-home/prevente' },
  { id: 'proforma', libelle: 'Proforma', icone: 'pi pi-file-edit', url: '/sales-home/devis' },
];

/**
 * Les destinations autorisées, dans l'ordre comptoir, pré-vente, proforma, SANS l'écran où l'on est déjà :
 * un bouton qui mène là où l'on se trouve n'a pas lieu d'être. Rien d'autre à proposer : liste vide, et le
 * bandeau n'affiche rien.
 */
export function listerDestinations(habilitations: HabilitationsDestinations, ecran: DestinationVente): Destination[] {
  return DESTINATIONS.filter(d => d.id !== ecran && habilitations[d.id]);
}
