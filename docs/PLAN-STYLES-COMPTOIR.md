# Style de l'espace de travail du comptoir — Billetage

> **Décision du 2026-10-03 : le style Billetage est retenu définitivement.** Les variantes qui ont servi à le choisir
> (actuel, cartes, accent), leur mécanisme de bascule (`ComptoirStyleService`, entrée de menu « Style du comptoir »,
> `?style=`, clé `pharmasmart_comptoir_style`), la grille de comparaison et le protocole d'essai ont été retirés.
> Ce document garde ce qui reste vrai : le constat de départ, ce qui est en place, les palettes, les mesures de contraste
> et les points ouverts.

## 1. Constat de départ

### 1.1 Ce que montraient les captures de l'ancien écran
- Le fond de page (`--pharma-app-bg`) et la zone de travail (blanc) se distinguaient à peine ; le panier n'avait ni
  bordure ni ombre.
- Trois bandeaux saturés de teintes différentes (teal, bleu acier, bleu-gris) sans fonction distincte.
- Le type de vente (comptant / assurance / carnet) ne se lisait qu'au texte, alors que les pastilles de la barre
  latérale (vert / bleu / orange) posaient déjà ce code couleur sans le propager.

### 1.2 Ce qui existe déjà et qu'on réutilise
- `core/theme/theme.service.ts` : trois thèmes (`menthe`, `ardoise`, `clair`) posés en `data-theme` sur `<html>`.
- `content/scss/_pharma-themes.scss` : couleurs **par rôle** (`app-bg`, `surface`, `border`, `text`, `accent`…) en
  variables `--pharma-*`, avec des ratios WCAG AA vérifiés.
- Le style du comptoir s'appuie sur ces variables plutôt que de les contredire : c'est un second axe, orthogonal au
  thème.

### 1.3 Ce que les mesures ont écarté
- Les accents de l'écran de billetage de caisse (`#22c55e`, `#3b82f6`, `#f59e0b`, `#06b6d4`) ne tiennent pas le texte
  blanc (2,15 à 4,23:1) et sont hors charte : ils n'ont servi que de référence d'agencement.
- Éclaircir ou assombrir le **fond** ne crée pas de relief : un gris clair contraste moins avec le blanc (1,10) que la
  menthe (1,16). Le relief vient de la **bordure et de l'ombre**.

### 1.4 La charte : `content/scss`

| Rôle (sévérité Aura / Bootstrap) | Couleur | Blanc dessus | Source |
|---|---|---|---|
| primaire (`$primary`) | emerald-700 `#047857` | 5,48 | `_pharma-bootstrap-palette.scss` |
| succès | green-700 `#15803d` | 5,02 | idem |
| info | sky-700 `#0369a1` | 5,93 | idem |
| avertissement | orange-700 `#c2410c` | 5,18 | idem |
| danger | red-600 `#dc2626` | 4,83 | idem |
| aide (`help`) | purple-600 `#9333ea` | 5,38 | idem |
| accent du thème menthe | teal-700 `#0f766e` | 5,47 | `_pharma-themes.scss` |

- Règle de la charte : *« nuance la plus claire de chaque famille dont le ratio avec le blanc atteint 4,5 »*.
- Élévation : `--pharma-elevation-0…3` (`surface-global.scss`) et `$pharma-shadow-sm/md/lg` (`_pharma-variables.scss`).
- Rayons : 4 / 6 / 8 px (échelle Aura) et `--p-border-radius-xl` à 12 px, que le style Billetage retient.

## 2. Ce qui est en place

- **Jetons** `--comptoir-*` posés sur `:root` (`content/scss/_comptoir-styles.scss`) : rayon de 12 px, bordure, ombre de
  carte (`--pharma-elevation-1`), ombre de pièce maîtresse (`--pharma-elevation-2`), accents par type de vente.
- **Accent du type de vente** : `sales-home` (hôte et `.pharma-sales-layout`) et `vente-depot` posent
  `--comptoir-accent`, `-dark` (78 % vers le noir) et `-tint` (14 % vers le blanc). Les panneaux latéraux (ventes en
  attente, fiche client) en héritent par l'hôte `app-sales-home`.
- **Règles de composants** dans les SCSS des composants de vente, via les mixins de
  `features/sales/shared/styles/_comptoir-style.scss`. Elles s'écrivent `:host-context(:root)`, ce qui leur garde la
  spécificité qu'elles avaient quand elles dépendaient d'un attribut sur `<html>`. Règles qui sortent de
  l'encapsulation : `content/scss/_comptoir-sales.scss` (en-tête de tableau, panneaux latéraux, anneau de focus, ng-select).
- **Cartes** : bordure, rayon et ombre pour la recherche produit, le panier, le résumé des montants avec ses actions
  (une seule carte), les modes de règlement et les panneaux.
- **Sobriété** : l'accent ne sert qu'en filets, icônes et teintes très claires (14 %), jamais en grand aplat saturé ;
  « À ENCAISSER » se détache par sa taille, sa bordure de 2 px et l'ombre.
- **Boutons à l'accent** : valider / enregistrer (plein), « Remise », « Passer en vente », « Changer de mode »
  (contour). « Annuler » reste rouge, « En attente » neutre.
- **Pastilles de règlement** : couleur de marque en liseré, fond à 14 %, texte foncé.
- **Anneau de focus** des champs : halo de 4 px à 30 % dans la couleur de l'accent, sur tout l'écran de vente.
- **Infobulles** (`ngbTooltip`, partout) : ardoise très foncé teinté de 25 % de l'accent du thème, texte blanc
  (`_pharma-themes.scss`).
- Hors accent, volontairement : cartes Assuré / Ayant droit / Tiers payant (couleur par rôle), « Annuler », « En attente ».

## 3. Palettes

La couleur suit **le type de vente** partout, y compris en prévente et en proforma, qui ont **leur propre palette**
pour qu'on ne les confonde pas avec une vente ; l'écran dépôt a la sienne.

| Type | Vente | Prévente et proforma |
|---|---|---|
| Comptant | emerald-700 `#047857` (163°) | indigo `#4f4aa8` (243°) |
| Assurance | bleu santé `#2a74a0` (202°) | prune `#8b3a78` (314°) |
| Carnet | amber-800 `#92400e` (23°) | olive `#6f6420` (52°) |
| **Dépôt** | violet `#5f3dab` (259°) | — |

Le libellé et l'icône du bandeau (portefeuille, bouclier, livre, marque-page, fichier, bâtiment) disent de quel écran il
s'agit : la couleur ne fait que renforcer. Limite connue : sous protanopie et deutéranopie, le brun du carnet (vente) et
l'olive du carnet (documents) se rapprochent (ΔE ≈ 9) ; les deux palettes ne sont jamais à l'écran en même temps.

## 4. Contraste

Référentiel : WCAG 2.2 niveau AA.

| Critère | Seuil | Où |
|---|---|---|
| Texte normal (1.4.3) | ≥ 4,5:1 | libellés, montants, en-têtes de tableau, sous-titres de lot |
| Texte large (1.4.3) | ≥ 3:1 | totaux, « À ENCAISSER » |
| Éléments d'interface et graphismes (1.4.11) | ≥ 3:1 | bordure des champs, case NR, icônes porteuses de sens |
| Focus visible (2.4.7, 2.4.11) | ≥ 3:1 | la bordure colorée du champ porte le contraste ; le halo à 30 % seul n'y suffirait pas |
| Information non portée par la couleur seule (1.4.1) | — | type de vente = couleur **et** icône **et** libellé |

Consigne de conception : **texte blanc seulement sur une couleur dont le ratio est ≥ 4,5:1**, ou texte foncé sur
accent clair ; **jamais** d'accent clair en texte sur blanc.

### 4.1 Outils et résultats

**Outils**
- `core/theme/comptoir-contraste.ts` + `.spec.ts` : lit `$comptoir-accents`, `$comptoir-accents-document`,
  `$comptoir-accent-depot`, les couleurs de pastille et les dérivés dans le SCSS (aucune couleur recopiée) ; échoue si
  une paire passe sous son seuil, si deux types deviennent trop proches (ΔE ≥ 18 sous vision normale, protanopie et
  deutéranopie) ou si l'infobulle perd son contraste. `npm run contraste:comptoir`, avec `CONTRASTE_RAPPORT=1` pour
  le tableau.
- Projet Playwright `a11y` (`npm run e2e:a11y`), avec `@axe-core/playwright` : `e2e/a11y/comptoir.spec.ts` (écrans à
  vide et simulation de daltonisme, captures dans `target/a11y-comptoir/`) et `e2e/a11y/comptoir-en-situation.spec.ts`
  (§4.2).

**Calcul** : 56 paires (3 types × 2 palettes, dépôt, 8 pastilles), toutes au-dessus de leur seuil. Marges les plus faibles :
accent sur teinte (icône, seuil 3:1) à 4,26 pour l'assurance ; blanc sur accent (seuil 4,5:1) à 5,12 pour l'assurance ;
pastille MTN à 5,16.

**Distinction des types (ΔE CIE76 entre accents ; seuil 18)**

| Vision | Vente : paire la plus proche | Documents : paire la plus proche |
|---|---|---|
| normale | comptant / assurance : 50 | comptant / assurance : 34 |
| protanopie | comptant / carnet : 29 | comptant / assurance : 25 |
| deutéranopie | comptant / carnet : 38 | comptant / assurance : 37 |
| tritanopie (≈ 0,01 % de la population) | comptant / assurance : 9 | assurance / carnet : 29 |

Le dépôt est à plus de 24 de chaque type de la vente, même sous protanopie et deutéranopie. Entre les deux palettes :
37 en vision normale.

**axe, écrans à vide** : 0 défaut. Limite : axe ne sait pas évaluer un texte posé sur un dégradé (« incomplets », 5 à 9
éléments par écran, dont le titre du bandeau) ; ces zones ne sont couvertes que par le calcul.

### 4.2 Contrastes en situation

`e2e/a11y/comptoir-en-situation.spec.ts` rejoue axe sur des écrans **remplis** : panier comptant avec « À ENCAISSER »
et les boutons, sélection de l'assuré, assurance avec son encart (matricule, taux, bon), vente carnet (liste des clients
carnet, puis client choisi avec une ligne), fiche client en panneau ouverte depuis l'assurance (couverture, dernières
délivrances, alertes), prévente avec une ligne (palette des documents), écran dépôt avec un dépôt et une ligne, panneau des ventes en attente, et chacune des pastilles
de mode de règlement. Chaque scénario ouvre puis abandonne
une vente : **à jouer sur la base de démonstration, jamais sur une base d'officine.**

Le premier passage a révélé des défauts de l'ancien écran, corrigés à la source (couleurs de base des composants) :

| Élément | Contraste mesuré | Correction |
|---|---|---|
| TOTAL de ligne et total du pied, vert `#5cb85c` | 2,48 sur blanc ; 1,93 sur la ligne sélectionnée | `#1b5e20`, puis l'accent assombri |
| Libellé « TOTAUX », `#5b89a6` | 3,76 | `#3f6178`, puis l'accent assombri |
| Taux du tiers payant (`.rate-value`) | 3,70 | `#991b1b` |
| Sous-texte « Les ventes mises en attente… » | 4,34 | texte atténué du thème, sans opacité |
| Pastilles Orange Money et Wave, blanc sur orange / cyan | 2,62 et 2,73 | liseré et teinte claire, texte foncé |

**Résultat : 0 défaut axe** sur les dix scénarios (panier comptant, sélection de l'assuré, encart assuré, sélection du
client carnet, encart carnet, fiche client, prévente avec lignes, dépôt, ventes en attente, pastilles). La fiche client chargée avec ses
données est aussi à 0 défaut quand axe ne regarde que le panneau (54 éléments contrôlés, aucun « incomplet »).

La prévente n'ayant pas de bouton « Annuler », son scénario l'enregistre puis la supprime de la liste, en la retrouvant par
un montant que les autres ventes de démonstration n'ont pas (7 × 19 310 = 135 170 F) ; il supprime toutes celles de ce
montant, et échoue si aucune n'est retrouvée.

**Le bandeau** (`.pharma-sales-header`), que axe ne sait pas évaluer (fond dégradé), est contrôlé à part par
`mesurerBandeau` à chaque scénario : le texte est rendu transparent, la page photographiée, et le pixel de fond le
moins contrasté derrière chaque texte, icône et bordure de contrôle est comparé à son seuil (4,5:1 texte, 3:1 icône et
bordure) ; la taille des cibles (44 px) est contrôlée dans le même passage. Il a relevé et fait corriger : bordure du
sélecteur de vendeur à 2,9:1 (assombrie de 30 %), icône « Sélectionner un client » à 2,5:1 (gray-600), bouton « En
attente » désactivé à 2,1:1 sur son compteur (teinte neutre pleine au lieu de 65 % d'opacité), triangle du plafond à
2,97:1 et icône téléphone du dépôt à 3,0:1. Le bandeau garde sa hauteur (62 px, 54 px au dépôt) : les cibles passent à
44 px par `min-height` et marge négative, sans marge, padding ni bordure ajoutés.

**Non couvert :** les textes sur dégradé hors bandeau, qu'axe classe « incomplets » (9 à 13 éléments sur les écrans de
vente remplis). Un faux positif a été vu : une transition de 0,2 s mesurée à mi-course ; le test coupe désormais les transitions avant de
mesurer.

## 5. Tenue aux tailles d'écran des officines (2026-10-03)

Mesurée sur un panier de 5 lignes (assurance, assuré choisi), aux tailles CSS utiles de chaque appareil :

| Appareil | Fenêtre | Types de vente | Règlement | Page |
|---|---|---|---|---|
| 24 pouces, 1080p à 100 % | 1920 × 960 | colonne | visible sans défiler | ne défile pas |
| 24 pouces / portable 15,6" à 125 % | 1536 × 740 | colonne | visible (collé en bas) | ne défile pas |
| Surface Pro 10 paysage | 1440 × 830 | **rangée** | visible | ne défile pas |
| iPad Pro 13 paysage | 1376 × 920 | **rangée** | visible | ne défile pas |
| Portable 1366 | 1366 × 650 | **rangée** | visible (collé en bas) | ne défile pas |
| Nest Hub Max | 1280 × 720 | **rangée** | visible (collé en bas) | ne défile pas |
| iPad Pro 13 portrait / Surface Pro 10 portrait | 1032 × 1260 / 960 × 1250 | **rangée** | visible | ne défile pas |

**Défauts trouvés et corrigés**
- **La page défilait toujours de 52 à 67 px** : les écrans de vente et dépôt avaient `height: 100vh` alors qu'ils commencent sous
  la barre de navigation. Le bas de la zone, où défile le panier, restait hors de l'écran. `HauteurEcranVenteDirective` mesure la
  place restante (`--hauteur-ecran-vente`) et la recalcule au redimensionnement.
- **Le règlement sortait de l'écran** dès que le panier dépassait quelques lignes : `app-payment-mode` est maintenant collé au bas de
  la zone qui défile (`position: sticky`). Son hôte est transparent et sans clic, pour ne pas masquer les boutons Finaliser,
  En attente et Annuler de la colonne de droite.
- **Les types de vente prennent 240 px de large** sur des écrans déjà étroits : à 1440 px et moins, ils passent en rangée
  au-dessus de la zone de travail (onglets de 44 px, sous-titres masqués, soulignement à l'accent du type de vente), et la zone de
  vente gagne environ 160 px. L'orientation de `ngbNav` suit, pour le clavier.
- **Trois modes de règlement** (au lieu de deux) : `maxPaymentModes` passe à 3 par défaut, les trois lignes se mettent côte à côte
  (la carte prend toute la largeur), et la répartition automatique du solde est généralisée — le solde va sur la dernière ligne
  qu'on ne saisit pas (l'avant-dernière si c'est la dernière qu'on saisit). Vérifié : 1 500 / 2 000 / 8 350, puis 1 500 / 9 850 / 500
  pour un total de 11 850.
- **Champ de recherche client** rangé derrière « Changer d'assuré » dès qu'un client et au moins un produit sont au panier.
- **Bandeau désorganisé à la création d'une vente, à 1440 px et moins** : le panneau client (400 px) s'ajoute à droite, le bloc de
  droite gardait ses 788 px et le titre passait sur 2 à 4 lignes (bandeau de 92 à 165 px) avec le sélecteur de vendeur qui débordait.
  À 1440 px et moins, « Vendeur », « Pré-vente », « Proforma » et « En attente » passent en icône seule (texte conservé pour les lecteurs
  d'écran, infobulle ajoutée sur les boutons de navigation), le sélecteur de vendeur passe de 200 à 160 px, le titre ne passe plus à la
  ligne et le panneau client prend 280 px (rétrécissable) ; sous 1100 px, le sous-titre disparaît, le sélecteur passe à 130 px et le
  panneau client à 220 px. Mesuré de 1536 à 960 px : une seule ligne, 62 à 67 px de haut. Le panneau client y était **simulé** (un bloc de
  même gabarit), pour ne pas créer de vente ; à confirmer avec le vrai panneau.

- **Menu principal de l'application qui débordait de la fenêtre à 1440 px et moins** (`navbar-collapse` de 1300 px, `scrollWidth` 1502 dans
  une fenêtre de 1366 : la page défilait à l'horizontale). Il a besoin de 1618 px au naturel, 1370 px avec libellés sur deux lignes.
  Trois paliers dans `layouts/navbar` : au-dessus de 1500 px, inchangé ; de 1200 à 1500 px, resserré (marges 0,35 rem, police 0,88 rem,
  version masquée) ; de 1200 à 1300 px, encore plus (0,25 rem, 0,85 rem, sans les flèches) ; sous 1200 px, replié derrière le bouton
  hamburger (`navbar-expand-xl`). Le gabarit s'ouvrait dès 768 px (`navbar-expand-md`) alors que le bouton restait affiché jusqu'à 992 px
  (`d-lg-none`) : les deux sont alignés sur 1200 px. Mesuré de 1920 à 960 px : plus aucun débordement horizontal ; le sous-menu s'ouvre
  depuis la liste repliée.
- **Champ de quantité** : avec les boutons −, + et ✓ à 44 px, il n'affichait plus que « Quanti » ; la zone passe de 210 à 250 px.
- **Pastille de mode de règlement** (`mode-chip`) réduite de 150 à 100 px : plus de place pour le montant, surtout à trois modes.

**Reste à voir** : en portrait à 960 px, le titre du bandeau passe sur trois lignes (le bandeau garde sa forme, non touché) ;
sous 1200 px, `isSmallScreen` (< 1800 px) est évalué une seule fois au chargement, pas à la rotation de l'appareil.

Garde-fou : `e2e/a11y/comptoir-responsive.spec.ts` rejoue ces tailles (page sans défilement propre, règlement à l'écran, types de vente
en rangée à 1440 px et moins, onglets de 44 px). **Il écrit dans la base** et suppose une base de démonstration réinitialisée, sans
vente en cours : il n'a pas encore été joué en entier — les mêmes mesures ont été prises par script.

## 6. Points ouverts

- Ajouter les accents retenus (assurance, carnet, palette des documents, dépôt) à la charte
  (`_pharma-bootstrap-palette.scss`) : ils n'y figurent pas, seuls leurs contrastes sont vérifiés.
- Cibles tactiles : le poste de vente est à 44 px (boutons de ligne, « Remise », −/+/✓, boutons d'action du pied, « Fermer »
  des panneaux, **bandeau compris** ; case NR à 24 px, le minimum WCAG 2.5.8). Reste sous 44 px la barre de navigation globale
  de l'application, hors de l'espace de travail.
- Fatigue visuelle et charge d'une couleur par onglet : aucun essai utilisateurs n'a eu lieu ; à surveiller à l'usage.
- Ventes de test laissées dans la base de démonstration par les essais de cette session : à purger depuis « Ventes en cours ».
