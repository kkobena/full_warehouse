export type ChromeName = 'actuel' | 'comptant' | 'assurance' | 'prevente-comptant' | 'prevente-carnet';

export interface ChromeOption {
  name: ChromeName;
  label: string;
  /** Pastille du sélecteur : l'accent du thème (`actuel` : l'accent de ses barres). */
  swatch: string;
}

/** Mêmes noms que `$pharma-chrome-themes` dans `_pharma-chrome-themes.scss` ; le défaut en tête. */
/**
 * Les libellés sont de simples noms de couleur : le thème colore toute l'application, pas un écran de vente.
 * Les identifiants gardent l'origine de chaque teinte (accent de la vente comptant, de l'assurance…).
 */
export const CHROMES: readonly ChromeOption[] = [
  { name: 'prevente-comptant', label: 'Indigo', swatch: '#4f4aa8' },
  { name: 'actuel', label: 'Bleu acier', swatch: '#34506b' },
  { name: 'comptant', label: 'Vert', swatch: '#047857' },
  { name: 'assurance', label: 'Bleu', swatch: '#2a74a0' },
  { name: 'prevente-carnet', label: 'Olive', swatch: '#6f6420' },
];

/**
 * Défaut du poste : Indigo, en attendant le retour des essais utilisateurs (D2). Une seule ligne à changer ;
 * le défaut reste en tête de `CHROMES`. « Bleu acier » (`actuel`) reste choisissable.
 */
export const DEFAULT_CHROME: ChromeName = 'prevente-comptant';
export const CHROME_STORAGE_KEY = 'pharmasmart_chrome';

/** Valeur mémorisée sur le poste, ou le défaut si elle est absente, inconnue ou illisible. */
export function readStoredChrome(): ChromeName {
  try {
    return toChrome(localStorage.getItem(CHROME_STORAGE_KEY));
  } catch {
    return DEFAULT_CHROME;
  }
}

export function toChrome(value: string | null): ChromeName {
  return CHROMES.some(c => c.name === value) ? (value as ChromeName) : DEFAULT_CHROME;
}

export function applyChrome(name: ChromeName, root: HTMLElement = document.documentElement): void {
  root.setAttribute('data-chrome', name);
}
