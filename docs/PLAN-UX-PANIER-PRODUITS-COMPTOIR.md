# Plan — Ergonomie du panier produits au comptoir (`product-list`)

> Fichier analysé : `pharmaSmart-app/src/main/webapp/app/features/sales/ui/product-list/`
> (`product-list.component.html` / `.ts` / `.scss`)

## État d'implémentation (2026-10-03)

**Fait** : raccourcis clavier du panier (lot 1) ; menu « Autres actions » avec détail du prix, ajout d'une
unité et exclusion du remboursement (lot 2, sans « Dupliquer » ni « Forcer stock ») ; prix négocié en
sous-titre et popover par tiers payant (lot 3) ; fusion des colonnes Qté (lot 4) ; badges générique /
stupéfiant / péremption proche dans le libellé, via `GET /api/sales/produits-signaux` ; clavier du popover de
remise (lot 5) ; tableau compact ; pagination au-delà de `seuilPagination` lignes ; filtre par libellé ;
colonne « NR » (vente assurance uniquement) qui exclut une ligne de la prise en charge — colonne
`sales_line.non_rembourse`, calcul `TiersPayantCalculationService`, et un second ticket pour ces lignes
(règlements et TVA répartis entre les deux tickets).

**Reste** : « Forcer stock » par ligne (règle de gestion : le forçage reste réactif à l'erreur de stock, avec
motif et autorisation) ; badge « stock faible / dernière boîte » ; historique d'une ligne.

## Le problème en une phrase

Le panier est la grille la plus regardée et la plus manipulée de toute la vente au comptoir — et
c'est pourtant l'écran qui demande **le plus de souris** : aucune ligne n'a de raccourci clavier,
aucune colonne n'affiche le prix négocié d'un tiers payant, et une seule action (supprimer) est
disponible alors que le comptoir en réclame cinq ou six.

---

## 1. Ce qui fonctionne déjà bien

- Édition en ligne des quantités et du prix via `<app-editable-cell>` (clic → input → Entrée) :
  pas de modale, c'est le bon pattern pour un POS.
- Alerte visuelle `pharma-row-warning` quand `quantitySold < quantityRequested` : utile pour
  repérer une rupture partielle en un coup d'œil.
- Ligne sélectionnée mise en évidence (`ligne-selected`) et sélection au clic sur la ligne.
- Remise globale gérée en popover (ajout/édition/suppression) sans quitter l'écran.
- Recherche/filtre produit dans le panier (utile quand le panier est long — ordonnance avec
  15-20 lignes par ex.).
- Footer récapitulatif (totaux Qté demandée / servie / montant) toujours visible.

---

## 2. Lacunes ergonomiques identifiées

### 2.1 Zéro raccourci clavier *dans la grille elle-même*

Le module `features/sales` a un système de raccourcis riche (`keyboard-shortcuts.mixin.ts` :
F2-F10, Alt+lettre, Ctrl en mode Tauri), mais **rien ne cible le panier** une fois qu'on y est :

| Action nécessaire au comptoir | Aujourd'hui | Lacune |
|---|---|---|
| Supprimer la ligne sélectionnée | Clic sur l'icône poubelle (petite cible, en bout de ligne) | Pas de touche `Suppr` / `Del` |
| Modifier la quantité servie de la ligne sélectionnée | Double clic sur la cellule QTÉ.S | Pas de raccourci type `+`/`-` ou `F3` contextuel |
| Naviguer ligne suivante/précédente **pour une correction** | Souris uniquement | Pas de navigation clavier dédiée à la relecture/correction du panier |
| Dupliquer une ligne (vente de 2 boîtes identiques scannées séparément) | Impossible | Pas de fusion automatique ni de raccourci "+1" |
| Annuler la dernière action sur une ligne | Impossible | Pas de `Ctrl+Z` local |

> ⚠️ **Point à ne pas casser** : aujourd'hui, valider la quantité servie avec `Entrée` referme la
> cellule **et** renvoie le focus vers le champ de recherche produit. C'est volontaire et c'est le
> cas d'usage dominant du comptoir : scanner → quantité → `Entrée` → scanner le produit suivant,
> en boucle, sans jamais quitter le clavier/la douchette. Ce comportement doit rester le défaut
> inchangé — voir §3.1 pour la stratégie qui ajoute la navigation ligne à ligne **sans** toucher à
> la sémantique de `Entrée`.

Un comptoir à fort débit (horaires de pointe, carnet d'ordonnances) vit au clavier/scanner-douchette ;
chaque aller-retour souris vers une icône de 24 px coûte 1 à 2 secondes — multiplié par 30-60
lignes/heure, c'est un vrai manque à gagner de productivité.

### 2.2 Colonne Actions sous-exploitée

Une seule action possible : **Supprimer**. Manquent, alors qu'ils sont des besoins *quotidiens* au
comptoir :

- **Voir/éditer le détail du prix** (prix de référence, remise ligne, prix négocié tiers payant)
  sans repasser par la cellule PU qui n'affiche qu'un seul nombre.
- **Dupliquer la ligne** (second conditionnement du même produit, quantité différente).
- **Appliquer un forçage de stock** (le modèle a déjà `forceStock` / `motifForcage` mais rien dans
  ce composant ne permet de le déclencher ou de le visualiser ligne par ligne).
- **Voir l'historique / origine de la ligne** (scan, recherche manuelle, import ordonnance) — utile
  en cas de litige client.
- **Basculer le produit en "non remboursable"** pour une vente mixte tiers payant/comptant (très
  fréquent avec les tiers payants plafonnés).

### 2.3 Pas de visibilité sur le prix négocié par assurance

C'est la lacune métier la plus importante signalée par la demande : le backend calcule déjà un
prix par tiers payant (`SaleItemInput.prixAssurances: List<TiersPayantPrixInput>`, avec
`OptionPrixType` = REFERENCE / POURCENTAGE / MIXED_REFERENCE_POURCENTAGE, et l'entité `SalesLine`
porte un champ JSON `rates: List<Rate>` par `compteTiersPayantId`), mais **rien de tout cela
n'atteint `ISalesLine` côté Angular ni l'écran panier**.

Résultat concret au comptoir : en vente ASSURANCE/CARNET, le caissier voit un seul prix (`PU`) sans
savoir s'il s'agit du prix public, du prix négocié, ou du prix de base — source d'erreurs et
d'appels au pharmacien titulaire pour vérification.

### 2.4 Densité d'information insuffisante / colonnes figées

- `LIBELLÉ` n'affiche que le nom produit : pas de DCI, pas de dosage/forme visible en un coup
  d'œil, pas de pictogramme stupéfiant/générique/périmé proche — autant d'info que le comptoir doit
  déjà connaître "par cœur" ou aller chercher ailleurs.
- `QTÉ.D` (demandée) et `QTÉ.S` (servie) sont deux colonnes séparées : au quotidien, 95 % des
  lignes ont Qté.D = Qté.S. Avoir deux colonnes pleines pour une info presque toujours redondante
  mange de l'espace utile pour, justement, le détail du prix.
- Pas de badge "stock faible" / "dernière boîte" dans la grille elle-même (alors que l'info existe
  côté stock) : le caissier ne le découvre qu'au moment de la rupture.

### 2.5 Accessibilité / affordance

- Icône poubelle seule (pas de libellé visible) : correcte en `ariaLabel`, mais petite cible
  tactile (~32 px) sur écran tactile comptoir.
- Pas de confirmation ni d'indication visuelle quand une quantité saisie est automatiquement
  plafonnée par le stock disponible — l'utilisateur doit déduire pourquoi sa saisie "n'a pas pris".
- Le popover de remise (`remisePopoverContent`) n'est pas navigable au clavier (pas de `tabindex`
  ni de gestion `↑/↓/Entrée` sur `.remise-popover-item`).

### 2.6 Pas de retour visuel différencié par type de vente

Le composant reçoit `saleType` (COMPTANT/ASSURANCE/CARNET) mais le tableau a un rendu identique
dans les trois cas. Hors, c'est justement en ASSURANCE/CARNET que l'info de prix négocié, de
plafond, et de part restant à charge est la plus critique visuellement.

---

## 3. Recommandations UX — comment le rendre plus efficace

### 3.1 Raccourcis clavier à ajouter dans la grille

**Principe directeur : deux modes d'interaction distincts, qui ne doivent jamais se marcher
dessus.**

1. **Mode saisie rapide (scan)** — le flux dominant, déjà en place et à préserver tel quel :
   scanner → `quantité` → `Entrée` → le focus repart automatiquement sur le champ recherche produit.
   On n'y touche pas.
2. **Mode correction panier** — un flux plus rare mais quotidien (relire/ajuster une ligne déjà
   ajoutée, souvent après avoir scanné plusieurs produits d'affilée). Ce mode se déclenche
   explicitement, par une touche *différente* de `Entrée`, pour qu'il n'y ait aucune ambiguïté ni
   régression sur le flux 1.

| Touche | Mode | Action | Pourquoi |
|---|---|---|---|
| `↓` / `↑` (focus hors input, ligne déjà sélectionnée) | Correction | Déplacer la sélection de ligne, sans ouvrir l'édition | Parcourir/relire le panier au clavier sans avoir à re-cliquer |
| `Ctrl+↓` / `Ctrl+↑` *(fonctionne même pendant l'édition d'une cellule)* | Correction | Valider la cellule en cours **et** ouvrir la même colonne sur la ligne suivante/précédente | Corriger plusieurs lignes de suite (ex. ajuster QTÉ.S sur 3 lignes) sans jamais déclencher le retour vers la recherche produit — à l'inverse de `Entrée`, qui lui ne bouge pas de la grille |
| `Suppr` (ligne sélectionnée, hors input) | Correction | Supprimer la ligne (avec la confirmation existante) | Évite l'aller-retour souris vers l'icône poubelle |
| `Entrée` sur `QTÉ.S`/`QTÉ.D`/`PU` | Scan *(inchangé)* | Valide la cellule et renvoie le focus vers la recherche produit | Comportement existant à préserver — c'est le cas d'usage dominant |
| `+` / `-` (ligne sélectionnée, hors édition) | Correction | Incrémenter/décrémenter `quantitySold` de 1 sans ouvrir l'input | Ajustement rapide type retour 1 boîte |
| `Ctrl+D` | Correction | Dupliquer la ligne sélectionnée | 2e conditionnement du même produit |
| `Espace` (ligne sélectionnée, hors édition) | Correction | Ouvrir le détail prix (prix négocié / remise ligne) de la ligne sélectionnée | Accès rapide à l'info sans clic précis |
| `Alt+N` *(à harmoniser avec le mixin existant)* | Correction | Appliquer forçage de stock sur la ligne sélectionnée | Cas rupture déjà géré par le modèle, pas encore exposé ici |

Ainsi, `Entrée` garde un seul sens partout dans l'écran de vente (retour à la recherche produit),
et la navigation ligne à ligne pour la correction passe par des touches qui n'entrent jamais en
collision avec lui (`↑/↓` simples quand on n'édite pas, `Ctrl+↑/↓` quand on édite).

Techniquement : exposer un `(keydown)` host binding sur `<app-data-table>` (ou sur le conteneur
`.product-list-container`), avec un filtre équivalent à `shouldHandleEvent()` du mixin existant
(ne pas intercepter les touches quand une cellule éditable est en cours de saisie, sauf `Ctrl+↑/↓`
qui doit justement fonctionner pendant l'édition, et `Échap`/`Entrée` déjà gérés par
`app-editable-cell`). Idéalement, factoriser un second mixin `createProductListShortcuts()` à côté
de `keyboard-shortcuts.mixin.ts`, pour rester cohérent avec le pattern déjà en place et apparaître
dans la modale d'aide F1.

### 3.2 Étoffer la colonne Actions (menu contextuel, pas juste une icône)

Remplacer le bouton poubelle unique par un petit groupe d'actions (ou un bouton "⋮" ouvrant un
menu, selon la place disponible) :

1. 🗑 Supprimer *(existant)*
2. 👁 Détail prix (ouvre le popover décrit en 3.3)
3. ⧉ Dupliquer
4. ⚠ Forcer stock *(visible seulement si rupture déjà détectée sur la ligne)*

Garder le bouton Supprimer seul et visible (c'est l'action la plus fréquente), et regrouper les
3 autres sous une icône secondaire pour ne pas surcharger visuellement chaque ligne.

### 3.3 Prix négocié par assurance : sous-titre dans la colonne PU (sans ajouter de colonne)

C'est la demande centrale. Proposition, en respectant la contrainte "pas de colonne
supplémentaire" :

**Dans la cellule `PU` existante**, afficher sur deux niveaux quand une négociation tiers payant
existe sur la ligne :

```
┌───────────────┐
│   1 250 F      │  ← prix affiché (celui réellement facturé / à comparer)
│   nég. 980 F   │  ← sous-titre, petit texte, uniquement si prix négocié ≠ prix régulier
└───────────────┘
```

- Si la vente est **COMPTANT** et qu'aucun tiers payant n'est engagé : comportement actuel,
  inchangé (pas de sous-titre, pas de régression visuelle).
- Si la vente est **ASSURANCE/CARNET** et qu'un tarif négocié existe pour le tiers payant
  sélectionné : afficher le prix négocié en sous-titre, avec une couleur distincte (ex. vert si
  négocié < régulier, orange si négocié > régulier — cas MIXED_REFERENCE_POURCENTAGE).
- Si **plusieurs** tiers payants sont engagés sur une même vente (carnet multi-garants), afficher
  le sous-titre du tiers payant **principal/prioritaire** (champ `priorite` déjà présent sur
  `IClientTiersPayant`), et rendre le sous-titre cliquable pour ouvrir un petit popover listant
  tous les prix négociés par tiers payant (même pattern popover que celui déjà utilisé pour les
  remises — réutilisable tel quel).

**Micro-maquette du popover multi-tiers-payant** (déclenché par clic sur le sous-titre) :

```
Prix négociés — Paracétamol 500mg
───────────────────────────────
CNPS          980 F   (POURCENTAGE -22%)
Mutuelle X   1 100 F   (REFERENCE)
Prix public  1 250 F
```

### 3.4 Modèle de données : champs à ajouter côté frontend

`ISalesLine` (frontend) devrait recevoir, en miroir des DTOs backend déjà existants
(`TiersPayantPrixInput`, `OptionPrixType`, `Rate`) :

```ts
export interface INegotiatedPrice {
  compteTiersPayantId: number;
  tiersPayantLibelle?: string;
  price: number;
  rate?: number;
  optionPrixType?: 'REFERENCE' | 'POURCENTAGE' | 'MIXED_REFERENCE_POURCENTAGE';
  priorite?: number;
}

export interface ISalesLine {
  // ...champs existants...
  negotiatedPrices?: INegotiatedPrice[];
}
```

Le mapping backend → frontend consiste à sérialiser le `rates`/`prixAssurances` déjà calculé par
`TiersPayantCalculationService` dans le DTO de ligne de vente exposé à l'API (actuellement non
exposé). C'est un ajout additif, sans risque de régression sur le flux de calcul existant.

### 3.5 Réduire la densité, regagner de l'espace visuel

- Fusionner visuellement `QTÉ.D` / `QTÉ.S` : n'afficher deux valeurs que si elles diffèrent
  (ex. `3` seul si égales, `3 / 2` avec alerte sinon). Gagne une colonne entière de large pour,
  justement, accueillir le sous-titre du prix négocié sans élargir la grille.
- Ajouter un petit badge discret dans `LIBELLÉ` (générique, stupéfiant, péremption proche) si ces
  infos existent déjà côté `IProduit` — à vérifier/compléter dans une itération séparée.

### 3.6 Accessibilité / affordances

- Agrandir légèrement la zone cliquable des boutons d'action (min 40×40 px) pour un usage tactile
  comptoir.
- Rendre le popover de remise et le futur popover prix négocié navigables au clavier
  (`tabindex="0"`, `(keydown.enter)`, `(keydown.arrowDown/Up)`).
- Indiquer visuellement (toast ou surlignage bref) quand une saisie de quantité a été plafonnée
  par le stock disponible, plutôt que de silencieusement ignorer l'excédent.

---

## 4. Plan d'implémentation proposé (par lots, non régressif)

| Lot | Contenu | Effort | Risque |
|---|---|---|---|
| 1 | Raccourcis clavier panier — mode correction (`↑/↓`, `Ctrl+↑/↓`, `Suppr`, `+/-`), sans modifier le comportement existant de `Entrée` | Faible — nouveau mixin + host binding | Faible, additif |
| 2 | Étoffer colonne Actions (dupliquer, détail prix, forcer stock) | Moyen — UI + câblage des outputs déjà existants (`lineRemoved` existe, ajouter `lineDuplicated`, `priceDetailRequested`) | Faible |
| 3 | `INegotiatedPrice[]` sur `ISalesLine` + sous-titre PU + popover multi-tiers-payant | Moyen/élevé — nécessite d'exposer les prix négociés déjà calculés côté backend dans le DTO de ligne de vente | Moyen (toucher le DTO de vente) |
| 4 | Fusion colonnes Qté.D/Qté.S + badges produit | Faible | Faible |
| 5 | Accessibilité clavier des popovers | Faible | Faible |

Recommandation : livrer le lot 1 (raccourcis) et le lot 3 (prix négociés) en priorité — ce sont les
deux manques les plus cités dans l'usage quotidien du comptoir ; les lots 2, 4, 5 peuvent suivre
sans dépendance bloquante.

