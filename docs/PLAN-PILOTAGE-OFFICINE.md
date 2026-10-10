# Plan — Menu « Pilotage de l'officine » et menu « Exports »

Rédigé le 2026-10-09 à partir d'une demande des pharmaciens. Synthèse de ce que proposent les autres éditeurs (dont une capture
d'un écran concurrent, prise comme exemple et non comme modèle) et de ce que Pharma-Smart sait déjà faire.
Complète, sans les remplacer : [ANALYSE-RAPPORTS-COMPARATIFS-EVOLUTIFS.md](ANALYSE-RAPPORTS-COMPARATIFS-EVOLUTIFS.md),
[ANALYSE-COMPARATIVE-RAPPORTS-OFFICINE.md](ANALYSE-COMPARATIVE-RAPPORTS-OFFICINE.md) (en partie périmées : plusieurs « GAP » y sont
depuis réalisés) et [PLAN-DASHBOARD-PERSONNALISABLE.md](PLAN-DASHBOARD-PERSONNALISABLE.md).
Le contenu détaillé de chaque onglet, avec son analyse comparative, est dans [PLAN-PILOTAGE-ONGLETS.md](PLAN-PILOTAGE-ONGLETS.md).

## En bref

- **Le besoin** : voir l'activité de l'officine d'un coup d'œil, suivre son évolution, comparer des périodes (mois / mois N-1, N / N-n,
  trimestre, année), par famille, sur le CA, la marge et les remises, et exporter les données.
- **Ce qu'on a** : beaucoup de matière — 29 rapports, 18 vues matérialisées, des rapports programmés, un tableau de bord
  personnalisable. **Ce qui manque n'est pas des chiffres, c'est un poste de pilotage** : une période et une comparaison communes, un
  seul chiffre par indicateur (le CA diffère aujourd'hui d'un écran à l'autre), plusieurs années, un endroit pour exporter.
- **Ce qu'on retient des autres** : la synthèse quotidienne, deux niveaux de lecture (titulaire / analyse), la descente de la vue
  d'ensemble jusqu'au produit, la comparaison au même mois N-1, l'accès mobile. **Ce qu'on écarte** : les dix onglets par domaine de
  données (on y cherche longtemps), le benchmark et l'observatoire de prix (ils demandent des données d'autres officines).
- **Ce qu'on ajoute, que les autres font peu** : l'**explication automatique des variations** (« le CA baisse de 8 % : −5 % de
  fréquentation, −3 % de panier ; la famille X y contribue pour moitié »), la comparaison **à date**, la **projection de fin de mois**,
  les **objectifs**, les **alertes** et un **bilan mensuel** envoyé automatiquement.
- **Une seule page « Pilotage de l'officine »**, comme sur la capture : la barre de période en haut, puis des onglets (`app-nav-tabs`) —
  Tableau de bord · Analyser · Comparer les années · Rentabilité & remises · Achats & stock · Trésorerie & tiers payant ·
  Clients & équipe · Objectifs. Les onglets sont rangés par question plutôt que par table. **Exports** reste une entrée de menu à part.
- **Effort** : ≈ 35 jours en huit phases ; première version présentable (tableau de bord + comparaisons) en ≈ 13 jours.

---

## 1. Le besoin

| Exprimé par les pharmaciens | Ce que cela demande |
|---|---|
| Vue complète de l'activité | Une page qui répond à « comment va l'officine ? » : ventes, marge, achats, caisse, tiers payant, stock, sur la même période |
| Courbes d'évolution, comparatives, variations | Séries temporelles avec la période de référence superposée ; variation en valeur, en %, en points |
| Tableaux | Détail par période avec la variation ligne à ligne, exportable |
| Mois / mois N-1, N / N-n, trimestre, année N-n | Période et comparaison choisies indépendamment, jusqu'à N-5 |
| Par famille | Toute analyse ventilable et filtrable par famille (et rayon, laboratoire, fournisseur, type de vente, TVA) |
| CA (N-n) | Plusieurs années superposées, tableau croisé années × mois / famille |
| Remises accordées | Montant, taux, par vendeur, famille, produit, type de vente ; ce qu'elles coûtent en marge |
| Menu dédié aux exports | Catalogue d'exports de données, formats, historique, programmation |

Derrière ces demandes, quatre questions reviennent chez tous les titulaires : **comment va l'officine ? pourquoi ça bouge ? où
est-ce que je gagne ou perds de l'argent ? est-ce que mes achats, mon stock et ma trésorerie sont maîtrisés ?** Le menu proposé est
rangé selon ces questions, pas selon les tables de la base.

---

## 2. Ce que le projet a déjà

### 2.1 Inventaire

| Domaine | Existant | Où |
|---|---|---|
| Rapports CA | 12 : tableau de bord CA, synthèse journalière, top produits (avec mois précédent), comparatif (mensuel / trimestriel / annuel, global / famille / fournisseur), saisonnalité N vs N-1, prévisions, rentabilité, remises, ventes par vendeur, rétention, panier type (market basket), génériques | `entities/reports`, menu `rapport-ventes` |
| Rapports stock | 5 : valorisation, ABC, alertes, démarque, récapitulatif produits vendus | `rapport-stock` |
| Partenaires | segmentation clients (RFM), performance fournisseurs | `rapport-partners` |
| Finance | 10 : créances, vieillissement créances et différés, taux de recouvrement TP, concentration payeurs, P&L analytique, BFR, avoirs… | `rapport-finance` |
| Agrégats | 18 vues matérialisées (`mv_dashboard_ca_daily`, `mv_dashboard_ca_product_families`, `mv_daily_sales_summary`, `mv_monthly_top_products`, `mv_product_profitability`, `mv_stock_*`, `mv_supplier_performance`, `mv_customer_rfm`…) | migrations Flyway |
| Rafraîchissement | 3 niveaux de cron (15 min / 1 h / 4 fois par jour), `REFRESH … CONCURRENTLY` | `MaterializedViewRefreshService` |
| Rapports programmés | 13 types, job horaire | `ScheduledReportService`, `ScheduledReportType` |
| Tableau de bord personnalisable | widgets par rôle, phases 0–3 réalisées | `features/dashboard`, `PLAN-DASHBOARD-PERSONNALISABLE.md` |
| Application mobile | rapports sur Android | `pharma-mobile-report` |
| Exports | ≈ 30 boutons PDF / Excel répartis dans les écrans, export comptable, déclaration de CA | divers |
| Conventions | lecture des vues par entités `@Immutable` (`MvStockAlert`, `MvStockValuationView`…) et JPQL | `pharmaSmart-domain` |

### 2.2 Ce qui manque ou gêne

1. **Pas de poste de pilotage.** Les 29 rapports sont rangés en quatre pages à onglets ; aucune page ne met ventes, marge, achats,
   caisse et stock côte à côte sur la même période, et aucune ne dit *pourquoi* un chiffre bouge.
2. **Comparaisons limitées.** Le comparatif ne connaît que « année N contre N-1 » sur l'année civile ; la saisonnalité aussi. Pas de
   période libre contre une référence libre, pas de N-2…N-5, pas de comparaison « à date » pour une période en cours, pas de 12 mois
   glissants partout, pas de jours ouvrés.
3. **Le CA n'a pas la même définition partout** (constat, à trancher — §8) :
   - `mv_daily_sales_summary` : ventes `CA` et `CA_DEPOT` sur des **lignes séparées** (groupées par type de vente et par catégorie),
     exclut les ventes importées, `ca_net = montant − remise` ; l'écran « Synthèse journalière » affiche le dépôt comme un type à
     part, mais son bandeau de totaux additionne tous les types, dépôt compris ;
   - `mv_dashboard_ca_daily` et `mv_dashboard_ca_product_families` : `CA` seul, ventes importées incluses ;
   - `ComparativeReportRepository` : CA net, `CA` seul, calculé en direct sur `sales` (`EXTRACT(YEAR …)`).
   Le même mois peut afficher trois chiffres selon l'écran — le défaut le plus coûteux en confiance pour un outil de pilotage.
4. **Ventes importées** : `imported = true` marque les ventes faites dans un sous-magasin physique (dépôt) puis importées. Elles
   doivent compter dans le CA du pilotage (décision du 2026-10-09). `mv_daily_sales_summary` les exclut ; elle reste telle quelle
   (décision du 2026-10-09) : le pilotage lit ses propres agrégats.
5. **Coût des requêtes** : les comparatifs lisent la table `sales` en direct, avec des fonctions sur la date qui empêchent l'usage d'un
   index ; le coût croît avec l'historique, précisément ce que N-n sollicite.
6. **Achats absents du pilotage** : aucun ratio ventes / achats, aucune courbe d'achats comparée aux ventes.
7. **Stock sans historique** : la valorisation est un instantané ; impossible de comparer le stock ou sa rotation à N-1 sans
   photographies périodiques.
8. **Ni objectifs, ni alertes** : rien ne dit si le mois est en avance ou en retard, rien ne prévient d'une dérive (remises d'un
   vendeur, chute d'une famille).
9. **Exports dispersés** : un bouton par écran, surtout du PDF ; pas d'export de données brutes, pas de catalogue, pas d'historique de qui
   a exporté quoi (données patients incluses).

---

## 3. Ce que font les autres éditeurs

### 3.1 Panorama

| Éditeur / outil | Ce qui est mis en avant |
|---|---|
| **Pharmagest — id.décisionnel** (ex-LGPI) | Tableaux de bord **simplifiés ou experts** ; six domaines : vente, conseil, services aux patients, gestion, achat, management ; temps réel, accès nomade |
| **Pharmagest — My Pilot** | Benchmark contre un panel (≈ 500 officines) ; ratios de gestion, stocks, achats, prix ; simulation de regroupement ; préparation des visites fournisseurs, simulation de marge selon les quantités |
| **Smart Rx 360** (ex-Alliadis, Cegedim) | **Synthèse quotidienne** ; analyses **macro → produit** ; par famille et par taux de TVA ; observatoire de prix (5 officines voisines) ; tableaux consolidés pour groupements (achats, stocks, ventes) ; cloud |
| **Winpharma — winStat** | Quatre onglets, « indicateurs clés en un clic », marges, performance de l'équipe, panel |
| **Ospharm — Datastat** | Panel d'environ 6 000 officines ; baromètres mensuels en évolution sur le **même mois de l'année précédente** |
| **Écran concurrent fourni** (éditeur local, FCFA) | Dix onglets par domaine ; tuiles ; courbe du CA sur 13 mois ; tableau mensuel avec variation sous chaque valeur ; Excel ; « Recalculer » |
| **BI généraliste** (Power BI, Metabase, Grafana) | « Time intelligence » (YTD, même période N-1, glissant) ; explorateur libre ; vues enregistrées ; export de la vue ; alertes sur seuil |

La presse professionnelle recommande au titulaire peu d'indicateurs, suivis régulièrement : évolution mensuelle du CA **en jours
ouvrés**, fréquentation, panier moyen **ordonnance / hors ordonnance**, marge commerciale (« l'indicateur le plus important »),
productivité du personnel, trésorerie. Ratios de bilan cités pour la France (à recalibrer pour la Côte d'Ivoire) : rotation du stock
≈ 45 jours, crédit clients ≈ 8 jours, crédit fournisseurs ≈ 38 jours, trésorerie d'un mois à un mois et demi d'achats.

### 3.2 Ce qu'on retient, ce qu'on écarte

| Idée | Vue chez | Décision | Pourquoi |
|---|---|---|---|
| Synthèse quotidienne en une page | Smart Rx 360, capture | **Retenue** — Tableau de bord | C'est la page qu'on ouvre tous les matins |
| Deux niveaux : simplifié / expert | id.décisionnel | **Retenue** — Tableau de bord (titulaire) / Analyser (expert) | Le titulaire veut une réponse, le gestionnaire veut fouiller |
| Descente vue d'ensemble → famille → produit → ventes | Smart Rx 360 | **Retenue**, partout | Un chiffre qui inquiète doit s'expliquer en deux clics |
| Ventilation par taux de TVA | Smart Rx 360 | **Retenue** comme dimension | Utile aux déclarations, coût nul |
| Référence « même mois N-1 » | Ospharm | **Retenue** comme comparaison par défaut | Neutralise la saisonnalité |
| Jours ouvrés, panier ordonnance / hors ordonnance | presse pro | **Retenues** | Comparaisons justes ; lecture métier du panier |
| Courbe 13 mois + tableau à variations | capture | **Retenues** comme composants | Lisibles, éprouvés |
| Performance de l'équipe | winStat, id.décisionnel | **Retenue** — Clients & équipe | Remises et annulations par vendeur : vrai levier |
| Simulation de marge à l'achat | My Pilot | **Plus tard** | Utile, mais relève du module commande |
| Accès nomade | id., Smart Rx (cloud) | **Retenue** via la même API pour `pharma-mobile-report` | L'application mobile existe déjà |
| Consolidation multi-sites | Smart Rx (groupements) | **Retenue** pour les magasins de l'officine | Le modèle connaît déjà les magasins |
| Vues enregistrées, export de la vue, alertes | BI généraliste | **Retenues** | Évitent de refaire dix fois le même réglage |
| Une seule page, une barre de période, des onglets | capture | **Retenue** | Tout est au même endroit, la période ne se ressaisit pas |
| Onglets découpés par domaine de données (« KPI Analyse », « Qualité–Exploitation »…) | capture | **Remplacée** par des onglets par question + un explorateur | On cherche où est l'information ; « Analyser » évite de multiplier les onglets |
| « Recalculer » visible de tous | capture | **Écartée** | Réservé à l'administrateur |
| Comparer un mois en cours à un mois entier | capture (« ▼ 78,6 % » au 9 octobre) | **Écartée** | Variation fausse ; on compare à date |
| Benchmark panel, observatoire de prix des voisins | My Pilot, Smart Rx, Ospharm | **Écartée pour l'instant** | Demande les données d'autres officines ; possible plus tard pour un groupement, avec accord |
| Domaines « conseil » et « services aux patients » | id.décisionnel | **Écartée pour l'instant** | Pas de données structurées pour les mesurer |

### 3.3 Ce qu'on ajoute, que les autres font peu

1. **Explication des variations** : chaque écart est décomposé en effets — fréquentation × panier moyen, puis panier = articles par
   vente × prix moyen — et les **principaux contributeurs** (familles, produits, vendeurs, organismes) sont listés. Le titulaire lit
   « pourquoi » au lieu de chercher.
2. **Comparaison à date** et en jours ouvrés, par défaut.
3. **Projection de fin de période** : « à ce rythme, octobre finira à 92 M (objectif 95 M) », d'après le réalisé à date et le profil
   du même mois N-1.
4. **Objectifs** mensuels (CA, marge, taux de remise maximal…) et suivi.
5. **Alertes** de pilotage sur seuil ou sur dérive.
6. **Bilan mensuel** : un PDF lisible envoyé automatiquement le 1er du mois (réutilise les rapports programmés).
7. **Événements** saisis sur les courbes (garde, rupture d'un grossiste, travaux, campagne) : une variation s'explique souvent par un
   fait que les chiffres ignorent.

**Positionnement** : sur l'analyse avancée (RFM, panier type, prévisions, P&L, BFR), Pharma-Smart est déjà au-dessus de la moyenne.
Le retard est sur l'**ergonomie de pilotage** et la **fiabilité perçue**. Les points 1 à 3 ci-dessus sont la vraie différence.

---

## 4. Principes

1. **Une définition par indicateur.** Un dictionnaire côté serveur (code, libellé, formule, unité, sens favorable, format) que tous les
   écrans, exports, widgets et l'application mobile lisent. Chaque indicateur affiché dit, en infobulle, comment il est calculé.
2. **Comparer à périmètre égal.** Une période en cours se compare à date ; option jours ouvrés. Une variation sur une base nulle ou
   incomplète s'affiche « n.s. », pas « ▲ 999 % ».
3. **Période et comparaison indépendantes**, mémorisées par utilisateur, et qui suivent d'un onglet à l'autre.
4. **Répondre avant de montrer** : un onglet commence par la réponse (indicateur, écart, explication), le détail vient ensuite.
5. **Descendre sans changer d'outil** : de la vue d'ensemble à la liste des ventes, la période et les filtres suivent.
6. **Tout ce qu'on voit s'exporte**, et les exports de données ont leur propre menu.
7. **Lire des agrégats, jamais `sales` en direct** pour une période longue.
8. **Droits** : chaque onglet et chaque export est un `nav_item` (ENFORCE) ; la marge, les achats et la performance par vendeur ne sont
   pas visibles de tous.

---

## 5. Page « Pilotage de l'officine »

Une seule entrée de menu, une seule page :

```
┌ Pilotage de l'officine ───────────────────────────────────────────────────────────────────────┐
│ Période [Mois en cours ▾]  Comparer à [Même période N-1 ▾] ☑ à date ☐ jours ouvrés            │
│ Granularité [Mois ▾]  Filtres [Magasin] [Type de vente] [Famille] …     [Exporter ▾] [Enregistrer]│
│ Octobre 2026 au 9 (01/10 → 09/10) comparé à octobre 2025 au 9                                   │
├───────────────────────────────────────────────────────────────────────────────────────────────┤
│ Tableau de bord │ Analyser │ Comparer les années │ Rentabilité & remises │ Achats & stock │      │
│ Trésorerie & tiers payant │ Clients & équipe │ Objectifs                                        │
├───────────────────────────────────────────────────────────────────────────────────────────────┤
│                         contenu de l'onglet actif                                             │
└───────────────────────────────────────────────────────────────────────────────────────────────┘
```

- **La barre de période est au-dessus des onglets** et vaut pour tous : changer d'onglet ne change ni la période, ni la comparaison,
  ni les filtres.
- Onglets en `app-nav-tabs` (`ngbNav` chez l'appelant, `[nav]` pour l'outlet) ; chaque onglet est un composant chargé **à
  l'activation** — un onglet jamais ouvert ne coûte aucune requête.
- L'onglet actif, la période et les filtres sont dans l'URL (`/pilotage?onglet=remises&du=…&cmp=N-1`) : un lien partagé, un retour
  arrière ou une descente depuis un autre onglet retombent au même endroit.
- Un onglet sans droit (`nav_item` de type `SECTION` sous `pilotage`) n'est pas affiché ; la page s'ouvre sur le premier onglet
  autorisé.
- Descendre d'un niveau (famille → produits, mois → jours) reste **dans la page** : le détail s'ouvre dans l'onglet « Analyser » avec
  la ventilation et les filtres positionnés, ou en panneau latéral pour la liste des ventes.

### 5.1 Barre commune

| Contrôle | Valeurs |
|---|---|
| **Période** | Aujourd'hui, Hier, Semaine en cours, Mois en cours, Mois précédent, Trimestre en cours / précédent, Semestre, Année en cours (YTD), Année précédente, 12 mois glissants, Personnalisée |
| **Comparer à** | Même période N-1 (défaut), Période précédente, N-2…N-5, Objectif, Personnalisée, Aucune ; « à date » (coché si la période est en cours) ; « jours ouvrés » |
| **Granularité** | Jour, Semaine, Mois, Trimestre, Année (proposée selon la longueur) |
| **Filtres** | Magasin, type de vente, avec / sans ordonnance, famille, rayon, laboratoire, fournisseur, TVA, vendeur |
| **Actions** | Exporter la vue (Excel, PDF), Enregistrer la vue, Programmer l'envoi |

Une ligne de contexte dit ce qui est comparé : « Octobre 2026 au 9 (01/10 → 09/10) comparé à octobre 2025 au 9 ».

### 5.2 Les onglets

**1. Tableau de bord** — *Comment va l'officine ?* (lecture titulaire, onglet d'ouverture)
- Huit indicateurs : CA, fréquentation, panier moyen, marge et taux, remises et taux, achats, part tiers payant, encaissements ;
  chacun avec sa variation, son objectif s'il existe et la projection de fin de période.
- **Ce qui a bougé** : l'écart de CA et de marge décomposé (fréquentation, panier, articles, prix) et les cinq plus fortes hausses et
  baisses par famille, produit, vendeur, organisme — chacune cliquable.
- **Alertes** du moment (§5.3).
- Courbe du CA sur 13 mois avec la référence superposée et les événements.
- Bouton « Bilan du mois » (PDF).

**2. Analyser** — *Pourquoi ça bouge ?* (lecture experte, un explorateur unique au lieu de dix onglets)
- On choisit un ou plusieurs **indicateurs**, une **ventilation** (famille, rayon, laboratoire, fournisseur, produit, type de vente,
  TVA, vendeur, organisme, heure, jour de semaine) et la barre commune.
- Résultat : courbe + **tableau à variations** (valeur ; dessous ▲▼ %, ou points pour un taux ; part du total) ; tri, descente d'un
  niveau au clic, jusqu'à la liste des ventes.
- **Vues enregistrées** livrées d'office : CA par famille N / N-1, marge par laboratoire, remises par vendeur, fréquentation par heure
  et jour, ventes par TVA, panier ordonnance / hors ordonnance… L'utilisateur enregistre les siennes.

**3. Comparer les années** — *Où en est-on par rapport aux années passées ?*
- Courbes N, N-1…N-5 superposées (mois en abscisse), en mensuel ou en cumul depuis janvier.
- Tableaux croisés années × mois et années × famille ; croissance annuelle moyenne ; saisonnalité.

**4. Rentabilité & remises** — *Où est-ce que je gagne ou perds de l'argent ?*
- Marge par famille, laboratoire, produit ; produits à faible marge ; évolution du taux.
- **Remises** : montant, taux sur le CA, ventes remisées, remise moyenne, par vendeur, famille, produit, type de vente, client ou
  organisme ; **remise rapportée à la marge** (ce qu'elle coûte réellement) ; évolution N / N-1.
- Démarque : périmés, casse, écarts d'inventaire.

**5. Achats & stock** — *Mes achats et mon stock sont-ils maîtrisés ?*
- Achats vs ventes (courbes superposées, ratio), par fournisseur et famille ; délais et taux de service ; RFA.
- Valeur du stock dans le temps (photographies mensuelles), rotation en jours, couverture, dormants, ruptures.

**6. Trésorerie & tiers payant** — *Mon argent rentre-t-il ?*
- Encaissements par mode, écarts de caisse ; part tiers payant vs comptant dans le temps ; créances par organisme, délai de
  paiement, vieillissement ; différés ; encaissements attendus.

**7. Clients & équipe** — *Qui vient, qui vend ?*
- Fréquentation par heure × jour (dimensionner les équipes), clients actifs / nouveaux / perdus, segments.
- Par vendeur : CA, ventes, panier, remises accordées, annulations et avoirs.

**8. Objectifs**
- Saisie d'objectifs mensuels (CA, marge, taux de remise maximal, par famille si besoin), éventuellement proposés d'après N-1 + x %.
- Suivi : réalisé à date, écart, projection.

Les 29 rapports actuels restent accessibles comme **analyses détaillées** : chaque onglet y renvoie avec la période en paramètre. Le
menu « Rapports & Statistiques » devient : **Pilotage de l'officine** (la page ci-dessus), **Analyses détaillées** (l'existant),
**Exports**.

### 5.3 Alertes

Calculées après chaque rafraîchissement des agrégats ; affichées sur le tableau de bord, envoyables par courriel ou sur le mobile.

| Alerte | Exemple de règle (seuil paramétrable) |
|---|---|
| Chute d'activité | CA à date < 90 % du même jour N-1 sur 7 jours |
| Famille en recul | Famille dont le CA baisse de plus de 15 % sur le mois, à date |
| Marge qui s'érode | Taux de marge du mois en baisse de plus d'1 point |
| Remises anormales | Taux de remise d'un vendeur > 2 × la moyenne de l'équipe |
| Objectif menacé | Projection de fin de mois < 95 % de l'objectif |
| Achats qui dérapent | Ratio ventes / achats < 0,8 sur 30 jours |
| Créances | Organisme dont l'encours dépasse N jours de chiffre |
| Écarts de caisse | Écart cumulé d'un caissier au-delà du seuil |

### 5.4 Dictionnaire d'indicateurs (extrait)

| Code | Libellé | Formule | Unité | Sens favorable | Statut |
|---|---|---|---|---|---|
| `ca_ttc` | Chiffre d'affaires TTC | Σ montant des ventes clôturées, non annulées | F | ↑ | à unifier |
| `ca_ht` | CA HT | Σ montant HT | F | ↑ | données présentes (`htAmount`) |
| `ca_net` | CA net de remises | `ca_ttc − remises` | F | ↑ | à unifier |
| `nb_ventes` | Nombre de ventes | Σ ventes | — | ↑ | existe |
| `frequentation` | Fréquentation | nb ventes par jour (ouvré) | — | ↑ | nouveau |
| `panier_moyen` | Panier moyen | `ca_ttc / nb_ventes` | F | ↑ | existe |
| `articles_par_vente` | Articles par vente | Σ quantités / nb ventes | — | ↑ | nouveau |
| `prix_moyen_article` | Prix moyen d'un article | `ca_ttc / Σ quantités` | F | — | nouveau |
| `marge_brute` | Marge brute | `ca_ht − coût d'achat HT des quantités vendues` | F | ↑ | à unifier (§8) |
| `taux_marge` | Taux de marge | `marge_brute / ca_ht` | % (variation en points) | ↑ | à unifier |
| `remises` | Remises accordées | Σ remises (ligne + vente) | F | ↓ | existe |
| `taux_remise` | Taux de remise | `remises / ca_ttc` | % | ↓ | nouveau |
| `remise_sur_marge` | Poids des remises dans la marge | `remises / (marge_brute + remises)` | % | ↓ | nouveau |
| `achats_ttc` | Achats TTC | Σ réceptions (bons de livraison) | F | — | nouveau dans le pilotage |
| `ratio_ventes_achats` | Ratio ventes / achats | `ca_ttc / achats_ttc` | — | ≈ 1 | nouveau |
| `part_tp` | Part tiers payant | part TP / `ca_ttc` | % | — | existe en partie |
| `rotation_stock` | Rotation du stock | `stock moyen / coût des ventes × jours` | jours | ↓ | nouveau (photographies) |
| `dso_tp` | Délai de paiement TP | créances TP / CA TP × jours | jours | ↓ | existe en partie |
| `taux_annulation` | Taux d'annulation | ventes annulées / ventes | % | ↓ | nouveau |

Chaque entrée porte aussi : agrégat source, ventilations autorisées, droit requis. La décomposition des écarts s'appuie sur
`ca = fréquentation × jours × panier` et `panier = articles par vente × prix moyen`.

---

## 6. Menu « Exports »

| Rubrique | Contenu |
|---|---|
| **Données** | Ventes (en-têtes, lignes), remises, encaissements, achats (commandes, réceptions), mouvements de stock, inventaires, tiers payant (factures, règlements), clients, produits et prix — pour une période et des filtres |
| **Comptabilité** | Export comptable existant, journaux, balance (rattacher l'existant) |
| **Déclarations** | Déclaration de CA, TVA, FNE (rattacher l'existant) |
| **Pilotage** | Toute vue du menu Pilotage, telle qu'affichée ; bilan mensuel |
| **Pour un outil de BI** | Un fichier plat « ventes jour × produit » avec toutes les ventilations (Excel TCD, Power BI) |

Fonctionnement :
- **Formats** : XLSX, CSV (UTF-8 avec BOM, séparateur `;`, décimales à la française, pour Excel), PDF pour les états.
- **Gros volumes** : génération en tâche de fond (EasyExcel en flux), notification à la fin, fichier disponible au téléchargement
  pendant N jours.
- **Historique** : qui, quand, quoi, quels filtres, combien de lignes — obligatoire dès qu'un export contient des données patients.
- **Modèles enregistrés**, rejouables en un clic et **programmables** (réutiliser `ScheduledReport`).
- **Droits** : un `nav_item` par rubrique ; les exports nominatifs ont leur propre droit.

---

## 7. Architecture

### 7.1 Données

| Élément | Contenu | Remarque |
|---|---|---|
| **Calendrier** (`dim_date`) | jour, semaine ISO, mois, trimestre, année, jour ouvré, férié, jour équivalent N-1 | Rempli par migration ; jours fériés ivoiriens à paramétrer |
| **Agrégat ventes (lignes)** | jour × produit × type de vente × magasin × vendeur : quantités, CA TTC, CA HT, remises, coût, nb ventes, nb lignes | Grain fin : famille, rayon, laboratoire, TVA s'en déduisent par jointure |
| **Agrégat ventes (entêtes)** | jour × heure × type de vente × magasin : nb ventes, CA, clients distincts, part TP, annulations, avec / sans ordonnance | Fréquentation, carte de chaleur |
| **Agrégat achats** | jour × produit × fournisseur : quantités, montants HT / TTC, nb bons | Ratio ventes / achats |
| **Agrégat caisse** | jour × mode de paiement × caissier : encaissements, écarts | Trésorerie |
| **Photographie du stock** | fin de mois × produit × magasin : quantité, valeur | Seule façon d'avoir du stock N-1 ; à démarrer tôt, l'historique ne se rattrape pas |
| **Objectifs, événements, alertes, vues enregistrées, historique des exports** | tables de paramétrage et de suivi | Entités JPA classiques |

Ordre de grandeur : quelques centaines de produits vendus par jour, soit quelques centaines de milliers de lignes par an pour
l'agrégat ventes — PostgreSQL les lit en millisecondes avec les bons index.

**Maintenance** : plutôt qu'une vue matérialisée recalculée en entier (coût croissant avec l'historique), une **table d'agrégats**
mise à jour pour les jours touchés (jour courant toutes les 15 min, J-7 la nuit pour les corrections) et une reconstruction d'une période
à la demande, réservée à l'administrateur (après une reprise de données ou une correction). Le SQL de mise à jour vit dans une
migration Flyway ; la lecture passe par des entités `@Immutable` et du JPQL, comme les vues actuelles — conformément aux conventions du
projet (DTO dans `service/dto/`, pas de SQL natif dans les services).

### 7.2 API

Un point d'entrée générique plutôt qu'un endpoint par écran :

```
GET /api/pilotage/series
    ?indicateurs=ca_ttc,marge_brute,taux_marge
    &du=2026-10-01&au=2026-10-09
    &comparaison=MEME_PERIODE_N_1   (AUCUNE | PERIODE_PRECEDENTE | N_MOINS_K | OBJECTIF | PERSONNALISEE)
    &aDate=true&joursOuvres=false
    &granularite=MOIS
    &ventilation=famille            (optionnel)
    &famille=…&typeVente=…&magasin=…
→ { periode, reference, series: [{ cle, valeur, valeurReference, variation, variationPct, partDuTotal }] }

GET /api/pilotage/ecarts       → décomposition de l'écart et principaux contributeurs
GET /api/pilotage/projection   → projection de fin de période
GET /api/pilotage/alertes      → alertes actives
```

Le calcul des périodes de référence (à date, jours ouvrés, N-k) est fait **une fois**, côté serveur, et testé. Les onglets, les exports,
les rapports programmés, les widgets du tableau de bord personnalisable et l'application mobile appellent la même API.

### 7.3 Front

- `features/pilotage/` : **une route**, un composant page (barre de période + `app-nav-tabs`) et un composant par onglet.
- État partagé dans un store de la page (signaux) : période, comparaison, granularité, filtres ; synchronisé avec l'URL ; chaque onglet
  le lit et recharge ses données quand il change.
- Composants communs : barre de période (`app-periode-comparaison`), tuiles (`app-kpi-strip`), courbes (`app-chart`), tableau à
  variations (export intégré), panneau « Ce qui a bougé ».
- L'onglet « Analyser » et les onglets thématiques sont construits avec les mêmes composants : un onglet thématique n'est qu'un
  assemblage de vues enregistrées.

---

## 8. Décisions

### 8.1 Prises le 2026-10-09

| Sujet | Décision |
|---|---|
| Ventes `imported = true` | Ventes d'un sous-magasin physique (dépôt) importées : **comptées** dans le CA et l'historique |
| Marge | Sur la **quantité demandée** |
| Année de référence | **Année civile** (droit comptable OHADA : l'exercice coïncide avec l'année civile) |
| Achats | Datés à la **réception** |
| Stock dormant | Seuil dans `app_configuration`, **90 jours** par défaut |
| Ruptures | `rupture` = ruptures fournisseurs (commandes) ; `avoir_client` = ruptures au comptoir (ventes manquées) |
| Remises | Une seule remise au comptoir (`RemiseProduit`), accordée par privilège ou après autorisation tracée dans `utilisation_cle_securite` |
| Encaissements attendus | Délai observé, sinon `delai_reglement` du groupe, sinon délai par défaut (`getDelaiReglement()`) |
| Arrondis de caisse | Affichés **à part**, hors écart |
| Ventes au dépôt (`CA_DEPOT`) | Sorties de l'officine vers le dépôt : **hors CA**, toujours **ventilées à part** (jamais additionnées au CA). Appliqué le 2026-10-09 à la « Synthèse journalière » et au tableau de bord CA |
| Client nominatif | Ventilation retenue, soumise à autorisation comme les onglets |
| Croisé | Deux ventilations |

Détail dans [PLAN-PILOTAGE-ONGLETS.md](PLAN-PILOTAGE-ONGLETS.md).

### 8.2 Encore ouvertes

1. **Référence du CA** : TTC ou HT par défaut ? Remises déduites ou non ? (proposition : CA TTC affiché, HT disponible, net de remises
   en indicateur séparé)
2. **Jours ouvrés** : quels jours (dimanche, gardes) et quels fériés ?
3. **Profondeur** : N-5 suffit-il ?
4. **Multi-magasin** : consolidation et comparaison entre magasins dès la première version ?
5. **Droits** : qui voit la marge, les achats, la performance par vendeur ?
6. **Fraîcheur** : 15 minutes de décalage acceptables pour la journée en cours ?
7. **Alertes** : lesquelles activer par défaut, et par quel canal (écran, courriel, mobile) ?
8. **Benchmark** entre officines (groupement) : à garder pour plus tard ?

---

## 9. Plan de réalisation

| Phase | Contenu | Effort |
|---|---|---|
| **0 — Définitions** | Décisions du §8 ; dictionnaire d'indicateurs ; test qui confronte les chiffres des écrans actuels (CA du mois selon chaque vue) et documente les écarts | 2 j |
| **1 — Socle données** | Calendrier, agrégats ventes (lignes et entêtes), achats, caisse ; mise à jour incrémentale et reconstruction ; photographie mensuelle du stock (à lancer dès cette phase) ; tests d'intégration sur base jetable | 5 j |
| **2 — Page et Tableau de bord** | Service de périodes et comparaisons (à date, jours ouvrés, N-k) avec tests ; `/api/pilotage/series` et `/ecarts` ; page unique (barre de période, `app-nav-tabs`, état dans l'URL, droits par onglet) ; onglet Tableau de bord avec « Ce qui a bougé » ; export de la vue | 6 j |
| **3 — Analyser** | Explorateur, tableau à variations, descente jusqu'aux ventes, vues enregistrées livrées d'office | 5 j |
| **4 — Comparer les années, Rentabilité & remises** | N à N-5, tableaux croisés, cumul ; marge, remises, démarque | 4 j |
| **5 — Exports** | Catalogue, XLSX / CSV en tâche de fond, historique, modèles, programmation ; rattachement des exports existants | 4 j |
| **6 — Achats & stock, Trésorerie & TP, Clients & équipe** | Onglets thématiques (vues enregistrées + rapports existants) | 4 j |
| **7 — Objectifs, projection, alertes, bilan mensuel** | Saisie et suivi des objectifs ; projection ; moteur d'alertes ; PDF mensuel programmé ; événements sur les courbes | 5 j |
| **8 — Suite** | Stock comparé (quand les photographies ont un an), application mobile sur la nouvelle API, réalignement des vues matérialisées actuelles sur le dictionnaire (sauf `mv_daily_sales_summary`, laissée telle quelle), widgets du tableau de bord ; simulation de marge à l'achat ; benchmark groupement en option | à chiffrer |

Première version présentable aux pharmaciens : fin de la phase 2 (≈ 13 jours) — un tableau de bord juste, comparé à date, qui explique
ses variations.

### État au 2026-10-09 — phase 0 réalisée, page posée en avance de phase 2

| Livré | Où |
|---|---|
| Entrée de premier niveau « Pilotage » (position 1, après les actions de vente), ses 8 onglets en `SECTION`, droits `ROLE_ADMIN` et `ROLE_PHARMACIEN` (affichage, accès, export) | `V2.1.32__nav_pilotage.sql` |
| Dictionnaire d'indicateurs (21 indicateurs : libellé, définition, unité, sens favorable, droit) | `IndicateurPilotage`, `UniteIndicateur`, `SensFavorable`, `DroitPilotage` (`domain/enumeration`) |
| Définition de référence du CA de l'officine, une seule fois, en JPQL | `PilotageVenteRepository.calculerChiffreAffairesOfficine` |
| Services (interface + implémentation) : dictionnaire filtré par droits, CA de référence | `service/pilotage/` |
| API `GET /api/pilotage/indicateurs`, `GET /api/pilotage/chiffre-affaires?du&au` (`@RequiresNavAccess("pilotage")`) | `web/rest/pilotage/PilotageResource` |
| Confrontation des sources (test d'intégration) : `mv_dashboard_ca_daily` = référence ; `mv_daily_sales_summary` = référence − ventes importées du dépôt (laissée telle quelle) ; comparatif N / N-1 = CA **net** de remises | `DefinitionDuChiffreAffairesIntegrationTest` |
| Page `/pilotage` : barre de titre, onglets `app-nav-tabs` filtrés par droit, onglet actif dans l'URL (`?onglet=`), liste d'onglets nommée ; chaque onglet affiche sa question et sa phase | `features/pilotage/` |
| Focus clavier visible des onglets (WCAG 2.4.7), à la couleur du thème (`--pharma-chrome-tab`), pour tous les écrans à onglets | `pharma-nav-tabs-global.scss` |

Tests : 4 (intégration) + 3 (dictionnaire) + 7 (page) ; `NavAccessCompletenessTest` vert.

Décisions encore ouvertes appliquées **par défaut**, à confirmer : CA affiché TTC (HT et net de remises en indicateurs séparés) ;
droits limités à l'administrateur et au pharmacien. La migration s'appliquera au prochain démarrage du backend.

### État au 2026-10-09 — phase 1 réalisée

| Livré | Où |
|---|---|
| Agrégats journaliers : lignes de vente (jour × produit × magasin × vendeur × nature × prescription × catégorie), en-têtes (+ heure, caissier, annulée), achats (datés à la réception), encaissements (type × mode × caissier) ; photographie mensuelle du stock | `V2.1.34__pilotage_agregats.sql`, entités `Pilotage*` (lecture seule) |
| Recalcul par fonctions PL/pgSQL (`pilotage_recalculer`, `pilotage_photographier_stock`), sérialisées par verrou consultatif ; chargement initial de tout l'historique dans la migration | idem |
| Jour ouvré = jour avec ventes au CA de l'officine (décision du 2026-10-09 : ni calendrier ni jours fériés à saisir) ; seuil du stock dormant (`APP_PILOTAGE_SEUIL_STOCK_DORMANT`, 90 j) | `PilotageVenteJourRepository.compterJoursOuvres`, `CalendrierPilotageService` |
| Tenue à jour : toutes les 15 min, recalcul des jours modifiés dans les 30 dernières minutes + photographie du stock du mois ; au démarrage, rattrapage sur 7 jours (les postes sont éteints la nuit : pas de tâche nocturne) | `service/scheduler/AgregatsPilotageScheduler` (`pharma-smart.pilotage.actualisation-cron`) |
| Reconstruction d'une période par l'administrateur | `POST /api/pilotage/agregats/recalcul?du&au` (droit d'exécution) |
| Lecture du CA de référence sur l'agrégat | `PilotageVenteJourRepository.calculerChiffreAffairesOfficine` |

Vérifié sur la base locale (janvier-septembre 2026) : en-têtes = référence au franc près ; lignes = même CA et mêmes remises ; marge
TTC des lignes = `mv_dashboard_ca_daily` ; stock = `mv_stock_valuation`. Constat : le prix d'achat est **TTC** (taxe comprise),
`order_line.tax_amount` porte la taxe incluse — les agrégats stockent coûts et achats en TTC et en HT.

Tests : 9 (intégration, agrégats et jour ouvré) ; `NavAccessCompletenessTest` vert. Services : interfaces dans `service/pilotage`,
implémentations dans `service/pilotage/impl`.

### État au 2026-10-09 — stock et valorisation à une date T (`V2.1.33__stock_a_date.sql`)

Les fonctions de V1.2.6 (`fn_stock_at_time`, `fn_stock_valuation_at_time`, `fn_stock_valuation_bulk`, `fn_stock_bilan_periode`)
sont corrigées sans changer de nom ; un socle unique, `fn_stock_quantites_at_time(T, magasin, produits)`, les alimente.

| Défaut | Correction |
|---|---|
| Sans photo antérieure à T, le stock partait de 0 | Remontée depuis la photo suivante ou le stock courant, en retranchant les mouvements postérieurs à T |
| Mouvements `INVENTAIRE` exclus | Comptés comme les autres : datés au comptage, ils précèdent la photo de clôture et ne sont jamais doublés |
| Négatif ramené à 0 | Conservé (règle de gestion) |
| UG absentes | La photo quotidienne porte `qty_ug` ; valorisation = (stock + UG) × prix |
| Photo quotidienne datée de minuit mais prise à 9 h : mouvements du matin comptés deux fois | Datée à l'instant de la prise ; une seule par jour (`StockSnapshotServiceImpl`) |
| `created_at` (UTC, sans fuseau) comparé dans le fuseau de la session | Lu explicitement en UTC |
| Le bilan ne retombait pas sur la fin (écarts d'inventaire absents) | Colonne `inventaires` ajoutée |

La photographie mensuelle du pilotage s'appuie sur `fn_stock_valuation_bulk` : les **38 mois** depuis le premier mouvement
(septembre 2023) sont reconstitués à la migration — le stock N-1 est disponible tout de suite.

Vérifié sur la base locale : stock à maintenant = stock courant (1 613 / 1 613) ; stock à la photo du 8 octobre = photo
(1 613 / 1 613) ; `v_stock_ecart_journal` sans écart ; bilan 2026 sans désaccord (1 101 lignes) ; valeur de vente à maintenant =
`mv_stock_valuation` (valeur d'achat : 21 815 F d'écart, le prix retenu étant celui du dernier mouvement et non le prix courant du
fournisseur principal). UG avant la première photo : celles de la première photo connue, à défaut les UG courantes (non journalisées).
Tests : `StockADateIntegrationTest` (5).

### État au 2026-10-09 — phase 2 réalisée

| Livré | Où |
|---|---|
| Période de référence : même période N-1, période précédente, année N-k (k ≤ 5), aucune ; « à date » tronque une période en cours à aujourd'hui et la référence d'autant ; décalage par mois entiers si la période est alignée sur des mois, sinon par sa durée | `PeriodeComparaisonService` |
| `GET /api/pilotage/series` : valeur, référence, écart et écart % par indicateur, et par tranche (jour → année) ; un taux varie en points, jamais en % ; indicateurs filtrés par les droits | `SeriesPilotageService`, `CalculateurIndicateurs`, `DecoupagePeriode` |
| `GET /api/pilotage/ecarts` : écart de CA décomposé en effets fréquentation, articles par vente et prix moyen (somme = écart) ; plus fortes hausses et baisses par famille, produit, vendeur, type de vente. Les vendeurs exigent le droit « Clients & équipe » | `EcartsPilotageService`, `VentilationPilotage.droit` |
| Barre de période (périodes prédéfinies ou libres, comparaison, à date, découpage), dans la barre de titre ; tout l'état dans l'URL — une période prédéfinie se recalcule à l'ouverture, seule une période libre garde ses dates | `ui/barre-periode`, `pilotage-page` |
| Onglet Tableau de bord : 8 tuiles avec variation, courbe du CA TTC et de sa référence, « Ce qui a bougé », tableau par tranche avec variations | `feature/onglet-tableau-de-bord` |
| Variation lisible sans la couleur (flèche + « en hausse de » pour les lecteurs d'écran, WCAG 1.4.1) ; couleur selon le sens favorable (hausse des remises = défavorable), jetons du thème | `ui/variation` |
| Export de la vue : tableau en CSV (valeurs non abrégées, virgule décimale, UTF-8 avec BOM), réservé au droit d'export de l'onglet | `OngletTableauDeBordComponent.exporter` |

Tests back : 9 (périodes) + 5 (calculs) + 3 (écarts, dont le droit sur les vendeurs) + 3 (dictionnaire) + 11 (agrégats et
mesures) ; front : 21 (page, barre de période, variation, onglet).

Reste à vérifier à l'écran sur la base locale, backend redémarré (migrations V2.1.32 à V2.1.34 appliquées au démarrage).

### État au 2026-10-09 — phase 3 (Analyser) réalisée

| Livré | Où |
|---|---|
| 16 axes : famille, produit, laboratoire, fournisseur principal, forme, gamme, DCI, TVA (lignes) ; type de vente, ordonnance / conseil, magasin, vendeur (lignes et en-têtes) ; caissier, heure (en-têtes) ; jour de semaine et période (déduits du jour). Chaque axe porte son droit (vendeur, caissier : « Clients & équipe ») | `AxeAnalyse`, `SourceAnalyse` |
| Une seule requête de ventilation, construite selon les axes (Criteria, jointures partagées entre axe et filtre) ; les quatre ventilations figées de la phase 2 sont retirées, « Ce qui a bougé » passe par elle | `PilotageAnalyseRepository` (+ `Custom`, `CustomImpl`), `VentilateurPilotage` |
| Choix de l'agrégat : celui qui connaît tous les axes et calcule le plus d'indicateurs demandés ; les autres sont signalés (« non ventilable par famille : nombre de ventes »), jamais calculés faux | `SourceAnalyse.choisir` |
| `GET /api/pilotage/analyse` : 1 à 3 indicateurs, top N + « autres », tri (valeur, plus fortes hausses, plus fortes baisses), part du total et contribution à l'écart (indicateur additif), croisé sur le premier indicateur (12 colonnes au plus, sauf axe ordonné) | `AnalysePilotageServiceImpl` |
| `GET /api/pilotage/analyse/explication` : pour chaque axe encore libre, part de l'écart concentrée sur ses 3 principaux éléments ; le plus concentré est proposé | `ExplicateurEcart` |
| Vues enregistrées : 9 livrées d'office, personnelles, partagées avec l'équipe ; seul l'auteur modifie ou supprime | `V2.1.35__pilotage_vues.sql`, `PilotageVue`, `VuesPilotageService` |
| Onglet : réglage dans l'URL (indicateurs, axes, top, tri, affichage, chemin), fil d'Ariane, barres (référence en fantôme), courbes des éléments dans le temps, tableau à variations, croisé, panneau « Expliquer l'écart », ventes d'un produit (historique produit existant), export CSV | `feature/onglet-analyser`, `ui/*` |
| Couleurs des graphiques relues à chaque changement de thème ; palette de séries contrastée (≥ 4,5:1), la première à l'accent du thème ; courbes distinguées aussi par la forme des points | `ChartThemeColorsService`, `--pharma-chart-serie-*` |

Écarts au plan d'onglets (§2.2), faute de donnée dans les agrégats : **rayon** (un produit peut être rangé dans plusieurs rayons),
**poste**, **organisme** et **client nominatif** (absents des agrégats : une colonne à ajouter, migration à part) ; la vue
« Génériques » devient « CA par DCI » (aucun indicateur de part générique). Ajouté au dictionnaire : `QUANTITES_VENDUES`.

Tests back : 5 (analyse) + 3 (explication) + 3 (ventilateur) + 3 (vues) + 3 (écarts du tableau de bord) ; intégration : chaque
axe lu sur chaque agrégat qui le connaît redonne le total, filtre de descente, en-têtes par heure, vues livrées (14 au total).
Front : 35 (dont réglage d'URL, tableau à variations, onglet). Migration V2.1.35 appliquée au prochain démarrage du backend.

### État au 2026-10-09 — phase 4 (Comparer les années, Rentabilité & remises) réalisée

**Comparer les années** (`GET /api/pilotage/annees`, droit de l'onglet)

| Livré | Où |
|---|---|
| Années civiles (OHADA), 2 à 6, l'année en cours arrêtée à aujourd'hui et comparée à la même date de l'année précédente ; une seule lecture mensuelle couvre toutes les années | `ComparaisonAnneesServiceImpl` |
| Mois seul, cumul depuis janvier, 12 mois glissants : sommes préfixées, une fenêtre = une soustraction | `CalendrierMensuel` |
| Par jour ouvré (indicateurs additifs seulement), filtre famille ou type de vente (les jours ouvrés restent ceux de l'officine) | `LecteurFiltrePilotage` |
| Courbes superposées (année en cours en trait plein, à l'accent), trimestres, saisonnalité moyenne des années closes, croissance annuelle moyenne, mois le plus élevé, synthèse annuelle (marge masquée sans le droit Rentabilité), familles × années et types de vente × années (« à date » : chaque année au même jour) | `CroiseurAnnees`, `feature/onglet-comparer-annees` |

**Rentabilité & remises** (`/api/pilotage/rentabilite/marge|remises|demarque`, droit de l'onglet)

| Livré | Où |
|---|---|
| Chaque vente porte son taux de remise (arrondi, % du brut), son mode d'octroi (privilège / autorisée par clé de sécurité, lu dans `utilisation_cle_securite`) et l'autorisant ; tout l'historique recalculé. Les tranches se calculent à la lecture : changer les seuils ne demande aucun recalcul | `V2.1.36__pilotage_remises.sql` (`pilotage_remises_ventes`), `TranchesRemise` |
| Paramètres : seuils des tranches (5, 10, 20 %), seuil de faible marge (15 %), multiple d'alerte vendeur (2) | `app_configuration`, `AppConfigurationService` |
| Axes ajoutés à l'onglet Analyser : tranche, octroi, autorisant (« Clients & équipe »), code remise | `AxeAnalyse` |
| Marge : tuiles (marge, taux, coefficient moyen, marge par vente — ajoutés au dictionnaire), taux dans le temps, marge par famille / laboratoire / produit… avec effet volume + effet taux (exacts), effet mix + effet taux du taux global, faibles marges, ventes à marge négative | `MargePilotageServiceImpl`, `PilotageVentesDetailRepository` |
| Remises : tuiles (remises, taux, part des ventes remisées, remise moyenne, poids dans la marge), courbe, octroi, tranches (marge par tranche), vendeurs signalés au-delà du multiple, plus fortes remises avec l'autorisant ; autres ventilations par l'onglet Analyser | `RemisesPilotageServiceImpl` |
| Démarque : ajustements de sortie par motif face à la référence, part du CA, produits les plus touchés (même définition que le rapport de démarque, en JPQL) | `DemarquePilotageServiceImpl`, `PilotageDemarqueRepository` |
| Noms (vendeur, autorisant) masqués sans le droit « Clients & équipe » | services ci-dessus |

Écarts : les **écarts d'inventaire** ne sont pas dans la démarque (elle reprend les ajustements de sortie, comme le rapport) ; la
démarque est valorisée au prix d'achat **actuel**. Courbe de saisonnalité seule (sans projection : phase 7).

Tests : 90 back (dont 16 d'intégration : migration V2.1.36, requêtes JPQL, tranches et octroi sur une vraie base) ; 44 front.
Migrations V2.1.35 et V2.1.36 appliquées au prochain démarrage du backend.

### État au 2026-10-10 — phase 5 (menu Exports) réalisée

| Livré | Où |
|---|---|
| Entrée de premier niveau « Exports » (après le pilotage), onglets Catalogue, Historique, Modèles ; un droit par rubrique : données, **nominatif** (clients — administrateur seul par défaut), BI | `V2.1.37__exports.sql`, `features/exports` |
| Catalogue de 8 exports de données : ventes, lignes de vente, encaissements, achats reçus, mouvements de stock, produits et prix, clients (nominatif), « ventes jour × produit » pour un outil de BI (toutes les ventilations du pilotage, marge comprise) | `ExportDonnees`, `ExportDonneesRepository` (JPQL en flux) |
| Exports existants rattachés, montrés à qui peut ouvrir leur écran : comptabilité, export comptable, déclaration de TVA, retraitement du CA, onglets du pilotage | `LienExport` |
| Génération en tâche de fond (lecture par lots de 500, écriture ligne à ligne) : CSV (UTF-8 avec BOM, « ; », virgule décimale) ou XLSX en flux (POI SXSSF, nouvelle feuille au-delà d'un million de lignes) ; un échec garde son message, sans fichier partiel | `GenerateurExports`, `EcrivainCsv`, `EcrivainXlsx` |
| Historique : qui, quand, quoi, période, nombre de lignes, taille ; chacun voit les siens, l'administrateur tout ; téléchargement par le demandeur (ou l'administrateur) tant que le droit tient et que le fichier n'a pas expiré (`APP_EXPORT_RETENTION_JOURS`, 7 j) ; la trace survit au fichier | `ExportsFichiersService` |
| Modèles : période relative recalculée à chaque exécution (mois précédent…), partage avec l'équipe, rejoués en un clic, programmés (chaque jour, semaine ou mois, à l'heure dite) ; postes éteints la nuit → un modèle dû part au premier passage après le démarrage | `ModelesExportService`, `ProgrammationExport`, `ExportsScheduler` |

Écart au §6 : la programmation ne passe **pas** par `ScheduledReport`. Ce mécanisme envoie des courriels dont les pièces jointes ne
sont pas implémentées (code en attente) ; un export programmé rejoint plutôt l'historique de l'auteur du modèle. L'envoi par
courriel viendra avec les pièces jointes du `MailService`. « Bilan mensuel » (rubrique Pilotage) : phase 7.

### État au 2026-10-10 — phase 6 (Achats & stock, Trésorerie & tiers payant, Clients & équipe) réalisée

| Onglet | Livré | Où |
|---|---|---|
| Achats & stock | Achats (datés à la réception) : TTC, HT, bons, délai de livraison pondéré par les bons, conformité des quantités ; par fournisseur et par famille, comparés à la référence | `AchatsPilotageService`, `PilotageAchatsStockRepository` |
| | Achats / ventes : ratio sur la période et sur 12 mois glissants, achats et coût des ventes HT par tranche, par famille | idem |
| | Stock : photographie de fin de mois face à N-1 (courbe sur 12 mois), rotation et couverture sur le coût TTC des ventes de 12 mois, dormants (`APP_PILOTAGE_SEUIL_STOCK_DORMANT`), par famille | `StockPilotageService` |
| | Ruptures fournisseurs (table `rupture`) et ventes manquées au comptoir (`avoir_client`, quantités en avoir) **séparées** ; périmé sur la période, à périmer sous 3 mois, péremptions par mois | `PilotageRupturesRepository` |
| Trésorerie & TP | Encaissements par mode (comparés, empilés par tranche), par caissier avec ses sessions (droit « Clients & équipe ») | `TresoreriePilotageService`, `PilotageTresorerieRepository` |
| | Tiers payant, mêmes définitions que le vieillissement des créances : facturé et réglé sur la période ; encours, DSO, vieillissement, concentration (3 / 5) à date ; échéancier avec le délai observé (≥ 3 factures réglées), sinon celui du groupe, sinon celui de l'officine — l'infobulle de chaque organisme dit lequel | `CreancesTiersPayant`, `OrganismeCreances` |
| | Différés : reste dû à date par ancienneté et par client ; avoirs clients émis et remboursés sur la période | idem |
| Clients & équipe | Fréquentation : carte de chaleur heure × jour (l'analyse), créneaux les plus chargés, ventes par jour | `features/pilotage/ui/carte-chaleur` |
| | Clients identifiés : actifs, nouveaux, revenus, perdus, part du CA avec un client identifié, CA moyen, clients en baisse | `ClientsEquipePilotageService`, `PilotageClientsRepository` |
| | Équipe : par vendeur, ventes, CA, panier, articles par vente, taux de marge et de remise, annulations, avoirs, part ordonnance, face à la référence, et la ligne de l'équipe ; CA par vendeur et par heure | idem |

Libellés et exports (demande du 2026-10-10) : le serveur nomme les tranches selon le découpage (« Oct. 2026 », « T4 2026 »,
« Sem. du 05/10 »), une tranche rognée le dit (« Oct. 2026 (au 10) ») — `LibellesPilotage.libellerTranche`. Les trois exports CSV
(détail par période, Analyser, Comparer les années) sont générés côté serveur par `ExportPilotageService` sur le
`CsvExportService` générique, sous le droit « export » de l'onglet (`/api/pilotage/{series,analyse,annees}/export`).

Organisation du code (demande du 2026-10-10) : `service/pilotage/impl` ne contient que les implémentations de service et leurs
composants ; records, accumulateurs et calculs statiques vivent dans `service/pilotage/calcul` (et `service/exports/modele`).

Non livré, à décider : **écart de caisse et arrondis** — `TicketingServiceImpl` recopie le montant compté dans
`cash_register.final_amount`, si bien que l'écart actuel (compté − `final_amount`) vaut toujours 0 ; une autre définition est une
règle de gestion à valider. Également écartés pour l'instant : RFA fournisseurs (non enregistrée), soldes de début et de fin par
organisme (exigent des règlements datés par facture), segmentation RFM (reste dans son rapport), classes ABC (rapport existant).

Tests : 122 back sur le pilotage et les exports (dont 5 d'intégration pour cette phase : chaque requête lue sur la base) ; 50 front.

Tests : 11 unitaires (écrivains CSV / XLSX relus, programmation, périodes relatives, droits, modèles) + 8 d'intégration (chaque
export lu sur la base, autant de valeurs par ligne que de colonnes annoncées) ; 6 front. Migration V2.1.37 au prochain démarrage.

---

### État au 2026-10-10 — phase 7, lot 1 (Objectifs) réalisé

| Livré | Où |
|---|---|
| Table `pilotage_objectif` (indicateur × année × mois, auteur et date de la dernière modification) ; droit de modification de l'onglet ouvert au titulaire (`V2.1.38__pilotage_objectifs.sql`) | `ObjectifPilotage`, `ObjectifPilotageRepository` |
| Indicateurs à objectif : CA TTC, marge brute, taux de marge, taux de remise (un **plafond** : tenu tant qu'on reste dessous) ; chacun soumis au droit de l'indicateur | `ObjectifsPilotageService` |
| Proposition d'après le même mois N-1 : × (1 + x %) pour un montant, la valeur N-1 pour un taux | `/api/pilotage/objectifs/proposition` (droit de modification) |
| Suivi mois par mois : objectif, réalisé, écart, atteinte, tenu / manqué ; cumul des mois clos ayant un objectif (montants seulement) | `SuiviObjectifs`, `/api/pilotage/objectifs/suivi` |
| Projection de fin du mois en cours : réalisé à date × profil du même mois N-1 (part du mois faite au même jour) ; sans vente N-1, au rythme des jours écoulés ; un taux garde sa valeur à date. La méthode est dite dans l'infobulle | `SuiviObjectifs.projeter` |
| Onglet : choix de l'année, tuiles de fin de mois, tableau mois par mois, grille de saisie par indicateur | `feature/onglet-objectifs` |

Écarts au plan : l'historique des modifications se limite au dernier auteur et à la dernière date par indicateur (pas de journal) ;
pas d'objectif par famille ni par type de vente.

Objectifs dans le Tableau de bord (2026-10-10) : chaque série porte l'objectif de la période et de chaque tranche — un montant
se somme mois par mois **au prorata des jours** d'un mois entamé, un taux ne vaut que sur un seul mois, un mois sans objectif
laisse la tranche sans objectif (`ObjectifsPeriode`) ; objectifs lus seulement avec le droit de l'onglet Objectifs. Tuiles :
objectif et, quand la période est le mois en cours à date, fin de mois projetée (méthode en infobulle). Courbe : ligne
d'objectif en escalier. Barre : comparaison « Objectifs » (`TypeComparaison.OBJECTIF`), proposée à qui voit l'onglet ; seules
les séries s'y comparent, les autres onglets restent sans référence (la ligne de contexte le dit).

Lots suivants de la phase 7 : moteur d'alertes (§5.3), bilan mensuel PDF programmé, événements sur les courbes.

### État au 2026-10-10 — phase 7, lot 2 (Alertes) réalisé

Calculées **à la demande** (`GET /api/pilotage/alertes`, droit du Tableau de bord) sur les agrégats, eux-mêmes rafraîchis toutes
les 15 minutes : pas de table d'alertes tant qu'aucun envoi (courriel, mobile) ne l'exige. Une règle par alerte
(`service/pilotage/alertes/regles`), évaluée seulement si l'utilisateur a le droit de l'onglet qu'elle concerne ; une règle en
échec est journalisée sans masquer les autres. Seuils dans `app_configuration` (`V2.1.39__pilotage_alertes.sql`).

| Alerte | Règle livrée | Seuil (défaut) | Droit | Mène à |
|---|---|---|---|---|
| Objectif menacé | Projection de fin de mois sous le seuil de l'objectif ; plafond (taux de remise) dépassé | 95 % | Objectifs | Objectifs |
| Chute d'activité | CA des 7 derniers jours sous le seuil des mêmes jours N-1 | 90 % | Tableau de bord | Tableau de bord |
| Marge qui s'érode | Taux de marge du mois à date en baisse de plus du seuil | 1 point | Rentabilité | Rentabilité & remises |
| Famille en recul | CA du mois à date en baisse de plus du seuil ; familles de moins de 2 % du CA N-1 ignorées | 15 % | Tableau de bord | Analyser |
| Remises anormales | Vendeur au-delà du multiple du taux de l'équipe (même signalement que l'onglet Remises) | × 2 | Rentabilité (+ Clients & équipe pour les noms) | Rentabilité & remises |
| Achats qui dérapent | Ventes / achats TTC des 30 derniers jours sous le seuil | 0,8 | Achats & stock | Achats & stock |
| Créances | Organisme dont l'encours dépasse le seuil en jours de chiffre (DSO) | 90 j | Trésorerie | Trésorerie & TP |

Affichage : bandeau repliable en tête du Tableau de bord, au plus cinq alertes, les plus graves d'abord ; la gravité est écrite
(« Grave », « À surveiller »), chaque alerte ouvre l'onglet où creuser. **Écarts de caisse** : non livrée, faute de définition de
l'écart (question ouverte, phase 6). Envoi par courriel ou sur le mobile : question ouverte §8.2 n° 7.

## 10. Jeu de démonstration

Le jeu de démonstration (`scripts/demo-data/`) doit tracer **tous** les mouvements de stock dans `inventory_transaction`, comme
l'application : la valorisation à une date T, le bilan de période et les historiques produit en dépendent.

### 10.1 Constat (base locale, 2026-10-09)

`16_mouvements.sql` ne dérive les mouvements que de quatre sources — réceptions (`ENTREE_STOCK`), ventes (`SALE`), retours de
dépôt (`RETOUR_DEPOT`), retraits de périmés (`RETRAIT_PERIME`) — puis **solde l'écart** avec le stock par un ajustement synthétique
du jour. Ce solde masque les trous suivants :

| Source dans la démo | Lignes | Mouvement attendu (application) | Script qui crée la source | Problème |
|---|---|---|---|---|
| Inventaire clos | 80 | `INVENTAIRE` | `11_inventaires.sql` | Aucun mouvement |
| Retours fournisseur | 86 | `RETOUR_FOURNISSEUR` | `12b_retours_fournisseurs.sql` | Aucun mouvement |
| Ajustements | 75 | `AJUSTEMENT_IN` / `AJUSTEMENT_OUT` | `16b_ajustements.sql` | Exécuté **après** 16 ; seul l'ajustement synthétique existe |
| Répartitions entre stockages | 40 | `MOUVEMENT_STOCK_IN` / `MOUVEMENT_STOCK_OUT` | `18_repartitions_stock.sql` | Exécuté après 16 |
| Retours client | 36 | `RETOUR_CLIENT` | `22_clients_suivi.sql` | Exécuté après 16 |
| Déconditionnements | 34 | `DECONDTION_IN` / `DECONDTION_OUT` | `23_exploitation.sql` | Exécuté après 16 |
| Ventes annulées | 0 | `CANCEL_SALE` | `09_ventes.sql` | Pas de cas dans la démo |
| Destructions de produits retournés | — | `DESTRUCTION` | — | Pas de cas dans la démo |

Conséquences : historique produit incomplet, valorisation à date fausse pour les produits concernés, bilan de période juste
seulement grâce à l'ajustement synthétique.

### 10.2 Correction proposée

1. **Un journal construit en dernier** : `16_mouvements.sql` passe après tous les scripts qui créent une source de mouvement
   (16b, 18, 22, 23), sous un nouveau numéro (ex. `24_mouvements.sql`), et dérive chaque type de mouvement de sa source, avec les
   mêmes conventions que l'application (`InventoryTransactionBuilder`, procédure de clôture d'inventaire) : `quantity_befor` /
   `quantity_after`, `entity_id`, `storage_id`, `created_at` en UTC.
2. **Plus d'ajustement synthétique** : le journal doit retomber sur le stock par construction. Le contrôle existant (« le dernier
   mouvement de chaque produit retombe sur son stock réel ») devient bloquant sans solde de rattrapage ; un écart révèle une source
   oubliée ou une incohérence du script qui la crée.
3. **Photos quotidiennes** : une photo de fin de jeu (stock et UG) pour que la valorisation à date dispose d'une ancre récente.
4. **Cas manquants** : au moins une vente annulée (`CANCEL_SALE`) et une destruction de produit retourné (`DESTRUCTION`).
5. **Contrôles** dans `99_verification.sql` : chaque source (inventaire, retour fournisseur, ajustement, répartition, retour
   client, déconditionnement) a son mouvement ; `v_stock_ecart_journal` sans écart ; `fn_stock_bilan_periode` sans désaccord sur la
   période de la démo.
6. **Validation** sur une base jetable (procédure habituelle : schéma de `pharma_smart_demo`, rechargement complet par
   `run_all.sql`), jamais sur `pharma_smart` directement.

Effort : ≈ 1,5 j.

### 10.3 État au 2026-10-10 — réalisé (sauf la vente annulée)

**Journal** : `16_mouvements.sql` devient `24_mouvements.sql`, joué après tous les scripts sources (et avant `25`, `99`). Il
dérive chaque mouvement de sa pièce, aux conventions de l'application : réceptions (reçu + UG) et ventes dépôt côté dépôt
(`ENTREE_STOCK`), ventes (`SALE`), retours dépôt côté officine (`RETOUR_DEPOT` — le côté dépôt n'est pas tracé, comme dans
l'application), périmés détruits (`RETRAIT_PERIME`), retours fournisseur hors « hors stock » (`RETOUR_FOURNISSEUR`), lignes de bons
d'ajustement **clôturés** (`AJUSTEMENT_IN/OUT`, quantité signée), répartitions (`MOUVEMENT_STOCK_OUT` signé / `_IN`),
déconditionnements (`DECONDTION_IN/OUT`), retours clients (`DESTRUCTION` s'ils ne sont pas remis en stock — c'est le cas des 35
de la démo, tous non conformes —, `RETOUR_CLIENT` sinon), lignes d'inventaire clôturé (`INVENTAIRE`, écart signé). Le journal est
tenu **par ligne de stock (produit × stockage)** : rayon, réserve, dépôt.

**Plus d'ajustement synthétique** (décision de l'utilisateur, 2026-10-10 : « inventaires réels ») : `07_stock.sql` pose le stock
courant sans lien avec trois ans d'achats et de ventes ; rejoué tel quel, l'historique passerait sous zéro. L'écart est porté par
deux inventaires clôturés, visibles dans l'écran Inventaires :

- **inventaire d'ouverture** (un par stockage, la veille du premier mouvement) : le stock de reprise, juste ce qu'il faut pour ne
  jamais être négatif et, autant que possible, retomber sur le stock courant ;
- **inventaire annuel** (un par stockage, compté la veille à 21 h 30) : il porte ce que l'ouverture ne peut absorber — stock
  constaté inférieur au théorique —, écarts qualifiés (casse, vol…). Une ligne dont le stock courant ne couvre pas une réception
  du jour (19 produits) est comptée juste après celle-ci ; l'inventaire se clôture après ce dernier comptage. Résultat sur le jeu :
  423 lignes en écart (rayon 402, dépôt 21), −4 397 unités.

Après l'inventaire annuel, le journal se lit à rebours depuis le stock courant : il y retombe par construction. Les pièces sources
reçoivent le stock avant / après du journal (lignes de vente et de commande, ajustements, retours fournisseur, répartitions,
déconditionnements, destructions, inventaire de `11`, dont l'écart compté est conservé).

**Photos** : une à chaque clôture d'inventaire (`INVENTAIRE_CLOTURE`), une de fin de jeu (`BATCH_QUOTIDIEN`, UG comprises).

**Contrôles** (`99_verification.sql`, 370 au vert sur une base jetable rechargée en entier) : fermeture de chaque mouvement selon
sa convention, aucun stock négatif, le dernier mouvement de chaque ligne de stock retombe sur son stock, `v_stock_ecart_journal`
sans écart, `fn_stock_bilan_periode` sans désaccord sur 90 jours, aucun ajustement sans bon clôturé, chaque source a son mouvement
(réception, inventaire, retour fournisseur, ajustement, répartition, déconditionnement, retour client), au moins une destruction.

**Reste à faire** : la **vente annulée** (`CANCEL_SALE`). Elle suppose une vraie annulation — copie négative de la vente, de ses
lignes et de ses règlements, ticket Z — qui touche les contrôles de chiffre d'affaires et de caisse ; à traiter à part.

**Objectifs** (`25_objectifs_pilotage.sql`) : CA TTC, marge brute, taux de marge et taux de remise (plafond), chaque mois des
années N et N-1, d'après le même mois de l'année d'avant comme le bouton « Proposer » (+ 5 % pour un montant, + 0,5 point pour le
taux de marge, + 10 % pour le plafond de remise), avec une variation propre au mois : des mois tenus et des mois manqués. 96
objectifs sur le jeu.

---

## Sources

- [Le Quotidien du Pharmacien — Plus intuitifs, plus complets, ils reculent les limites de l'analyse statistique](https://www.lequotidiendupharmacien.fr/gestion-de-lofficine/agencement-equipement/plus-intuitifs-plus-complets-ils-reculent-les-limites-de-lanalyse-statistique)
- [Le Quotidien du Pharmacien — Une solution de business intelligence pour les pharmacies (id.décisionnel)](https://www.lequotidiendupharmacien.fr/une-solution-de-business-intelligence-pour-les-pharmacies)
- [Le Quotidien du Pharmacien — Un tableau de bord pour mon officine](https://www.lequotidiendupharmacien.fr/gestion-de-lofficine/agencement-equipement/un-tableau-de-bord-pour-mon-officine)
- [Le Quotidien du Pharmacien — Synthétiser les statistiques de son officine (winStat)](https://www.lequotidiendupharmacien.fr/gestion-de-lofficine/agencement-equipement/synthetiser-les-statistiques-de-son-officine)
- [Le Quotidien du Pharmacien — Pour piloter votre officine, utilisez un tableau de bord](https://www.lequotidiendupharmacien.fr/archives/pour-piloter-votre-officine-utilisez-un-tableau-de-bord)
- [Le Quotidien du Pharmacien — Bien connaître les ratios de l'officine](https://www.lequotidiendupharmacien.fr/gestion-de-lofficine/fiscalite/bien-connaitre-les-ratios-de-lofficine)
- [Le Quotidien du Pharmacien — Baromètres Ospharm (exemple : février 2016)](https://www.lequotidiendupharmacien.fr/medicament-parapharmacie/medicament/timide-retour-la-croissance)
- [Lonasanté — id. (anciennement LGPI)](https://www.lonasante.com/id-lgpi/)
- [Winpharma — livre blanc gestion d'officine](https://www.winpharma.com/wp-content/uploads/livre-blanc-gestion-officine-logiciel-pharmacie.pdf)
- [Kolonell — logiciel de gestion de pharmacie à Abidjan (blog éditeur, chiffres non vérifiés)](https://kolonell.com/fr/blog/logiciel-gestion-pharmacie-ordonnances-abidjan-2026)
