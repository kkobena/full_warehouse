# Plan — Contenu des onglets de la page « Pilotage de l'officine »

Rédigé le 2026-10-09. Détaille, onglet par onglet, le contenu de la page décrite dans
[PLAN-PILOTAGE-OFFICINE.md](PLAN-PILOTAGE-OFFICINE.md) (structure, dictionnaire d'indicateurs, socle de données, API, décisions,
phases) : ce document ne les répète pas, il s'y réfère.

Pour chaque onglet : la question à laquelle il répond, une **analyse comparative** (ce que proposent les autres éditeurs et outils,
ce que Pharma-Smart a déjà), ce qu'on en retient, puis le **contenu détaillé** (tuiles, graphiques, tableaux, descente, alertes), les
données et l'API, les droits, les questions ouvertes et l'effort.

**Sur les sources** : les éditeurs d'officine publient peu le détail de leurs écrans. Ce qui est attribué à un éditeur vient de la
presse professionnelle ou de sa documentation publique (liens en fin de document) ; ce qui relève de la pratique générale du pilotage
ou de la BI est présenté comme tel. Les lignes « capture » renvoient à l'écran concurrent fourni par l'utilisateur.

---

## Sommaire

0. [Éléments communs à tous les onglets](#0-éléments-communs)
1. [Tableau de bord](#1-tableau-de-bord) — *Comment va l'officine ?*
2. [Analyser](#2-analyser) — *Pourquoi ça bouge ?*
3. [Comparer les années](#3-comparer-les-années) — *Où en est-on par rapport aux années passées ?*
4. [Rentabilité & remises](#4-rentabilité--remises) — *Où est-ce que je gagne ou perds de l'argent ?*
5. [Achats & stock](#5-achats--stock) — *Mes achats et mon stock sont-ils maîtrisés ?*
6. [Trésorerie & tiers payant](#6-trésorerie--tiers-payant) — *Mon argent rentre-t-il ?*
7. [Clients & équipe](#7-clients--équipe) — *Qui vient, qui vend ?*
8. [Objectifs](#8-objectifs) — *Suis-je en avance ou en retard ?*
9. [Récapitulatif](#9-récapitulatif)

---

## 0. Éléments communs

### 0.1 Ce que chaque onglet hérite de la page

- La **barre de période** (période, comparaison, « à date », jours ouvrés, granularité, filtres) — un onglet ne la redéfinit pas.
- Les **filtres** qui n'ont pas de sens pour un onglet sont grisés avec une infobulle (ex. « vendeur » dans Achats & stock), jamais
  ignorés en silence.
- L'action **Exporter la vue** : chaque bloc de l'onglet part dans l'export (une feuille Excel par tableau, le PDF reprend tuiles et
  graphiques).

### 0.2 Composants partagés

| Composant | Rôle | Base |
|---|---|---|
| **Tuile d'indicateur** | Libellé, valeur, variation (▲▼ % ou points), valeur de référence au survol, objectif et projection si disponibles, infobulle « comment c'est calculé » | `app-kpi-strip` / `app-kpi-item` |
| **Tableau à variations** | Chaque cellule : la valeur ; dessous, la variation et, au choix, la part du total. Tri, ligne de total, ligne « période en cours » marquée « au 9 », clic = descente | `app-data-table` |
| **Courbe comparée** | Série de la période + série de référence (pointillés), événements en repères verticaux, objectif en ligne horizontale | `app-chart` |
| **Barres classées** | Top N avec la valeur de référence en fantôme derrière chaque barre | `app-chart` |
| **Panneau de détail** | Liste des ventes / lignes / bons qui composent un chiffre, ouverte en panneau latéral | `app-offcanvas` + `app-data-table` |

### 0.3 Règles d'affichage

| Règle | Détail |
|---|---|
| Couleur des variations | Selon le **sens favorable** de l'indicateur (dictionnaire) : une hausse des remises est rouge, une hausse du CA verte |
| Taux | Variation en **points** (« ▲ 0,4 pt »), jamais en % d'un % |
| Base faible ou nulle | « n.s. » (non significatif) si la référence est nulle ou sous un seuil, au lieu de « ▲ 999 % » |
| Période incomplète | Libellé « au 9 » ; la comparaison est à date par défaut |
| Montants | Abrégés (« 19,2 M ») dans les tuiles et graphiques, complets dans les tableaux et au survol ; devise via `APP_DEVISE`, format via `format-utils` |
| Données absentes | « — » ; un graphique sans données dit pourquoi (« aucune vente sur la période ») |
| Fraîcheur | « Données à 10 h 15 » discret en pied d'onglet |

---

## 1. Tableau de bord

**Question** : comment va l'officine ? **Pour qui** : le titulaire, chaque matin, en moins d'une minute. **Onglet d'ouverture.**

### 1.1 Analyse comparative

| Source | Ce qui est proposé | Commentaire |
|---|---|---|
| Smart Rx 360 | **Synthèse quotidienne** des chiffres clés : CA, invendus, stock, meilleures ventes ; envoi automatique des chiffres clés | La référence du genre : peu d'indicateurs, tous les jours |
| Pharmagest id.décisionnel | Tableaux de bord **simplifiés** ou **experts**, six domaines (vente, conseil, services aux patients, gestion, achat, management) | Le niveau « simplifié » correspond à cet onglet |
| winStat (Winpharma) | « Indicateurs clés en un clic », peu d'onglets | Même philosophie |
| Capture fournie | Six tuiles (CA TTC, marge et taux, achats TTC et nb de bons, ratio ventes / achats, part tiers payant, nb ventes et panier) + courbe 13 mois + tableau mensuel à variations | Bon choix d'indicateurs ; mais comparaison fausse sur période en cours et aucune explication des écarts |
| Presse pro (experts-comptables) | Suivre peu d'indicateurs, régulièrement : CA mensuel en jours ouvrés, fréquentation, panier ordonnance / hors ordonnance, marge, trésorerie | Fixe la liste des tuiles |
| BI généraliste | Tuiles avec variation et objectif ; « smart narratives » (phrase de synthèse générée) | Idée de la phrase de synthèse |
| **Pharma-Smart** | `dashboard-ca` : CA jour / semaine / mois / année avec évolution, transactions, panier, taux de marge (`DashboardCASummaryDTO`) ; tableau de bord personnalisable par widgets | Les chiffres existent, mais à périodes figées (jour / semaine / mois / année) et sans comparaison au choix |

**Retenu** : une page courte (Smart Rx, presse pro) ; la sélection d'indicateurs de la capture complétée par fréquentation, remises
et encaissements ; le tableau à variations de la capture. **Ajouté** : comparaison à date, projection, objectif, et surtout le bloc
« Ce qui a bougé ». **Écarté** : invendus et meilleures ventes en tuile (ils vont dans « Analyser » et « Achats & stock »).

### 1.2 Contenu

**Phrase de synthèse** (une ligne, générée à partir des chiffres, sans IA) :
> « Octobre au 9 : 19,2 M de CA, en hausse de 4,1 % sur octobre 2025 au 9. La fréquentation progresse (+6 %), le panier recule (−2 %).
> Marge 28,3 % (+0,1 pt). Projection fin de mois : 66 M (objectif 70 M). »

**Tuiles** (8, deux rangées) :

| Tuile | Indicateur | Sous-ligne |
|---|---|---|
| Chiffre d'affaires | `ca_ttc` | variation ; projection fin de période ; objectif |
| Fréquentation | `nb_ventes` (et par jour ouvré) | variation |
| Panier moyen | `panier_moyen` | ordonnance / conseil séparés |
| Marge | `marge_brute`, `taux_marge` | variation en valeur et en points |
| Remises accordées | `remises`, `taux_remise` | variation (rouge si hausse) |
| Achats | `achats_ttc` | nb de bons ; ratio ventes / achats |
| Part tiers payant | `part_tp` | montant restant dû par les organismes |
| Encaissements | encaissé sur la période | écart de caisse cumulé |

**Ce qui a bougé** (bloc à droite des tuiles) :
- Décomposition de l'écart de CA en cascade : référence → effet fréquentation → effet articles par vente → effet prix moyen →
  période (graphique en cascade).
- Les **5 plus fortes contributions** à l'écart, toutes ventilations confondues (famille, produit, vendeur, organisme, type de vente),
  positives et négatives : « Antalgiques −1,2 M (−18 %) », « Assurance MUGEF-CI +0,8 M ». Chaque ligne ouvre « Analyser » positionné.

**Alertes actives** (bandeau repliable, voir [onglet 8](#8-objectifs) pour les objectifs et [PLAN-PILOTAGE-OFFICINE.md §5.3](PLAN-PILOTAGE-OFFICINE.md)) :
au plus 5, triées par gravité, chacune cliquable.

**Courbe** : CA sur 13 périodes (selon la granularité), série de référence en pointillés, objectif, événements.

**Tableau à variations** (repris de la capture, corrigé) : une ligne par période (mois par défaut), de la plus récente à la plus
ancienne ; colonnes CA TTC, marge, taux de marge, remises, achats, ventes, panier moyen, part TP ; sous chaque valeur la variation
par rapport à la **même période N-1** (et non au mois précédent, qui mélange saisonnalité et tendance — option possible).

**Actions** : « Bilan du mois » (PDF), « Envoyer chaque matin » (programmation de la synthèse par courriel, sur le modèle de l'envoi
automatique des chiffres clés de Smart Rx).

### 1.3 Données et API

| Bloc | Source | API |
|---|---|---|
| Tuiles, courbe, tableau | agrégats ventes (entêtes), achats, caisse | `/api/pilotage/series` |
| Ce qui a bougé | agrégat ventes (lignes) | `/api/pilotage/ecarts` |
| Projection | agrégat ventes + calendrier | `/api/pilotage/projection` |
| Alertes | table des alertes | `/api/pilotage/alertes` |

`DashboardCAResource` reste pour l'existant ; le nouvel onglet n'en dépend pas.

### 1.4 Droits

Tuiles marge et achats masquées sans le droit correspondant (la page reste utile sans elles).

### 1.5 Questions ouvertes

- Comparaison par défaut du tableau mensuel : même mois N-1 (proposé) ou mois précédent (capture) ?
- Huit tuiles ou six (sans encaissements ni remises) ?

**Effort** : 4 j (hors socle et API génériques).

---

## 2. Analyser

**Question** : pourquoi ça bouge, et où exactement ? **Pour qui** : le titulaire qui veut creuser, l'adjoint ou le gestionnaire.
Remplace la multiplication d'onglets par domaine.

### 2.1 Analyse comparative

| Source | Ce qui est proposé | Commentaire |
|---|---|---|
| Smart Rx 360 | Analyses **macro → produit** ; par famille, par taux de TVA ; hit-parade des produits par laboratoire | Le modèle de la descente |
| LEO (Isipharm) — témoignage | Ventes par taux de TVA, molécules sans substitution générique suffisante | Ventilations métier utiles |
| id.décisionnel | Niveau « expert » | Correspond à cet onglet |
| Capture fournie | Onglets « Ventes », « KPI Analyse » : vues figées par domaine | Rigide : chaque nouvelle question demande un nouvel onglet |
| Power BI | **Arbre de décomposition** : une mesure éclatée dimension après dimension, avec une option qui choisit la ventilation la plus explicative ; « influenceurs clés » | L'idée à reprendre pour « pourquoi ça bouge » |
| Metabase / Power BI | Explorateur libre, **vues enregistrées**, export de la vue | Évite de refaire le même réglage |
| **Pharma-Smart** | Rapports séparés : top produits (mois vs mois précédent), comparatif par famille et fournisseur (année vs N-1), synthèse journalière par type de vente, panier type, génériques | Les ventilations existent, mais chacune dans son rapport, avec ses propres périodes |

**Retenu** : une ventilation au choix (Smart Rx, BI), TVA et laboratoire comme ventilations (Smart Rx, LEO), la descente jusqu'au
produit puis à la vente, les vues enregistrées. **Ajouté** : la variation sous chaque valeur et la **décomposition** façon arbre, mais
guidée (on propose la ventilation qui explique le plus l'écart). **Écarté** : les onglets figés par domaine.

### 2.2 Contenu

**Sélecteurs** (sous la barre de période) :

| Sélecteur | Valeurs |
|---|---|
| Indicateurs (1 à 3) | tout le dictionnaire : CA, quantités, ventes, panier, marge, taux, remises, achats… |
| Ventiler par | famille, rayon, laboratoire, fournisseur, produit, forme, gamme, DCI, type de vente, ordonnance / conseil, TVA, vendeur, caissier, poste, organisme, magasin, heure, jour de semaine |
| Puis par (facultatif) | une seconde ventilation (tableau croisé) |
| Afficher | courbe, barres classées, tableau, croisé |
| Top | 10, 20, 50, tout |

**Résultat** :
- **Barres classées** de la ventilation, référence en fantôme.
- **Courbe** des N premiers éléments dans le temps.
- **Tableau à variations** : élément, valeur, variation, part du total, contribution à l'écart global ; ligne de total ; tri.
- **Croisé** (si deux ventilations) : ex. familles × mois, vendeurs × type de vente.
- **Descente** : clic sur un élément → filtre posé, ventilation suivante proposée (famille → produit → ventes) ; fil d'Ariane
  « Toutes familles › Antalgiques › Doliprane 1000 » pour remonter.
- **« Expliquer l'écart »** : sur un élément, calcule pour chaque ventilation disponible la part de l'écart qu'elle concentre, et
  propose la plus explicative (« 70 % de la baisse des Antalgiques vient de 3 produits »). Méthode simple et déterministe (contribution
  des éléments à l'écart, concentration), pas d'apprentissage automatique.

**Vues enregistrées livrées d'office** :

| Vue | Indicateurs | Ventilation |
|---|---|---|
| CA par famille N / N-1 | CA, part, variation | famille |
| Meilleures ventes | CA, quantités | produit, top 50 |
| Hit-parade par laboratoire | CA, marge | laboratoire → produit |
| Ventes par taux de TVA | CA HT, TVA | TVA |
| Ordonnance / conseil | CA, panier, ventes | type de prescription |
| Comptant / assurance / carnet | CA, panier, part | type de vente |
| Fréquentation horaire | ventes | heure × jour de semaine |
| Génériques | CA, part générique | famille → DCI |
| Produits en recul | CA, variation | produit, tri par variation |

L'utilisateur enregistre ses propres vues (nom, partage avec l'équipe ou non).

### 2.3 Données et API

- `/api/pilotage/series` avec `ventilation` (et `ventilation2`), `top`, `tri`.
- `/api/pilotage/ecarts?element=…` pour « Expliquer l'écart ».
- Panneau de détail : liste des ventes filtrée (API ventes existante, paramètres de période et de filtres).
- Ventilations par heure : agrégat ventes (entêtes) ; par produit, famille, TVA… : agrégat ventes (lignes) joint au référentiel produit.

### 2.4 Droits

Même logique d'autorisation que les onglets : chaque indicateur et chaque ventilation sensible est un `nav_item` ; sans le droit, il
n'apparaît pas dans les sélecteurs. Marge et achats suivent les droits des onglets Rentabilité et Achats ; la ventilation « vendeur »
celui de l'onglet Clients & équipe (sous-section Équipe) ; la ventilation par **client nominatif** a son propre droit, comme la liste
des clients de cet onglet.

### 2.5 Décisions

- Ventilation par client nominatif : **retenue**, soumise à autorisation comme les onglets (décidé le 2026-10-09).
- Croisé : **deux ventilations** dans un premier temps (décidé le 2026-10-09).

**Effort** : 5 j.

---

## 3. Comparer les années

**Question** : où en est-on par rapport aux années passées ? **Pour qui** : le titulaire, en fin de mois, de trimestre, d'exercice ; la
préparation d'un rendez-vous avec l'expert-comptable ou la banque.

### 3.1 Analyse comparative

| Source | Ce qui est proposé | Commentaire |
|---|---|---|
| Ospharm (baromètres) | Évolution sur le **même mois de l'année précédente** | La référence métier |
| Presse pro | Évolution mensuelle du CA **en tenant compte des jours ouvrés** | Un mois à 27 jours ouvrés contre 25 fausse la lecture |
| Capture fournie | Onglet « Comparateur » (contenu non visible) ; courbe avec « période comparée » | Comparaison à une seule référence |
| BI généraliste | Cumul depuis le début d'année (YTD) vs même cumul N-1, glissant 12 mois, croissance annuelle moyenne | Mécanique standard |
| **Pharma-Smart** | `comparative-analysis` : mensuel / trimestriel / annuel, global, par famille, par fournisseur — **N contre N-1 seulement** ; `seasonality` : N vs N-1 | Bonne base, limitée à deux années et à l'année civile |

**Retenu** : même mois N-1, jours ouvrés, cumul et glissant. **Ajouté** : jusqu'à 6 années superposées, tableaux croisés, croissance
annuelle moyenne, saisonnalité moyenne. **À éviter** : superposer des années incomplètes sans le dire.

### 3.2 Contenu

**Choix** : indicateur (CA par défaut, puis marge, ventes, panier, remises, achats), années (N à N-5, cases à cocher), mode
(mensuel / cumulé depuis janvier / glissant 12 mois), jours ouvrés (oui / non), filtre famille ou type de vente.

**Graphiques** :
- **Courbes superposées** : une courbe par année, mois en abscisse ; l'année en cours s'arrête au mois en cours (à date).
- **Barres groupées** par trimestre et par année.
- **Saisonnalité** : poids de chaque mois dans l'année, moyenne des années disponibles (sert aussi à la projection, onglet 1).

**Tableaux** :

| Tableau | Lignes × colonnes | Cellule |
|---|---|---|
| Années × mois | mois × années | valeur ; variation vs année précédente |
| Années × famille | familles × années | valeur, part, variation |
| Années × type de vente | comptant / assurance / carnet / dépôt × années | idem |
| Synthèse annuelle | années | CA, marge, taux, ventes, panier, remises, croissance, **croissance annuelle moyenne** sur la période |

**Lecture** : une ligne de commentaire automatique (« Meilleur mois historique : décembre 2025. Croissance annuelle moyenne
2021-2025 : +6,8 %. »).

### 3.3 Données et API

- Agrégat ventes (entêtes) pour le global, (lignes) pour les familles ; calendrier pour les jours ouvrés.
- `/api/pilotage/series?granularite=MOIS&comparaison=N_MOINS_K&k=1..5` ou un endpoint dédié `/api/pilotage/annees` qui renvoie la
  matrice années × mois d'un coup (une requête plutôt que six).
- `ComparativeReportResource` reste pour l'existant ; à terme, réaligné sur les agrégats.

### 3.4 Décisions

- **Ventes `imported = true`** : ce sont les ventes réalisées dans un sous-magasin physique (dépôt) et importées ; elles font partie du
  CA et de l'historique du pilotage (décidé le 2026-10-09). `mv_daily_sales_summary`, qui les exclut, reste telle quelle.
- **Année civile** : en Côte d'Ivoire, le droit comptable OHADA fait coïncider l'exercice avec l'année civile (Acte uniforme de 2000,
  art. 7 : « L'exercice coïncide avec l'année civile » ; seul le premier exercice peut être plus court ou plus long). Le projet n'a pas
  de notion d'exercice décalé. On compare donc des années civiles ; un exercice atypique (premier exercice, reprise en cours d'année)
  se traite avec une période personnalisée. Pas de paramètre « début d'exercice » tant qu'aucune officine n'en a besoin.
  À confirmer sur le texte de 2017 (AUDCIF), qui a remplacé celui de 2000.

**Effort** : 3 j.

---

## 4. Rentabilité & remises

**Question** : où est-ce que je gagne ou perds de l'argent ? **Pour qui** : le titulaire ; c'est l'onglet où se lit la **remise
accordée**, demandée explicitement par les pharmaciens.

### 4.1 Analyse comparative

| Source | Ce qui est proposé | Commentaire |
|---|---|---|
| Presse pro | La marge commerciale est « l'indicateur de gestion le plus important » | Justifie un onglet dédié |
| My Pilot (Pharmagest) | Analyse des ratios, politique de prix, simulation de marge selon les quantités commandées | La simulation relève des achats (plus tard) |
| winStat | « Optimiser vos marges » | Analyse de marge par produit |
| Smart Rx 360 | Observatoire des prix (5 officines voisines) | Écarté : données externes |
| Rx30 Insights (États-Unis) | Tableau de bord **marge brute**, ventes à **marge négative**, par assureur | Idée : lister les ventes à marge négative |
| Pratique du commerce de détail | Remise moyenne, part des ventes remisées, **répartition des ventes par tranche de remise** et marge par tranche (repérer à partir de quel niveau une remise détruit la marge), remise et marge **par vendeur**, transactions extrêmes | Le cœur de l'analyse des remises |
| Capture fournie | Onglet « Marge » (contenu non visible) ; pas d'onglet remises | — |
| **Pharma-Smart** | `remises-analysis` : total, CA après remise, taux, nb ventes remisées (`RemisesAnalysisKpiDTO`) et top produits remisés ; `profitability-analysis` : marge par produit, filtre famille, faible marge ; `pnl-analytique` ; `demarque` (par motif) ; `mv_product_profitability` | Les briques existent ; il manque la comparaison, la ventilation par vendeur et le croisement remise × marge |

**Retenu** : marge par famille / laboratoire / produit, faible marge, ventes à marge négative (Rx30), analyse des remises par tranche,
par vendeur et rapportée à la marge (pratique du commerce de détail). **Écarté** : observatoire de prix des voisins. **Plus tard** :
simulation de marge à l'achat (module commande).

### 4.2 Contenu

**Sous-sections** (ancres en haut de l'onglet) : Marge · Remises · Démarque.

**Marge**
- Tuiles : marge brute, taux de marge (en points), coefficient moyen (prix de vente / prix d'achat), marge par vente.
- Courbe : taux de marge dans le temps, avec la référence.
- Tableau : familles (puis laboratoires, produits) — CA HT, coût, marge, taux, variation du taux en points, **contribution à
  l'écart de marge** (effet volume vs effet taux).
- Listes : produits à faible marge (seuil paramétrable), **ventes à marge négative** sur la période (lien vers la vente).
- Effet mix : « la marge recule de 0,6 pt dont 0,4 pt dû au mix (plus de produits à faible marge vendus) et 0,2 pt aux prix ».

**Remises**
- Tuiles : remises accordées, taux de remise sur le CA, part des ventes remisées, remise moyenne par vente remisée, **poids des
  remises dans la marge** (`remises / (marge + remises)`).
- Courbe : remises et taux de remise dans le temps, avec la référence.
- Ventilations (tableau à variations, sélecteur) : **par vendeur**, par famille, par produit, par code remise (grille), par type de
  vente, par organisme / client, et **par mode d'octroi** (privilège du vendeur / autorisation, avec la personne qui a autorisé).
- **Tranches de remise** : nombre de ventes et marge réalisée par tranche (0, 0-5 %, 5-10 %, 10-20 %, > 20 %) — montre à partir de
  quelle tranche la marge devient faible ou négative.
- Vendeurs : taux de remise de chacun vs moyenne de l'équipe ; alerte au-delà d'un multiple (paramétrable).
- Liste : ventes avec les plus fortes remises (lien vers la vente, vendeur, autorisation éventuelle).

**Démarque**
- Valeur perdue par motif (périmés, casse, vol, écarts d'inventaire) dans le temps et vs N-1 ; en % du CA.
- Produits les plus touchés.

### 4.3 Données et API

- Agrégat ventes (lignes) : CA HT, coût, remises par ligne ; vendeur et type de vente portés par l'agrégat.
- Tranches de remise : calculées sur l'agrégat **entêtes** (taux de remise par vente) — à ajouter à l'agrégat (colonne « tranche »).
- Démarque : `DemarqueReportResource` (`/by-motif`) étendu à une période et une comparaison.
- La remise se lit sur les montants de remise des ventes et lignes ; on ne s'appuie pas sur `RemiseClient`, voué à disparaître.
- **Une seule remise au comptoir** : la `RemiseProduit`, appliquée selon la grille du code remise du produit (`CodeGrilleRemise`,
  VO / VNO). Le vendeur l'accorde **soit** parce qu'il détient le privilège `PR_AJOUTER_REMISE_VENTE`, **soit** après autorisation :
  un détenteur du privilège saisit sa clé de sécurité, ce qui écrit une ligne dans `utilisation_cle_securite` (propriétaire de la clé,
  utilisateur connecté, caisse, date, privilège, vente). On peut donc distinguer, sans nouvelle donnée, les remises **accordées par
  privilège** et les remises **autorisées**, et dire **qui** les a autorisées.

### 4.4 Droits

Onglet entier sous le droit « marge » ; la ventilation par vendeur sous « performance de l'équipe ».

### 4.5 Décisions et questions ouvertes

- **Marge sur la quantité demandée** (décidé le 2026-10-09) — comme les vues actuelles ; une vente forcée compte pour ce qui a été
  demandé, cohérent avec la règle du stock négatif.
- Remises autorisées : isolées grâce à `utilisation_cle_securite` (voir §4.3).
- Seuils des tranches de remise et du « faible marge » : paramètres de l'officine (`app_configuration`).

**Effort** : 4 j.

---

## 5. Achats & stock

**Question** : mes achats et mon stock sont-ils maîtrisés ? **Pour qui** : le titulaire et le responsable des commandes.

### 5.1 Analyse comparative

| Source | Ce qui est proposé | Commentaire |
|---|---|---|
| Smart Rx 360 | Tableaux de pilotage autour de **trois axes : achats, stocks, ventes** (et consolidés pour les groupements) ; mini / maxi recalculés en continu | Le modèle de l'onglet |
| La Fabrique du Net (Smart Rx) | Tableaux de bord de suivi des **dates de péremption** et d'optimisation du stock | Péremption dans l'onglet |
| My Pilot | Préparation des visites fournisseurs, simulation de marge selon les quantités | Plus tard |
| Capture fournie | Onglets « Achats », « Stock », « Achats / Ventes », tuile « ratio ventes / achats » | Le ratio est une bonne idée de tuile |
| Pratique de gestion des stocks | **Rotation** (coût des ventes / stock moyen), **durée d'écoulement / couverture** (stock moyen / coût des ventes × 365), **taux de rupture** et **taux de service**, **stock dormant** (nombre et valeur), délai de réapprovisionnement ; lecture croisée (rotation faible + couverture forte = surstock) | Les définitions |
| Presse pro (ratios) | Rotation du stock ≈ 45 jours, crédit fournisseurs ≈ 38 jours (France) | Repères à recalibrer |
| **Pharma-Smart** | `stock-valuation` (instantané), `stock-rotation` (taux annuel, jours en stock, produits lents), `stock-abc`, `stock-alerts`, `supplier-performance` (délai moyen, taux de conformité, score), `remises-rfa`, table `rupture` (produit, fournisseur, quantité), lots et péremptions, SEMOIS (suggestions) | Riche, mais **sans historique** du stock ni comparaison de période |

**Retenu** : les trois axes (Smart Rx), le ratio ventes / achats (capture), les définitions standard (rotation, couverture, rupture,
service, dormant), la péremption. **Ajouté** : photographies mensuelles du stock pour comparer à N-1. **Plus tard** : simulation de
marge à l'achat, mini / maxi (relèvent du module commande).

### 5.2 Contenu

**Sous-sections** : Achats · Achats / ventes · Stock · Ruptures & péremptions.

**Achats**
- Tuiles : achats TTC / HT, nombre de bons, délai moyen de livraison, taux de conformité des livraisons.
- Courbe : achats par période vs référence.
- Tableau à variations : par fournisseur (montant, part, variation, délai, conformité, RFA estimée / reçue), puis par famille.

**Achats / ventes**
- Tuile : ratio ventes / achats sur la période et sur 12 mois glissants.
- Courbe : achats et coût des ventes superposés (un écart durable = stock qui gonfle ou fond).
- Tableau : par famille et par fournisseur — achats, coût des ventes, écart, ratio.

**Stock**
- Tuiles : valeur du stock (fin de période) et variation vs N-1, rotation (fois / an), couverture (jours), stock dormant (valeur et
  nombre de produits sans vente depuis X jours).
- Courbe : valeur du stock fin de mois, vs N-1 (photographies).
- Tableau : par famille / rayon — valeur, part, couverture, rotation, dormant ; classes ABC.

**Ruptures & péremptions**
- Tuiles : taux de rupture fournisseurs, ventes manquées au comptoir (valeur), valeur périmée sur la période, valeur à périmer dans
  3 mois.
- Tableaux : ruptures fournisseurs par fournisseur et par produit ; ventes manquées par produit et famille ; péremptions à venir par
  mois.
- **Deux ruptures distinctes**, à ne pas additionner :
  - **ruptures fournisseurs** : table `rupture` — produits commandés que le fournisseur n'a pas livrés (produit, fournisseur,
    quantité, date) ; taux = lignes en rupture / lignes commandées ;
  - **ruptures au comptoir** : table `avoir_client` — quantités demandées par un client et non servies (produit, client, ligne de
    vente, quantité, montant) ; ce sont les **ventes manquées**, en nombre et en valeur ; taux = quantités en avoir / quantités
    demandées.

### 5.3 Données et API

- Agrégat achats (jour × produit × fournisseur) ; agrégat ventes (lignes) pour le coût des ventes.
- **Photographie mensuelle du stock** (fin de mois × produit × magasin) — à démarrer dès la phase 1 du plan général : la comparaison
  N-1 du stock ne sera possible qu'un an après.
- Ruptures fournisseurs : table `rupture` ; ruptures au comptoir : table `avoir_client` ; péremptions : lots ; dormants : dernière date
  de vente par produit, comparée au seuil `app_configuration` (90 jours par défaut).
- Réutilisation : `SupplierPerformanceReportResource`, `StockRotationReportResource`, `StockValuationReportResource`,
  `ABCParetoReportResource` (paramétrés par la période).

### 5.4 Droits

Onglet sous le droit « achats » ; la valeur du stock au prix d'achat sous le droit « marge ».

### 5.5 Décisions

- Achats datés à la **réception** (décidé le 2026-10-09).
- Seuil du « dormant » : paramètre dans `app_configuration`, **90 jours** par défaut (décidé le 2026-10-09) ; lu par un accesseur
  d'`AppConfigurationService` sur le modèle de `getDelaiReglement()`.
- Ruptures : `rupture` pour les fournisseurs (à la commande), `avoir_client` pour le comptoir (décidé le 2026-10-09) — voir §5.2.

**Effort** : 4 j (hors photographies, comptées dans le socle).

---

## 6. Trésorerie & tiers payant

**Question** : mon argent rentre-t-il ? **Pour qui** : le titulaire et la personne chargée de la facturation tiers payant. En
Côte d'Ivoire, la part assurance et les délais de règlement des organismes pèsent lourd : c'est un onglet clé.

### 6.1 Analyse comparative

| Source | Ce qui est proposé | Commentaire |
|---|---|---|
| PioneerRx (États-Unis) | Ventes par canal, paiements tiers ; **solde de contrôle par tiers payeur** (ventes, paiements, soldes de début et de fin) « pour vérifier que les paiements arrivent de façon prévisible » ; solde des comptes clients | Le modèle du suivi par organisme |
| EnlivenHealth | Rapprochement des remboursements jusqu'à la prescription | Pharma-Smart a déjà un rapprochement |
| Presse pro | Trésorerie à suivre ; plan de trésorerie réactualisé chaque mois ; une officine peut être bénéficiaire et voir sa trésorerie se dégrader (stock qui gonfle) ; crédit clients ≈ 8 jours | Lien avec l'onglet Achats & stock |
| Capture fournie | Onglet « Caisse & tiers-payant », tuile « part tiers payant » | — |
| **Pharma-Smart** | `situation-creances`, `vieillissement-creances` (par tranche), `dso-organisme` (DSO, délai de règlement, fiabilité), `taux-recouvrement-tp`, `concentration-payers`, `cash-flow-bfr`, `vieillissement-differes`, rapport de caisse journalier, rapprochement TP, avoirs | Très complet — il manque la vue d'ensemble sur une période comparée |

**Retenu** : le solde par organisme façon PioneerRx (facturé, réglé, solde début / fin), le DSO et le vieillissement existants, le lien
trésorerie / stock (presse pro). **Ajouté** : encaissements attendus (échéancier des créances), comparaison de période.

### 6.2 Contenu

**Sous-sections** : Encaissements & caisse · Tiers payant · Différés & crédit.

**Encaissements & caisse**
- Tuiles : encaissé sur la période, dont espèces / mobile money / carte / chèque / virement ; écart de caisse cumulé ; **arrondis de
  caisse, à part** (l'arrondi au multiple de 5, sur les espèces, est une règle et non une erreur : il n'entre pas dans l'écart).
- Courbe : encaissements par jour ou mois vs référence, empilés par mode.
- Tableau : par caissier et par poste — encaissé, nombre de sessions, écarts.

**Tiers payant**
- Tuiles : part TP dans le CA (et variation en points), facturé aux organismes, réglé, **encours**, délai moyen de paiement (DSO).
- Courbe : part TP vs comptant dans le temps.
- **Tableau des organismes** (cœur de la sous-section) : solde de début, facturé, réglé, avoirs, solde de fin, DSO, retard, taux de
  recouvrement, variation de l'encours ; tri par encours.
- Vieillissement : encours par tranche (0-30, 31-60, 61-90, > 90 jours) et son évolution.
- **Encaissements attendus** : échéancier des créances par mois. Délai retenu pour chaque organisme, dans cet ordre :
  1. le **délai observé** (moyenne de ses règlements passés), s'il a assez d'historique (seuil à fixer, ex. 3 factures réglées) ;
  2. sinon le **délai contractuel** de son groupe (`groupe_tiers_payant.delai_reglement`) ;
  3. sinon le **délai par défaut** de l'officine (`AppConfigurationService.getDelaiReglement()`, clé
     `APP_DELAI_REGLEMENT_FACTURE`, 30 jours si absent).
  L'infobulle de chaque organisme dit quel délai a servi.
- Concentration : part des 3 / 5 premiers organismes dans l'encours.

**Différés & crédit**
- Encours des ventes différées, par client ; vieillissement ; évolution.
- Avoirs clients émis et remboursés.

### 6.3 Données et API

- Agrégat caisse (jour × mode × caissier) ; factures et règlements TP ; différés.
- Réutilisation : `VieillissementCreancesResource` (`/dso-organisme`, `/encours-evolution`), `ConcentrationPayersResource`,
  `TiersPayantReportResource`, `CashRegisterReportResource`, `CashFlowBfrResource`, paramétrés par la période.

### 6.4 Droits

Sous-section caisse visible du responsable de caisse ; tiers payant et différés sous le droit « finances ».

### 6.5 Décisions

- Encaissements attendus : délai observé, sinon délai du groupe (`delai_reglement`), sinon délai par défaut (`getDelaiReglement()`)
  (décidé le 2026-10-09) — voir §6.2.
- Arrondis de caisse : **affichés à part**, hors écart (décidé le 2026-10-09).
- Reste ouvert : nombre minimal de règlements pour se fier au délai observé.

**Effort** : 4 j.

---

## 7. Clients & équipe

**Question** : qui vient, et qui vend ? **Pour qui** : le titulaire (organisation des équipes, animation commerciale).

### 7.1 Analyse comparative

| Source | Ce qui est proposé | Commentaire |
|---|---|---|
| winStat, id.décisionnel (« management ») | Suivi des **performances de l'équipe** | Domaine reconnu |
| LEO (Isipharm) — témoignage | Outils de planning qui montrent les **créneaux où il faut plus de personnel** | La fréquentation horaire sert à ça |
| Presse pro | Fréquentation hebdomadaire et mensuelle ; productivité du personnel ; CA par salarié équivalent temps plein | — |
| Pratique du commerce de détail | Par vendeur : CA, panier, **taux de marge**, **taux de remise**, écart à l'objectif **en %** | Lecture par vendeur équilibrée (pas seulement le CA) |
| **Pharma-Smart** | `sales-by-staff`, `customer-segmentation` (RFM, champions, à risque), `client-retention` (actifs, à risque, perdus, CA moyen par client), panier type | Bonne base, sans comparaison ni fréquentation horaire |

**Retenu** : performance de l'équipe sur plusieurs axes (CA, panier, marge, remises, annulations), carte de chaleur horaire pour le
planning (LEO), fréquentation (presse pro). **Écarté** : un classement des vendeurs par seul CA (pousse à la remise).

### 7.2 Contenu

**Sous-sections** : Fréquentation · Clients · Équipe.

**Fréquentation**
- **Carte de chaleur** heure × jour de semaine (nombre de ventes, ou CA), sur la période et sa référence ; créneaux les plus chargés.
- Courbe des ventes par jour ; ventes par jour ouvré.

**Clients**
- Tuiles : clients identifiés actifs, nouveaux, revenus, perdus ; part du CA réalisée avec un client identifié.
- Segments (RFM existant) et leur évolution ; CA moyen par client.
- Liste : clients en baisse (lien fiche client) — sous droit.

**Équipe**
- Tableau par vendeur : ventes, CA, panier moyen, articles par vente, taux de marge, **taux de remise**, annulations, avoirs,
  part ordonnance / conseil — chacun avec la variation vs la référence et l'écart à la moyenne de l'équipe.
- Graphique : CA par vendeur et par heure (qui est présent quand).
- Par caissier : encaissements, écarts (renvoi vers l'onglet 6).

### 7.3 Données et API

- Agrégat ventes (entêtes) avec heure, vendeur, caissier ; agrégat lignes pour la marge et les remises par vendeur.
- Réutilisation : `CustomerSegmentationReportResource`, `ClientRetentionReportResource`.

### 7.4 Droits

Sous-section Équipe sous le droit « performance de l'équipe » ; un vendeur sans ce droit peut voir **sa** ligne seulement (périmètre
« moi », comme pour les widgets du tableau de bord personnalisable).

### 7.5 Questions ouvertes

- Faut-il afficher les noms des vendeurs à toute l'équipe, ou seulement au titulaire ?
- CA par équivalent temps plein : nécessite les temps de présence (non gérés aujourd'hui) — plus tard.

**Effort** : 3 j.

---

## 8. Objectifs

**Question** : suis-je en avance ou en retard ? **Pour qui** : le titulaire ; c'est aussi la source des alertes « objectif menacé ».

### 8.1 Analyse comparative

| Source | Ce qui est proposé | Commentaire |
|---|---|---|
| Expert-comptables (budget prévisionnel) | Budget sur 12 mois, **contrôle budgétaire** à la semaine ou au mois ; suivi du CA mensuel et **cumulé** par rapport aux objectifs ; actions correctives | Le principe |
| Pratique du suivi d'objectifs | Tableau en quatre colonnes : **objectif, réalisé, écart (valeur), degré d'atteinte (%)** ; écart présenté en % | La forme |
| Smart Rx, id.décisionnel, capture | Pas d'objectifs visibles dans ce qui est publié | Différenciant pour Pharma-Smart |
| BI généraliste | Objectif en ligne sur les courbes, jauge d'atteinte, projection | — |
| **Pharma-Smart** | Rien | À créer |

**Retenu** : budget mensuel, réalisé / écart / atteinte, cumul (expert-comptable). **Ajouté** : objectifs proposés d'après N-1,
projection de fin de période, alertes.

### 8.2 Contenu

**Saisie** (droit dédié) :
- Grille année × mois pour : CA, marge (ou taux), taux de remise maximal, éventuellement par famille ou par type de vente.
- « Proposer » : remplit la grille d'après N-1 + x % (et la saisonnalité de l'onglet 3) ; l'utilisateur ajuste.
- Historique des modifications (qui, quand).

**Suivi** :
- Tableau mensuel : objectif, réalisé, écart, atteinte (%), cumul depuis janvier (objectif, réalisé, atteinte).
- Jauges du mois en cours avec **projection** de fin de mois (réalisé à date + reste estimé d'après le profil du même mois N-1, en
  jours ouvrés).
- Les objectifs apparaissent dans les autres onglets : tuiles du Tableau de bord, ligne sur les courbes, comparaison « Objectif » dans
  la barre de période.

**Alertes** liées : objectif menacé (projection sous un seuil), objectif dépassé.

### 8.3 Données et API

- Table `objectif_pilotage` (indicateur, période, magasin, valeur, ventilation facultative) — entité JPA classique.
- `/api/pilotage/objectifs` (CRUD) ; la comparaison `OBJECTIF` de `/api/pilotage/series` les lit ; `/api/pilotage/projection`.

### 8.4 Questions ouvertes

- Objectifs par vendeur : utiles, mais à manier avec précaution (effet sur les remises) — plus tard ?
- Objectif en TTC ou HT (suit la décision sur la référence du CA).

**Effort** : 4 j (avec projection et alertes).

---

## 9. Récapitulatif

| Onglet | Réutilise | Nouveau | Différence avec les autres éditeurs | Effort |
|---|---|---|---|---|
| Tableau de bord | `dashboard-ca` (logique), tableau de la capture | phrase de synthèse, « Ce qui a bougé », projection, alertes | Explique l'écart au lieu de le constater ; compare à date | 4 j |
| Analyser | top produits, comparatif famille / fournisseur, génériques | explorateur, croisé, descente, « Expliquer l'écart », vues enregistrées | Un seul outil au lieu d'onglets figés | 5 j |
| Comparer les années | `comparative-analysis`, `seasonality` | N à N-5, croisés, jours ouvrés, croissance moyenne | Six années, jours ouvrés | 3 j |
| Rentabilité & remises | `profitability`, `remises-analysis`, `demarque`, `pnl` | remises par vendeur, tranches de remise, poids dans la marge, effet mix, marge négative | Analyse des remises rarement proposée | 4 j |
| Achats & stock | rotation, valorisation, ABC, fournisseurs, RFA, ruptures, lots | ratio ventes / achats, photographies, dormant, ventes manquées | Stock comparé à N-1 | 4 j |
| Trésorerie & TP | créances, DSO, vieillissement, concentration, caisse, BFR | solde par organisme sur la période, encaissements attendus | Pensé pour la part assurance | 4 j |
| Clients & équipe | segmentation, rétention, ventes par vendeur | carte de chaleur, vendeur multicritère, périmètre « moi » | Équipe jugée sur marge et remises, pas seulement CA | 3 j |
| Objectifs | — | saisie, proposition, suivi, projection | Absent chez les concurrents publiés | 4 j |

Total des onglets : ≈ 31 j, **en plus** du socle (données, API génériques, page) chiffré dans le plan général. Le plan général
(§9) reste la référence pour l'ordre de réalisation ; ce document en détaille le contenu.

---

## Sources

- [Le Quotidien du Pharmacien — Plus intuitifs, plus complets, ils reculent les limites de l'analyse statistique (Smart Rx 360, My Pilot)](https://www.lequotidiendupharmacien.fr/gestion-de-lofficine/agencement-equipement/plus-intuitifs-plus-complets-ils-reculent-les-limites-de-lanalyse-statistique)
- [Le Quotidien du Pharmacien — Une solution de business intelligence pour les pharmacies (id.décisionnel)](https://www.lequotidiendupharmacien.fr/une-solution-de-business-intelligence-pour-les-pharmacies)
- [Le Quotidien du Pharmacien — Synthétiser les statistiques de son officine (winStat)](https://www.lequotidiendupharmacien.fr/gestion-de-lofficine/agencement-equipement/synthetiser-les-statistiques-de-son-officine)
- [Le Quotidien du Pharmacien — Un tableau de bord pour mon officine](https://www.lequotidiendupharmacien.fr/gestion-de-lofficine/agencement-equipement/un-tableau-de-bord-pour-mon-officine)
- [Le Quotidien du Pharmacien — Pour piloter votre officine, utilisez un tableau de bord](https://www.lequotidiendupharmacien.fr/archives/pour-piloter-votre-officine-utilisez-un-tableau-de-bord)
- [Le Quotidien du Pharmacien — Bien connaître les ratios de l'officine](https://www.lequotidiendupharmacien.fr/gestion-de-lofficine/fiscalite/bien-connaitre-les-ratios-de-lofficine)
- [Le Quotidien du Pharmacien — Performances de l'officine : plus qu'une affaire de chiffres (LEO)](https://www.lequotidiendupharmacien.fr/gestion-de-lofficine/comptabilite/performances-de-lofficine-plus-quune-affaire-de-chiffres)
- [Le Quotidien du Pharmacien — La gestion des stocks, c'est automatique](https://www.lequotidiendupharmacien.fr/gestion-de-lofficine/agencement-equipement/la-gestion-des-stocks-cest-automatique)
- [Le Quotidien du Pharmacien — Budget prévisionnel : pourquoi faut-il anticiper](https://www.lequotidiendupharmacien.fr/exercice-pro/budget-previsionnel-pourquoi-faut-il-anticiper)
- [Le Quotidien du Pharmacien — Baromètres Ospharm (exemple : février 2016)](https://www.lequotidiendupharmacien.fr/medicament-parapharmacie/medicament/timide-retour-la-croissance)
- [Smart Rx — centre d'aide, Statistiques / Listes](https://intercom.help/smart-rx/fr/collections/616685-statistiques-listes)
- [La Fabrique du Net — Smart Rx](https://www.lafabriquedunet.fr/logiciel/smart-rx)
- [PioneerRx — Six financial reports you should be running](https://www.pioneerrx.com/blog/six-financial-reports-you-should-be-running)
- [Outcomes — Insights for Rx30, guide utilisateur](https://outcomes.com/knowledge-base/outcomes-insights-user-guide)
- [EnlivenHealth — Pharmacy claims reconciliation](https://enlivenhealth.co/pharmacy-solutions/pharmacy-claims-reconciliation)
- [Hayot Expertise — Optimiser la gestion des stocks : rotation, surstock et cash immobilisé](https://hayot-expertise.fr/blog/optimiser-gestion-stocks-rotation-cash)
- [Cleverence — Indicateurs clés de la gestion des stocks](https://www.cleverence.com/articles/business-blogs-fr/inventory-metrics-kpis-you-should-know-5938271/)
- [Mémoire Online — Gestion de stock des produits de santé d'une officine](https://www.memoireonline.com/10/22/13135/m_Gestion-de-stock-des-produits-de-sant-d-une-officine-de-pharmacie--cas-de-la-pharmacie-Saint-Lu29.html)
- [Foucher — Concevoir un tableau de bord de suivi d'équipe](https://www.foucherconnect.fr/19mec97)
- [Fiducial — Budget prévisionnel, tableau de bord](https://www.fiducial.fr/Expert-comptable/Budget-previsionnel)
- [Team400 — Power BI : arbre de décomposition et influenceurs clés](https://team400.ai/blog/2026-05-power-bi-ai-visuals-key-influencers-decomposition)
- [Energent — Analyse tarifaire et promotions (commerce de détail, source éditeur)](https://www.energent.ai/ugc/fr/tool/pricing-promotion-analytics)
