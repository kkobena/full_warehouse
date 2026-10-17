# Formats d'étiquettes — supports, catalogue A4, rouleaux thermiques

> **2026-10-16.** Issu de l'inventaire des supports d'étiquettes du marché, confronté à ce que
> l'application imprime réellement — géométrie **mesurée** dans les PDF produits, pas déduite du
> CSS. Prolonge [PLAN-DATAMATRIX-MARCHE-CI-V2.md](PLAN-DATAMATRIX-MARCHE-CI-V2.md), qui a ajouté
> l'étiquette « lot » à DataMatrix.
>
> **Problème** : l'application ne connaissait qu'une seule planche, codée en dur dans le gabarit,
> et son calage ne correspondait à aucune planche du commerce. Une officine qui achète un autre
> consommable ne peut rien imprimer de droit.

---

## 0. État d'avancement (2026-10-16)

> Dans tout ce document : **[x]** livré et vérifié, **[ ]** reste à faire.

| Lot | État | Détail |
|-----|------|--------|
| **0 — Borne de la case de départ** | **fait** | 64 contre 65 selon l'écran : la borne vient maintenant de la capacité du format |
| **1 — Catalogue de formats A4** | **fait** | `FormatEtiquette` (7 formats), endpoint, gabarits pilotés par la géométrie, sélecteur dans les deux modales |
| **2a — Rouleau, PDF au format de l'étiquette** | **fait** | 8 rouleaux au catalogue, `SupportEtiquette`, deux gabarits dédiés, case de départ masquée |
| **2b — Rouleau, génération ZPL** | **fait** | `ZplEtiquetteService`, quatre endpoints `text/plain`, commande Rust `print_zpl` en RAW |
| **3 — Réglette, posologie, préparation magistrale** | **fait** | `TypeEtiquette`, gabarit commun des usages, posologie tirée de l'ordonnance, préparation saisie à l'impression |

**Ce que la mesure a révélé.** L'ancien gabarit déclarait des cases de 38 mm et en imprimait de
**40**. La table du gabarit n'avait pas de largeur explicite : elle occupait les 200 mm de la page
et répartissait ses cinq colonnes à parts égales, ce qui écrasait le `width: 38mm` des cases. Pas
vertical, lui, respecté à 21,2 mm. Mesuré sur le placement des images du PDF — le seul endroit où
les coordonnées sont absolues :

```
x =   6,27   46,28   86,28   126,29   166,29 mm   → pas horizontaux : 40,01 / 40,00 / 40,01 / 40,00
y = 273,09  251,88 mm                             → pas vertical    : 21,21
```

Conséquence : **aucune planche du commerce ne correspondait** à ce calage. Un Avery L7651 veut
38,1 mm de pas et 9,75 mm de marge ; l'application imprimait 40 mm et 5 mm, soit une dérive de
3,5 mm sur la dernière colonne.

**Décision.** Le format par défaut, `A4_65_HISTORIQUE`, **décrit ce calage réel** (marge 5 mm,
étiquette 38 mm, gouttière 2 mm) : rien ne bouge pour qui imprimait déjà, et le PDF par défaut
reste identique au millimètre. Le vrai L7651 est offert **à côté**, en `A4_65_L7651`, à qui veut
le calage de la planche du commerce. Corriger le défaut en silence aurait décalé toutes les
planches entamées de l'officine.

---

## 1. Ce que l'application fait

| Point | État |
|-------|------|
| Deux types d'étiquette : standard (Code 39) et lot (GS1 DataMatrix) | livré, cf. plan DataMatrix §6 |
| Choix du type à la génération, dans les deux modales d'impression | livré |
| Case de départ sur une planche entamée | livré, borne désormais calculée |
| Catalogue de formats de planche A4 | livré, cf. §4 |
| Rouleaux, en PDF au format de l'étiquette | livré, cf. §5 |
| Rouleaux, en ZPL direct (Zebra, TSC, Godex) | livré, cf. §5.5 |
| Réglette, posologie, préparation magistrale | livré, cf. §6 |

Les deux points d'impression : `ProduitResource` (`/produits/{id}/etiquettes` et
`/produits/{id}/lots/{lotId}/etiquettes`) et `StockEntryDataResource`
(`…/entree-stock/etiquettes/{id}/{orderDate}` et son pendant `etiquettes-lot`).

---

## 2. Les familles de supports

### 2.1 Planches A4 — laser ou jet d'encre

Le consommable bureautique. Chaque référence est une grille figée ; se tromper de grille gâche la
planche entière. Les cotes ci-dessous sont celles des références du commerce.

| Étiquette | Grille | Par planche | Référence | Usage |
|---|---|---|---|---|
| 38,1 × 21,2 mm | 5 × 13 | 65 | Avery L7651, Herma 4333 | prix + code sur la boîte |
| 45,7 × 21,2 mm | 4 × 12 | 48 | Avery L7636 | idem, un peu plus large |
| 45,7 × 25,4 mm | 4 × 10 | 40 | Avery L7654 | code + prix + lot |
| 63,5 × 33,9 mm | 3 × 8 | 24 | Avery L7159 | lot et péremption au large |
| 63,5 × 38,1 mm | 3 × 7 | 21 | Avery L7160 | le format « adresse », facile à trouver |
| 99,1 × 38,1 mm | 2 × 7 | 14 | Avery L7163 | préparation magistrale, posologie |
| 25,4 × 10 mm | 7 × 27 | 189 | Avery L7658 | très petits articles, prix seul |

### 2.2 Rouleaux — imprimante d'étiquettes dédiée

Classe de matériel différente : le PDF A4 n'est plus le bon véhicule.

**Bureautique**, piloté par un pilote Windows — l'application imprime comme sur n'importe quelle
imprimante, en fixant le format de page à la taille de l'étiquette :

| Marque | Référence | Étiquette |
|---|---|---|
| Dymo LabelWriter | 11354 | 57 × 32 mm |
| | 99012 | 89 × 36 mm |
| | 11352 | 54 × 25 mm |
| | 11355 | 51 × 19 mm |
| Brother QL | DK-11201 | 29 × 90 mm |
| | DK-11204 | 17 × 54 mm |
| | DK-11209 | 29 × 62 mm |
| | DK-22205 | continu, 62 mm de laize |

**Industriel**, piloté en **ZPL** (Zebra) ou **EPL**, parfois en ESC/POS étendu : Zebra, TSC,
Godex, Xprinter. Tailles die-cut courantes : 25 × 15, 30 × 20, 40 × 20, **40 × 30**, 50 × 25,
**50 × 30**, 58 × 40, 100 × 50 mm, plus les rouleaux continus où le logiciel fixe la longueur.

### 2.3 Les usages, qui sont aussi des « types »

| Type | Contenu propre | État |
|---|---|---|
| Prix / rayon | code, libellé, prix | livré (étiquette standard) |
| Lot / traçabilité | + numéro de lot, péremption, GS1 DataMatrix | livré |
| Réglette de gondole | prix et libellé, collée sur l'étagère — format allongé | livré, cf. §6 |
| Posologie / délivrance | patient, prescripteur, instructions de prise | livré, cf. §6 |
| Préparation magistrale | composition, date, péremption, prescripteur | livré, cf. §6 |
| Quarantaine / destruction | motif et date, souvent sur fond de couleur | à faire |

---

## 3. Contraintes techniques

### 3.1 Taille minimale du symbole DataMatrix

Mesuré sur la trame réelle de l'étiquette « lot » (`240` + CIP + AI 10 + AI 17) :

| Contenu | Modules | à 0,255 mm | à 0,380 mm | à 0,500 mm |
|---|---|---|---|---|
| code + péremption (20 car.) | 16 × 16 | 4,1 mm | 6,1 mm | 8,0 mm |
| code + lot + péremption (32 car.) | **20 × 20** | 5,1 mm | 7,6 mm | 10,0 mm |
| lot de longueur maximale (42 car.) | 22 × 22 | 5,6 mm | 8,4 mm | 11,0 mm |

L'application imprime le symbole à **11 mm**, soit 0,55 mm par module : largement au-dessus du
minimum GS1 de 0,255 mm pour la distribution générale. Il pourrait descendre à ~6 mm sur une
imprimante 300 dpi, ce qui ouvre les petits formats en rouleau — mais pas le 25,4 × 10 mm, où
6 mm de symbole sur 10 mm de hauteur ne laissent plus de place au texte.

### 3.2 Largeur du texte dans la case

Mesuré en Arial gras 8 px à 96 DPI, pour la colonne de texte de 23,0 mm de l'étiquette « lot »
au format par défaut :

| Ligne | Largeur |
|---|---|
| `3794609` | 7,4 mm |
| `Lot L26050002` | 13,8 mm |
| `Exp 30/05/2027` | 13,8 mm |
| `1 500 CFA` | 9,5 mm |
| `Lot ` + 20 caractères | **23,3 mm — déborde** |

**Limite connue** : `lot.num_lot` est un `varchar(20)`. Un numéro de longueur maximale déborde la
colonne. Non corrigé : le numéro est de toute façon dans le symbole, et les lots réels font 6 à
10 caractères. À traiter si un fournisseur sort des numéros longs.

### 3.3 Pièges de Flying Saucer

1. **Une table sans largeur explicite occupe toute la page** et répartit ses colonnes à parts
   égales, en écrasant le `width` des cases. C'est la cause du 40 mm au lieu de 38. Toute grille
   d'étiquettes doit porter sa largeur : `table.container { width: <grille>mm }`.
2. **`table-layout: fixed` ne lit que la première rangée** pour fixer les colonnes. Une première
   rangée incomplète laisse les colonnes manquantes se partager le reste.
3. **`width` est la boîte de contenu**, pas la boîte de bordure : le retrait s'ajoute. Déclarer un
   pas de 40 mm avec 1 mm de retrait donne une case de 42 mm. Déclarer `pas − 2 × retrait`.
4. **Les doubles sont rendus par Thymeleaf comme Java les écrit** : `5.0mm`, pas `5mm`. CSS
   valide, mais les assertions de test doivent en tenir compte.
5. **`overflow: hidden` ne protège pas d'un débordement** comme le ferait un navigateur : le
   texte sort de sa case ou chevauche la voisine. D'où la mesure du 3.2 plutôt qu'une confiance
   dans le CSS.
6. **Le placement des images est absolu** dans le flux de contenu (`cm` avant `Do`), alors que les
   `Td` du texte sont relatifs à leur objet texte. Pour mesurer une grille dans un PDF, lire les
   images, pas le texte.

---

## 4. Lot 1 — Catalogue de formats A4 — [x]

### 4.1 Modèle — [x]

`domain/enumeration/FormatEtiquette` porte sept formats et leur géométrie complète, en
millimètres : largeur, hauteur, colonnes, rangées, marges latérale et haute, gouttières
horizontale et verticale. Trois cotes dérivées : `getPasHorizontalMm()`, `getPasVerticalMm()` et
`getLargeurGrilleMm()` — cette dernière est ce que le gabarit pose sur la table.

| Code | Étiquette | Grille | Capacité | Calage |
|---|---|---|---|---|
| `A4_65_HISTORIQUE` | 38 × 21,2 | 5 × 13 | 65 | **défaut** — marge 5 mm, gouttière 2 mm (le calage mesuré) |
| `A4_65_L7651` | 38,1 × 21,2 | 5 × 13 | 65 | Avery L7651, marge 9,75 / 10,7, sans gouttière |
| `A4_48_45X21` | 45,7 × 21,2 | 4 × 12 | 48 | Avery L7636 |
| `A4_40_45X25` | 45,7 × 25,4 | 4 × 10 | 40 | Avery L7654 |
| `A4_24_63X34` | 63,5 × 33,9 | 3 × 8 | 24 | Avery L7159 |
| `A4_21_63X38` | 63,5 × 38,1 | 3 × 7 | 21 | Avery L7160 |
| `A4_14_99X38` | 99,1 × 38,1 | 2 × 7 | 14 | Avery L7163 |

> Les marges de `A4_48_45X21` et `A4_40_45X25` sont **déduites de la grille**, non relevées sur une
> fiche technique : à confronter à la planche réelle avant de s'en servir en production. Les autres
> viennent des cotes publiées.

### 4.2 Garde-fou de géométrie — [x]

Une cote fausse ne se voit pas à la relecture du code : elle se voit sur la planche gâchée.
`FormatEtiquetteTest` contrôle donc, pour **chaque** format :

- la grille tient dans 210 mm de large et 297 mm de haut, gouttière de dernière colonne exclue ;
- la marge restante est inférieure à une étiquette — au-delà, c'est une grille mal transcrite, le
  format tiendrait une rangée ou une colonne de plus ;
- toutes les cotes sont strictement positives, la capacité égale la grille ;
- la largeur de grille tient entre les marges ;
- aucun doublon de géométrie dans le catalogue.

### 4.3 Exposition — [x]

`GET /api/etiquettes/formats` rend le catalogue (`FormatEtiquetteDTO` : code, libellé, référence,
cotes, capacité, défaut). Le front le **lit** au lieu de l'embarquer : un format ajouté côté
serveur apparaît sans redéploiement du bundle, et la borne de la case de départ le suit.

Les quatre endpoints d'impression gagnent un `format` facultatif. Absent, il vaut
`FormatEtiquette.parDefaut()` — les appels en place continuent de fonctionner tels quels.

### 4.4 Gabarits pilotés par la géométrie — [x]

`etiquetes/main.html` et `etiquetes/lot.html` ne portent plus de cote en dur. Le modèle de case :

- la case **porteuse** (`.container-tr-td`) fait **un pas**, gouttière comprise ;
- l'étiquette (`.etiquette-table`) fait **sa largeur exacte**, à l'intérieur ;
- la dernière colonne de chaque rangée (`.last-item`) perd sa gouttière, qui sortirait de la page ;
- la table porte `width: <largeurGrille>mm`, sans quoi elle écrase tout (cf. 3.3).

Le découpage en rangées suit `format.getColonnes()` au lieu du `partition(items, 5)` codé en dur.

### 4.5 Borne de la case de départ — [x]

La modale de la fiche produit bornait à **65**, celle du bon de livraison à **64** : une des deux
avait tort, la planche en compte 65. La borne vient maintenant de `capacite()`, calculée sur le
format choisi, dans les deux écrans. Changer de format pour une planche plus petite ramène la case
de départ à 1 plutôt que de laisser une valeur hors bornes.

### 4.5 bis Case de départ : une case de trop sur le bon de livraison — [x]

Trouvé en écrivant les tests du rouleau, corrigé : `buildEtiquettes()` sautait `startAt` cases au
lieu de `startAt - 1`. Un départ à 3 posait donc la première étiquette du bon de livraison sur la
**case 4**, alors que l'étiquette produit et l'étiquette de lot la posaient sur la case 3, et que
l'écran annonce « case de la première étiquette ». Les trois chemins sautent désormais le même
nombre de cases, et un test le vérifie en les comparant.

### 4.6 Grille mesurée, format par format — [x]

Imprimé sur la base de démonstration, puis mesuré dans le placement des images de chaque PDF. La
colonne `x₁` est l'abscisse du premier symbole ; le décalage constant de +0,27 mm est le rendu
propre de la table interne, identique sur tous les formats et identique au PDF d'avant le
catalogue.

| Format | `x₁` mesuré | Pas H attendu | Pas H mesuré | Pas V attendu | Pas V mesuré |
|---|---|---|---|---|---|
| `A4_65_HISTORIQUE` | 6,27 mm | 40,0 | **40,01** | 21,2 | **21,21** |
| `A4_65_L7651` | 11,02 mm | 38,1 | **38,11** | 21,2 | **21,21** |
| `A4_48_45X21` | 11,13 mm | 48,2 | **48,20** | 21,2 | **21,21** |
| `A4_40_45X25` | 11,13 mm | 48,2 | **48,20** | 25,4 | **25,40** |
| `A4_24_63X34` | 8,52 mm | 66,0 | **66,01** | 33,9 | **33,91** |
| `A4_21_63X38` | 8,52 mm | 66,0 | **66,01** | 38,1 | **38,10** |
| `A4_14_99X38` | 7,17 mm | 99,1 | **99,11** | 38,1 | **38,10** |

**Non-régression du défaut, prouvée par la mesure** : le PDF d'avant le catalogue donnait
`x₁ = 6,27` et des pas de `40,01 / 21,21`. Le format par défaut donne les mêmes chiffres.

Deux pièges attrapés à la mesure, et seulement là :

1. **Le modèle de retrait.** Déplacer le retrait de 1 mm de la case porteuse vers l'étiquette
   décalait tout de 1 mm (`x₁ = 5,27` au lieu de 6,27). Le retrait est resté sur la case.
2. **`width` est la boîte de CONTENU** chez Flying Saucer : le retrait s'y ajoute. Déclarer le pas
   donnait des cases de pas + 2 mm — mesuré à 42 mm pour un pas de 40. D'où
   `getLargeurCaseContenuMm()`, qui déclare `pas − 2`.

Aucun des deux ne se voyait dans le CSS, ni dans un test qui n'aurait lu que le gabarit rendu.

### 4.7 Vérifications — [x]

- 54 contrôles de géométrie et de catalogue (`FormatEtiquetteTest`).
- Un test par format qui rend le vrai gabarit, vérifie que la géométrie déclarée se retrouve dans
  le CSS et que le PDF sort (`EtiquetteLotExportReportServiceImplTest`).
- Un test de non-régression : le format par défaut rend bien `margin: 5.0mm`, `width: 38.0mm`,
  `height: 21.2mm`.
- 17 tests front sur la modale, dont la borne 65 (et non 64), le repli à 1 sur une planche plus
  petite, et l'impression sans format si le catalogue est injoignable.
- Grille **mesurée dans les PDF réellement produits**, format par format (§10).

---

## 5. Lot 2 — Rouleau thermique

Le chantier le plus rentable pour la Côte d'Ivoire : les planches Avery sont un consommable
importé et cher, alors que les rouleaux die-cut 40 × 30 et 50 × 30 et les imprimantes Zebra / TSC /
Godex / Xprinter sont disponibles localement et bon marché.

**Décision prise** : un champ `support` sur le même enum, et le choix revient à l'utilisateur —
planches et rouleaux cohabitent dans la même liste déroulante, groupés par support.

### 5.1 Ce qui ne marchera pas — [x] écarté

Rendre un PDF A4 et le laisser au pilote. Une imprimante d'étiquettes n'a pas de page A4 : elle a
une laize et un capteur de découpe. Le gabarit de rouleau fixe donc la page à la taille de
l'étiquette, et c'est **mesuré dans le PDF produit** (§5.4), pas supposé d'après le CSS.

### 5.2 Deux chemins, selon le matériel

| Chemin | Pour | État |
|---|---|---|
| **PDF au format de l'étiquette** | Dymo, Brother, et tout matériel à pilote Windows | **livré** — `@page { size: L H }`, une étiquette par page |
| **ZPL** | Zebra, TSC, Godex, Xprinter | **livré**, §5.5 |

Le premier réutilise tout l'existant — Flying Saucer, les gabarits, la branche DataMatrix de
`EtiquetteBarcodeReplacedElement` — et ne touche pas au Rust. Le second donne le contrôle du
contraste et de la vitesse, et le DataMatrix y est une primitive (`^BX`), donc rendu par
l'imprimante. Les deux sont livrés ; l'écran laisse le choix, rouleau par rouleau.

### 5.3 Ce que le modèle a absorbé — [x]

`SupportEtiquette` (`PLANCHE_A4` / `ROULEAU`) porte la distinction, sur le **même** enum
`FormatEtiquette` : l'option « une interface et deux implémentations » aurait été plus propre, mais
une planche et un rouleau partagent déjà libellé, référence, largeur, hauteur et code-barres — deux
hiérarchies pour quatre champs sans objet ne se justifiaient pas.

Un rouleau se déclare par un constructeur court : libellé, référence, largeur, hauteur. Colonnes et
rangées valent 1, marges et gouttières 0 — la page **est** l'étiquette.

| Code | Étiquette | Référence |
|---|---|---|
| `ROULEAU_40X30` | 40 × 30 mm | die-cut 40 × 30 — le plus répandu localement |
| `ROULEAU_50X30` | 50 × 30 mm | die-cut 50 × 30 |
| `ROULEAU_50X25` | 50 × 25 mm | die-cut 50 × 25 |
| `ROULEAU_40X20` | 40 × 20 mm | die-cut 40 × 20 |
| `ROULEAU_30X20` | 30 × 20 mm | die-cut 30 × 20 — le plus petit qui porte un DataMatrix de 11 mm |
| `ROULEAU_58X40` | 58 × 40 mm | die-cut 58 × 40 |
| `ROULEAU_DYMO_57X32` | 57 × 32 mm | Dymo LabelWriter 11354 |
| `ROULEAU_BROTHER_62X29` | 62 × 29 mm | Brother QL DK-11209 |

**La case de départ disparaît en rouleau**, à deux endroits plutôt qu'un :

- à l'écran, le champ est **masqué**, pas grisé, et la quantité n'est plus bornée par la planche ;
- côté serveur, `normaliserCaseDeDepart()` la ramène à 1. Sans cela, demander la case 40 aurait
  fait dérouler 39 étiquettes blanches — du consommable perdu que l'écran seul n'aurait pas
  empêché, l'endpoint étant appelable directement.

Le format par défaut reste `A4_65_HISTORIQUE` : un appel sans `format` imprime comme avant.

### 5.4 Gabarits de rouleau, mesurés — [x]

`etiquetes/rouleau.html` (Code 39) et `etiquetes/rouleau-lot.html` (GS1 DataMatrix) posent
`@page { size: <largeur>mm <hauteur>mm }` et une étiquette par page. Trois choix que seule la
mesure a tranchés :

1. **Le saut de page est posé AVANT chaque étiquette sauf la première.** Un `page-break-after` sur
   la dernière ferait sortir une étiquette blanche à chaque impression.
2. **Le Code 39 porte une hauteur déclarée** (`getHauteurCodeBarresMm()`, 40 % de la hauteur, 8 mm
   au minimum). Seul sur sa page, sans hauteur, le symbole prend toute la place et chasse le prix
   hors de l'étiquette — ce que Flying Saucer ne signale pas (cf. 3.3 §5).
3. **Le nom de l'officine n'apparaît qu'à partir de 25 mm de haut.** En dessous, il pousse le prix
   hors de la découpe.

Grille mesurée dans les PDF produits, comme au §4.6 : page lue dans le `MediaBox`, contenu lu dans
le placement absolu des images.

| Format | Page mesurée | Symbole « lot » | Code 39 « standard » | Contenu |
|---|---|---|---|---|
| `ROULEAU_30X20` | 30,0 × 20,0 | 11,0 × 11,0 en (1,3 ; 5,2) | 27,0 × 8,0 | dans la page |
| `ROULEAU_40X20` | 40,0 × 20,0 | 11,0 × 11,0 en (1,3 ; 5,2) | 37,0 × 8,0 | dans la page |
| `ROULEAU_40X30` | 40,0 × 30,0 | 11,0 × 11,0 en (1,3 ; 12,7) | 37,0 × 12,0 | dans la page |
| `ROULEAU_50X25` | 50,0 × 25,0 | 11,0 × 11,0 en (1,3 ; 7,7) | 47,0 × 10,0 | dans la page |
| `ROULEAU_50X30` | 50,0 × 30,0 | 11,0 × 11,0 en (1,3 ; 12,7) | 47,0 × 12,0 | dans la page |
| `ROULEAU_58X40` | 58,0 × 40,0 | 11,0 × 11,0 en (1,3 ; 22,7) | 55,0 × 16,0 | dans la page |
| `ROULEAU_DYMO_57X32` | 57,0 × 32,0 | 11,0 × 11,0 en (1,3 ; 14,7) | 54,0 × 12,8 | dans la page |
| `ROULEAU_BROTHER_62X29` | 62,0 × 29,0 | 11,0 × 11,0 en (1,3 ; 11,7) | 59,0 × 11,6 | dans la page |

Le symbole DataMatrix reste à 11 mm sur tous les rouleaux, soit 0,55 mm par module sur une trame
20 × 20 : le même que sur planche, largement au-dessus du minimum GS1 (§3.1).

### 5.5 ZPL — [x]

Pour Zebra, TSC, Godex, Xprinter : le backend écrit le flux, le poste l'envoie tel quel. Rien n'est
rendu en image — l'imprimante trace elle-même le code-barres et le DataMatrix, d'où le contrôle du
contraste et de la vitesse.

**Le chemin.** `ZplEtiquetteService` part des mêmes `EtiquetteDTO` que les gabarits PDF : les deux
services d'export ont été ouverts (`genererZplPourBon`, `genererZplPourLot`,
`genererZplPourProduit`) plutôt que dupliquer la construction des étiquettes. Quatre endpoints en
`text/plain` à côté des quatre endpoints PDF, puis `TauriPrinterService.printZpl()` et la commande
Rust `print_zpl`, qui écrit en **RAW** dans la file d'impression — le pilote ne réinterprète rien.
Le travail s'appelle « Etiquettes ZPL » dans la file, pour qu'un travail bloqué se reconnaisse.

**Cinq décisions, et ce qu'elles évitent :**

| Décision | Sans elle |
|---|---|
| Un bloc `^XA … ^XZ` par étiquette, **page redéclarée à chaque bloc** (`^PW`, `^LL`) | l'imprimante garde la dernière configuration reçue : une étiquette imprimée après un travail d'un autre format sort à la longueur de celui-ci |
| `^CI28` en tête | un libellé accentué sort en caractères de remplacement |
| FNC1 écrits `_1`, l'échappement étant déclaré au `^BX` | pas de GS1 DataMatrix : le firmware attend le FNC1 dans la donnée, là où ZXing le pose lui-même |
| `^`, `~` et `_` d'un libellé ou d'un lot remplacés par `-` | les deux premiers ouvrent une commande ZPL, le troisième porte nos FNC1 : un libellé « SIROP ^TOUX » couperait l'étiquette en deux |
| **Code 39 seulement s'il tient** à 0,25 mm de module ; sinon DataMatrix du code (AI 240) | un code de dix caractères demande 39 mm de large : sur un 30 × 20 il sortirait tronqué, donc illisible |

**La résolution est un choix, pas une constante.** `ResolutionEtiquette` (203, 300, 600 dpi) :
le ZPL ne connaît que le point, et convertir avec la mauvaise résolution sort une étiquette à la
bonne forme et à la mauvaise taille — un tiers d'écart entre 203 et 300. 203 par défaut, la
résolution des Zebra et TSC d'entrée de gamme.

**À l'écran**, le chemin d'impression n'apparaît **que** sur un rouleau **et** depuis le poste :
un navigateur ne parle pas à l'imprimante. Le choix fait, la modale demande l'imprimante (liste du
poste, celle par défaut présélectionnée) et la résolution ; le bouton devient « Envoyer à
l'imprimante », puisqu'il n'y a plus rien à télécharger. Revenir à une planche repasse en PDF.

Un aperçu, étiquette « lot » sur un 40 × 30 à 203 dpi :

```
^XA
^CI28
^PW320
^LL240
^LH0,0
^FO8,8^A0N,21,21^FDPHARMACIE DU PLATEAU^FS
^FO8,32^A0N,21,21^FDDOLIPRANE 1000MG^FS
^FO8,56^BXN,4,200,,,,_,1^FD_12403794609_110L26050002_117270530^FS
^FO104,56^A0N,21,21^FD3794609^FS
^FO104,80^A0N,21,21^FDLot L26050002^FS
^FO104,104^A0N,21,21^FDExp 30/05/2027^FS
^FO104,128^A0N,26,26^FD1 500 CFA^FS
^XZ
```

## 6. Lot 3 — Réglette, posologie, préparation — [x]

Trois usages qui demandent chacun leur contenu, pas seulement un format. `TypeEtiquette`
(`STANDARD`, `LOT`, `REGLETTE`, `POSOLOGIE`, `PREPARATION`) porte la distinction : c'est lui, et
non le format, qui choisit la mise en page — sur planche comme sur rouleau, en PDF comme en ZPL.

| Type | Ce qu'il porte | D'où il part |
|---|---|---|
| Réglette de gondole | libellé, **prix en gros**, code en petit ; pas de lot | fiche produit |
| Posologie | patient, produit, instructions, durée, prescripteur ; **jamais de prix** | panneau ordonnance du comptoir |
| Préparation magistrale | nom, composition, patient, posologie, les deux dates, mention | fiche produit, saisie à l'impression |

### 6.1 Un gabarit, trois contenus — [x]

Deux gabarits seulement : `etiquetes/usage.html` (planche) et `etiquetes/rouleau-usage.html`
(rouleau). Ils ne portent que la grille — déjà mesurée au §4.6 — et appellent le fragment
`etiquetes/usage-contenu.html`, qui bascule sur le type. Le contenu des trois usages se lit donc à
**une seule place**, et un ajout de format les suit sans rien recopier.

Le fragment vit dans son propre fichier : laissé dans `usage.html`, Thymeleaf l'aurait rendu une
seconde fois en clair, hors de la table.

### 6.2 La posologie ne se ressaisit pas — [x]

`Ordonnance`, `OrdonnanceLigne.posologie` et `Prescripteur` existaient déjà : l'étiquette les lit.
Une par ligne prescrite, ou une seule avec `ligneId` — le comptoir n'étiquette parfois qu'une
boîte. Patient, prescripteur, instructions et durée viennent de l'ordonnance, donc **rien ne peut
différer de ce qui a été délivré**.

### 6.3 La préparation magistrale ne persiste rien — [x]

Décision prise : le contenu est saisi dans la modale d'impression, l'étiquette sort, rien n'est
conservé. Aucune entité, aucune migration. Le **registre des préparations** reste un chantier à
part, qui demande que le contenu réglementaire soit arrêté avant d'être figé en base.

Le nom du pot est prérempli avec l'article d'où l'impression part, et la composition est exigée :
une étiquette de préparation sans composition n'en est pas une.

### 6.4 Rendu — [x]

- **PDF** : `EtiquetteUsageService` et son impl, qui passent par `EtiquettePdf` — le montage
  Flying Saucer + fabrique de symboles, écrit une fois au lieu d'être recopié une troisième fois.
- **ZPL** : le rendu bascule lui aussi sur le type. La réglette garde son code **en clair** quand
  le Code 39 ne tient pas, au lieu de se replier sur un DataMatrix : au rayon, le code n'est
  qu'une vérification. Posologie et composition passent par `^FB`, qui déclare la largeur du bloc
  et le nombre de lignes — ZPL ne replie rien tout seul, et mieux vaut couper que déborder.
- Les six endpoints suivent la paire PDF / ZPL déjà en place ; la préparation est en **POST**,
  son contenu n'existant nulle part en base.

### 6.5 Vérifications — [x]

- `EtiquetteUsageServiceImplTest` : 13 contrôles, dont le **texte réellement extrait du PDF** —
  le patient, le produit, la posologie et la durée pour une ordonnance ; la composition et les
  deux dates pour une préparation ; l'absence de prix sur une posologie.
- `ZplEtiquetteServiceImplTest` : 37 contrôles, dont les trois mises en page d'usage.
- Spécifications front : réglette, préremplissage et exigence de composition, envoi de la saisie,
  réglette en ZPL (modale produit, 31 cas) ; choix de la ligne, rouleau et ZPL (modale posologie,
  6 cas).

## 7. Hors périmètre

| Sujet | Raison |
|-------|--------|
| Registre des préparations magistrales | l'étiquette est une sortie, pas un enregistrement ; un registre demande une entité, une migration et un contenu réglementaire arrêté — décision prise de ne pas persister la saisie |
| Étiquette de quarantaine ou de destruction | reste à faire : elle demande un consommable de couleur plus qu'un développement, et aucun parcours ne la réclame |
| Éditeur de format libre à l'écran | le catalogue couvre les planches du commerce ; un éditeur demande une persistance et une validation de géométrie, pour un besoin non exprimé |
| Correction du calage par défaut | changerait l'alignement des planches entamées de l'officine ; le vrai L7651 est offert à côté, c'est à l'utilisateur de choisir |
| EPL, ESC/POS étendu, et les autres dialectes d'étiquette | le ZPL couvre Zebra, TSC, Godex et les Xprinter qui l'émulent — l'essentiel du marché local ; un deuxième dialecte se rajoutera le jour où une imprimante le réclamera, le générateur est isolé derrière `ZplEtiquetteService` |
| Mémoriser l'imprimante d'étiquettes du poste | la liste présélectionne l'imprimante par défaut ; un réglage de poste est un autre chantier (`poste_device` porte déjà des périphériques série) |
| Étiquette de quarantaine sur fond couleur | demande un consommable spécifique, pas un développement |

---

## 8. Fichiers créés / modifiés

| État | Lot | Fichier | Action |
|------|-----|---------|--------|
| [x] | 1 | `domain/enumeration/FormatEtiquette.java` | **Créer** — sept formats et leur géométrie |
| [x] | 1 | `service/dto/etiquette/FormatEtiquetteDTO.java` | **Créer** — le catalogue vu de l'écran |
| [x] | 1 | `web/rest/stock/ProduitResource.java` | **Modifier** — `GET /etiquettes/formats`, `format` sur deux endpoints |
| [x] | 1 | `web/rest/commande/StockEntryDataResource.java` | **Modifier** — `format` sur deux endpoints |
| [x] | 1 | `service/stock/StockEntryDataService.java` + impl | **Modifier** — `format` sur les deux impressions |
| [x] | 1 | `service/stock/EtiquetteLotExportService.java` + impl | **Modifier** — surcharges avec format |
| [x] | 1 | `service/stock/impl/EtiquetteExportReportServiceImpl.java` | **Modifier** — surcharges avec format |
| [x] | 1 | `service/report/Constant.java` | **Modifier** — `ETIQUETTES_FORMAT` |
| [x] | 1 | `service/stock/impl/EtiquetteExportReportServiceImpl.java` | **Modifier** — case de départ : `startAt - 1` cases sautées, et non `startAt` (§4.5 bis) |
| [x] | 1 | `templates/etiquetes/main.html`, `templates/etiquetes/lot.html` | **Modifier** — géométrie paramétrée, largeur de grille explicite |
| [x] | 1 | `features/products/data-access/services/products-api.service.ts` | **Modifier** — `listerFormatsEtiquette`, `format` sur deux appels |
| [x] | 1 | `entities/commande/delevery/delivery.service.ts` | inchangé — la requête était déjà générique |
| [x] | 0 | `features/products/ui/etiquette-modal` | **Modifier** — sélecteur de format, borne calculée |
| [x] | 0 | `features/commande/ui/delivery/etiquette` | **Modifier** — sélecteur de format, borne calculée (le 64 en dur) |
| [x] | 1 | `FormatEtiquetteTest.java` | **Créer** — 54 contrôles |
| [x] | 1 | `EtiquetteLotExportReportServiceImplTest`, `StockEntryDataServiceImplTest` | **Modifier** — rendu par format |
| [x] | 1 | `produit-etiquette-modal.component.spec.ts` | **Modifier** — 17 cas |
| [x] | 2a | `domain/enumeration/SupportEtiquette.java` | **Créer** — planche A4 / rouleau |
| [x] | 2a | `domain/enumeration/FormatEtiquette.java` | **Modifier** — support, 8 rouleaux, `normaliserCaseDeDepart`, hauteur du code-barres |
| [x] | 2a | `service/dto/etiquette/FormatEtiquetteDTO.java` | **Modifier** — support et libellé de support, pour grouper la liste |
| [x] | 2a | `templates/etiquetes/rouleau.html` | **Créer** — Code 39, une étiquette par page |
| [x] | 2a | `templates/etiquetes/rouleau-lot.html` | **Créer** — GS1 DataMatrix, une étiquette par page |
| [x] | 2a | `service/report/Constant.java` | **Modifier** — les deux gabarits de rouleau |
| [x] | 2a | `service/stock/impl/EtiquetteExportReportServiceImpl.java` | **Modifier** — gabarit selon le support, case de départ normalisée |
| [x] | 2a | `service/stock/impl/EtiquetteLotExportReportServiceImpl.java` | **Modifier** — idem |
| [x] | 2a | `features/products/data-access/services/products-api.service.ts` | **Modifier** — `support` sur le modèle |
| [x] | 2a | `features/products/ui/etiquette-modal` | **Modifier** — liste groupée, case de départ masquée, quantité déplafonnée |
| [x] | 2a | `features/commande/ui/delivery/etiquette` | **Modifier** — idem |
| [x] | 2a | `FormatEtiquetteTest`, `EtiquetteLotExportReportServiceImplTest` | **Modifier** — garde-fous et page mesurée, rouleau par rouleau |
| [x] | 2a | `produit-etiquette-modal.component.spec.ts` | **Modifier** — 3 cas de rouleau |
| [x] | 2b | `domain/enumeration/ResolutionEtiquette.java` | **Créer** — 203 / 300 / 600 dpi |
| [x] | 2b | `service/etiquette/zpl/ZplFormatage.java` | **Créer** — conversion en points, assainissement, FNC1 |
| [x] | 2b | `service/etiquette/zpl/ZplEtiquetteService.java` + impl | **Créer** — écriture du flux |
| [x] | 2b | `service/stock/impl/EtiquetteExportReportServiceImpl.java` | **Modifier** — `genererZplPourBon`, `genererZplPourProduit` |
| [x] | 2b | `service/stock/EtiquetteLotExportService.java` + impl | **Modifier** — `genererZplPourBon`, `genererZplPourLot` |
| [x] | 2b | `service/stock/StockEntryDataService.java` + impl | **Modifier** — les deux chemins ZPL du bon |
| [x] | 2b | `web/rest/stock/ProduitResource.java`, `web/rest/commande/StockEntryDataResource.java` | **Modifier** — quatre endpoints `text/plain` |
| [x] | 2b | `src-tauri/src/printer.rs`, `src-tauri/src/main.rs` | **Modifier** — commande `print_zpl`, envoi RAW nommé |
| [x] | 2b | `shared/services/tauri-printer.service.ts` | **Modifier** — `printZpl` |
| [x] | 2b | `products-api.service.ts`, `delevery/delivery.service.ts` | **Modifier** — appels ZPL |
| [x] | 2b | `features/products/ui/etiquette-modal`, `features/commande/ui/delivery/etiquette` | **Modifier** — chemin d'impression, imprimante, résolution |
| [x] | 2b | `ZplEtiquetteServiceImplTest` | **Créer** — 31 contrôles |
| [x] | 2b | `EtiquetteExportReportServiceImplTest`, `EtiquetteLotExportReportServiceImplTest`, `produit-etiquette-modal.component.spec.ts` | **Modifier** — chemins ZPL |
| [x] | 3 | `domain/enumeration/TypeEtiquette.java` | **Créer** — standard, lot, réglette, posologie, préparation |
| [x] | 3 | `service/dto/EtiquetteDTO.java` | **Modifier** — patient, prescripteur, posologie, durée, composition, mention |
| [x] | 3 | `service/dto/etiquette/EtiquettePreparationDTO.java` | **Créer** — la saisie de la préparation |
| [x] | 3 | `templates/etiquetes/usage.html`, `rouleau-usage.html`, `usage-contenu.html` | **Créer** — grille, rouleau, et le contenu des trois usages en un seul endroit |
| [x] | 3 | `service/etiquette/EtiquettePdf.java` | **Créer** — le montage Flying Saucer, écrit une fois |
| [x] | 3 | `service/etiquette/EtiquetteUsageService.java` + impl | **Créer** — les trois usages, PDF et ZPL |
| [x] | 3 | `service/etiquette/zpl/…` | **Modifier** — rendu par type, blocs `^FB` |
| [x] | 3 | `web/rest/stock/ProduitResource.java` | **Modifier** — réglette, PDF et ZPL |
| [x] | 3 | `web/rest/OrdonnanceResource.java` | **Modifier** — posologie, PDF et ZPL |
| [x] | 3 | `web/rest/etiquette/EtiquettePreparationResource.java` | **Créer** — préparation en POST |
| [x] | 3 | `features/products/…/etiquette-modal` | **Modifier** — types réglette et préparation, formulaire de saisie |
| [x] | 3 | `features/sales/ui/posologie-etiquette-modal` | **Créer** — modale d'étiquettes de posologie |
| [x] | 3 | `features/sales/ui/ordonnance-panel` | **Modifier** — bouton « Étiquettes » par ordonnance |
| [x] | 3 | `EtiquetteUsageServiceImplTest` | **Créer** — 13 contrôles, texte extrait du PDF |
| [x] | 3 | `posologie-etiquette-modal.component.spec.ts` | **Créer** — 6 cas |

---

## 9. Ordre et charge

| État | # | Étape | Charge |
|------|---|-------|--------|
| [x] | 1 | Borne de la case de départ (64/65) | 0,2 j |
| [x] | 2 | Catalogue A4 : enum, DTO, endpoint, garde-fou | 1 j |
| [x] | 3 | Gabarits pilotés par la géométrie | 0,8 j |
| [x] | 4 | Sélecteur dans les deux modales | 0,5 j |
| [x] | 5 | Rouleau — PDF au format de l'étiquette | 1,5 j |
| [x] | 6 | Rouleau — générateur ZPL et envoi Tauri | 3 j |
| [x] | 7 | Réglette, posologie, préparation | 3 j |

**Les trois lots sont livrés : 10 j. Ne restent que les recettes sur matériel (§10, points 8, 13 et 20).**

---

## 10. Recette

| # | Point | Couvert par un test | Passé à l'écran |
|---|-------|---------------------|-----------------|
| 1 | Le format par défaut imprime comme avant le catalogue (non-régression) | [x] `geometrieHistoriqueParDefaut`, grille mesurée | [ ] sur papier |
| 2 | Chaque format rend sa géométrie et sort un PDF | [x] `chaqueFormatRendSaGeometrie` | [ ] |
| 3 | Grille réellement imprimée = géométrie déclarée, format par format | [x] mesurée sur les sept PDF (§4.6) | [ ] sur papier |
| 4 | La case de départ se borne à 65, pas 64 | [x] spec front | [ ] |
| 5 | Changer pour une planche plus petite ramène la case de départ à 1 | [x] spec front | [ ] |
| 6 | Catalogue injoignable → impression au format par défaut | [x] spec front | [ ] |
| 7 | Une grille qui ne tient pas dans A4 fait échouer la compilation des tests | [x] `FormatEtiquetteTest` | — |
| 8 | **Imprimer et confronter à une planche Avery réelle** | — | [ ] **à faire sur le matériel** |
| 9 | Chaque rouleau fixe la page à la taille de l'étiquette | [x] `chaqueRouleauFixeLaPage`, `MediaBox` mesuré | [ ] |
| 10 | Un rouleau sort une page par étiquette, sans page blanche finale | [x] `unePageParEtiquetteEnRouleau` | [ ] |
| 11 | Un rouleau ignore la case de départ au lieu de dérouler du blanc | [x] service et spec front | [ ] |
| 12 | La case de départ est masquée, la quantité déplafonnée, sur rouleau | [x] spec front | [ ] |
| 13 | **Imprimer sur une vraie imprimante à rouleau** | — | [ ] **à faire sur le matériel** |
| 14 | Les trois chemins d'impression sautent le même nombre de cases (§4.5 bis) | [x] `memeCaseDeDepartQueLesAutresChemins` | [ ] sur papier |
| 15 | Le flux ZPL déclare sa page, son UTF-8, un bloc par étiquette | [x] `ZplEtiquetteServiceImplTest` | [ ] |
| 16 | La trame GS1 passe en ZPL avec ses FNC1, sans `{GS}` | [x] idem, et test de service | [ ] |
| 17 | Un libellé à `^`, `~` ou `_` ne casse pas l'étiquette | [x] idem | [ ] |
| 18 | Un Code 39 trop large cède la place à un DataMatrix | [x] idem, pour chaque rouleau et 7 à 10 caractères | [ ] |
| 19 | Le ZPL n'est offert qu'en rouleau et depuis le poste ; le flux part à l'imprimante choisie | [x] spec front | [ ] |
| 20 | **Imprimer en ZPL sur une Zebra ou une TSC réelle** | — | [ ] **à faire sur le matériel** |
| 21 | La réglette porte le prix en gros et le code en petit | [x] service et spec front | [ ] sur papier |
| 22 | La posologie reprend patient, prescripteur, instructions et durée de l'ordonnance | [x] texte extrait du PDF | [ ] |
| 23 | Une posologie ne porte jamais de prix | [x] `EtiquetteUsageServiceImplTest`, rendu ZPL | [ ] |
| 24 | Une ligne précise n'imprime qu'elle | [x] service et spec front | [ ] |
| 25 | La préparation exige une composition et porte les deux dates | [x] service, texte du PDF, spec front | [ ] |
| 26 | **Faire valider le contenu de l'étiquette de préparation** | — | [ ] **décision métier, hors code** |

> Les points 8, 13 et 20 sont les seuls qui comptent vraiment pour les formats ajoutés : la géométrie est
> cohérente et vérifiée dans le PDF, mais seule une planche du commerce passée dans l'imprimante
> dira si les marges relevées sont les bonnes. Les deux formats dont les marges sont déduites
> (`A4_48_45X21`, `A4_40_45X25`) sont à vérifier en premier. Côté rouleau, le pilote peut à son tour
recadrer ou faire tourner la page : le PDF est au bon format, ce que la file d'impression en fait
reste à constater sur le matériel.

### 10.1 Parcours e2e écrits (2026-10-17)

Ce que l'écran promet est maintenant parcouru par Playwright, et illustré dans le guide :

| Parcours | Points couverts |
|----------|-----------------|
| `REF-66` — choisir le format de planche ou le rouleau | 2, 4, 5, 11, 12 (la case de départ est masquée, la capacité suit le format) |
| `REF-07` — étiquette standard, format par défaut | 1 (non-régression du calage par défaut, vue de l'écran) |
| `REF-65` — étiquette d'un lot | — (plan DataMatrix §10, points 13 et 14) |
| `REF-67` — réglette de gondole | 21 |
| `REF-68` — étiquette de préparation magistrale | 25 |
| `VTE-75` — étiquettes de posologie d'une ordonnance | 22, 24 |
| `ACH-76` — étiquettes d'un bon, type et format | 14 (la borne de la case de départ suit le format) |

**Le ZPL n'a aucun parcours, et n'en aura pas** : le chemin d'impression n'apparaît que sur un
rouleau **et** depuis le poste, c'est-à-dire sous Tauri. Un navigateur ne parle pas à
l'imprimante ; les points 15 à 20 restent couverts par `ZplEtiquetteServiceImplTest` et les
spécifications front, et validés sur le matériel.

**Un parcours périmé par la livraison.** `ACH-76` attendait « comprise entre 1 et 64 » et
`REF-07` « Planche A4 de 65 étiquettes » : les deux libellés ont disparu avec le catalogue des
formats. Corrigés en écrivant les nouveaux parcours — c'est la première fois que le lot 1 était
confronté à l'écran.
