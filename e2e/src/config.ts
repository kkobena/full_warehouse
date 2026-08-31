/**
 * Réglages de la campagne, tous surchargeables par variable d'environnement.
 *
 * Aucune valeur sensible ici : les identifiants par défaut sont ceux du jeu de démonstration,
 * jamais ceux d'une installation cliente.
 */
import { resolve } from 'node:path';

/**
 * Racine du dépôt, déduite de l'emplacement de ce fichier (e2e/src/config.ts).
 *
 * `__dirname` et non `import.meta.dirname` : le package.json racine n'est pas un module ES,
 * Playwright transpile donc ces fichiers en CommonJS, où `import.meta` est une erreur de
 * syntaxe — levée au chargement de la configuration, avant tout message utile.
 */
export const RACINE = resolve(__dirname, '..', '..');

/** URL de l'application. Le serveur Angular de développement écoute sur 4200. */
export const BASE_URL = process.env.E2E_BASE_URL ?? 'http://localhost:4200';

export const IDENTIFIANTS = {
  utilisateur: process.env.E2E_USER ?? 'admin',
  motDePasse: process.env.E2E_PASSWORD ?? 'admin',
};

/**
 * Nom du projet Playwright qui déclenche la prise d'images. Le mode capture est porté par le
 * projet plutôt que par une variable d'environnement : `CAPTURE=1 playwright …` dans un script
 * npm échoue sous Windows, où npm passe par cmd.exe. `--project=captures` marche partout.
 */
export const PROJET_CAPTURES = 'captures';

/** Force le mode capture depuis n'importe quel projet — dépannage et configurations jetables. */
export const CAPTURE_FORCEE = process.env.CAPTURE === '1';

/** Dossier des images produites. Non versionné — cf. e2e/.gitignore. */
export const DOSSIER_CAPTURES = process.env.E2E_CAPTURES_DIR
  ? resolve(process.env.E2E_CAPTURES_DIR)
  : resolve(RACINE, 'e2e', 'captures');

/** Index des captures, lu ensuite par generate-cahier-recette-json.ts (lot 4). */
export const FICHIER_INDEX = resolve(DOSSIER_CAPTURES, 'captures.json');

/** État d'authentification produit par auth.setup.ts et rejoué par tous les parcours. */
export const FICHIER_SESSION = resolve(RACINE, 'e2e', '.auth', 'session.json');

/**
 * JPEG plutôt que PNG : ~4x plus léger pour une capture d'interface, différence invisible à
 * l'impression. 180 images pèsent ainsi ~8 Mo au lieu de ~35 Mo.
 *
 * `pleinePage` capture le document entier plutôt que la seule zone visible. Utile pour un
 * écran plus haut que la fenêtre — un long formulaire, un tableau sans pagination. À laisser
 * inactif par défaut : sur une application à en-tête fixe, le rendu plein page duplique
 * souvent les barres et donne une image difficile à lire.
 */
export const IMAGE = {
  type: 'jpeg' as const,
  quality: Number(process.env.E2E_JPEG_QUALITY ?? 80),
  pleinePage: process.env.E2E_FULL_PAGE === '1',
};

/**
 * Taille de la fenêtre du navigateur, c'est-à-dire **l'écran que l'application croit avoir**.
 * C'est ce réglage — et non la netteté — qui décide du nombre de colonnes affichées, du
 * repliement des barres d'outils et du passage en disposition compacte.
 *
 * Défaut 1920 × 1080 : la résolution d'un poste d'officine. En dessous, le manuel montre une
 * application plus à l'étroit que ce que voit l'utilisateur.
 */
export const FENETRE = {
  width: Number(process.env.E2E_VIEWPORT_WIDTH ?? 1920),
  height: Number(process.env.E2E_VIEWPORT_HEIGHT ?? 1080),
};

/**
 * Densité de pixels. À 2, l'image fait le double en largeur comme en hauteur pour la MÊME
 * mise en page : rien de plus n'est visible, c'est plus net à l'impression et environ trois
 * fois plus lourd.
 *
 * À ne pas confondre avec FENETRE : agrandir la fenêtre montre PLUS ; augmenter l'échelle
 * montre la MÊME CHOSE en plus fin.
 */
export const ECHELLE = Number(process.env.E2E_SCALE ?? 1);

/**
 * Cadrage de l'image.
 *
 *   « essentiel » (défaut) : la hauteur est ramenée au bas du contenu applicatif. L'écran des
 *                            différés s'arrête à 768 px sur une fenêtre de 1080 : sans cadrage,
 *                            le tiers inférieur de chaque capture n'est que le fond d'écran
 *                            décoratif de l'application. Fidèle, mais du papier perdu dans un
 *                            manuel imprimé.
 *   « plein »              : toute la fenêtre, fond compris.
 *
 * La LARGEUR n'est jamais rognée : c'est elle qui porte la mise en page, et la recadrer
 * donnerait une image qui ne correspond à aucun écran réel.
 */
export const CADRAGE_ESSENTIEL = (process.env.E2E_CADRAGE ?? 'essentiel') !== 'plein';

/** Marge sous le contenu, pour ne pas raser la bordure de la carte. */
export const MARGE_CADRAGE = 16;

/**
 * Écrase l'index au lieu de le compléter. Par défaut la campagne fusionne, pour qu'une
 * exécution ciblée sur un seul scénario ne fasse pas disparaître les captures des autres.
 */
export const REINITIALISER_INDEX = process.env.E2E_CAPTURES_RESET === '1';
