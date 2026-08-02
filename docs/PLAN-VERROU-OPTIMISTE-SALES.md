# Plan — Verrou optimiste sur `Sales` (colonne `version` / `@Version`)

> Rédigé le 2026-07-31. Cible : `pharmaSmart-domain/.../domain/Sales.java` et la table
> partitionnée `sales`. Précède la phase 5 du
> [plan inventaire](PLAN-AMELIORATION-INVENTAIRE-OFFICINE.md), dont il reprend le
> mécanisme éprouvé sur `store_inventory_line`.

## 1. Objectif

Empêcher qu'une vente soit écrasée par deux opérations concurrentes — double
encaissement, reprise simultanée d'une vente en attente sur deux postes,
transformation d'une prévente déjà transformée. Aujourd'hui le dernier écrivain
gagne, silencieusement.

## 2. Ce que le verrou protège — et ce qu'il ne protège pas

Distinction structurante pour le phasage, car les deux mécanismes n'ont ni le même
coût ni le même périmètre :

| Mécanisme | Détecte | Coût client |
|---|---|---|
| **`@Version` automatique** (Hibernate) | Deux **transactions concurrentes** qui chargent la même vente et écrivent : la seconde échoue | **Aucun** — rien à changer côté client |
| **Version renvoyée par le client** | Un client qui écrit à partir d'une vente **lue il y a longtemps** (vente en attente reprise 20 min plus tard) | Chaque flux doit transporter la version |

Le premier couvre déjà le scénario le plus coûteux en officine : le double-clic sur
« Encaisser » qui part en deux requêtes parallèles, et deux caisses qui encaissent la
même vente en attente au même instant. Il s'obtient **sans toucher aux clients**.

Ce que le verrou ne protège pas : les incohérences de stock (gérées ailleurs), ni les
modifications concurrentes de `SalesLine` qui ne remontent pas sur `Sales` (voir §7).

## 3. État des lieux (vérifié dans le code)

| Constat | Conséquence |
|---|---|
| `@Inheritance(SINGLE_TABLE)` — `Sales`, `CashSale`, `ThirdPartySales`, `VenteDepot` partagent la table `sales` | Une seule colonne `version` couvre les 4 types. Rien à faire par sous-classe. |
| `@IdClass(SaleId.class)` : clé composite `(id, sale_date)` | La table est **partitionnée `PARTITION BY RANGE (sale_date)`** → voir §5, le vrai risque du chantier. |
| `implements Persistable<SaleId>` avec `@Transient isNew` + `@PrePersist @PostLoad markNotNew()` | La détection insert/update est **explicite**, elle ne repose pas sur la nullité de la version. `@Version` ne la perturbe donc pas. |
| Les services chargent puis mutent (`findOneWithEagerSalesLines`, `getReferenceById`) — **aucun `merge()` d'entité reconstruite depuis un DTO** | Pattern sûr pour `@Version` : la version courante est toujours chargée avant écriture. C'est ce qui rend la phase A quasi sans risque. |
| Aucun `@Lock` / `LockModeType` dans tout le projet | Pas d'interaction avec un verrouillage pessimiste existant. |
| `ExceptionTranslator` mappe déjà `OptimisticLockException` et `ObjectOptimisticLockingFailureException` → **409** | Aucune infrastructure d'erreur à écrire. |
| ~20 endpoints mutent une vente (`SalesResource` : put-on-hold, save, add-item, update-item/*, delete-item, cancel, transform, finalize-prevente, clone-devis, add/remove-remise…) | Surface large : la validation doit être fonctionnelle, pas seulement unitaire. |

## 4. Pourquoi c'est plus sensible que l'inventaire

`sales` est la table la plus sollicitée de l'application, en écriture, pendant les
heures d'ouverture. Deux différences par rapport à `store_inventory_line` :

- **Un faux conflit bloque une vente au comptoir**, avec un client en face. Le coût
  d'un faux positif est bien supérieur à celui d'un comptage à refaire.
- **La table est partitionnée** : la migration ne se joue pas de la même façon (§5).

D'où le phasage : on prend d'abord le gain sans risque client, on n'ajoute le
transport de version que sur les flux qui le justifient.

## 5. Migration — le point d'attention principal

```sql
ALTER TABLE sales ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
```

- PostgreSQL ≥ 11 : un `DEFAULT` non volatile **ne réécrit pas** la table — l'opération
  est quasi instantanée quel que soit le volume.
- **Mais** sur une table partitionnée, l'ordre prend un `ACCESS EXCLUSIVE` sur la table
  parente **et sur chaque partition**. Toute vente en cours est bloquée le temps de
  l'acquisition des verrous.
- **À exécuter pharmacie fermée**, comme toute migration touchant `sales`. Prévoir un
  `lock_timeout` court (ex. `SET lock_timeout = '5s'`) pour échouer proprement plutôt
  que de bloquer la caisse si une transaction traîne.
- Vérifier le nombre de partitions existantes avant : `\d+ sales`.

## 6. Découpage

### Phase A — protection des transactions concurrentes (aucun changement client)

| # | Tâche |
|---|---|
| A1 | Migration : colonne `version BIGINT NOT NULL DEFAULT 0` sur `sales` |
| A2 | `Sales.java` : champ `@Version @Column(name = "version") private Long version;` + getter/setter |
| A3 | `clone()` : remettre `version` à `null` en même temps que `isNew = true` (flux `clone-devis`) — sinon Hibernate tenterait un UPDATE sur une vente inexistante |
| A4 | Audit des flux `REQUIRES_NEW` / multi-transactions touchant la même vente (recherche `@Transactional(propagation = ...)` dans `service/sale/`) |
| A5 | Message client : le 409 renvoyé par l'`ExceptionTranslator` dit « Le stock a été modifié… » — libellé à adapter au contexte vente (soit un handler dédié, soit un message côté front sur `status === 409`) |

**Résultat attendu** : deux requêtes parallèles sur la même vente → la seconde reçoit
409 au lieu d'écraser la première. Aucun client modifié.

### Phase B — protection contre les données client périmées (flux longs uniquement)

Ne cibler que les flux où la vente reste ouverte longtemps côté client :

| # | Tâche |
|---|---|
| B1 | Exposer `version` dans `CashSaleDTO` / `ThirdPartySaleDTO` (lecture) |
| B2 | La renvoyer à l'écriture pour : `comptant/save`, `put-on-hold`, `comptant/transform`, `finalize-prevente` |
| B3 | Comparaison explicite serveur (même patron que `InventaireServiceImpl.updateQuantityOnHand`) : version client non nulle et différente → `OptimisticLockException` ; version nulle = pas de contrôle (compatibilité) |
| B4 | Front : la version transite via le `SalesStore` (signal `currentSale`) — vérifier qu'elle survit aux opérations de ligne qui rafraîchissent la vente |
| B5 | UX 409 : message explicite + rechargement de la vente depuis le serveur, **sans fusion silencieuse** |

**Ne pas inclure en phase B** les opérations de ligne (`add-item`,
`update-item/quantity`, `update-item/price`…) : elles s'enchaînent en rafale depuis un
seul poste, le risque de périmé est nul et l'exigence de version y multiplierait les
faux conflits.

## 7. Points de vigilance

- **`SalesLine` n'est pas versionnée** par ce plan. Deux postes modifiant des lignes
  différentes de la même vente incrémenteront tous deux `sales.version` (les totaux sont
  recalculés sur l'agrégat) → conflit possible sur une opération pourtant disjointe.
  À surveiller en phase A ; si le cas se produit vraiment, l'option est de ne pas
  toucher l'agrégat à chaque ligne, pas d'abandonner le verrou.
- **`clone-devis`** : le clone doit repartir avec `version = null` (tâche A3).
- **Ventes importées / `copy` / `toIgnore`** : ces lignes sont écrites par des batchs ;
  vérifier qu'aucun d'eux ne recharge une vente détachée pour la sauvegarder.
- **Procédures stockées** touchant `sales` (cf. `V1.0.5__procedure.sql`) : un UPDATE SQL
  direct **n'incrémente pas** la version. Acceptable pour des traitements terminaux,
  à recenser sinon.
- **Ne pas déployer phase A et phase B ensemble** : en cas de hausse des 409, il faut
  pouvoir distinguer les conflits réels (A) des régressions de transport de version (B).

## 8. Critères d'acceptation

- Deux `PUT /api/sales/comptant/save` simultanés sur la même vente → une seule
  encaisse, l'autre reçoit 409 ; aucun double mouvement de stock, aucun double paiement.
- Vente mise en attente sur le poste A, reprise et encaissée sur B, puis encaissée sur
  A → le poste A reçoit un 409 explicite et recharge la vente (phase B).
- Une prévente déjà transformée ne peut pas l'être une seconde fois.
- Journée de caisse normale en préproduction : **zéro 409** sur les opérations
  séquentielles d'un poste unique (absence de faux positifs).
- La migration s'applique en moins de quelques secondes sur une base de volume réel.

## 9. Ordre recommandé

Phase A seule, mise en observation sur une pharmacie pilote pendant quelques jours
(compter les 409 dans les logs), puis phase B si les scénarios de données périmées se
matérialisent. Le gain principal — le double encaissement — est acquis dès la phase A.
