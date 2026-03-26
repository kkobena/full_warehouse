# Analyse UX/Fonctionnelle — Saisie · Réception · Historique Bons de Livraison
## Pharma-Smart vs Logiciels de référence (Winpharma · Périscopie · Pharmagest iSoft)
## Date : Mars 2026

---

## Périmètre analysé

| Composant | Fichier | Rôle |
|---|---|---|
| **commande-requested** | `feature/commande-requested/` | Saisie et modification d'une commande fournisseur |
| **commande-received** | `feature/commande-received/` | Réception et validation d'une livraison |
| **list-bons** | `ui/list-bons/` | Historique des bons de livraison clôturés |

---

## 1. `commande-requested` — Saisie de commande

### Ce qu'on a aujourd'hui

Layout : Header toolbar + bandeau reliquat/PharmaML + zone recherche (fournisseur → produit → quantité)
+ table 10 colonnes (col-8) + panneau PharmaML (col-4).

**Points positifs existants :**
- Raccourcis clavier F9 (créer bon) et Escape (reset recherche) — bon réflexe B2B
- Bandeau contextuel reliquat et PharmaML lock bien visibles
- Meta produit sélectionné (CIP, prix, stock) sous la barre de recherche
- Inline cell editing pour PA/PU/Qté

---

### Problèmes identifiés

#### 1.1 — Densité colonnes excessive 🟠

La table comporte **10 colonnes actives** : #, Code, Description, Stock, P.A, P.A Machine, P.U, P.U Machine, Qté, Qté UG — plus la colonne Lots conditionnelle.

| Colonne | Usage réel | Verdict |
|---|---|---|
| P.A | Prix commandé (modifiable inline) | ✅ nécessaire |
| P.A Machine | Prix catalogue actuel (lecture seule) | 🟡 secondaire |
| P.U | Prix vente commandé (modifiable inline) | ✅ nécessaire |
| P.U Machine | Prix vente actuel (lecture seule) | 🟡 secondaire |

Les colonnes "Machine" n'ont de sens que quand elles **diffèrent** de la valeur commandée.
Actuellement l'écart est signalé uniquement par la couleur de ligne (`pharma-row-danger`,
`pharma-row-warning`). L'utilisateur doit donc scanner mentalement deux colonnes pour chaque
ligne pour détecter une anomalie — charge cognitive élevée.

**Ce que font les logiciels de référence :**
- **Winpharma** : une seule colonne "P.A" avec une icône ⚠ inline si écart détecté. Clic sur l'icône → popup comparaison avant/après.
- **Pharmagest** : colonne "Variation %" en dernier, colorée uniquement si écart > seuil. Le reste est masqué.

#### 1.2 — Panneau PharmaML toujours présent (col-4) 🟠

Le panneau PharmaML occupe **33% de la largeur** même lorsque la commande n'a pas encore de
lignes ou n'est pas encore soumise. La table produit se retrouve compressée à col-8.

Pour la majorité des commandes manuelles courantes, ce panneau est vide ou peu utile
pendant la phase de saisie. Il devient pertinent après l'envoi PharmaML.

**Impact :** La table produits doit afficher des prix en 8 colonnes dans 66% de l'écran,
forçant les libellés à être tronqués.

#### 1.3 — "Depuis suggestion" indiscoverable 🟠

Le bouton "Depuis suggestion" (`pi pi-lightbulb`, `severity="help"`) est caché dans le
header toolbar et n'apparaît **que si** `currentCommande?.commandeId && !isLocked`.

En pratique le workflow le plus commun est exactement celui-ci : l'utilisateur arrive
sur la page avec une commande existante et veut importer des lignes de suggestion SEMOIS.
Ce bouton est le CTA le plus fréquent — il devrait être en position primaire, pas tertiaire.

**Ce que font les logiciels de référence :**
- **Périscopie** : Bouton "Importer suggestions" en position 1 dans la toolbar, avec compteur de lignes disponibles (badge).
- **Winpharma** : L'import de suggestions génère automatiquement une commande, le flux est inversé (suggestion → commande).

#### 1.4 — Pagination sur tableau de saisie active 🔴

La `p-table` a `[paginator]="true"` avec `[rows]="10"`. Pendant la saisie, si l'utilisateur
ajoute plus de 10 lignes, les nouvelles lignes disparaissent derrière la page 2.

C'est un **anti-pattern critique** pour les formulaires de saisie : l'utilisateur
croit que sa ligne n'a pas été ajoutée. Les erreurs de doublon augmentent.

**Règle UX (Jakob Nielsen, Heuristic #1 — Visibility of system status) :**
Toujours montrer les lignes nouvellement ajoutées sans que l'utilisateur
doive naviguer pour les trouver.

#### 1.5 — Checkbox multi-sélection en dernière colonne 🟡

La checkbox de sélection (`p-tableHeaderCheckbox`, `p-tableCheckbox`) est placée en
**dernière position** alors que les patterns B2B standard (SAP, Odoo, Sage) la placent
systématiquement en premier. L'utilisateur cherche la checkbox à gauche.

#### 1.6 — Aucun indicateur de couverture stock 🟡

À la sélection d'un produit, le méta-bloc affiche `Stock: 123` en unités brutes.
Aucun indicateur de couverture (jours/semaines). Le pharmacien ne sait pas si ce stock
couvre 2 jours ou 3 mois.

**Ce que fait Pharmagest :** `Stock : 48 unités (~12 jours)` — couverture calculée
à partir de la vélocité SEMOIS.

---

### Tableau de synthèse commande-requested

| # | Problème | Impact | Effort |
|---|---|---|---|
| 1.1 | Colonnes PA/PU doublées → charge cognitive | Moyen | Faible |
| 1.2 | PharmaML col-4 toujours visible → table compressée | Moyen | Faible |
| 1.3 | "Depuis suggestion" indiscoverable | Fort | Faible |
| 1.4 | Pagination pendant saisie active | **Critique** | Faible |
| 1.5 | Checkbox en dernière colonne | Faible | Très faible |
| 1.6 | Absence couverture stock | Moyen | Moyen |

---

## 2. `commande-received` — Réception de livraison

### Ce qu'on a aujourd'hui

Layout : Header toolbar + Filters toolbar + col-3 (concordance + summary card) + col-9 (table 15 colonnes).

**Points positifs existants :**
- `CommandeStatusBarComponent` → progression visible en permanence
- `ReceptionConcordanceComponent` en panneau latéral gauche — bonne idée
- `lineStatut` (Servi/Rupture/Partiel/Excédent) avec severity color → bon
- Vérification variations de prix avant finalisation → excellente sécurité métier
- `computeAfterStock` en temps réel → précieux pour la pharma

---

### Problèmes identifiés

#### 2.1 — 15 colonnes dans la table de réception 🔴

La table comporte : #, Code, Description, Stock, P.A, P.A Machine, P.U, P.U Machine,
Qté cmdée, Qté reçue, Qté UG, Stock après, Statut, Lots, Actions = **15 colonnes**.

Sur un écran 1920px, la colonne "Description" (17%) ne peut afficher que ~20 caractères.
Sur 1366px (laptop standard en officine), les colonnes se compriment ou défilent.

La réception est une opération **opérationnelle urgente** (le livreur attend, le stock
est bloqué). L'utilisateur a besoin de 5 colonnes essentielles :
Description | Qté commandée | Qté reçue | Statut | Actions

Les données comptables (PA, PU, variations) sont secondaires et peuvent être dans
un panneau ou une vue "détail".

**Ce que font les logiciels de référence :**
- **Winpharma** : vue réception = 6 colonnes maximum. Toggle "Mode comptable" pour voir les prix.
- **Pharmagest** : mode scanner dédié (scan CIP → saisie Qté), le tableau n'a que 4 colonnes.
- **LGPI** (autre référence officine) : 2 modes — "Mode rapide" (scan + quantité) et "Mode détaillé" (toutes colonnes).

#### 2.2 — Libellés "PA Machine" / "PU Machine" non standards 🟡

Le terme "Machine" n'est pas le vocabulaire officine standard.
Les pharmaciens parlent de **"Prix Tarif"**, **"Prix Catalogue"**, ou **"Prix Grossiste"**
(selon les référentiels PharmML, CNAM, etc.).

Un utilisateur nouveau ne comprend pas la distinction PA / PA Machine.
La mise en rouge de la ligne est l'unique signal — mais l'utilisateur ne sait pas
**pourquoi** la ligne est rouge sans lire attentivement 4 colonnes de prix.

#### 2.3 — Workflow finalisation = 3 modales successives 🔴

```
onConfirmFinalize()
  → checkPriceVariation()
    → si anomalie → confirmDialog #1 "Variation de prix détectée"
      → confirmFinalizeApresAlertesPrix()
        → confirmDialog #2 "Confirmation finale"
          → checkPutawayAndFinalize()
            → si putaway → modal PutawayModal
              → onFinalize()
                → confirmPrintTicket()
                  → confirmDialog #3 "Impression"
```

**5 interactions successives** pour finaliser une réception. Chaque modale interrompt le
flux et force l'utilisateur à recontextualiser.

**Nielsen Heuristic #8 (Aesthetic and minimalist design) + #3 (User control and freedom) :**
Regrouper toutes les décisions (anomalies prix, putaway, impression) dans un **écran de
synthèse unique** avant la finalisation irréversible.

**Ce que fait Pharmagest :** Un seul écran "Synthèse de réception" avec :
- Tableau des anomalies (si présentes)
- Option putaway (si configuré)
- Choix impression
→ Un seul bouton "Confirmer et valider".

#### 2.4 — "Tout valider" sans confirmation ni protection 🟠

Le bouton "Tout valider" met `quantityReceived = quantityRequested` pour **toutes les lignes**
immédiatement, avec seulement un tooltip comme information. Il n'y a pas de dialog de
confirmation pour cette action.

Un clic accidentel en cherchant "Valider" (bouton juste à côté) entraîne une réception
erronée de toutes les lignes. En contexte officine, cela peut signifier l'entrée en stock
de médicaments non réellement reçus.

**Règle UX (Nielsen #5 — Error prevention) :** Les actions de masse irréversibles
doivent toujours demander confirmation explicite.

#### 2.5 — Panel concordance caché derrière la table sur mobile 🟡

En `col-sm-12`, le panneau de concordance (col-3 desktop) s'empile **au-dessus** de la
table, et le `reception-summary-card` utilise `position: sticky` seulement sur desktop
(`@media max-width: 1200px` → `position: static`). Sur laptop 1366px, le card n'est
plus sticky et l'utilisateur perd le contexte financier en scrollant.

#### 2.6 — Aucune progression globale de réception 🟡

L'utilisateur ne sait pas combien de lignes restent à traiter. Une barre "22/35 lignes
validées" ou un compteur dans le status bar serait suffisant. La `ReceptionConcordanceComponent`
a probablement les données mais le composant parent ne les expose pas.

---

### Tableau de synthèse commande-received

| # | Problème | Impact | Effort |
|---|---|---|---|
| 2.1 | 15 colonnes → illisible en opérationnel | **Critique** | Moyen |
| 2.2 | Libellés "PA Machine" non standards | Moyen | Très faible |
| 2.3 | 3 modales successives pour finaliser | Fort | Moyen |
| 2.4 | "Tout valider" sans confirmation | Fort | Très faible |
| 2.5 | Concordance cachée sur 1366px | Faible | Faible |
| 2.6 | Absence compteur lignes validées | Moyen | Faible |

---

## 3. `list-bons` — Historique des bons de livraison

### Ce qu'on a aujourd'hui

Layout : Toolbar (search + fournisseur + date début/fin) + p-table avec expand rows
+ table imbriquée dans les lignes expandées.

**Points positifs existants :**
- Lazy loading correctement implémenté (pagination serveur)
- Row expansion pour les détails sans quitter la page — bonne idée
- Filtres date début/fin fonctionnels avec `onSelect` immédiat

---

### Problèmes identifiés

#### 3.1 — Statut hardcodé `CLOSED` uniquement 🔴

```typescript
const query: any = { statut: 'CLOSED', ... };
```

Cette liste n'affiche **jamais** les bons en cours (`PENDING`, `PARTIALLY_RECEIVED`) ni
les annulés. C'est une vue archive, mais son titre "Liste des bons" laisse croire que c'est
la liste complète.

Un utilisateur qui cherche un bon de livraison reçu partiellement hier ne le trouvera pas ici.
Il devra aller dans la vue commande-received — mais rien ne le guide vers là.

**Conséquence :** Les utilisateurs pensent que des livraisons ont disparu. Support inutile.

#### 3.2 — État vide absent 🔴

```html
@if (deliveries && deliveries.length > 0) {
  <p-table ...>
```

Quand la liste est vide (aucun bon sur la période, aucun résultat de recherche),
**rien n'est affiché**. L'utilisateur ne sait pas si la page charge, si la recherche
a échoué, ou s'il n'y a vraiment aucun résultat.

**Nielsen Heuristic #1 (Visibility of system status) :** Toujours afficher un état vide
explicite avec message et CTA ("Aucun bon de livraison pour cette période. Modifier les filtres.").

#### 3.3 — Table imbriquée dans row expansion 🟠

La ligne expandée contient une `p-table` complète avec **pagination propre** (rows=10,
paginator=true). C'est un **anti-pattern UX reconnu** (Nielsen Group, 2023) :
l'utilisateur gère deux niveaux de pagination simultanément, augmentant la charge
cognitive. De plus, la table enfant re-reçoit ses données depuis `item.receiptItems`
déjà chargé — mais si un bon a 100+ articles, la pagination enfant est inutile
(données déjà en mémoire).

**Alternatives standard :**
- Drawer latéral (Pharmagest, SAP Fiori)
- Page dédiée `/bon-livraison/:id` (Winpharma)
- Expansion simple sans pagination enfant (scroll interne)

#### 3.4 — Trois colonnes financières redondantes 🟡

| Colonne | Contenu | Priorité |
|---|---|---|
| Montant TTC | receiptAmount | ✅ P0 |
| Montant HT | netAmount | ✅ P0 |
| Taxe | taxAmount | 🟡 P1 — déductible (TTC - HT) |

"Taxe" est mathématiquement redondante (TTC - HT). Elle peut aller dans le détail expandé.
Récupérer 8% de largeur permet d'agrandir les colonnes Description/Fournisseur.

#### 3.5 — Aucun total de période 🟠

Pas de ligne de pied de tableau avec montant total HT/TTC pour la période filtrée.

En officine, la liste des bons est consultée pour la **réconciliation comptable**
(rapprochement avec les factures grossiste). L'absence de total force l'utilisateur
à faire une somme manuelle ou à exporter. Tous les logiciels de référence affichent
un total sur la période filtrée.

#### 3.6 — Pas d'action corrective depuis la liste 🟡

Les seules actions sur chaque ligne sont "Imprimer PDF" et "Imprimer étiquettes".
Il est impossible de :
- Rouvrir un bon pour corriger une erreur de saisie
- Voir la commande d'origine
- Dupliquer pour recommander les mêmes produits

**Winpharma** et **Pharmagest** proposent tous deux "Recréer commande similaire" depuis
l'historique des bons — gain de temps significatif pour les commandes récurrentes.

---

### Tableau de synthèse list-bons

| # | Problème | Impact | Effort |
|---|---|---|---|
| 3.1 | Statut CLOSED hardcodé → bons en cours invisibles | **Critique** | Très faible |
| 3.2 | État vide absent | Fort | Très faible |
| 3.3 | Table imbriquée paginée → charge cognitive | Moyen | Moyen |
| 3.4 | Colonne Taxe redondante | Faible | Très faible |
| 3.5 | Aucun total financier sur la période | Fort | Faible |
| 3.6 | Aucune action corrective ou duplication | Moyen | Moyen |

---

## 4. Problèmes transversaux aux 3 composants

### 4.1 — Duplication de logique métier entre les deux composants

`commande-requested` et `commande-received` partagent :
- `orderLineTableColor()` → **code identique** dans les deux fichiers
- `onAddLot()` → **logique identique** (ListLotComponent vs FormLotComponent selon qty)
- `editLigneInfos()` → appel identique à `EditProduitComponent`
- `refreshCommande()` / `reloadCommande()` → deux méthodes différentes pour le même effet

Ce n'est pas un problème UX visible, mais c'est un problème de maintenabilité qui
induit des **bugs de divergence** : si la logique de `lineStatut` change dans `received`,
elle ne change pas dans `requested`.

### 4.2 — Navigation : pas de chemin clair entre les trois vues

Le workflow naturel est : `list-bons → commande-received → commande-requested` (consulter
l'historique, voir une réception en cours, retourner à la saisie). Actuellement :
- `commande-received` → `previousState()` = `window.history.back()` → imprévisible
- `commande-requested` → idem
- `list-bons` → pas de lien vers la réception ou la saisie associée

Les boutons "Retour" basés sur `window.history.back()` sont fragiles : si l'utilisateur
arrive sur la page via un lien direct ou depuis un autre module, le retour peut mener
n'importe où.

### 4.3 — Accessibilité : `h1` vs `h5` incohérent

`commande-requested.html` : `<h1>Saisie de commande</h1>` (taille normale via CSS)
`commande-received.html` : `<h5>Réception de commande</h5>`

L'un est h1, l'autre h5. Les titres de page doivent être sémantiquement cohérents
(toujours h1 ou toujours h2 selon la hiérarchie globale).

---

## 5. Ce que font les logiciels de référence — Synthèse

| Fonctionnalité | Winpharma | Pharmagest | Périscopie | Pharma-Smart actuel |
|---|---|---|---|---|
| Mode scanner réception | ✅ dédié | ✅ dédié | ❌ | ❌ absent |
| Colonnes réception (mode opérationnel) | 6 max | 4 (scanner) | — | 15 |
| Finalisation en 1 étape | ✅ | ✅ | — | ❌ (3 modales) |
| Total financier liste BL | ✅ | ✅ | ✅ | ❌ absent |
| État vide explicite | ✅ | ✅ | ✅ | ❌ absent |
| Bons en cours dans historique | ✅ | ✅ | — | ❌ CLOSED seulement |
| Couverture stock à la saisie | ❌ | ✅ jours | ✅ | ❌ absent |
| "Depuis suggestion" en position primaire | ✅ | ✅ | ✅ | 🟡 caché |
| Duplication commande depuis BL | ✅ | ✅ | ❌ | ❌ absent |
| Progression réception (x/y lignes) | ✅ | ✅ | — | ❌ absent |

---

## 6. Recommandations UX experts — Principes appliqués au contexte officine

### 6.1 Fitts' Law — Boutons d'action primaire

En officine, les préparateurs travaillent debout, parfois avec des gants, souvent avec
une souris de bureau basique. Les boutons primaires (Valider, Créer bon) doivent être :
- **Larges** (min 36px hauteur) — déjà le cas avec PrimeNG
- **Consistants** en position à travers les écrans
- **Distants** des boutons destructifs (Supprimer, Tout valider)

**Actuellement :** "Tout valider" (secondary) et "Valider" (primary) sont côte à côte
dans le header de commande-received. Risque de clic accidentel élevé.

### 6.2 Recognition over recall

L'utilisateur ne doit pas mémoriser ce que fait chaque colonne.
"PA Machine" nécessite une connaissance préalable du vocabulaire interne.
Remplacer par "PA Catalogue" ou "PA Tarif" + un tooltip d'explication.

### 6.3 Progressive disclosure

Montrer les données dans l'ordre d'importance opérationnelle :
1. Identification produit (Code + Nom)
2. Quantités (commandée / reçue / écart)
3. Statut
4. Prix (en mode développé ou colonne masquable)

### 6.4 Error prevention over error correction

Les 3 confirmations pour finaliser une réception sont la preuve que le système
**corrige** plutôt que **prévient**. Un écran de synthèse unique avant la validation
finale est la solution standard (Gestalt principle of closure).

---

## 7. Priorisation globale

| Priorité | Composant | Action | Impact | Effort |
|---|---|---|---|---|
| 🔴 P0 | list-bons | Supprimer le filtre CLOSED hardcodé + ajouter filtre statut | Critique — bons invisibles | Très faible |
| 🔴 P0 | list-bons | Ajouter état vide explicite | Fort | Très faible |
| 🔴 P0 | commande-requested | Désactiver pagination pendant saisie (virtual scroll ou rows=1000) | Critique — perte de lignes | Très faible |
| 🔴 P0 | commande-received | Confirmation pour "Tout valider" | Fort — entrée stock erronée | Très faible |
| 🟠 P1 | commande-received | Fusionner 3 modales en 1 écran synthèse | Fort — fluidité workflow | Moyen |
| 🟠 P1 | commande-received | Réduire à 8 colonnes + mode comptable toggle | Fort | Moyen |
| 🟠 P1 | list-bons | Ajouter total financier de période | Fort — réconciliation comptable | Faible |
| 🟠 P1 | commande-requested | Remonter "Depuis suggestion" en position primaire | Fort | Très faible |
| 🟡 P2 | commande-requested | PharmaML panel → collapsible ou drawer | Moyen | Faible |
| 🟡 P2 | commande-requested | Ajouter couverture stock (jours) dans méta produit | Moyen | Moyen |
| 🟡 P2 | commande-received | Indicateur progression x/y lignes validées | Moyen | Faible |
| 🟡 P2 | commande-received | Renommer "PA Machine" → "PA Tarif" | Moyen | Très faible |
| 🟡 P2 | list-bons | Supprimer table imbriquée paginée → drawer ou page dédiée | Moyen | Moyen |
| 🟡 P2 | list-bons | Action "Recréer commande similaire" | Moyen | Moyen |
| 🟢 P3 | transversal | Extraire `orderLineTableColor` + `onAddLot` en service partagé | Maintenabilité | Faible |
| 🟢 P3 | transversal | Remplacer `window.history.back()` par navigation router explicite | Faible | Faible |
| 🟢 P3 | transversal | Homogénéiser h1/h5 entre les deux composants | Accessibilité | Très faible |

---

## 8. Synthèse

Les trois composants sont **fonctionnellement corrects** et couvrent le périmètre métier.
Les problèmes sont de nature UX et maintenabilité, pas de logique métier.

**Problème central :** La réception (`commande-received`) est conçue comme une **vue de
gestion** (toutes les données visibles en permanence) plutôt qu'une **vue opérationnelle**
(données essentielles + accès rapide aux détails). En officine, la réception se fait dans
l'urgence — le livreur attend, le stock doit entrer rapidement. 15 colonnes et 3 modales
successives vont à l'encontre de cette réalité terrain.

**Trois quick wins (< 30 min chacun) :**
1. Supprimer `statut: 'CLOSED'` hardcodé + ajouter `p-select` statut dans list-bons
2. Ajouter `@if (deliveries.length === 0)` état vide dans list-bons
3. Ajouter dialog de confirmation sur "Tout valider" dans commande-received

Ces trois corrections seules éliminent les risques métier les plus sérieux.