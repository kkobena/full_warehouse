# Plan — Thèmes multiples et charte contextuelle

> Analyse de faisabilité et plan de mise en œuvre, rédigés le 24 août 2026.
> Toutes les mesures de ce document ont été relevées sur l'arbre à cette date et sont
> reproductibles par les commandes indiquées.

---

## 1. Résumé

Le multi-thème est **réalisable**, mais pas là où on l'attend. La couche de tokens existe déjà
et fonctionne ; l'obstacle est ailleurs, dans **2 071 couleurs écrites en dur** réparties sur
181 feuilles de composants.

Plus important : **l'application livre déjà un demi-thème sombre, actif, et cassé**. Le bloc
`@media (prefers-color-scheme: dark)` de `_pharma-tokens.scss` est présent dans le
`styles.css` de production. Sur un poste dont le système est en mode sombre, une partie des
couleurs bascule pendant que les 2 071 valeurs figées restent claires. Ce n'est pas une
hypothèse : `customer-edit-modal.component.scss` peint `background: white` (l. 111) et
`color: var(--p-text-color)` (l. 131) — en mode sombre, ce token vaut `#ffffff`, soit du
**texte blanc sur fond blanc**.

Le chantier proposé n'est donc pas « ajouter des thèmes » mais « régulariser un thème déjà
livré, puis en tirer parti ». Cela change l'ordre des lots : le premier lot a une valeur
propre, même si l'on décidait ensuite de ne jamais livrer de second thème.

Trois thèmes sont proposés, plus un mécanisme paramétrique :

| Thème | Contexte officinal | Statut |
|---|---|---|
| **Comptoir** | Vente au comptoir, journée, éclairage plafonnier | Existant, devient le défaut explicite |
| **Garde** | Service de nuit, officine en garde, lumière ambiante éteinte | Nouveau |
| **Confort** | Presbytie, écrans anciens, forte lumière parasite | Nouveau |
| **Charte enseigne** | Groupement ou enseigne imposant ses couleurs | Mécanisme paramétrique, adossé à la licence |

---

## 2. État des lieux mesuré

### 2.1 Quatre couches de couleur, trois régimes différents

| Couche | Volume | Commutable à l'exécution ? |
|---|---|---|
| Variables **Sass** (`$primary`, palette Bootstrap) | 138 l. dans `_pharma-bootstrap-palette.scss` | **Non** — figées à la compilation |
| Tokens **`--p-*`** (snapshot Aura) | 160 tokens, **473 usages** | **Oui** |
| Tokens **`--pharma-*`** (nav, focus ring) | 9 tokens, définis dans `global.scss` l. 1388 | **Oui** |
| **Hex en dur** dans les SCSS de composants | **2 071 occurrences / 181 fichiers** (sur 369) | **Non** |

```bash
# Reproduire les mesures
cd pharmaSmart-app/src/main/webapp
grep -rohE "#[0-9a-fA-F]{3,8}\b" --include=*.scss app/ | wc -l   # 2071
grep -rlE  "#[0-9a-fA-F]{3,8}\b" --include=*.scss app/ | wc -l   # 181
grep -roh  "var(--p-[a-z0-9-]*"  --include=*.scss --include=*.html app/ content/ | wc -l  # 473
```

Les dix teintes les plus répandues concentrent l'essentiel du travail :

| Occurrences | Valeur | Ce qu'elle représente | Token de destination |
|---|---|---|---|
| 58 | `#6b7280` | gris de texte secondaire | `--p-text-muted-color` |
| 58 | `#5b89a6` | bleu ardoise « PharmaSmart » | *(à promouvoir en token d'accent)* |
| 55 | `#ffffff` | fond de carte | `--p-surface-0` |
| 55 | `#f8f9fa` | fond de zone | `--p-surface-50` |
| 55 | `#e2e8f0` | séparateur | `--p-content-border-color` |
| 52 | `#dc3545` | danger (Bootstrap 4 hérité) | `--p-red-600` |
| 44 | `#6c757d` | gris secondaire (Bootstrap 4 hérité) | `--p-surface-500` |
| 37 | `#008cba` | **teal Bootswatch yeti, officiellement abandonné** | à supprimer |
| 35 | `#e5e7eb` | séparateur clair | `--p-content-border-color` |
| 33 | `#e9ecef` | fond de zone | `--p-surface-100` |

Deux enseignements. D'abord, ces dix valeurs représentent ~24 % du total : un traitement par
fréquence décroissante donne un rendement très supérieur à un balayage fichier par fichier.
Ensuite, la liste révèle **trois générations de palettes superposées** — Bootstrap 4
(`#dc3545`, `#6c757d`), Bootswatch yeti (`#008cba`, pourtant déclaré abandonné par
[PLAN-DECOMMISSIONNEMENT-BOOTSWATCH.md](PLAN-DECOMMISSIONNEMENT-BOOTSWATCH.md) §5) et Aura
(`#e2e8f0`, `#6b7280`). Le multi-thème est l'occasion de solder cette dette, mais il en
hérite aussi : **il ne peut pas être livré sans la solder au moins partiellement**.

### 2.2 Le cas Bootstrap : moins bloquant qu'annoncé

`_pharma-bootstrap-palette.scss` avertit qu'un alias CSS n'aurait « aucun effet » sur les
boutons. C'est exact pour les *variables Sass*, mais la sortie compilée est plus favorable
qu'il n'y paraît :

```css
.btn-primary{--bs-btn-color:#fff;--bs-btn-bg:#047857;--bs-btn-border-color:#047857;
             --bs-btn-hover-bg:rgb(3.4,102,73.95); … }   /* 14 déclarations */
```

Bootstrap 5.3 **fige les valeurs, mais dans des propriétés personnalisées**. Un thème peut
donc les réécrire :

```scss
[data-theme='garde'] .btn-primary {
  --bs-btn-bg: #34d399;
  --bs-btn-color: #12181f;
  /* … */
}
```

Coût réel : 21 blocs `.btn-*` colorés et 68 règles `.text-*` / `.bg-*` / `.border-*` dans le
`styles.css` de 428 Ko. C'est mécanique, donc **générable par un mixin Sass** — pas 89 blocs
à écrire à la main. Aucune recompilation par thème n'est nécessaire.

### 2.3 Le point d'entrée existe déjà

`app/core/theme/theme.service.ts` est un stub, conservé après le retrait de PrimeNG
précisément « le temps qu'un futur thème custom PharmaSmart ne le remplace ». Il est déjà
injecté et appelé par `app.component.ts` au démarrage. Le câblage applicatif est donc à
écrire, mais sa place est réservée et documentée.

### 2.4 Un écart d'accessibilité préexistant

Mesuré sur les valeurs actuelles :

| Élément | Valeur | Ratio | Seuil WCAG | Verdict |
|---|---|---|---|---|
| Bordure de champ `--p-form-field-border-color` | `#cbd5e1` sur blanc | **1,48:1** | 3:1 (1.4.11) | ✗ |
| Bordure au survol `--p-form-field-hover-border-color` | `#94a3b8` sur blanc | **2,56:1** | 3:1 | ✗ |
| Texte principal | `#334155` sur blanc | 10,35:1 | 4,5:1 | ✓ AAA |
| Texte atténué | `#64748b` sur blanc | 4,76:1 | 4,5:1 | ✓ |
| Bouton primaire | blanc sur `#047857` | 5,48:1 | 4,5:1 | ✓ |

Le travail mené sur `$min-contrast-ratio` (plan Bootswatch §3.3) a correctement traité le
**texte**. Le critère 1.4.11 « Contraste des éléments non textuels », qui couvre la bordure
d'un champ de saisie lorsqu'elle est le seul indicateur de sa présence, ne l'a pas été. Ce
n'est pas un défaut du multi-thème, mais le thème **Confort** y répond directement, et le
contrat de tokens (§4) empêche la régression de se rejouer sur chaque nouveau thème.

---

## 3. Ce qui rend le chantier possible, et ce qui le contraint

**Favorable.** Le vocabulaire sémantique existe (`--p-text-color`, `--p-surface-*`,
`--p-highlight-*`) et il est déjà consommé 473 fois ; la structure à deux niveaux
(primitives numérotées → tokens sémantiques) est celle qu'exige un système multi-thème ; un
bloc sombre complet est déjà écrit et sert de référence de valeurs ; le point d'injection
applicatif est réservé.

**Contraignant.** Les 2 071 valeurs figées imposent que la conversion précède les thèmes —
sans quoi chaque thème ajouté multiplie les régressions visuelles. La palette Sass Bootstrap
ne pourra jamais varier à l'exécution *au niveau Sass* : le contournement passe par les
`--bs-*`, ce qui crée deux chemins de theming à maintenir. Enfin, `vendor.scss` et
`global.scss` doivent rester en `@import` (cf. [PLAN-MIGRATION-SASS-USE.md](PLAN-MIGRATION-SASS-USE.md)
§5.2) : le mixin de génération devra vivre dans un partial compatible.

---

## 4. Le contrat de tokens

Rien de ce qui suit ne fonctionne sans une règle unique, et elle doit être tenue :

> **Une feuille de composant ne nomme jamais une couleur. Elle nomme un rôle.**

Trois niveaux, dans cet ordre de dépendance :

1. **Primitives** — `--p-emerald-700`, `--p-slate-200`. Une valeur littérale, aucune
   signification. *Ne varient jamais selon le thème.*
2. **Sémantiques** — `--p-text-color`, `--p-surface-0`, `--p-content-border-color`,
   `--pharma-nav-bg`. Un rôle, pointant vers une primitive. **C'est la seule couche qu'un
   thème redéfinit.**
3. **Composant** — `--activity-card-accent`, `--kpi-strip-value-color`. Dérivées des
   sémantiques, locales à un composant, jamais redéfinies par un thème.

Le thème s'applique par un attribut sur `<html>` :

```html
<html data-theme="garde">
```

```scss
:root                      { /* Comptoir — valeurs par défaut */ }
:root[data-theme='garde']  { /* redéfinit uniquement la couche 2 */ }
:root[data-theme='confort']{ /* idem */ }
```

**Pourquoi un attribut et non une classe** : une classe sur `<body>` n'atteint pas les
overlays du CDK ni les modales ng-bootstrap, qui sont déplacés en fin de `<body>` — mais
tous restent descendants de `<html>`. Le projet en a déjà fait les frais côté z-index
(`global.scss` l. 1383, `.cdk-overlay-container` remonté à 1052).

**Sur `prefers-color-scheme`** : le bloc média actuel doit être **remplacé**, pas complété.
Une préférence système ne doit pas court-circuiter un choix explicite. La logique correcte
est celle-ci — et elle est déjà, à la lettre, celle que ce dépôt applique ailleurs :

```scss
:root:not([data-theme='comptoir'])  { @media (prefers-color-scheme: dark) { /* tokens Garde */ } }
:root[data-theme='garde']           { /* tokens Garde — l'emporte dans les deux sens */ }
```

---

## 5. Les thèmes proposés

Tous les ratios ci-dessous ont été calculés (formule WCAG 2.1 relative luminance), pas
estimés.

### 5.1 Comptoir — le défaut, inchangé

Aucune couleur nouvelle. Le thème actuel devient une valeur nommée plutôt qu'un implicite.

**Justification.** Le comptoir est un contexte de lecture rapide, debout, sous éclairage
plafonnier fort, souvent avec un écran mat vieillissant. Le fond clair maximise le contraste
utile dans ces conditions et reste le meilleur choix. L'émeraude `#047857` est déjà
argumentée et conforme ; la remettre en cause n'apporterait rien.

### 5.2 Garde — service de nuit

C'est le thème dont l'argument est le plus spécifiquement officinal, et le seul qu'un
logiciel généraliste n'aurait pas.

**Justification.** Une officine de garde travaille dans une salle dont l'éclairage est
réduit, et le pharmacien fait des allers-retours entre son poste et le sas de nuit. Un écran
clair plein format à 2 h du matin produit deux effets mesurables : il détruit l'adaptation
scotopique — la récupération complète de la vision nocturne demande vingt à trente minutes,
soit plus que l'intervalle entre deux clients —, et il provoque une halation marquée chez les
sujets presbytes, d'autant que la pupille est dilatée. Le thème sombre n'est donc pas une
préférence esthétique dans ce contexte : il conditionne la capacité à identifier une
personne derrière la vitre du sas juste après avoir lu une ordonnance à l'écran.

**Choix de conception.** Le fond n'est pas noir. `#000000` maximise le contraste, mais il
provoque du *halo* sur les caractères clairs et, sur dalle OLED, une rémanence au défilement
des tableaux. Un bleu-gris très sombre conserve la profondeur sans ces défauts. Le texte
n'est pas blanc pur non plus : 21:1 fatigue en session longue ; 15:1 suffit largement.

| Rôle | Valeur | Ratio sur le fond | Verdict |
|---|---|---|---|
| Fond principal `--p-surface-0` | `#12181f` | — | — |
| Fond surélevé `--p-surface-50` | `#1a212a` | — | — |
| Fond de carte `--p-surface-100` | `#222b36` | — | — |
| Texte principal | `#e6edf3` | **15,11:1** | AAA |
| Texte atténué | `#9fb0c0` | **8,03:1** | AAA |
| Primaire | `#34d399` | **9,29:1** | AAA |
| Succès | `#4ade80` | **10,25:1** | AAA |
| Avertissement | `#fbbf24` | **10,70:1** | AAA |
| Danger | `#f87171` | **6,45:1** | AA |
| Information | `#38bdf8` | **8,33:1** | AAA |
| Bordure de champ | `#5b6575` | **3,03:1** | AA (1.4.11) |
| Texte sur bouton primaire | `#12181f` sur `#34d399` | **9,29:1** | AAA |

Deux points méritent d'être signalés. Le **danger** est le plus faible du jeu à 6,45:1 : sur
fond sombre, un rouge saturé plus foncé passerait sous le seuil, et un rouge plus clair
virerait au rose et perdrait sa valeur d'alerte. 6,45:1 dépasse confortablement AA ; c'est le
bon compromis. La **bordure de champ à 3,03:1** a été calculée pour atteindre exactement le
seuil 1.4.11 : mes premières valeurs candidates (`#2c3a4a`, `#3d5166`) plafonnaient à 1,54:1
et 2,18:1 et ont été écartées par le calcul, pas par l'œil.

Le bouton primaire porte un texte **sombre**, non blanc. C'est contre-intuitif mais imposé
par la physique : sur un émeraude clair, le blanc plafonnerait à 1,7:1.

### 5.3 Confort — contraste renforcé

**Justification.** Deux populations, une contrainte réglementaire. La démographie des
titulaires et préparateurs rend la presbytie courante ; les postes de comptoir sont
fréquemment exposés à une vitrine, donc à une lumière rasante qui écrase le contraste
perçu ; et le critère WCAG 1.4.11, aujourd'hui non tenu sur les bordures de champ, cesse
d'être un détail dès qu'un appel d'offres ou un groupement l'exige.

Ce thème ne se contente pas d'assombrir le texte : il **rend visibles les frontières** que
le thème Comptoir suggère par des gris très clairs.

| Rôle | Valeur | Ratio sur blanc | Verdict |
|---|---|---|---|
| Texte principal | `#111827` | **17,74:1** | AAA |
| Texte atténué | `#374151` | **10,31:1** | AAA |
| Primaire | `#065f46` | **7,68:1** | AAA |
| Danger | `#991b1b` | **8,31:1** | AAA |
| Avertissement | `#9a3412` | **7,31:1** | AAA |
| Information | `#1e40af` | **8,72:1** | AAA |
| Bordure de champ | `#6b7280` | **4,83:1** | AA (1.4.11 largement tenu) |

S'y ajoutent, hors couleur : anneau de focus porté à 3 px, séparateurs de tableau à 1 px
opaques au lieu des `rgba()` actuels, et suppression des dégradés de fond de carte — un
dégradé fait varier le contraste local le long de la surface, ce qui interdit de garantir un
ratio.

### 5.4 Charte enseigne — mécanisme, pas thème

**Justification.** C'est le seul volet à valeur commerciale directe, et
[ARGUMENTAIRE-COMMERCIAL-PHARMASMART.md](ARGUMENTAIRE-COMMERCIAL-PHARMASMART.md) en est le
destinataire naturel. Un groupement qui déploie sur trente officines veut ses couleurs ;
c'est une ligne de facturation, pas une préférence utilisateur.

**Ce qu'il ne faut pas faire** : ouvrir toute la palette. Un client qui choisit librement
quinze couleurs produira un jeu non conforme dès la première tentative, et le support en
héritera.

**Ce qu'il faut faire** : n'exposer qu'**une seule** couleur d'accent, et dériver le reste.
L'échelle 50→800 se calcule, le choix texte clair / texte sombre se déduit du ratio, et une
teinte dont aucune nuance n'atteint 4,5:1 est **refusée à la saisie**, avec la nuance
conforme la plus proche proposée en remplacement. Les couleurs sémantiques — succès,
avertissement, danger — ne sont **jamais** paramétrables : leur valeur est leur signification,
et un « danger » vert au nom de la charte d'un groupement est un risque d'erreur de
dispensation.

Adossement licence : une entrée `CHARTE_ENSEIGNE(true, "Charte graphique personnalisée")`
dans `Feature`, en `optional = true` — donc accordée uniquement si explicitement listée,
conformément à la règle d'octroi documentée dans l'enum.

### 5.5 Deux thèmes envisagés puis écartés

**« Back-office ».** L'idée d'une variante désaturée pour les sessions longues de
comptabilité ne résiste pas : la fatigue en session longue tient à la densité, à
l'interlignage et à la taille de police, pas à la saturation. Cela relève d'un **axe de
densité** (`data-density="compact|confort"`), orthogonal au thème et à traiter séparément.
Le déguiser en thème de couleur reviendrait à livrer un placebo.

**« Tablette / rayon ».** Même diagnostic : le besoin réel est une cible tactile de 44 px et
une police plus grande sous forte lumière. C'est de la densité et du responsive, pas de la
couleur.

---

## 6. Découpage

Les lots sont ordonnés par **valeur décroissante par unité d'effort**, pas par ordre logique
de construction. T1 a une valeur propre même si le chantier s'arrête là.

### T0 — Neutraliser le demi-thème actif *(préalable, 0,5 j)*

Retirer ou neutraliser le bloc `@media (prefers-color-scheme: dark)` de
`_pharma-tokens.scss`. Il ne rend service à personne aujourd'hui et produit du texte blanc
sur fond blanc chez tout utilisateur en mode sombre système. **À faire indépendamment de la
suite** : c'est une correction de bug, pas une étape de theming.

### T1 — Le contrat et l'outillage *(3 j)*

Figer la liste des tokens sémantiques ; écrire le mixin de génération des `--bs-*` par
thème ; écrire un **script de vérification de contraste** exécuté en CI, qui refuse toute
paire rôle/fond sous le seuil. Sans ce script, la conformité tiendra le temps d'une revue et
se dégradera ensuite.

Ajouter une règle de lint interdisant les hex littéraux dans `app/**/*.scss` — en
avertissement d'abord, le temps de T2.

### T2 — Conversion des couleurs figées *(le gros du chantier)*

Par fréquence décroissante (§2.1) et non par fichier. Les dix premières valeurs couvrent
~24 % du volume et se remplacent par `sed` avec relecture. Le `#008cba` de yeti (37
occurrences) est à supprimer, pas à convertir : il est déjà déclaré abandonné.

Méthode de vérification, reprise de la phase 1 du plan Bootswatch parce qu'elle a fait ses
preuves : build → copie de `styles.css` → modification → rebuild → `diff`. Sur un lot de
conversion pure, **le diff doit être vide**.

### T3 — Thème Garde *(2 j après T2)*

Le bloc de tokens, la bascule, la persistance. Effort faible une fois T2 acquis — c'est
précisément la démonstration que le contrat tient.

### T4 — Thème Confort *(2 j)*

Mêmes mécanismes, plus les ajustements non colorimétriques du §5.3.

### T5 — Charte enseigne *(4 j, dont backend)*

Dérivation de l'échelle, validation de conformité au moment de la saisie, entrée `Feature`,
écran d'administration.

---

## 7. Où vit la préférence

Trois portées, et elles ne se valent pas :

| Portée | Support | Pourquoi |
|---|---|---|
| **Officine** | `AppConfiguration` (`name`/`value`, table clé-valeur existante) | La charte enseigne et le défaut de l'officine. Aucune migration de schéma. |
| **Utilisateur** | `localStorage` | Le thème est une préférence de poste et d'horaire ; le préparateur de nuit et la titulaire du matin partagent souvent un login. Le stocker sur `AppUser` obligerait un aller-retour serveur au démarrage et se tromperait de granularité. |
| **Automatique** | horaire | *À décider — voir §9.* |

L'ordre de résolution : `localStorage` → `AppConfiguration` → `comptoir`.

Un point d'implémentation qui ne pardonne pas : le thème doit être appliqué **avant le
premier rendu**, par un script inline dans `index.html` lisant `localStorage` et posant
`data-theme` sur `<html>`. Passer par `ThemeService` dans `app.component.ts` laisserait un
flash clair de plusieurs centaines de millisecondes à chaque chargement — inacceptable
précisément dans le cas d'usage qui justifie le thème Garde.

---

## 8. Risques

**La conversion T2 est un chantier de régression visuelle, pas de fonctionnalité.** 181
fichiers, 2 071 substitutions ; un `diff` vide sur `styles.css` est la seule garantie sérieuse
et il faut la tenir lot par lot.

**Le double chemin Sass / CSS restera.** Bootstrap continuera d'être coloré à la compilation
pour le thème par défaut, et réécrit par variables pour les autres. C'est une asymétrie
permanente ; il faut l'assumer et la documenter plutôt qu'espérer la résorber.

**Le thème Garde révélera des contrastes que personne n'a jamais regardés.** Les badges, les
pastilles de statut, les fonds de ligne `rgba()` des tableaux ont été réglés à l'œil sur fond
clair. Prévoir une passe de reprise après T3 — elle n'est pas optionnelle.

**Le mode sombre et l'impression ne se mélangent pas.** Les états PDF et les tickets ESC/POS
sont générés côté serveur et ne sont pas concernés, mais les aperçus HTML imprimés depuis le
navigateur devront forcer le thème clair par `@media print`.

---

## 9. Ce qui reste à décider

1. **Bascule automatique horaire ?** Techniquement trivial, ergonomiquement discutable : un
   changement de thème non sollicité pendant qu'on saisit une vente est déroutant. Ma
   recommandation : proposer, ne jamais imposer — une invite discrète à l'ouverture d'une
   session après 21 h, jamais en cours de session.
2. **Le bleu `#5b89a6`** (58 occurrences) est-il une couleur de marque à promouvoir en token
   d'accent, ou un résidu à absorber dans `--p-surface-*` ? La réponse change le §5.4.
3. **T2 en une fois ou étalé ?** Étalé, la base reste bicolore plusieurs semaines. En une
   fois, la revue est massive. Mon avis : par famille de tokens (`surface`, puis `text`, puis
   les severities), chaque famille livrée avec son `diff` vide.
4. **Périmètre du thème Confort** : préférence utilisateur, ou réglage d'officine imposé ?
   La réponse conditionne son emplacement au §7.

---

## 10. Liens

- [PLAN-DECOMMISSIONNEMENT-BOOTSWATCH.md](PLAN-DECOMMISSIONNEMENT-BOOTSWATCH.md) — origine de
  la palette actuelle, du travail sur `$min-contrast-ratio`, et de l'abandon du `#008cba`.
- [PLAN-MIGRATION-SASS-USE.md](PLAN-MIGRATION-SASS-USE.md) — contraint l'endroit où le mixin
  de thème peut vivre.
- [PLAN-HARMONISATION-NAVBAR-SIDEBAR.md](PLAN-HARMONISATION-NAVBAR-SIDEBAR.md) — les tokens
  `--pharma-nav-*`, seule famille déjà entièrement commutable.
- [ARGUMENTAIRE-COMMERCIAL-PHARMASMART.md](ARGUMENTAIRE-COMMERCIAL-PHARMASMART.md) —
  destinataire du volet charte enseigne.
