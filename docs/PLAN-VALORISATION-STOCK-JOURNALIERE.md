# Plan — Valorisation du stock jour par jour sur une période

Rédigé le 2026-10-09. S'appuie sur la correction du stock à date (`V2.1.33__stock_a_date.sql`, voir
[PLAN-PILOTAGE-OFFICINE.md §9](PLAN-PILOTAGE-OFFICINE.md)) et sur le jeu de démonstration corrigé (même plan, §10).

## En bref

- **Le besoin** : voir la valeur du stock **chaque jour** d'une période choisie — courbe, tableau, variations — et comprendre
  pourquoi elle bouge (achats, ventes, ajustements, inventaires, retours), par famille, rayon, magasin ou produit.
- **Ce qu'on a** : un écran de valorisation **instantanée** (`mv_stock_valuation`) et, depuis `V2.1.33`, des fonctions qui donnent
  le stock et sa valeur à **n'importe quel instant** (`fn_stock_quantites_at_time`, `fn_stock_valuation_bulk`) et le bilan d'une
  période (`fn_stock_bilan_periode`). Il manque la série jour par jour et l'écran qui la montre.
- **Proposition** : un écran « Valorisation journalière » dans **Stock & Inventaire**, une fonction SQL qui calcule la série en une
  seule passe (cumul des mouvements, pas un appel par jour), une API, des exports. La même série alimente la courbe de l'onglet
  « Achats & stock » du pilotage.
- **Décision clé** : la **méthode de valorisation** (dernier prix d'achat, prix moyen pondéré ou prix courant).
- **Effort** : ≈ 6 jours.

---

## 1. Le besoin

| Question du titulaire | Réponse attendue |
|---|---|
| Combien vaut mon stock aujourd'hui, au 31 décembre, au dernier jour de chaque mois ? | Valeur à une date, en prix d'achat et en prix de vente |
| Comment a-t-elle évolué sur la période ? | Courbe jour par jour, avec la même période de l'année précédente |
| Pourquoi a-t-elle bougé ? | Décomposition quotidienne : stock de début + entrées − sorties ± ajustements ± inventaires = stock de fin |
| Où est l'argent immobilisé ? | Ventilation par famille, rayon, magasin, stockage, produit |
| Le stock gonfle-t-il plus vite que les ventes ? | Couverture en jours (valeur du stock / coût des ventes quotidien moyen) |
| Que dire à l'expert-comptable à la clôture ? | Valorisation à la date de clôture, exportable, détaillée par produit |

---

## 2. Ce que le projet a déjà

| Élément | Ce qu'il fait | Limite pour ce besoin |
|---|---|---|
| Écran « Valorisation du stock » (`rapport-stock.stock-valuation`, `mv_stock_valuation`) | Valeur actuelle par produit, famille, rayon | Instantané seulement |
| `fn_stock_quantites_at_time(T, magasin, produits)` (V2.1.33) | Stock et UG de chaque ligne à T, depuis les photos et le journal | Un instant ; appelée jour par jour sur un an, 365 passes |
| `fn_stock_valuation_bulk(magasin, produits, T)` | Valeur à T (stock + UG) au prix du dernier mouvement | Idem |
| `fn_stock_bilan_periode(magasin, début, fin)` | Début, entrées, sorties, ajustements, inventaires, fin d'une période | Une seule période, pas la série |
| `inventory_transaction` | Tous les mouvements, avec quantité avant / après, prix d'achat et de vente | — |
| `stock_produit_snapshot` | Photo quotidienne (stock et UG, depuis V2.1.33) | Ancre, pas une série valorisée |
| `pilotage_stock_mensuel` | Stock valorisé en fin de mois, depuis septembre 2023 | Mensuel |

---

## 3. Ce que font les autres

Les éditeurs publient peu le détail de cette fonction. Ce qui ressort :
- la **méthode de valorisation** est mise en avant : plusieurs critères possibles, le **prix d'achat moyen pondéré** étant souvent
  présenté comme le plus juste ;
- la **gestion de l'historique** est un point faible reconnu (historique des inventaires tournants mal géré selon des inventoristes) ;
- un **état à une date passée** suppose un inventaire permanent qui conserve tout l'historique des mouvements et applique une même
  méthode de valorisation — c'est ce que Pharma-Smart a désormais (journal + photos).

**À retenir** : proposer la valorisation à date et son évolution, ce qui reste rare, et rendre la méthode explicite et paramétrable.

---

## 4. Décisions à prendre

1. **Méthode de valorisation** :

   | Méthode | Principe | Pour | Contre |
   |---|---|---|---|
   | Dernier prix d'achat (actuel des fonctions) | Prix du dernier mouvement avant le jour | Simple, déjà en place | Une hausse de prix revalorise tout le stock d'un coup |
   | Prix moyen pondéré (CMP / PMP) | Moyenne des entrées pondérée par les quantités | Mis en avant par les éditeurs, admis en comptabilité (SYSCOHADA : coût moyen pondéré ou premier entré premier sorti) | À calculer et à stocker à chaque entrée |
   | Prix courant du fournisseur principal | Prix d'aujourd'hui, quelle que soit la date | Identique à l'écran actuel | Faux pour le passé |

   Proposition : **dernier prix d'achat** en première version (déjà calculé), **PMP** en seconde si l'expert-comptable le demande.
2. **Instant retenu pour « la valeur du jour »** : fin de journée (23 h 59, heure locale) — proposition.
3. **Profondeur** : période maximale affichée en granularité jour (proposition : 13 mois ; au-delà, semaine ou mois).
4. **Emplacement** : onglet de « Stock & Inventaire » (proposé, §5.1), ou section de l'onglet « Achats & stock » du pilotage.
5. **Droits** : qui voit la valeur d'achat (même règle que la marge).

UG : **incluses** (décision du 2026-10-09).

---

## 5. L'écran

### 5.1 Emplacement

Nouvelle section `rapport-stock.valorisation-journaliere` dans la page **Stock & Inventaire**, à côté de « Valorisation du stock »
(qui reste l'instantané détaillé). La courbe de l'onglet **Achats & stock** du pilotage lit la même API et renvoie ici pour le détail.

### 5.2 Barre de sélection

| Contrôle | Valeurs |
|---|---|
| Période | du / au ; raccourcis : mois en cours, mois précédent, trimestre, année en cours, année précédente, 12 mois glissants |
| Comparer à | même période N-1, aucune |
| Granularité | jour (défaut), semaine, mois — la valeur d'une semaine ou d'un mois est celle de son dernier jour |
| Valeur | prix d'achat (défaut), prix de vente, les deux |
| Filtres | magasin, stockage, famille, rayon, laboratoire, fournisseur principal |

### 5.3 Contenu

**Tuiles** : valeur au début, valeur à la fin, variation (valeur et %), valeur moyenne, minimum et maximum (avec leur date),
couverture en jours à la fin.

**Courbe** : valeur jour par jour (achat et/ou vente), période de comparaison en pointillés ; repères des inventaires.

**Tableau journalier** (une ligne par jour, ou semaine / mois) :

| Jour | Valeur début | Entrées | Sorties | Ajustements | Inventaires | Retours | Valeur fin | Variation |
|---|---|---|---|---|---|---|---|---|

Entrées, sorties, etc. sont valorisées au prix de leur mouvement : la ligne se lit comme une explication de la variation. Clic sur
une cellule = liste des mouvements du jour concernés.

**Ventilation** (sélecteur) : par famille, rayon, magasin, stockage — tableau croisé « élément × période » (valeur de fin) avec la
variation, ou part du total à la date de fin.

**Détail produit** : courbe du stock et de sa valeur pour un produit, avec ses mouvements.

**Exports** : Excel et CSV du tableau journalier et de la ventilation ; **valorisation détaillée par produit à une date** (PDF et
Excel) pour la clôture comptable.

### 5.4 Accessibilité et thème

Composants du Design System (`app-toolbar`, `app-kpi-strip`, `app-chart`, `app-data-table`, `app-nav-tabs`), couleurs lues dans les
jetons du thème, contraste vérifié par le garde-fou existant, tableau navigable au clavier, nom accessible sur chaque graphique (titre
et résumé textuel de la série).

---

## 6. Données

### 6.1 Une fonction en une passe

Appeler `fn_stock_valuation_bulk` pour chaque jour d'une année ferait 365 reconstitutions. À la place :

```
fn_stock_valorisation_journaliere(p_du DATE, p_au DATE, p_magasin_id INT, p_produit_ids INT[])
  → jour, produit_id, storage_id, qty_stock, qty_ug, prix_achat, prix_vente,
    entrees, sorties, ajustements, inventaires, retours (quantités et valeurs du jour)
```

1. Stock de départ = `fn_stock_quantites_at_time(veille de p_du, fin de journée)` (une seule reconstitution).
2. Mouvements de la période agrégés par jour × produit × stockage et par type.
3. Stock de chaque jour = départ + somme cumulée des mouvements (fonction de fenêtre), sur la grille des jours
   (`generate_series`) — un produit sans mouvement garde son stock.
4. Prix du jour = dernier prix connu à cette date (report du dernier mouvement par fenêtre) ; UG : celles de la photo du jour, à
   défaut la dernière connue.
5. Les agrégations (par famille, rayon…) se font dans le dépôt, en JPQL ou en requête sur la fonction, avant de remonter : l'API ne
   renvoie jamais 365 × 1 600 lignes.

Ordre de grandeur : une année × 1 600 lignes de stock ≈ 600 000 lignes intermédiaires, agrégées en base — attendu sous la seconde.
Si ce n'est pas le cas sur une grosse officine : table `stock_valorisation_jour` alimentée chaque jour par le recalcul du pilotage
(même mécanisme que les agrégats, rattrapage au démarrage).

### 6.2 API

```
GET /api/stock/valorisation-journaliere?du&au&granularite&valeur&magasin&storage&famille&rayon&comparaison
    → { periode, reference?, series: [{ jour, valeurDebut, entrees, sorties, ajustements, inventaires, retours, valeurFin }] }
GET /api/stock/valorisation-journaliere/ventilation?…&ventilation=famille
GET /api/stock/valorisation-journaliere/mouvements?jour&produit…
GET /api/stock/valorisation-a-date?date&format=pdf|xlsx        (clôture comptable)
```

`@RequiresNavAccess("rapport-stock.valorisation-journaliere")` ; export sous le droit d'export.

### 6.3 Code

Conventions du projet : DTO dans `service/dto/stock/`, requêtes dans un dépôt (custom pour l'appel de la fonction), service
interface + implémentation dans des paquetages séparés, un seul passage sur chaque liste.

---

## 7. Tests

- Intégration (`StockADateIntegrationTest` étendu) : la série retombe sur `fn_stock_valuation_bulk` à chaque jour ; valeur de fin
  du jour J = valeur de début du jour J+1 ; début + entrées − sorties ± ajustements ± inventaires = fin, chaque jour ; produit sans
  mouvement constant ; stock négatif conservé ; UG incluses.
- Cohérence avec le pilotage : la valeur du dernier jour d'un mois = `pilotage_stock_mensuel`.
- Front : Jest (sélecteurs, granularité, droits) ; e2e : un parcours « valorisation au 31 décembre ».

---

## 8. Phases

| Phase | Contenu | Effort |
|---|---|---|
| 0 | Décisions du §4 | — |
| 1 | Fonction `fn_stock_valorisation_journaliere` + tests de cohérence | 1,5 j |
| 2 | Dépôt, service, API (série, ventilation, mouvements du jour) | 1 j |
| 3 | Écran : barre, tuiles, courbe, tableau journalier, détail | 2 j |
| 4 | Exports, dont la valorisation détaillée à date pour la clôture | 1 j |
| 5 | Intégration à l'onglet « Achats & stock » du pilotage | 0,5 j |
| Plus tard | Prix moyen pondéré, si retenu | à chiffrer |

---

## Sources

- [Le Quotidien du Pharmacien — Mener son inventaire en toute autonomie, est-ce possible ?](https://www.lequotidiendupharmacien.fr/gestion-de-lofficine/agencement-equipement/mener-son-inventaire-en-toute-autonomie-est-ce-possible)
- [Mémoire Online — Gestion de stock des produits de santé d'une officine](https://www.memoireonline.com/10/22/13135/m_Gestion-de-stock-des-produits-de-sant-d-une-officine-de-pharmacie--cas-de-la-pharmacie-Saint-Lu20.html)
- [Kolonell — Logiciel de gestion de stock pharmacie (blog éditeur)](https://kolonell.com/fr/blog/logiciel-gestion-stock-pharmacie-dakar-alertes-peremption-2026)
