# Analyse — Module de commande fournisseur

> Audit du module d'approvisionnement de **Pharma-Smart** du point de vue de la gestion
> d'officine : couverture des besoins quotidiens, intégrité de la chaîne d'approvisionnement,
> ergonomie de saisie, lacunes et plan de travail.
>
> Date de l'audit : **2026-11-13** — branche `master`.

---

## 1. Périmètre audité

| Couche | Fichiers principaux |
|---|---|
| Domaine | `Commande.java`, `OrderLine.java`, `OrderStatut.java`, `FournisseurProduit.java`, `Fournisseur.java`, `SemoisConfiguration.java`, `SemoisClasseConfig.java`, `AvoirClient.java` |
| Services | `CommandServiceImpl.java` (1232 l.), `CommandeDataServiceImpl.java`, `SemoisCalculationService.java` (≈1100 l.), `SuggestionProduitServiceImpl.java` (810 l.), `StockEntryServiceImpl.java` (1155 l.), `RetourBonServiceImpl.java` (779 l.), `AvoirClientDocumentServiceImpl.java`, `PharmaMlServiceImpl.java` |
| REST | `web/rest/commande/` (CommandeResource, CommandeDataResource, StockEntryResource, PharmaMlResource), `web/rest/stock/SemoisResource.java`, `web/rest/reassort/` |
| Batch | `pharmaSmart-batch/.../semois/`, `.../classification/`, `.../pipeline/` |
| Front | `webapp/app/features/commande/` (commande-home, commande-requested, commande-received, suggestion, semois-suggestions, pharmaml, retour-fournisseur, bon-entree-diverse) |
| SQL | `V1.0.5__procedure.sql`, `V1.3.7__stock_virtuel_rupture_vmm.sql`, `V1.7.0/1.7.4/1.7.5` (vue SEMOIS), `V1.9.6__fix_mv_supplier_performance_statuts.sql`, `V2.1.34__pilotage_agregats.sql` |
| Recette | 86 parcours e2e dans `e2e/parcours/achats/` |

---

## 2. Cartographie fonctionnelle — ce qui existe

### 2.1 Six portes d'entrée pour créer une commande

C'est le point fort le plus immédiat : la ressaisie manuelle n'est jamais le seul chemin.

| Porte | Entrée technique |
|---|---|
| Saisie manuelle | `commande-requested.component` — fournisseur + recherche produit + quantité, `F9` crée le bon |
| Commande rapide | `CommandeRapideDTO` / `POST /api/commandes/rapide` — depuis la fiche produit et les actions rapides de l'accueil |
| Import fichier grossiste | `uploadNewCommande` — CSV/TXT, stratégies dédiées par répartiteur + formats génériques (CIP + qté, avec ou sans prix d'achat) |
| Depuis une suggestion | `createCommandeFromSuggestion` / `createCommandeFromSelection`, ou injection dans une commande existante (`importSuggestionIntoCommande`, `importSuggestionLinesIntoCommande`) |
| Depuis les urgents SEMOIS | `createCommandeFromSemoisLines` |
| Reliquat | `createReliquat` — bascule les quantités non livrées d'une commande clôturée dans une nouvelle commande |

S'ajoutent la **fusion** de deux commandes du même grossiste (`fusionner`) et le **changement de
grossiste** sur une commande en cours (`changeGrossiste`).

### 2.2 Le moteur de réassort

Deux modèles au choix (`ModelReapprovisionnement` : `CLASSIQUE`, `SEMOIS`), paramétré par
`APP_MODEL_REAPPRO`. Le modèle SEMOIS (`SemoisCalculationService.java:435-620`) est nettement
au-dessus du réassort à seuil fixe qu'on trouve habituellement :

- **VMM pondérée** sur N mois, poids dégressifs vers le passé ;
- **exclusion des mois de rupture fournisseur** (`est_rupture_fournisseur`) avec repli sur tous les
  mois si aucun mois n'est valide — le détail qui évite qu'une rupture s'auto-entretienne en tirant
  la moyenne vers le bas ;
- **marge de sécurité** = `VMM × délai_livraison × coefficient_classe / 30 × facteur_saisonnier` ;
- **facteur saisonnier automatique**, calculé sur N-1/N-2 et borné à `[0,50 ; 3,00]` ;
- **stock de rotation** = `VMM × fréquence_commande / 30` (consommation entre deux commandes) ;
- **plafond de péremption** à `3 × VMM`, activable par classe ou par produit ;
- **cascade de paramétrage** du délai et de la fréquence : produit → fournisseur → groupe grossiste
  → défaut 7 jours ;
- **déduction de l'encours de commande** (`OrderLineRepository.findPendingQtyByProduitIds`) ;
- **classes de criticité A–E** paramétrables (coefficient, profondeur d'historique, limite de
  péremption), alimentées par une **classification ABC/Pareto** en batch ;
- **exclusions produit** datées et motivées, **budget mensuel** de commande (`BudgetCommandeDTO`).

### 2.3 EDI PharmaML

Couverture inhabituellement complète pour un logiciel de cette taille : envoi (commande normale ou
**exceptionnelle**), renvoi, accusés de réception, **ruptures signalées** par le grossiste,
**substitutions proposées** à accepter ou refuser ligne à ligne, historique des envois, **demande
de disponibilité multi-grossistes** (`demanderDisponibiliteMulti`), et envoi des bons de retour.
Une commande soumise est verrouillée à l'écran (`isLocked`).

### 2.4 Réception

| Fonction | Où |
|---|---|
| Saisie unitaire, en masse, séquentielle | `commande-received`, `reception-sequential` |
| Scan CIP et **DataMatrix** (lot + péremption extraits automatiquement) | `processScanReception`, `DataMatrixParserServiceImpl` (716 l.) |
| Unités gratuites (UG), TVA, remise, net | colonnes de la grille de réception |
| **Détection des écarts de prix d'achat** | `findLignesAvecEcartPrix`, seuil `APP_SEUIL_VARIATION_PRIX` (20 % par défaut) |
| **Historique des 24 derniers prix** par produit fournisseur | `getPriceHistory` |
| PCB, stock avant / après, couverture | colonnes de la grille |
| Prévisualisation du rangement rayon → réserve | `getPutawayPreview`, `autoExecuteOverflowForProducts` |
| Étiquettes du bon, étiquettes de lot | `EtiquetteLotExportService` |
| Bon d'entrée diverse (hors commande) | `BedServiceImpl`, `MotifBed` (8 motifs) |

### 2.5 Aval de la chaîne

Rapprochement facture fournisseur avec écart (`ReconciliationFactureFournisseur`), retours
fournisseur (depuis un lot périmé, groupés sur plusieurs lots, retour complet d'une commande),
avoirs fournisseur, comptes fournisseurs avec vieillissement et règlements (`AccountsPayable`).

**Suivi RFA** : `RemiseRfaService` calcule le CA annuel par fournisseur sur les commandes `CLOSED`,
le compare à `palierRfa`, estime la remise au taux `tauxRfa`, la rapproche des avoirs fournisseur
remboursés et alerte à partir de 80 % du palier. Exposé par `RemiseRfaResource` et par un écran de
`features/finances` — mais nulle part dans l'écran de commande, voir §7 vague 2.

### 2.6 La dette client pilote la réception — le circuit « commande client »

**Ce circuit existe, et il est bien construit.** Il ne porte pas le nom de « commande client » mais
celui d'**avoir client**, et il relie le comptoir à la réception de bout en bout :

1. **Au comptoir** — une ligne de vente servie partiellement laisse un reste :
   `salesLine.setQuantityAvoir(quantityRequested - quantitySold)`, puis
   `createAvoirsFromSale(...)` si ce reste est positif (`SalesLineServiceImpl.java:352-355`).
   L'`AvoirClient` créé porte le client, le produit, la quantité due, le montant, une référence
   unique, une date d'expiration et le statut `OUVERT`.
2. **À la réception** — `StockEntryServiceImpl.java:355` appelle `linkCommandeToAvoirs(commande)`,
   qui rapproche les avoirs ouverts sans commande des produits du bon réceptionné et les y rattache.
3. **Notification** — `prevenirLesClients` enregistre l'envoi en `afterCommit` (une réception
   annulée n'annonce rien), puis `AvoirClientNotificationService.notifierProduitsDisponibles`
   envoie e-mail (Brevo) et/ou SMS **sous réserve du consentement du client par canal**
   (`CanalConsentement`) et de l'activation du paramètre.
4. **Clôture** — `cloturerAvoir` avec deux modes : `RETOUR_PRODUIT` (remise des unités, totale ou
   partielle, avec contrôle que le stock couvre la dette) ou imputation du montant. Un job
   (`AvoirExpirationJob`) passe les avoirs non retirés en `EXPIRE`.

C'est plus abouti que la simple « réservation » attendue d'un logiciel d'officine : le lien est
automatique à la réception, la notification est multicanal et respecte le consentement, et la
clôture raisonne en unités et non en montant.

**La lacune résiduelle n'est donc pas la fonction, c'est son absence côté commande.** Aucune
référence à `AvoirClient` n'existe dans `service/stock/` (suggestions) ni dans
`SemoisCalculationService`, et rien dans `features/commande/` ne l'affiche. Conséquences
concrètes :

- la quantité due aux clients **n'entre pas** dans le calcul de la quantité à commander : le
  réassort raisonne sur `stock_objectif − stock_actuel`, sans le passif client ;
- l'écran de saisie ne dit pas « 3 clients attendent ce produit » ;
- aucun écran ne liste les avoirs ouverts **dont le produit n'est dans aucune commande** — or c'est
  exactement la question du matin : « qu'ai-je promis que je n'ai pas encore commandé ? » ;
- rien ne priorise ces lignes dans les suggestions ni ne les marque comme urgentes.

Le mécanisme est donc **réactif** (il exploite la réception) mais pas **proactif** (il ne déclenche
ni n'oriente la commande).

### 2.7 Pilotage

`CommandeDashboardDTO` (commandes en attente / à réceptionner / envois PharmaML en attente),
`ResponsableCommandeDashboardService` (alertes stock, péremptions, rotation, analyse ABC,
performance fournisseurs), `getStatsService` (taux de service et délai moyen de livraison par
fournisseur sur 30/60/90 jours), `mv_supplier_performance`, exports CSV et PDF des commandes et des
suggestions.

---

## 3. Réponses aux questions posées

### 3.1 Répond-il aux besoins courants de l'officine ?

**Oui pour l'essentiel.** Constituer une commande, la passer, la recevoir, contrôler ce qui arrive,
traiter les manquants, retourner, se faire créditer, payer, mesurer le fournisseur, prévenir le
client qui attend : tout le cycle quotidien est couvert, et plusieurs maillons (DataMatrix,
substitutions EDI, écarts de prix, avoirs client notifiés) sont au niveau des logiciels de
référence.

**Deux besoins courants restent absents :**

| Besoin | État |
|---|---|
| « Combien me manque-t-il pour atteindre le franco de port / le palier de remise ? » | **Partiel.** Le palier RFA est bien suivi (`RemiseRfaService`, §2.5) mais dans un écran financier consulté après coup, jamais dans la commande en cours de saisie. Le franco de port, lui, n'existe pas : aucun champ sur `Fournisseur`. |
| « Qu'ai-je promis à des clients que je n'ai pas encore commandé ? » | **Absent** — voir §2.6. La donnée existe, elle n'est pas exploitée du côté commande. |

> **Hors périmètre, décidé :** les horaires de passation et l'heure de coupe des grossistes ne sont
> pas à porter par le logiciel. Le gestionnaire de stock les connaît, et `frequenceCommandeJours`
> suffit au calcul du stock de rotation.

### 3.2 Répond-il à la chaîne d'approvisionnement ?

**Non entièrement, et le défaut est structurel : `REQUESTED` recouvre deux états métier
incompatibles — « en cours de frappe » et « envoyée au grossiste ».** La commande naît en
`REQUESTED` dès la première ligne (`Commande.java:105`, valeur par défaut). Trois conséquences en
cascade :

1. **L'encours ment.** `findPendingQtyByProduitIds` (`OrderLineRepository.java:139-152`) somme les
   lignes des commandes `REQUESTED` et les présente comme « en cours de livraison » — son propre
   Javadoc écrit « envoyée au fournisseur ». Une commande entamée hier et oubliée éteint les
   suggestions SEMOIS sur ses produits. Le remède au surstock devient une cause de rupture. Même
   lecture dans `SuggestionLineRepositoryCustomImpl.java:211` et `EtatProduitServiceImpl.java:49`.
2. **Le taux de service est faussé.** `fetchStatsService`
   (`CommandeRepository.java:208-224`) calcule le délai moyen comme
   `AVG(updated_at − order_date)` sur les commandes `CLOSED`, sans savoir quand la commande est
   partie. Une commande saisie lundi, envoyée jeudi, livrée vendredi impute quatre jours de délai au
   grossiste au lieu d'un.
3. **Hors PharmaML, rien ne dit si la commande est partie.** `hasBeenSubmittedToPharmaML` ne couvre
   que la voie EDI. Pour un laboratoire en direct (export PDF/CSV, mail, téléphone), aucune trace
   d'envoi, donc aucun moyen de distinguer l'oubli du retard.

**Aucune alerte de retard de livraison.** `delaiLivraisonJours` est paramétré et consommé par le
calcul SEMOIS, mais jamais confronté à la date de commande pour dire « cette commande devait
arriver avant-hier ». `joursCritique` existe mais sert au vieillissement des **dettes**
(`AccountsPayableServiceImpl.java:390`), pas aux livraisons. `CommandeDashboardDTO` ne porte que
trois compteurs et deux listes — alors que le cahier de recette ACH-10 promet « en retard selon
échéance ». Le pharmacien doit ouvrir les commandes une par une.

**PCB et quantité minimale non opposés à la saisie** — **corrigé**, voir §7.1.
`FournisseurProduit.qteColis` et
`qteMinimaleCommande` existent, sont affichés en réception et portés par le modèle de suggestion —
mais `createOrUpdateOrderLine` (`CommandServiceImpl.java:182`) ne les lit pas, et aucun arrondi
n'existe côté génération de suggestion (`grep qteColis` sur `SuggestionProduitServiceImpl` : zéro
occurrence). Commander 7 unités d'un produit livré par boîte de 12 produit un écart systématique à
la réception, qu'il faut ensuite justifier dans le rapprochement facture. Le paramétrage existe, la
contrainte n'est pas appliquée.

**Rollback de réception inopérant** — déjà identifié et volontairement masqué (cahier ACH-09,
`hidden: true`) : l'endpoint rebascule le statut en `REQUESTED` sans retirer le stock injecté. Une
réception fautive ne se corrige donc que par retour fournisseur ou ajustement d'inventaire, et
l'historique de la commande reste faux.

### 3.3 Facilite-t-il la saisie et la commande ?

**Oui pour les parcours balisés.** Les six portes d'entrée évitent la ressaisie dans la grande
majorité des cas ; l'import grossiste et l'import de suggestion couvrent le gros du volume. La
grille AG Grid édite en un clic, navigation verticale après édition, suppression groupée. Les
bandeaux contextuels (verrou PharmaML, reliquat, montant élevé) sont au bon endroit. L'écran affiche
les deux chiffres qui engagent l'officine : montant d'**achat** et montant de **vente**.

**Trois frictions :**

- **Un seul raccourci clavier** (`F9` créer le bon, `Échap`). Rien pour le fournisseur, la recherche
  produit, ni le retour automatique au champ de recherche après ajout : la saisie en rafale reste à
  la souris.
- **Pas d'aide à la décision à la ligne.** Au moment de taper la quantité, la grille ne montre ni
  la VMM, ni la couverture en jours, ni l'encours déjà commandé ailleurs, ni le nombre de clients en
  attente. La colonne « Couverture » n'existe qu'en réception. Le pharmacien saisit de mémoire, ou
  garde l'écran des suggestions ouvert à côté.
- **Aucune remise d'achat, nulle part** — mais ce n'est peut-être pas une lacune. `discountAmount`
  existe sur la commande et sur la ligne et reste à `0` partout, les colonnes « Remise » et « Net »
  de la réception sont masquées et non éditables, `OrderLine.netAmount` n'est jamais calculé. Le
  prix du médicament étant administré en zone UEMOA, la remise ligne à ligne n'a probablement pas
  cours : les avantages réels sont les **unités gratuites** et la **RFA**, tous deux supportés.
  Usage à confirmer, voir la question ouverte en §7 vague 2.

### 3.4 Synthèse

| Axe | Note | Commentaire |
|---|---|---|
| Création / saisie de commande | ●●●●○ | Six portes d'entrée, import grossiste, fusion ; manque les aides à la décision et les raccourcis |
| Calcul du réassort | ●●●●● | SEMOIS est au niveau du marché, voire au-dessus |
| Suivi de ce qu'on attend du grossiste | ●●○○○ | **Point faible.** Pas de statut d'envoi, pas de relance de retard |
| Conditions d'achat (PCB, franco, UG, RFA) | ●●●●○ | PCB et quantité minimale désormais opposés à la saisie (§7.1) ; franco inexistant, seule lacune restante. UG et RFA — les deux mécanismes réels sous prix administré — sont bien supportés, le RFA hors écran de commande |
| Réception et contrôle | ●●●●● | DataMatrix, écarts de prix, historique, séquentiel, rangement |
| Aval (retours, avoirs, facture, règlements) | ●●●●○ | Complet ; rollback de réception manquant |
| Circuit client en attente | ●●●○○ | Mécanisme solide, mais invisible depuis la commande |
| Pilotage | ●●●○○ | Nombreux indicateurs, mais pas de notion de retard ; dashboard non paginé |

---

## 4. Défauts techniques ponctuels

| # | Défaut | Localisation |
|---|---|---|
| D1 | `fusionner()` ne vérifie **ni le fournisseur ni le statut**. La règle « même grossiste » n'existe que dans l'UI (explicite dans `ach-05`). Un appel API direct mélange deux répartiteurs dans une commande. | `CommandServiceImpl.java:315` |
| D2 | `createReliquat()` **n'est pas idempotent** : aucune recherche d'un reliquat existant par `reliquatDeCommandeId`. Deux clics produisent deux commandes de solde, qui comptent deux fois dans l'encours. | `CommandServiceImpl.java:1189` |
| D3 | `getDashboard()` fait **deux `findAll` sans pagination** sur `commande`, trie en mémoire puis tronque à 20 ; les totaux viennent de `list.size()`. Tient sur le jeu de démo, pas sur trois ans d'historique. | `CommandeDataServiceImpl.java:265-307` |
| D4 | `deleteAll(List<CommandeId>)` supprime sans aucun contrôle de statut : une commande déjà réceptionnée est supprimable par l'API. | `CommandServiceImpl.java:346` |
| D5 | `rollback()` rebascule le statut sans reprise de stock (connu, endpoint non exposé). | `CommandServiceImpl.java:273` |
| D6 | `OrderLine.receiptDate` porte un `//TODO a supprimer` et reste renseigné : deux sources de date de réception coexistent (`Commande.receiptDate` et `OrderLine.receiptDate`). | `OrderLine.java:90` |
| D7 | La vue `v_semois_suggestion` et le calcul Java **ne somment pas le même stock** — voir §6.3. | `V1.7.5`, `SemoisCalculationService.java:645` |

---

## 5. Chantier 1 — Distinguer la commande en saisie de la commande envoyée

C'est le correctif à plus fort effet de levier : il débloque à lui seul l'encours juste, le taux de
service juste et la relance de retard. Mais il touche un enum partagé, six objets SQL et une
contrainte de table. Cette section mesure le rayon d'impact avant de choisir la stratégie.

### 5.1 Contrainte n° 1 — l'enum `OrderStatut` est partagée par trois flux

`Commande` sert de table unique à trois objets métier, discriminés par `type`
(`TypeDeliveryReceipt` : `ORDER` / `DIRECT`) et par `motifBed` (non nul pour un BED) :

| Flux | Discriminant | Usage de `REQUESTED` |
|---|---|---|
| Commande fournisseur | `type = ORDER`, `motifBed IS NULL` | « en saisie **ou** envoyée » — l'ambiguïté à lever |
| Bon d'entrée diverse (BED) | `motifBed` non nul | **déjà interprété comme brouillon** : `bed-home.component.ts:144` fait `activeTab() === "BROUILLON" ? "REQUESTED" : "CLOSED"`, et `bed-detail.component.ts:58` nomme `isBrouillon` le test `orderStatus === "REQUESTED"` |
| Retour / bon direct | `type = DIRECT` | statut de travail |

Conséquence : **ajouter une valeur à l'enum ne peut pas être un changement local.** Tout code qui
teste `REQUESTED` pour un BED ou un retour devrait rester inchangé, tandis que le même test devrait
changer de sens pour une commande. Cinq des six gardes de `BedServiceImpl`
(lignes 154, 189, 213, 228, 276) sont dans ce cas.

### 5.2 Contrainte n° 2 — `order_status` est dans la clé d'unicité

`Commande.java:40-43` :

```java
@UniqueConstraint(columnNames = {"receipt_reference", "fournisseur_id", "order_date", "order_status"})
```

Un statut supplémentaire élargit mécaniquement l'espace d'unicité : la même
`receipt_reference` pourrait exister en `BROUILLON` **et** en `ENVOYEE` pour le même fournisseur et
la même date. Ce n'est pas un blocage, mais c'est une régression silencieuse de contrainte.

### 5.3 Impacts base de données

#### Contrainte CHECK

`V1.3.7__stock_virtuel_rupture_vmm.sql:68-79` énumère les valeurs permises :

```sql
ALTER TABLE commande DROP CONSTRAINT commande_order_status_check;
ALTER TABLE commande ADD CONSTRAINT commande_order_status_check
  CHECK (order_status IN ('REQUESTED', 'RECEIVED', 'CLOSED', 'ARCHIVED'));
```

Toute nouvelle valeur exige une migration qui reconstruit cette contrainte. À défaut, les
insertions échouent en production avec une erreur de contrainte sans rapport apparent avec le
changement.

#### Objets SQL à revoir

| Objet | Fichier | Lecture de `order_status` | Impact |
|---|---|---|---|
| `mv_supplier_performance` (vue matérialisée) | `V1.9.6:26` | `IN ('RECEIVED','CLOSED')` | Aucun changement de filtre, mais `delivery_days` = `receipt_date − order_date` doit devenir `receipt_date − date_envoi` pour que la performance fournisseur soit juste |
| `tableau_pharmacien_commandes_report(date, date, text)` | `V1.7.0:48-82` | `= p_order_status` | Le paramètre est un texte libre passé par l'applicatif : la nouvelle valeur devient appelable, les rapports existants doivent être audités |
| `tableau_pharmacien_commandes_mois_report(date, date, text)` | `V1.7.0:93-127` | `= p_order_status` | Idem |
| `get_product_order_summary(date, date, text, …)` | `V1.0.5:2016-2067` | `d.order_status = p_statut` | Historique d'achat par produit — doit exclure les brouillons |
| `get_product_order_summary_monthly(date, date, text, …)` | `V1.0.5:2069-2099` | `d.order_status = p_statut` | Idem |
| `pilotage_recalculer_achats(date, date)` | `V2.1.34:183` | `IN ('RECEIVED','CLOSED')` | Pas d'impact (les brouillons ne sont ni reçus ni clôturés) — à confirmer par test |
| `v_semois_suggestion` | `V1.7.5` | **aucune** référence à `order_status` | Pas d'impact direct ; l'encours est calculé côté Java |

Aucun **trigger** sur `commande` ni sur `order_line` (vérifié sur l'ensemble des migrations) :
c'est une bonne nouvelle, le rayon d'impact SQL se limite aux objets ci-dessus.

#### Index

`order_status_index` existe déjà (`Commande.java:46`). Un filtre `date_envoi IS NULL` fréquent
justifierait un index partiel `(fournisseur_id, order_date) WHERE date_envoi IS NULL`.

### 5.4 Impacts Java — 25 occurrences, 10 fichiers

| Fichier | Lignes | Nature | Sens attendu après scission |
|---|---|---|---|
| `Commande.java` | 105 | valeur par défaut | → brouillon |
| `CommandServiceImpl.java` | 275, 409, 445, 503, 579, 1080, 1202 | affectations à la création (manuelle, suggestion, SEMOIS, import, reliquat) + garde `importSuggestion` l. 1120 | → brouillon à la création ; l. 1120 doit refuser l'import dans une commande **envoyée** |
| `OrderLineRepository.java` | 147 | `findPendingQtyByProduitIds` | → **envoyée uniquement** (correctif central) |
| `SuggestionLineRepositoryCustomImpl.java` | 211 | encours dans les lignes de suggestion | → envoyée uniquement |
| `EtatProduitServiceImpl.java` | 49, 60, 71 | `canSuggere` — un produit déjà commandé ne resuggère pas | → envoyée uniquement (sinon un brouillon bloque la suggestion) |
| `SuggestionProduitServiceImpl.java` | 320 | `IN ('REQUESTED','RECEIVED')` — montant déjà commandé du mois (budget) | **à décider** : un brouillon engage-t-il le budget ? Recommandation : non |
| `CommandeDataServiceImpl.java` | 268 | dashboard | → séparer les deux compteurs |
| `ResponsableCommandeDashboardServiceImpl.java` | 70, 81 | SQL natif, alertes | → envoyée uniquement pour « en cours de livraison » |
| `CaissierDashboardServiceImpl.java` | 232 | compteur | à qualifier |
| `BedServiceImpl.java` | 88, 154, 189, 213, 228, 276 | gardes BED | **inchangé** — le BED garde `REQUESTED` = brouillon |
| `CommandeRepository.java` | 208-224 | `fetchStatsService` | `AVG(updated_at − order_date)` → `AVG(receipt_date − date_envoi)` |

### 5.5 Impacts front

| Fichier | Nature |
|---|---|
| `shared/model/enumerations/order-statut.model.ts` | l'enum front ne liste que `CLOSED`, `RECEIVED`, `REQUESTED` — déjà désynchronisé du back (`ARCHIVED` manquant) |
| `commande-requested-home.component.ts:162` | filtre `orderStatuts: ['REQUESTED']` → deux onglets ou un filtre « non envoyées » |
| `commande-requested.component` | le verrou d'édition dépend aujourd'hui de `hasBeenSubmittedToPharmaML` ; il doit dépendre de l'état d'envoi, tous canaux confondus |
| `commande-detail.component.ts` | aiguillage REQUESTED → saisie / RECEIVED → réception |
| `suggestions-unified.component.ts:44,129` | badge « Commandes à passer » = `countByStatut("REQUESTED")` → doit compter les brouillons |
| `bon-entree-diverse/*` | **inchangé** si `REQUESTED` reste le brouillon du BED |

### 5.6 Impacts recette

Parcours à reprendre : `ach-01` (création), `ach-07` (produit déjà en commande — l'assertion
repose sur l'encours), `ach-10` (tableau de bord), `ach-16`/`ach-17` (commander une suggestion),
`ach-28` à `ach-31` (PharmaML), `ach-73` (importer une suggestion), `ref-50` (seuil recommandé).
Le jeu de démo doit être enrichi d'au moins une commande brouillon et une commande envoyée en
retard, sans quoi aucun parcours ne couvrira la distinction.
Rappel : réinitialiser la démo (base **et** schéma) avant la campagne, backend e2e sur 9081.

### 5.7 Deux stratégies

#### Option A — ajouter une valeur à l'enum (`BROUILLON` ou `ENVOYEE`)

- Modélisation explicite, lisible dans la base.
- Mais : migration de la contrainte CHECK, élargissement de la clé d'unicité, enum partagée avec le
  BED qu'il faut alors désolidariser (ou accepter deux sémantiques pour une même valeur selon
  `type`), reprise des 25 sites Java avec un risque d'oubli silencieux, deux enums front à
  resynchroniser, et une bascule des données existantes à décider (tout le `REQUESTED` historique
  est-il envoyé ou brouillon ?).

#### Option B — colonne `date_envoi` + `canal_envoi`, statut inchangé — **recommandée**

```sql
ALTER TABLE commande ADD COLUMN date_envoi      timestamp;
ALTER TABLE commande ADD COLUMN canal_envoi     varchar(20);  -- EDI, MAIL, PDF, TELEPHONE, AUTRE
ALTER TABLE commande ADD COLUMN envoi_user_id   integer REFERENCES app_user(id);
CREATE INDEX commande_non_envoyee_idx ON commande (fournisseur_id, order_date)
  WHERE date_envoi IS NULL;
-- Reprise : les commandes REQUESTED antérieures sont réputées envoyées à leur date de commande.
UPDATE commande SET date_envoi = order_date::timestamp, canal_envoi = 'AUTRE'
 WHERE order_status = 'REQUESTED' AND type = 'ORDER' AND motif_bed IS NULL;
```

Ce que l'option B apporte :

- **enum, contrainte CHECK et clé d'unicité intactes** — zéro impact sur le BED et les retours ;
- chaque site impacté devient un **ajout de prédicat** (`AND c.date_envoi IS NOT NULL`) et non une
  réécriture de condition : la revue est mécanique et le compilateur ne peut pas masquer un oubli,
  puisqu'on part de la liste exhaustive du §5.4 ;
- la **date** d'envoi est ce dont on a réellement besoin pour l'alerte de retard
  (`date_envoi + delai_livraison_jours < today`) et pour un délai de livraison honnête — un statut
  seul ne l'aurait pas donné, il aurait fallu l'ajouter de toute façon ;
- le **canal** résout le trou hors PharmaML : une commande partie par mail ou par téléphone est
  tracée comme telle ;
- `hasBeenSubmittedToPharmaML` devient redondant et pourra être retiré dans un second temps
  (`canal_envoi = 'EDI'`).

Le statut métier affiché à l'écran (« Brouillon » / « Envoyée ») se dérive alors d'un champ calculé
du DTO, sans toucher la base.

### 5.8 Ordre des travaux

1. Migration Flyway : trois colonnes + index partiel + reprise des données.
2. `Commande` + `CommandeLiteDTO`/`CommandeWrapperDTO` : champs et dérivé `estEnvoyee`.
3. Service d'envoi : un point de passage unique qui horodate et renseigne le canal, appelé par
   l'envoi PharmaML **et** par l'export PDF/CSV (avec confirmation « marquer comme envoyée ? »),
   plus une action manuelle « marquer comme envoyée ».
4. Les quatre requêtes d'encours (`OrderLineRepository:147`,
   `SuggestionLineRepositoryCustomImpl:211`, `EtatProduitServiceImpl`, `SuggestionProduitServiceImpl:320`).
5. `fetchStatsService` et `mv_supplier_performance` : délai sur `date_envoi`.
6. Dashboard : compteurs « brouillons », « envoyées », « en retard ».
7. Front : onglets, verrou d'édition, badge des suggestions.
8. Parcours e2e et jeu de démo.

---

## 6. Chantier 2 — Le réassort est mono-magasin

### 6.1 Ce qui est déjà multi-emplacement, et bien fait

À l'intérieur d'une officine, le modèle gère plusieurs emplacements et le réassort en tient compte :

- `Storage` porte un `StorageType` et appartient à un `Magasin` ;
- `StorageType` qualifie chaque emplacement : `PRINCIPAL` (rayon, vendable, réassort suggéré),
  `SAFETY_STOCK` (réserve, non vendable, réassort suggéré), `TOXIQUE` (vendable, **hors** réassort),
  `QUARANTAINE` (non vendable, **hors** réassort) ;
- `StockProduit` est unique par couple (produit, storage), avec `seuilMini` et `stockReassort`
  propres à l'emplacement ;
- le réassort rayon ↔ réserve existe (`SuggestionReassortService`, `RepartitionStockService`,
  `autoPutawayRayonToReserve` en FEFO, prévisualisation à la réception,
  `autoExecuteOverflowForProducts`).

**Le titre « monoposte de stockage » de la première lecture était donc inexact :** l'intra-officine
est couvert. Le vrai sujet est le **multi-magasin**.

### 6.2 Ce qui est mono-magasin

`EntityConstant.DEFAULT_MAGASIN = 1`, et le calcul SEMOIS s'y adosse en dur :

```java
// SemoisCalculationService.java:645
private int getStockActuel(Integer produitId) {
    Integer stock = stockProduitRepository.findTotalQuantityByMagasinIdIdAndProduitId(
        EntityConstant.DEFAULT_MAGASIN, produitId);
    return stock != null ? stock : 0;
}
```

Même constante dans `SemoisBatchJobService`. Ailleurs dans l'application, le magasin est au
contraire résolu dynamiquement (`storageService.getUser().getMagasin().getId()`, par exemple dans
`AvoirClientDocumentServiceImpl:173`) : **le modèle de données est multi-magasin, le réassort ne
l'est pas.** À noter aussi `search_produits_json(qtext, magasin integer DEFAULT 1, …)`
(`V1.0.5:2385`), même raccourci côté SQL.

### 6.3 Un désaccord vue / Java — **corrigé** (migration `V2.1.48`)

C'était le point le plus important de cette section, car il produisait déjà de faux chiffres en
mono-magasin. Ce qui suit décrit le défaut, puis la règle retenue et ce qui a été écrit.

#### Le défaut

La vue `v_semois_suggestion` (`V1.7.5:19`) calculait le stock actuel ainsi :

```sql
COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) AS stock_actuel
...
LEFT JOIN stock_produit sp ON sp.produit_id = p.id
```

**Aucun filtre** : ni sur le magasin, ni sur le type d'emplacement. La vue sommait donc le rayon, la
réserve, **les toxiques et la quarantaine**, et tous les magasins s'il en existait plusieurs.

Le chemin Java (`getStockActuel`) filtrait, lui, sur le magasin 1 — mais pas davantage sur le type
d'emplacement.

Résultat : la **liste** des suggestions (servie par la vue) et le **détail** calculé en repli ne
répondaient pas à la même question, et les deux comptaient le stock en quarantaine comme s'il
couvrait la demande, ce qui réduisait la quantité suggérée et retardait la commande d'autant.

#### La règle retenue

Une première version de ce document proposait `storage_type IN ('PRINCIPAL','SAFETY_STOCK')`, sur
une **lecture erronée** de `StorageType.isReassortSuggere()` : ce drapeau qualifie le réassort
interne rayon ↔ réserve, pas la couverture de la demande. L'appliquer aurait exclu l'armoire à
toxiques, qui est vendable (`TOXIQUE` porte `vendable = true`) et sert les ordonnances de
stupéfiants et de liste I/II — on aurait surcommandé précisément les produits qu'on ne veut pas en
excédent.

Règle arbitrée, **seule la quarantaine est écartée** :

| Emplacement | Compte ? | Pourquoi |
|---|---|---|
| `PRINCIPAL` (rayon) | oui | stock dispensable |
| `SAFETY_STOCK` (réserve) | oui | non dispensable sur place, mais alimente le rayon |
| `TOXIQUE` (toxiques) | oui | armoire vendable, elle sert les ordonnances |
| `QUARANTAINE` | **non** | attend un retour, une destruction ou un contrôle : ne servira aucune demande |

#### Ce qui a été écrit

| Fichier | Changement |
|---|---|
| `V2.1.48__semois_view_stock_couvrant_la_demande.sql` | vue SEMOIS v6 : jointure sur `storage` et `CASE WHEN st.storage_type <> 'QUARANTAINE'` dans `stock_actuel` **et** dans `quantite_a_commander` |
| `StorageType.java` | nouveau drapeau `couvreLaDemande` + `listerTypesCouvrantLaDemande()` — la règle est nommée une fois, au lieu d'être déduite d'un drapeau qui dit autre chose |
| `StockProduitRepository.java` | `findTotalQuantityCouvrantLaDemande(magasinId, produitId, types)`. L'ancienne `findTotalQuantityByMagasinIdIdAndProduitId` est **inchangée** : l'inventaire et la clôture d'un avoir client veulent bien compter tous les emplacements |
| `SemoisCalculationService.getStockActuel` | consomme la nouvelle méthode |
| `SemoisStockActuelIntegrationTest.java` | 4 tests sur PostgreSQL : égalité vue ↔ Java, quantité à commander non éteinte, lecture tous emplacements préservée, règle des emplacements |

Le jeu d'essai du test pose **100 unités en quarantaine contre 13 ailleurs** : si l'un des deux
chemins comptait la quarantaine, la quantité à commander tomberait à zéro et l'écart serait visible
plutôt que silencieux. Les deux chemins rendent 13, l'ancienne lecture rend toujours 113.

### 6.4 Scénarios métier à trancher avant de coder

Le multi-magasin n'est pas une seule fonctionnalité mais trois, d'ampleurs très différentes :

| Scénario | Besoin | Ampleur |
|---|---|---|
| **S1 — Officine unique, plusieurs emplacements** | Déjà couvert ; reste à corriger §6.3 | Faible |
| **S2 — Officine avec dépôt ou annexe** (un seul établissement juridique, deux lieux) | Un réassort par magasin, et un transfert inter-magasin plutôt qu'une commande quand l'autre lieu a du stock | Moyenne |
| **S3 — Groupement / plusieurs officines** | Commande groupée pour obtenir un palier, réassort par officine, consolidation des suggestions, cloisonnement des données | Forte — relève d'une architecture multi-tenant, pas d'un correctif de réassort |

**Recommandation : traiter S1 maintenant, instrumenter pour S2, exclure S3 du périmètre** tant
qu'aucune installation ne le demande. S3 touche la licence, la sécurité, le cloisonnement des
caisses et la consolidation comptable : ce n'est pas un chantier du module commande.

### 6.5 Plan pour S1 + S2

**Phase 1 — corriger les faux chiffres (S1, aucune fonctionnalité nouvelle) — ✅ faite**

1. ✅ Migration `V2.1.48` : vue SEMOIS v6, quarantaine exclue (voir §6.3).
2. ✅ Chemin Java aligné via `findTotalQuantityCouvrantLaDemande` et `StorageType.couvreLaDemande`.
3. ✅ `SemoisStockActuelIntegrationTest` : égalité vue ↔ Java vérifiée sur PostgreSQL.
4. ⬜ **Reste à faire** : vérifier l'impact sur les suggestions du jeu de démo. Les quantités
   changent dès qu'un produit a du stock en quarantaine — c'est voulu, mais les parcours e2e qui
   assertent une quantité suggérée doivent être relus. Aujourd'hui aucun chemin applicatif ne
   remplit la quarantaine (seul l'ajustement manuel le permet), l'impact devrait donc être nul sur
   la démo : à confirmer par une campagne après réinitialisation.

**Phase 2 — retirer la constante en dur (S2, sans changer le comportement à un magasin)**

5. `getStockActuel(produitId, magasinId)` ; le magasin devient un paramètre du calcul.
6. `SemoisConfiguration` : décider si la configuration est par (produit) ou par (produit, magasin).
   Un stock objectif n'a de sens que par lieu de stockage — donc clé composée, avec une migration
   qui duplique les configurations existantes sur le magasin 1.
7. Le batch SEMOIS itère sur les magasins actifs au lieu de `DEFAULT_MAGASIN`
   (`SemoisBatchJobService`, `recalculateAllConfigurations`).
8. La vue expose `magasin_id` et les écrans de suggestion portent un sélecteur de magasin — masqué
   s'il n'y en a qu'un, pour ne pas alourdir l'écran de la très grande majorité des installations.
9. `search_produits_json` : retirer le `DEFAULT 1` et rendre le paramètre obligatoire côté appelant.

**Phase 3 — transfert plutôt que commande (S2)**

10. Avant de suggérer une commande, vérifier si un autre magasin de la même officine dispose d'un
    excédent au-delà de son stock objectif ; proposer alors un **transfert inter-magasin**. Le
    mouvement existe déjà comme motif (`MotifBed.TRANSFERT_ENTRANT`) : il reste à le relier à la
    suggestion.

**Garde-fou :** aucune de ces phases ne doit modifier le comportement observable d'une installation
à un seul magasin, hors correction des quantités de la phase 1. C'est le critère d'acceptation des
phases 2 et 3.

---

## 7. Autres axes d'amélioration, par ordre de valeur

### Vague 1 — réparer la boucle d'approvisionnement

1. ~~**Tracer l'envoi**~~ — **fait** (§7.0), option B retenue : colonnes `date_envoi`,
   `canal_envoi`, `envoi_user_id`, migrations `V2.1.48` et `V2.1.49`.
2. ~~**Alerte de retard**~~ — **fait** (§7.0), `date_envoi + delai_livraison_jours < today`,
   trois compteurs dans `CommandeDashboardDTO` et pastille dans la liste.
3. ~~**Opposer PCB et quantité minimale à la saisie**~~ — **fait** (§7.1). Deux décisions ont
   été prises avant d'écrire : **ajuster et informer** plutôt que bloquer, et un périmètre
   restreint aux quantités dont l'officine décide. Le « blocage sous `qteMinimaleCommande` » que
   proposait une première version de ce document a été écarté : traiter deux contraintes de même
   nature de deux façons différentes n'était pas justifiable, et un refus au comptoir oblige à
   ressaisir.
4. ~~**Corriger le stock du réassort**~~ — **fait**, migration `V2.1.48` (§6.3).

### 7.0 A1 et A2 — tracer l'envoi, signaler le retard (fait)

**Le défaut.** `order_status = REQUESTED` recouvrait deux situations opposées : un bon en cours de
saisie, et une commande partie chez le grossiste dont on attend la livraison. Rien ne les
distinguait, avec trois conséquences mesurables :

- l'encours du réassort comptait comme déjà commandé un bon qui n'était parti nulle part. Le
  calcul croyait du stock en route, suggérait moins, et la rupture arrivait ;
- le délai de livraison se mesurait depuis la date de **saisie**, imputant au grossiste le temps
  pendant lequel le bon dormait chez nous. Un bon préparé le lundi, transmis le jeudi, livré le
  vendredi donnait quatre jours au lieu d'un, et `performance_score` le sanctionnait pour notre
  propre retard ;
- aucune relance n'était possible, faute de savoir depuis quand on attend.

**Option B retenue** (§5.7) : horodater l'envoi plutôt qu'ajouter une valeur à `OrderStatut`.
L'énumération, sa contrainte CHECK et la clé d'unicité sont partagées avec les bons d'entrée
diverse et les retours, pour qui `REQUESTED` signifie déjà « brouillon ». Chaque requête impactée
devient alors un ajout de prédicat, non une réécriture de condition. La date est par ailleurs ce
dont on a réellement besoin : un statut seul n'aurait donné ni l'alerte de retard ni un délai
honnête.

**Migration `V2.1.48`.** Trois colonnes sur `commande`, un index partiel
`commande_non_envoyee_idx … WHERE date_envoi IS NULL`, une clé étrangère vers `app_user`. La table
étant **partitionnée** (RANGE sur `order_date`), la migration a été éprouvée sur une copie avant
d'être versionnée : `ADD COLUMN`, index partiel et clé étrangère se propagent correctement aux
partitions.

Pas de contrainte CHECK sur `canal_envoi` : l'énumération Java `CanalEnvoi` en est le seul
garde-fou, délibérément. `commande_order_status_check` (§5.3) a déjà montré qu'une telle
contrainte transforme l'ajout d'une valeur en migration de reconstruction.

**Reprise des données.** Toutes les commandes fournisseur existantes — 768 sur la base de recette,
tous statuts confondus — sont réputées envoyées à leur date de commande, canal `AUTRE`. Ce choix
**préserve le comportement actuel plutôt que de l'interpréter** : marquer d'office les `REQUESTED`
comme brouillons ferait chuter l'encours du jour au lendemain et déclencherait une vague de
suggestions sur des produits réellement en route. Les `RECEIVED` et `CLOSED` doivent de toute façon
porter une date d'envoi, sans quoi `mv_supplier_performance` perdrait tout son historique.

**Migration `V2.1.49`.** `mv_supplier_performance` recalcule `delivery_days` depuis `date_envoi`.
La reprise ayant posé `date_envoi = order_date`, les chiffres antérieurs sont inchangés : la mesure
ne devient exacte qu'à partir des commandes réellement tracées — convergence, pas rupture de série.
`CommandeRepository.fetchStatsService` suit le même changement, et ne mesure plus
`updated_at - order_date` mais `receipt_date - date_envoi`.

**Un seul point de passage.** `EnvoiCommandeService` est le seul à écrire la date, parce qu'elle
alimente trois mesures : encours, délai, retard. Si chaque chemin d'envoi l'écrivait à sa façon,
ces mesures divergeraient selon la façon dont l'officine travaille, et l'écart serait invisible.
Le marquage est **idempotent** : repasser par un envoi déjà tracé ne décale pas la date, sans quoi
un second téléchargement du bon rajeunirait l'attente et effacerait le retard.

Trois chemins y mènent, conformément à la décision prise :

| Chemin | Canal | Déclenchement |
|---|---|---|
| Envoi PharmaML | `EDI` | automatique — le seul envoi dont l'application est témoin de bout en bout |
| Export CSV / PDF | au choix | **proposé**, jamais imposé : on exporte un bon aussi bien pour l'envoyer que pour le relire |
| Action « Marquer comme envoyée » | au choix | manuel, pour le mail et le téléphone |

`EDI` n'est pas proposé dans le choix manuel : le canal s'horodate seul, et le choisir à la main
laisserait croire à une transmission dont l'application n'a aucune trace. Une commande marquée par
erreur se dé-marque (`DELETE …/envoi`) ; une commande déjà réceptionnée ne change plus d'état
d'envoi, son envoi étant attesté par la livraison.

**Les quatre requêtes d'encours** (le risque identifié au §8) comptent désormais les seules
commandes transmises — décision de l'utilisateur : « un brouillon n'engage rien ».

| Requête | Fichier | Changement |
|---|---|---|
| Encours du réassort | `OrderLineRepository.findPendingQtyByProduitIds` | `AND c.date_envoi IS NOT NULL` |
| Blocage de resuggestion | `EtatProduitServiceImpl.canSuggere` | nouvelle `countCommandesEnvoyeesEnAttente` |
| Encours affiché | `SuggestionLineRepositoryCustomImpl` | `AND c.date_envoi IS NOT NULL` |
| Budget du mois | `SuggestionProduitServiceImpl` | `RECEIVED` ou (`REQUESTED` et envoyée) |

Le prédicat retenu pour les lots multi-statuts est `date_envoi IS NOT NULL OR order_status <>
REQUESTED` : un brouillon ne compte que s'il est parti, tout ce qui a dépassé le brouillon compte
de toute façon. Formulation plus robuste qu'un simple test sur la date, qui aurait exclu une
réception dont l'envoi n'aurait jamais été marqué.

**A2 — l'alerte de retard.** `Commande.joursDeRetard(LocalDate, int)` est une règle d'entité, pure
et sans chargement différé : le délai lui est passé en paramètre plutôt que lu sur le fournisseur.
`Fournisseur.delaiLivraisonEffectif()` le résout — délai de l'agence, à défaut celui du grossiste
parent, à défaut une semaine. Une commande non envoyée ou déjà reçue ne rend jamais de retard : on
n'attend rien d'un bon qui dort chez nous, et une fois reçue le retard appartient à l'historique,
pas aux relances du jour.

`CommandeDashboardDTO` porte trois compteurs là où il n'y en avait qu'un — brouillons, envoyées,
en retard — plus la liste des retards, les plus en retard d'abord.

**Front.** La colonne d'état de la liste porte désormais `BROUILLON`, `ENVOYÉE` ou `RETARD N J`.
Deux actions au menu : « Marquer comme envoyée » (masquée si elle l'est déjà) et « Annuler
l'envoi » (masquée sinon). La modale de choix du canal utilise `app-pill-selector` du Design
System.

**Tests.** `CommandeRetardTest` (8 cas : décompte, jour d'échéance, brouillon, commande reçue,
héritage du délai agence → grossiste), `EnvoiCommandeServiceImplTest` (5 cas : horodatage,
idempotence, annulation, refus d'une commande réceptionnée, commande introuvable), et trois cas
ajoutés au tableau de bord. Parcours `ACH-88` au cahier de recette, et jeu de démonstration
enrichi : `05_commandes.sql` répartit désormais les commandes en attente en trois tiers —
brouillon, envoyée dans les temps, envoyée depuis trente jours — avec un garde-fou qui échoue si
l'un des trois états manque, car l'écran ne se recetterait alors que partiellement.

**Le verrouillage de l'édition — tranché : le verrou est étendu.** Une commande envoyée ne se
modifie plus à la main, quel qu'ait été le canal. Ce que le grossiste a reçu ne doit plus bouger
chez nous, sans quoi le bon imprimé et la commande en base cessent de dire la même chose.
L'objection — un marquage manuel est déclaratif, et le pharmacien peut vouloir corriger avant
confirmation — est levée par « Annuler l'envoi », qui rouvre la commande : le verrou n'enferme
personne.

Élargir `isLocked` tel quel aurait eu deux effets de bord, parce que ce même indicateur servait à
trois choses distinctes :

| Usage | Gouverné par | Pourquoi |
|---|---|---|
| Édition des lignes, ajout de produit, suppression, import de suggestion | **envoi, tous canaux** | ce sont les modifications à la main |
| Panneau PharmaML et ses actions | **EDI seul** | une commande partie par mail n'a aucune réponse EDI à afficher |
| Bouton « Importer réponse » | **EDI seul** | c'est l'étape normale après un envoi par mail ; la verrouiller casserait ce circuit |

D'où un second accesseur, `estSoumisePharmaml`, distinct du verrou. « Créer le bon » n'était déjà
pas verrouillé et ne l'est pas davantage : une commande envoyée est précisément celle qu'on
réceptionne. Dans la liste, « Supprimer » disparaît pour une commande partie — une livraison
arriverait sans bon en face.

**Le verrou reste côté écran.** Rien ne l'impose côté serveur, ni pour l'envoi ni, déjà, pour
PharmaML : la seule garde existante (`PharmaMlServiceImpl`) empêche un second envoi EDI, pas
l'édition. Étendre la règle jusqu'aux services d'écriture est un chantier à part, qui toucherait
les imports, les reliquats et la fusion de commandes.

---

### 7.1 A4 — PCB et quantité minimale opposés à la saisie (fait)

**Ce qui existait déjà, et que la première analyse avait manqué.**
`FournisseurProduit.appliquerColisage(int)` implémentait déjà la règle exacte —
`max(max(1, ceil(qty / colis) * colis), minimum)` — avec un test unitaire fourni
(`FournisseurProduitColisageTest`, 18 cas). **Personne ne l'appelait.** A4 n'était donc pas un
chantier de calcul mais de câblage : la règle avait été écrite puis laissée orpheline.

**Décision 1 — ajuster et informer, pas bloquer.** Une quantité non conforme est portée au multiple
supérieur du colis, ou au minimum fournisseur, et l'écran dit ce qui a changé et pourquoi. Un refus
obligerait à ressaisir ; une correction muette donnerait l'impression d'un logiciel qui change les
chiffres tout seul. Le report (`AjustementColisage`) n'est produit que si la quantité a bougé, et
voyage dans `CommandeLiteDTO` — nul le reste du temps, donc non sérialisé. Il ne porte **aucune
phrase** : le service nomme la contrainte qui a décidé (`ContrainteColisage.COLISAGE` ou
`QUANTITE_MINIMALE`) et les nombres, la formulation se compose dans l'écran. Une première version
fabriquait le libellé français dans le service, ce qui mêlait présentation et gestion.

**Décision 2 — le périmètre est ce dont l'officine décide.** Huit points d'entrée sont soumis à la
règle : première ligne d'une commande, ajout de ligne, cumul sur une ligne déjà présente, édition
dans la grille, commande rapide, reprise d'une sélection de suggestion, import d'une suggestion
dans une commande existante, urgents SEMOIS — plus la génération des suggestions, pour que l'écran
ne propose pas d'emblée une quantité que la commande changera.

En sont exclus, et c'est volontaire : l'**import de fichier grossiste**, la **réponse de commande**
et le **bon de livraison** — ce sont ses données, les réécrire ferait diverger notre bon de sa
réponse ; le **reliquat**, parce qu'arrondir 7 manquants à 12 ferait commander 5 unités que
personne n'attend ; la **fusion** de deux commandes ; et l'**édition manuelle d'une ligne de
suggestion**, qui porte déjà un drapeau `quantiteModifieeManuel` signifiant un choix assumé — la
règle s'y appliquera de toute façon au moment où la suggestion deviendra commande.

Cette frontière n'est pas qu'une intention : le test `nePasArrondirLeReliquat` la tient par son
contre-exemple, et vaut autant que les cas positifs.

**Ce que le pharmacien voit.** La colonne « Qté » de l'écran de saisie porte un badge bleu —
informatif, pas une alerte, puisque la quantité enregistrée est déjà conforme — qui nomme la
contrainte au survol. `OrderLineDTO` expose désormais `qteMinimaleCommande` à côté de `qteColis`,
qui y était déjà.

**Vérifications.** 169 tests au vert, dont 9 sur le service d'ajustement et 5 sur le câblage
(`CommandServiceImplTest.Conditionnement`). Typage TypeScript propre. **Impact e2e mesuré, pas
supposé** : les cinq produits nommés par les parcours d'achats ont tous un PCB de 1 et aucun
minimum, et les 32 lignes de suggestion du jeu de démo sont déjà conformes au colisage — aucun
parcours ne change de résultat. La contrainte n'est pourtant pas inerte : 702 des 1 691 lignes
fournisseur du jeu de démo portent un colis de 5 ou de 10.

### Vague 2 — l'amont contractuel

#### 5. Franco de port à la saisie, et palier RFA ramené dans l'écran de commande — **fait**

Le suivi RFA **existe déjà** : `RemiseRfaService` calcule le CA annuel par fournisseur sur les
commandes `CLOSED`, le compare à `palierRfa`, estime la remise au taux `tauxRfa`, la rapproche des
avoirs fournisseur remboursés et alerte à partir de 80 % du palier. Il est exposé par
`RemiseRfaResource` et par un écran de `features/finances`.

Ce qui manque n'est donc pas le calcul mais **le moment** : l'information vit dans un écran
financier qu'on ouvre après coup, pas dans le bandeau de totaux de la commande qu'on est en train
de saisir — seul instant où elle peut changer une décision.

**Un défaut à corriger d'abord — le CA est cumulé par agence, le palier vit sur le grossiste.**
`CommandeRepository.sumCaByFournisseur` groupe par `f.id`, c'est-à-dire par **agence**, puis
compare ce CA au palier résolu par `COALESCE(f.palierRfa, pf.palierRfa)` — donc, en pratique, au
palier du **groupe**. Mesuré sur la base de recette : aucune des 12 agences ne porte de palier,
les 4 grossistes en portent un, et chacun a 3 agences. Le pourcentage affiché est donc environ
trois fois trop bas, et l'alerte à 80 % ne peut quasiment jamais se déclencher.

| Agence | CA | Palier | % affiché |
|---|---|---|---|
| COPHARMED ABIDJAN ADJAME | 5 586 515 | 120 000 000 | 4,7 % |
| COPHARMED ABIDJAN PLATEAU | 15 142 075 | 120 000 000 | 12,6 % |
| COPHARMED SAN-PEDRO | 7 965 030 | 120 000 000 | 6,6 % |
| **COPHARMED (groupe réel)** | **28 693 620** | **120 000 000** | **23,9 %** |

**Décision de l'utilisateur : cumuler par groupe et corriger les deux écrans** — le bandeau de
saisie et l'écran financier `finances.remises-rfa` afficheront le même chiffre. La clé de
regroupement devient `COALESCE(pf.id, f.id)`, et le palier se lit `COALESCE(pf.palierRfa,
f.palierRfa)` : l'ordre du `COALESCE` s'inverse, le palier étant désormais une notion de groupe et
non d'agence. Constant par groupe, il peut rester dans le `GROUP BY` sans agrégat.

Deux travaux distincts :

- **Palier RFA dans la saisie** : afficher le palier dans le bandeau existant de
  `commande-requested`, en réutilisant `RemiseRfaService`. Trois points tranchés à la conception :
  - le nouvel endpoint ne peut pas passer par `RemiseRfaResource`, qui exige le droit
    `facturation.remises-rfa` : la personne qui saisit une commande ne l'a pas forcément. Il va
    dans `CommandeResource`, à côté de `/commandes/fournisseurs/{id}/stats-service` ;
  - **ne pas projeter** le montant de la commande en cours sur le pourcentage : le CA compte les
    commandes `CLOSED` par `receipt_date`, et une commande passée aujourd'hui peut être reçue
    l'an prochain. Afficher plutôt le couple « il reste X avant le palier » et « cette commande :
    Y », que le pharmacien compare lui-même — actionnable sans rien affirmer de faux ;
  - `RemiseRfaServiceImpl.buildAlerte` compose des phrases françaises dans un service, exactement
    ce qui a été corrigé sur `ColisageServiceImpl` (§7.1). Le champ `alerte` doit devenir un état
    nommé (`EtatPalierRfa`), la formulation passant au front — ce qui touche aussi
    `remises-rfa.component.html`, seul consommateur actuel.
- **Franco de port** : à créer. Aucun champ de franco n'existe sur `Fournisseur` (ni sur
  `GroupeFournisseur`). Il faut le champ, sa cascade agence → grossiste comme pour
  `delaiLivraisonJours`, et l'indication « 142 300 F — franco à 150 000 F, il manque 7 700 F ».

##### Ce qui a été écrit

**Le regroupement, d'abord.** `sumCaByFournisseur` groupe désormais sur `COALESCE(pf.id, f.id)`
et lit le palier côté groupe. Vérifié sur la base de recette : cinq lignes de groupes au lieu de
treize lignes d'agences, et COPHARMED passe de 4,7 / 12,6 / 6,6 % à **23,9 %**.

**Une seule requête sert les deux écrans**, filtrée par un `groupeId` nullable. Deux requêtes
jumelles auraient fini par diverger, et les deux écrans par afficher deux chiffres pour le même
grossiste — ce qui était précisément le défaut à corriger.

**La formulation a quitté le service.** `buildAlerte` composait « À 92 % du palier RFA —
accélérer les commandes ! » dans une couche métier qui ne sait ni dans quelle langue ni dans quel
écran elle sera lue. Le service rend maintenant un `EtatPalierRfa` (`SANS_ACCORD`,
`LOIN_DU_PALIER`, `EN_COURS`, `PROCHE_DU_PALIER`, `ATTEINT`) et la phrase se compose au front,
comme pour `ContrainteColisage` (§7.1). **Les bornes d'origine sont conservées à l'identique**, y
compris la règle discrète selon laquelle un fournisseur sans taux négocié n'est jamais signalé en
retard : il n'y a rien à gagner à combler l'écart. Seule la couche de rédaction a changé.

**Le bandeau de saisie** affiche le pourcentage du groupe et, en infobulle, le chiffre d'affaires,
le palier et le reste à facturer. Il **ne projette pas** le bon en cours sur le pourcentage : le
chiffre d'affaires compte les commandes closes à leur date de *réception*, et un bon passé
aujourd'hui peut être reçu l'an prochain. Une pastille « Ce bon franchit le palier » apparaît
quand le montant du bon dépasse le reste à facturer — un fait, pas une projection.

L'endpoint vit dans `CommandeResource` et non dans `RemiseRfaResource`, dont le droit
`facturation.remises-rfa` manque à qui saisit une commande : le bandeau lui aurait renvoyé un 403
là où l'information lui est précisément destinée.

**Franco de port** : colonne `franco_de_port` sur `fournisseur` (migration `V2.1.50`), cascade
agence → grossiste par `Fournisseur.francoEffectif()`, champ dans la fiche fournisseur, et
indicateur au bandeau de saisie — « il manque 7 700 F » tant que le seuil n'est pas atteint,
« atteint » ensuite. La résolution de la cascade se fait côté serveur, dans `FournisseurDTO` ;
l'écran ne fait que l'afficher.

#### 6. Remise d'achat à la ligne — **suspendu : l'usage reste à établir**

> **Question ouverte, préalable à tout développement.** Les grossistes-répartiteurs d'Afrique de
> l'Ouest accordent-ils réellement des remises ligne à ligne ? Plusieurs indices disent que non, et
> il faut le vérifier avant d'écrire une ligne de code.
>
> - **Le prix du médicament est administré en Côte d'Ivoire**, pas négocié : le prix public se
>   déduit du PGHT par un coefficient (1,99 importé, 1,761 production locale), avec des marges
>   fixées à 14 % pour le grossiste-répartiteur et 32,6 % pour l'officine (décret n°94-667 du
>   21 décembre 1994 ; Politique Pharmaceutique Nationale, qui note que le cadre n'a pas été revu).
>   Sur une spécialité homologuée, le prix d'achat **est** la structure de prix : il n'y a pas
>   d'écart « catalogue / négocié » à représenter.
> - **Aucun des six formats d'import ne porte de taux de remise.** `ParsedCsvRecord` lit code,
>   quantités, prix d'achat, prix de vente, UG, taxe, lot et péremption ; `ReponseCommandeColumnMap`
>   ne lit que CIP et quantité. Or `CommandeModel` couvre LABOREX, DPCI, TEDIS et COPHARMED, c'est-à-dire
>   le marché ivoirien. Si ces grossistes remisaient à la ligne, **leurs propres fichiers le
>   porteraient** — ces formats ont été écrits en lisant de vrais fichiers.
> - **Ce qui est pratiqué est déjà supporté** : les **unités gratuites** (`OrderLine.freeQty`,
>   `quantityUg` dans les imports, colonne « Qté.UG » à la réception) — la forme que prend
>   l'avantage commercial quand le prix est réglementé, on ne touche pas au prix, on livre plus ;
>   et la **RFA** sur volume annuel (`RemiseRfaService`), versée a posteriori en avoir.
>   Attention à l'arithmétique des UG : 1 gratuite pour 10 commandées fait 1/11, soit 9,09 %, pas 10 %.
>
> La notion de « remise à la ligne » vient du modèle **français**, où la remise est juridiquement
> encadrée et plafonnée, donc où il faut bien la stocker. Une première version de ce document l'a
> transposée sans vérifier qu'elle avait cours ici, et a classé l'action en valeur forte sur ce seul
> raisonnement comptable.
>
> **À vérifier, du plus décisif au moins :** une facture réelle d'UBIPHARM / COPHARMED / DPCI /
> TEDIS porte-t-elle une remise ? Un fichier de réponse de commande reçu par un client en
> porte-t-il un taux ? Que répond un pharmacien client à « remise, ou unités gratuites » ?
>
> **Si la réponse est non** — hypothèse la plus probable — le sujet se referme : le besoin résiduel
> se limite à la parapharmacie et aux produits à prix libres, pour lesquels le prix d'achat
> éditable (`updateOrderCostAmount`) suffit. Le reste de cette section documente alors le constat,
> pas un chantier.

**Constat : les champs existent et sont morts.** `Commande.discountAmount` et
`OrderLine.discountAmount` sont mis à `0` à huit endroits (`CommandServiceImpl:416,452,508,1209`,
`BedServiceImpl:97,170,323,331`, `StockEntryServiceImpl:650,683`) et jamais à autre chose — le seul
code qui les lit, `ProduitMergeServiceImpl:603`, additionne deux zéros. `OrderLine.netAmount` n'a
aucun `setNetAmount` côté achat. Les colonnes « Remise » et « Net » de la grille de réception sont
`hide: true` et non éditables : démasquées, elles afficheraient des zéros. Les imports grossiste ne
lisent aucun taux (`ParsedCsvRecord`, `ReponseCommandeColumnMap`). `GrilleRemise`, `Remise`,
`RemiseProduit` sont côté **vente**, sans rapport.

La remise d'achat n'est donc pas « partiellement implémentée » : elle est absente, derrière une
coquille qui en a l'apparence.

**Pourquoi ça compte.** La facture du grossiste arrive **en net**. Tant que le logiciel ne connaît
que le brut, le rapprochement facture (ACH-77/78) affiche un écart sur chaque facture, et l'écart
qui mérite attention — celui où le grossiste a cessé d'appliquer la remise négociée — se perd dans
le bruit. En officine, deux lignes de la même commande chez le même grossiste n'ont pas le même
taux : remise de base du catalogue, remise par laboratoire ou par famille, remise par produit
référencé, palier quantitatif. Une remise en pied de commande ne peut pas représenter ça.

**Le contournement actuel** consiste à saisir directement le prix net dans le prix d'achat — qui
est éditable (`updateOrderCostAmount`). Montants, valorisation et marge sont alors justes, mais le
prix catalogue est effacé : on ne voit plus le taux obtenu, et on ne peut plus détecter qu'il a
baissé.

**Étape 1 — rendre la remise saisissable et auditable** *(à faire en premier)*

Pas de grille, pas de paramétrage. Le prix catalogue reste dans `orderCostAmount`, la remise devient
éditable à la ligne (en taux ou en montant), `netAmount` en découle, les deux colonnes sont
démasquées, et le total de la commande s'affiche en brut **et** en net. La remise est également
saisissable à la réception, puisque la facture peut différer de la commande, et l'écart alimente le
rapprochement.

Valeur immédiate : le rapprochement facture devient juste, et l'historique de prix
(`getPriceHistory`) enregistre le taux obtenu — donc une remise qui disparaît se voit.

**Étape 2 — automatiser par une grille de conditions** *(conditionnel)*

```
ConditionAchatFournisseur
  fournisseur, portée (GLOBALE | LABORATOIRE | FAMILLE | PRODUIT),
  référence de la portée, taux, date_debut, date_fin
```

Résolution du plus spécifique au plus général (produit → laboratoire/famille → globale), application
automatique à la création de ligne. À ne faire **que si** l'officine a des conditions négociées
stables : sinon on paye un écran de paramétrage pour une remise que l'utilisateur tape plus vite à
la main.

**Deux précautions, qui comptent plus que le modèle :**

- **ne pas toucher à la `@Formula` `grossAmount`** (`OrderLine.java:70`). Elle doit rester le brut ;
  le net est un champ à part. La modifier réécrirait la lecture de tout l'historique d'achat, y
  compris celle de `get_product_order_summary` et de `pilotage_recalculer_achats`.
- **trancher le niveau.** `discountAmount` existe sur `Commande` **et** sur `OrderLine`. Le second
  est la remise de la ligne ; le premier ne doit être que leur somme, jamais une saisie
  indépendante — sinon on obtient deux vérités et un pied de commande qui ne se recalcule pas.

**Séquencement :** à mener avec l'action A4 (PCB et quantité minimale). Les deux modifient la même
méthode `createOrUpdateOrderLine` et la même grille, et servent le même objectif — que le bon de
commande annonce ce que le grossiste va facturer.

### Vague 3 — rendre visible la dette client (§2.6)

7. ~~**Exposer les avoirs ouverts côté commande**~~ — **fait** (§7.2).
8. ~~**Intégrer la dette client au calcul**~~ — **fait** (§7.3), mais *pas* comme le prévoyait
   cette ligne : la dette se retranche du stock, elle ne s'ajoute pas à la quantité. Le marquage
   d'urgence n'a pas été repris, le badge de A6 le servant déjà.
9. **Avoir sans client** : `createAvoirsFromSale` crée un avoir même quand `customer` est nul (la
   garde `isNull(customer) && quantityAvoir <= 0` ne peut jamais être vraie depuis son appelant), et
   `notifierProduitsDisponibles` sort alors immédiatement. Ces avoirs au porteur ne sont jamais
   notifiés : à assumer explicitement (ticket imprimé) ou à traiter.

### Vague 4 — ergonomie et dette technique

10. **Enrichir la grille de saisie** : VMM, couverture en jours, encours toutes commandes
    confondues, dernier prix d'achat, clients en attente. Toutes ces données existent, aucune n'est
    remontée sur cet écran.
11. **Raccourcis clavier complets** et retour automatique au champ de recherche après ajout.
12. **Défauts D1 à D6** du §4 : garde de fournisseur et de statut sur `fusionner`, idempotence du
    reliquat, pagination du dashboard, garde de statut sur `deleteAll`.
13. **Reprise de stock sur rollback**, pour sortir ACH-09 du placard.

---

### 7.2 A6 — ce qui est promis et qui n'est pas commandé

**Le défaut n'était pas la fonction, c'était son sens unique.** Le circuit d'avoir client
(§2.6) est abouti : la promesse faite au comptoir est enregistrée, la réception rapproche
automatiquement l'avoir du produit reçu, la notification respecte le consentement par canal, la
clôture raisonne en unités. Mais il **exploitait** la réception sans jamais **déclencher** la
commande. Un produit promis que personne n'avait pensé à commander n'apparaissait nulle part :
aucune référence à `AvoirClient` dans `service/stock/`, aucune dans `features/commande/`. Le
client attendait un appel qui ne pouvait pas venir.

**Ce que « déjà commandé » veut dire.** C'est le point où A1 change la réponse. Le filtre retient
les avoirs `OUVERT`, sans commande rattachée, dont le solde reste dû, **et** dont le produit ne
figure dans aucune ligne de commande *en attente et datée d'un envoi*. Un brouillon ne couvre
rien : il n'est pas parti, le grossiste ne l'a jamais vu, et le compter ferait disparaître de
cette liste précisément les produits qu'on a oublié d'envoyer. Avant la date d'envoi, cette
distinction était impossible à écrire — toute commande en attente passait pour transmise.

**Deux chiffres, parce qu'ils ne mesurent pas la même chose.** Le service rend `nbClients`
(clients identifiés distincts) et `nbDemandes` (avoirs ouverts). L'écart entre eux est une
information, pas une incohérence : `createAvoirsFromSale` crée un avoir même sans client (§ vague
3, point 9), et un même client peut avoir demandé deux fois. Ne compter que les clients
identifiés ferait disparaître les avoirs au porteur de l'écran, alors que la promesse a bien été
faite ; ne compter que les avoirs annoncerait deux clients là où il n'y en a qu'un. L'écran
affiche le plus grand des deux, qui est le nombre de promesses à tenir.

**Un dû que le stock couvre déjà n'appelle pas une commande mais un appel au client.** Ces lignes
sont signalées à part, sans quoi l'écran ferait commander ce qui est sur l'étagère. Le stock peut
être négatif — une vente forcée sort la quantité demandée même à zéro, et ce négatif *est* la
dette d'avoir.

**Où cela se voit.** Un cinquième onglet « Dus aux clients » dans *Commandes & Réceptions*, sous
le droit déjà porté par son parent — aucun `nav_item` nouveau, donc aucun droit à oublier. Sa
pastille se charge à l'initialisation comme les autres et non à l'ouverture de l'onglet : une
promesse non tenue qu'il faut aller chercher ne vaut guère mieux qu'une promesse invisible. Le
même dû s'affiche à côté du produit dans la grille de saisie et dans les propositions d'achat, là
où la quantité se décide. Ce badge-là compte **tous** les avoirs ouverts, y compris ceux qu'une
commande partie couvre : « 3 clients attendent ce produit » reste vrai pendant que le bon est en
route, et c'est ce qui justifie de ne pas baisser la quantité.

**Il montre, il ne décide pas.** A6 n'ajoute aucune quantité à aucun calcul. Intégrer la dette
client au réassort est A7, et c'est une règle de gestion distincte.

**Tests.** `ProduitDuIntegrationTest`, 13 cas sur Postgres réel, dont le cas décisif — un
brouillon ne couvre pas —, l'avoir au porteur, le même client deux fois, le solde partiel et
l'ordre par ancienneté. Le jeu de démonstration laisse délibérément un avoir ouvert sur trois non
rattaché, avec un garde-fou qui échoue au chargement si plus aucun produit n'est dû : sans lui, le
parcours `ACH-90` tomberait sur une liste vide dont la cause serait introuvable.

### 7.3 A7 — la dette client retranchée du stock disponible

**Le défaut.** Le calcul comparait le stock objectif au stock **physique** :

```sql
quantite_a_commander = GREATEST(0, GREATEST(objectif - stock, marge - stock))
```

Or une partie de ce stock peut être déjà promise. Trois boîtes dues à des clients sont dans le
rayon et comptaient comme disponibles, alors qu'elles sont réservées. La couverture était
surestimée d'exactement la dette, et la commande partait trop tard — pour un client qui avait
déjà payé.

**Retrancher du stock, et non ajouter au résultat.** La formulation du plan (« ajouter la
quantité due à la quantité à commander ») est fausse dès que le stock dépasse l'objectif :

| | ajouter au résultat | retrancher du stock |
|---|---|---|
| | `max(0, objectif − stock) + D` | `max(0, objectif − (stock − D))` |
| stock 2, objectif 10, dette 5 | 13 | 13 |
| stock 12, objectif 10, dette 5 | **5** | 3 |
| stock 30, objectif 10, dette 5 | **5** | 0 |

Les deux formes coïncident tant que le stock est sous l'objectif, et l'addition surcommande
partout ailleurs — jusqu'à commander alors qu'on a trois fois l'objectif en rayon. La dette ne
crée pas un besoin supplémentaire : elle réduit ce dont on dispose. Mesuré sur la base de
recette : DOLIPRANE 500, stock 26, dette 2, objectif 8 → **0** (l'addition aurait proposé 2) ;
COTON HYDROPHILE, stock −2, dette 2, objectif 4 → **8** au lieu de 6.

**Deux risques de double compte, tous deux écartés.**

- *Stock négatif.* Une vente forcée sort la quantité demandée :
  `quantityAvoir = quantityRequested − quantitySold` vaut alors zéro et **aucun avoir n'est
  créé**. Vente forcée et avoir client s'excluent ligne à ligne ; un produit qui cumule les deux
  les tient de ventes différentes, et l'addition est légitime.
- *Commandes déjà parties.* La vue ne déduit aucun encours : `canSuggere` écarte en amont tout
  produit porté par une commande envoyée ou reçue, et `getPendingQty` retranche l'encours en aval.
  La dette ne s'applique donc qu'aux produits que A6 liste déjà.

**Les deux chemins, du même geste.** La règle vit dans la vue `v_semois_suggestion` (liste des
suggestions) et dans `SemoisCalculationService` (détail et repli) — c'est déjà le cas pour la
quarantaine, et `SemoisStockActuelIntegrationTest` existe précisément parce que les deux avaient
divergé. `SemoisDetteClientIntegrationTest` tient le même rôle ici, avec un cas qui confronte
explicitement les deux lectures.

**La dette n'est pas figée par le batch.** Le calcul nocturne enregistre `stockObjectif` dans
`semois_configuration` ; la dette, elle, est lue au moment de l'affichage. C'est voulu : elle
change à chaque vente servie partiellement, et la geler jusqu'au lendemain ferait commander sur
une photo périmée.

**Ce qui reste visible.** `stock_actuel` demeure le stock physique — c'est ce que l'écran affiche
et ce qu'on compte en rayon. La dette est exposée à part (`dette_client`) et s'affiche sous le
stock : sans elle, une quantité supérieure à « objectif moins stock » passerait pour une erreur de
calcul.

**Non fait, et assumé.** Le marquage d'urgence prévu par le plan n'a pas été repris : le badge de
A6 signale déjà ces lignes dans les suggestions, et promouvoir la classe de criticité aurait des
effets au-delà de cette commande-ci, la classe pilotant aussi les seuils.

**Tests.** `SemoisDetteClientIntegrationTest`, 9 cas sur Postgres réel : absence de dette (non-
régression), dette sous l'objectif, **stock au-dessus de l'objectif** (le cas qui départage les
deux formules), bascule autour de l'objectif, stock négatif cumulé à une dette, solde partiel,
avoir clôturé, avoir au porteur, et accord entre la vue et le calcul Java.

**Au passage — du code mort retiré.** `SuggestionProduitServiceImpl.computeQtyReappro` s'est
révélée sans aucun appelant. Elle portait une **troisième** formule de réassort, jamais exécutée
mais plausible au point d'être prise pour la bonne : c'est précisément le genre de méthode qu'on
corrige en croyant corriger le calcul réel. Elle est supprimée. Sa disparition n'orpheline rien —
`computeSemoisStockObjectif` et `resolveDelaiLivraisonJours` restent atteintes par
`seuilReappro`.

**La coupe a été étendue au reste du bloc**, sur décision explicite. Six méthodes privées de
`SuggestionProduitServiceImpl` ont disparu, par propagation :

| Méthode | Pourquoi |
|---|---|
| `computeQtyReappro` | aucun appelant |
| `seuilReappro` | aucun appelant |
| `loadVmmForProduitsBySemoisClass` | aucun appelant |
| `computeSemoisStockObjectif` | n'était atteinte que par les deux précédentes |
| `resolveDelaiLivraisonJours` | n'était atteinte que par les trois précédentes |
| `loadVmmForProduits` | n'était atteinte que par `loadVmmForProduitsBySemoisClass` |

Avec elles, sept imports devenus inutiles — dont `BigDecimal` et `RoundingMode`, seules traces
qu'un calcul de marge vivait là. Aucun champ injecté n'est devenu orphelin.

**`loadPendingOrderQty` est tombée ensuite**, sur décision explicite elle aussi. Elle était morte
indépendamment de la coupe précédente, et elle était le dernier lecteur de `orderLineRepository`
dans cette classe : la dépendance a donc disparu du champ, du constructeur et de ses deux
appelants de test, ainsi qu'un `@Mock` devenu sans objet. **Sept méthodes privées au total.**

Une remarque de méthode : un champ injecté mort ne se repère pas en comptant ses mentions. Celui-ci
en avait quatre — import, déclaration, paramètre du constructeur, affectation — alors qu'aucune
n'était une lecture. C'est un angle mort facile, et il a failli me faire déclarer la classe propre
alors qu'elle traînait encore une dépendance vers le dépôt des lignes de commande.

Sa requête `findPendingQtyByProduitIds` reste, elle, bien vivante : quatre appelants dans
`SemoisBatchJobService` et `SemoisCalculationService`, ce qui vaut mieux puisque A1 l'a modifiée
pour qu'elle ne compte que les commandes réellement envoyées.

## 8. Récapitulatif des actions

| # | Action | Effort | Valeur | Risque |
|---|---|---|---|---|
| ~~A1~~ | ~~Colonnes `date_envoi` / `canal_envoi` / `envoi_user_id` + reprise des données~~ — **fait**, §7.0 (`V2.1.48`, `V2.1.49`) | M | **Très forte** | Moyen (4 requêtes d'encours à ne pas rater) |
| ~~A2~~ | ~~Alerte de retard de livraison~~ — **fait**, §7.0 | S | **Très forte** | Faible |
| A3 | ~~Filtre `storage_type` dans `v_semois_suggestion` et alignement Java~~ — **fait** (V2.1.48) | S | **Forte** | Moyen (les quantités suggérées changent) |
| ~~A4~~ | ~~PCB et quantité minimale opposés à la saisie et à la suggestion~~ — **fait**, §7.1 | M | **Forte** | Faible |
| ~~A5a~~ | ~~Palier RFA affiché dans la saisie~~ — **fait** ; corrige au passage un cumul par agence face à un palier de groupe | S | **Forte** | Faible |
| ~~A5b~~ | ~~Franco de port : champ sur `Fournisseur` + cascade + indicateur à la saisie~~ — **fait** (`V2.1.50`) | M | Forte | Faible |
| ~~A6~~ | ~~Écran « produits dus non commandés » + badge clients en attente~~ — **fait** | M | Forte | Faible |
| ~~A7~~ | ~~Dette client intégrée au calcul de réassort~~ — **fait** (`V2.1.52`) | M | Moyenne | Moyen (change les quantités) |
| A8 | Remise d'achat saisissable à la ligne (étape 1) — **suspendu, usage à vérifier** (§7 vague 2) | M | **À confirmer** | Moyen (ne pas toucher la `@Formula` `grossAmount`) |
| A9 | Grille `ConditionAchatFournisseur` (étape 2) — **conditionnel à A8** | L | Faible | Moyen |
| A10 | Paramétrage du magasin dans SEMOIS (S2 phase 2) | L | Moyenne (conditionnée au besoin) | Fort |
| A11 | Défauts D1–D4 | S | Moyenne | Faible |
| A12 | Reprise de stock sur rollback | M | Moyenne | Fort (touche le stock) |
| A13 | Raccourcis clavier et colonnes d'aide à la décision | S | Moyenne | Faible |
| — | Multi-officine / groupement (S3) | XL | — | **Hors périmètre** du module commande |

---

## 9. Conclusion

Le module est **solide sur l'aval et sur le calcul**, et **incomplet sur le suivi et sur le
contrat**. Autrement dit : il sait très bien calculer ce qu'il faut commander et très bien traiter
ce qui arrive, mais il perd le fil entre les deux — il ne sait pas si la commande est partie, ni si
elle est en retard, ni si elle respecte les conditions du grossiste.

Les quatre actions A1 à A4 fermaient cet écart pour un effort modéré et sans fonctionnalité
nouvelle à concevoir : elles exploitaient des champs et des paramètres déjà présents en base.
**Elles sont faites.** Le module sait maintenant si une commande est partie (§7.0), depuis quand on
l'attend, si elle est en retard, et il oppose les conditions de conditionnement du grossiste dès la
saisie (§7.1).

Le détail : **A3** (migration `V2.1.48`, §6.3) — la vue et le calcul Java s'accordent sur ce qu'est
le stock disponible, et un test d'intégration les tient. **A4** (§7.1) — la règle de colisage
existait déjà dans l'entité, orpheline ; elle est câblée. **A1 et A2** (§7.0, migrations `V2.1.48`
et `V2.1.49`) — l'envoi est horodaté, les quatre requêtes d'encours ne comptent plus les
brouillons, et le délai fournisseur se mesure enfin depuis l'envoi plutôt que depuis la saisie.

**A5 est faite** aussi. A5a devait coûter un appel ; elle a surtout révélé que le suivi RFA
comparait le chiffre d'affaires d'une *agence* au palier d'un *groupe*, soit un pourcentage
environ trois fois trop bas et une alerte d'approche qui ne pouvait pratiquement jamais se
déclencher. Les deux écrans affichent désormais le cumul du groupe. A5b ajoute le franco de port,
qui n'existait nulle part.

**A8 est suspendu**, et c'est la bonne décision. Le prix du médicament étant administré en zone
UEMOA, la remise d'achat ligne à ligne n'a probablement pas cours ici, et ce que les grossistes
pratiquent — unités gratuites et RFA — est déjà supporté. Que `discountAmount` soit resté à zéro
depuis l'origine n'est donc pas un oubli : c'est le signe que personne n'en a eu besoin.
L'usage reste à confirmer sur une facture réelle (§7 vague 2).
