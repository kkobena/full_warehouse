# Plan — Verrou sur les règlements (factures tiers-payant et différés)

> Rédigé le 2026-09-27. Cible : `pharmaSmart-app/.../service/reglement/`. Fait suite au
> [verrou optimiste sur les ventes](PLAN-VERROU-OPTIMISTE-SALES.md).

## État d'avancement

**R1 à R9 implémentés (2026-09-27).** Option idempotence non faite (à ne faire que si le cas
se présente).

- R1–R3 : `FacturationRepository.verrouiller` / `verrouillerFilles` — `SELECT … FOR UPDATE`
  natif (pas de `@Lock` JPA : un `FOR UPDATE` généré sur une entité à jointures externes est
  refusé par PostgreSQL), précédé de `set_config('lock_timeout', '10s', true)`. Relecture par
  `findById` après le verrou.
- R4–R5 : contrôles et statut dans `AbstractReglementService` (`refuserSiSoldee`,
  `refuserSiRienRegle`, `appliquerStatut[Groupe]`). Le statut se déduit du reste dû des
  dossiers : il ne dépend plus ni du `montantFacture` de l'écran (limité aux dossiers non
  soldés, il pouvait marquer `PAID` une facture partiellement réglée) ni du cumul
  `montantRegle`. En groupe partiel, le versement s'impute sur le **réellement** réglé de
  chaque fille, et plus sur le montant demandé par l'écran.
- R6 : `InvoicePaymentRepository.findFactureIdOf` (JPQL scalaire) → verrou → `findById`.
- R7 : `FneCertificationTransactionService.enregistrerReponse` relit la facture et ne pose que
  `fneResponse` ; utilisé par la certification planifiée et par `FneServiceImpl`.
- R8 : refus des ventes différées soldées ; `payrollAmount` cumule le montant réglé.
- R9 : `ExceptionTranslator.handlePessimisticLock` → 409 `concurrent.lock`.
- Front : l'espace de règlement, le rapprochement et le différé affichent le message du
  serveur et relisent l'état réel après un refus.
- **Non couvert** : les tests d'intégration (Testcontainers) n'ont pas pu tourner sur le poste
  de développement ; ce sont eux qui exercent le `FOR UPDATE` natif et `set_config` sur un vrai
  PostgreSQL. Aucun test ne lance deux transactions réellement concurrentes (le socle
  d'intégration annule chaque test dans une transaction unique).
- Reste ouvert (§5) : dossiers modifiables par les ventes.

**Règle métier : une facture provisoire ne se règle pas (2026-09-27).** Refus dans
`AbstractReglementService.verrouillerFacture` (les 4 modes) et sur chaque fille des règlements
groupés ; l'onglet « Régler » du détail est masqué. Les factures provisoires sont exclues du
rapprochement (lignes, totaux, exports) : ce ne sont pas des créances, et leurs dossiers seront
repris par la facture définitive — les compter doublait le facturé. Les
règlements antérieurs d'une provisoire restent annulables (l'annulation ne passe pas par ce
contrôle), et l'édition définitive refuse toujours de reprendre les dossiers d'une provisoire
encore réglée.

**Édition et annulation de facture (2026-09-27).**

- Édition : verrou consultatif de transaction
  (`pg_advisory_xact_lock(hashtext(current_schema()), hashtext('facturation.edition'))` —
  le schéma courant fait partie de la clé : deux officines hébergées dans la même base ne se
  bloquent pas), pris en tête des
  deux points d'entrée (`AbstractEditionFactureService.createFactureEdition`,
  `EditionByGroupTiersService.createFactureEdition`), **avant** la lecture du dernier numéro
  et des dossiers. Une seule édition à la fois : plus de dossier facturé deux fois, plus de
  numéro de facture en double. Une seconde édition attend au plus 10 s, puis 409.
- L'édition définitive reprend les dossiers des factures provisoires : ces factures sont
  verrouillées comme pour un règlement, puis relues en SQL ; une provisoire déjà réglée
  bloque l'édition avec un message qui la nomme.
- Annulation (`EditionDataServiceImpl.deleteFacture`, unitaire et en lot) : facture et filles
  verrouillées en une instruction, dans l'ordre des règlements ; refus d'une facture réglée
  (la clé étrangère de `payment_transaction` la refusait déjà, en HTTP 500) ; « déjà
  annulée » au lieu d'une erreur. L'annulation en lot traite désormais les filles d'un groupe.
- Front : les listes de factures retirent une facture annulée ailleurs ; l'annulation de
  règlement du rapprochement affiche le message du serveur et recharge.

## 1. Objectif

Qu'une même facture, un même dossier ou une même vente différée ne puisse pas être réglé deux
fois : ni par un double clic, ni par deux utilisateurs, ni par un écran resté ouvert.

## 2. État des lieux (vérifié dans le code)

| Constat | Conséquence |
|---|---|
| Aucune des entités du règlement n'a de `@Version` : `FactureTiersPayant`, `ThirdPartySaleLine`, `InvoicePayment`, `InvoicePaymentItem`, `DifferePayment` | Le dernier écrivain gagne |
| Aucun `@Lock` / `SELECT … FOR UPDATE` | Rien ne sérialise deux règlements |
| Les montants dus sont recalculés côté serveur (`montant - montantRegle`) **à partir de la lecture en début de transaction** | Deux transactions concurrentes voient toutes deux le même reste dû |
| Aucun garde-fou « facture déjà soldée » | Un second règlement séquentiel crée un paiement à 0 au lieu d'être refusé |
| `updateStatut` s'appuie sur `reglementParam.getMontantFacture()`, envoyé par le client | Un écran périmé peut fixer un statut faux |
| `ReglementFactureSelectionneesService` charge les dossiers par leurs seuls identifiants (`selectionBonCriteria`), sans vérifier qu'ils appartiennent à la facture réglée | À confirmer : un dossier d'une autre facture pourrait être réglé sous celle-ci |
| Les règlements de différé modifient des `Sales`, versionnées depuis la phase A | Les règlements **concurrents** de différé sont déjà refusés (409) |

## 3. Scénarios de double règlement

**S1 — Double soumission concurrente (double clic, deux postes) sur la même facture.**
Les deux transactions lisent `montantRegle = 0`, calculent chacune le reste dû complet et créent
chacune un `InvoicePayment` du montant total. Les `UPDATE` sur la facture et les dossiers
s'écrasent (même valeur finale), mais **deux encaissements** sont enregistrés. Même chose en mode
partiel et en mode groupé (sur chaque facture fille).

**S2 — Seconde soumission séquentielle** (la première est déjà validée). Le reste dû relu vaut 0 :
le service crée un `InvoicePayment` à 0 avec `montantVerse` renseigné. Pas d'argent compté deux
fois, mais une pièce de règlement fantôme au lieu d'un refus explicite.

**S3 — Double annulation d'un règlement.** En concurrence, elle échoue aujourd'hui *par accident* :
le second `DELETE` de l'`InvoicePayment` ne touche aucune ligne et Hibernate lève une
`StaleStateException`. En séquentiel, `getReferenceById` sur un règlement supprimé donne un
HTTP 500 au lieu d'un message.

**S4 — Règlement effacé par la certification FNE (défaut existant, hors double règlement).**
`FneServiceImpl.certifierFacturesPendantes` lit les factures **hors transaction**
(`NOT_SUPPORTED`), appelle l'API FNE facture par facture, puis
`FneCertificationTransactionService.certifier` fait un `save()` (donc un `merge`) de l'entité
détachée. Sans `@DynamicUpdate`, ce `merge` réécrit **toutes** les colonnes, dont
`montant_regle` et `statut`, avec les valeurs lues avant les appels HTTP. Un règlement saisi
entre-temps est annulé sans trace.

**S5 — Différé.** Les règlements concurrents sont couverts par la version de `Sales` (phase A).
En séquentiel, une vente déjà soldée (`restToPay = 0`) produit une ligne à 0, comme en S2.
Défaut voisin relevé : `sale.setPayrollAmount(... + amountToPay)` ajoute le reste dû **complet**
même quand le règlement n'est que partiel.

## 4. Stratégie recommandée : verrou pessimiste + garde-fous métier

### Pourquoi pessimiste ici, alors que les ventes sont en optimiste

| Critère | Ventes (`sales`) | Règlements |
|---|---|---|
| Contention | Table la plus sollicitée, au comptoir | Back-office, quelques utilisateurs |
| Durée de transaction | Courte, mais très fréquente | Courte, rare |
| Issue souhaitée pour la seconde requête | Refus 409, le poste recharge | **Recalcul sur l'état à jour, puis refus métier explicite** (« facture déjà soldée ») |
| Schéma | Colonne ajoutée | Aucune modification de tables partitionnées |

Un `SELECT … FOR UPDATE` sur la facture sérialise les règlements : la seconde transaction attend
la première, puis relit un reste dû **à jour**. Le garde-fou métier suffit alors à la refuser
proprement. Avec un `@Version`, la seconde requête échouerait en 409 sans dire pourquoi.

### Tâches

| # | Tâche |
|---|---|
| R1 | `FacturationRepository` : méthode `@Lock(PESSIMISTIC_WRITE)` de chargement de facture par `FactureItemId`. L'utiliser **en premier** dans les 4 `doReglement` et dans `deleteReglement`, à la place de `getReferenceById` |
| R2 | Mode groupé : verrouiller la facture de groupe, puis les factures filles **dans un ordre stable** (`invoice_date`, `id`), pour qu'un règlement de groupe et un règlement individuel ne s'interbloquent pas |
| R3 | Délai d'attente : ne pas bloquer indéfiniment derrière une transaction qui traîne. Sur PostgreSQL, l'indication JPA `jakarta.persistence.lock.timeout` ne se traduit nativement que par `NOWAIT` (0) ou `SKIP LOCKED` (-2) ; pour un délai fini, poser `SET LOCAL lock_timeout` dans la transaction. **À valider sur Hibernate 7.4** |
| R4 | Garde-fous sous verrou : refuser une facture `PAID` ou de reste dû nul ; en mode partiel, refuser les dossiers soldés **et ceux qui n'appartiennent pas à la facture** ; refuser un règlement dont `montantPaye` calculé vaut 0 |
| R5 | Statut calculé côté serveur à partir des dossiers relus sous verrou, et non de `reglementParam.getMontantFacture()` |
| R6 | Annulation : relire le règlement par `findById` sous le verrou de sa facture ; absent → erreur métier « règlement déjà annulé » (et non HTTP 500) |
| R7 | FNE (S4) : dans `certifier`, recharger la facture dans la transaction `REQUIRES_NEW` et ne poser **que** `fneResponse` (ou un `UPDATE` ciblé de `fne_response`), au lieu de fusionner l'entité détachée |
| R8 | Différé : refuser les ventes à `restToPay <= 0` ; corriger le cumul de `payrollAmount` sur un règlement partiel |
| R9 | Message 409 : si une `PessimisticLockException` / `LockTimeoutException` remonte (délai R3 dépassé), la traduire en 409 « Règlement en cours sur un autre poste, réessayez » dans `ExceptionTranslator` |

### Option, en second temps : clé d'idempotence

Le verrou et les garde-fous refusent un doublon, mais le premier clic peut réussir alors que le
client ne reçoit pas la réponse (coupure réseau), puis réessayer. Une clé d'idempotence (UUID
généré par le front, colonne unique sur `payment_transaction`) permettrait de renvoyer le
règlement déjà créé au lieu d'une erreur. À ne faire que si ce cas se présente : colonne et index
sur une table partitionnée, avec une contrainte unique qui doit inclure la clé de partition.

## 5. Points de vigilance

- **`ThirdPartySaleLine` est aussi écrit par les ventes** (clôture et édition de vente assurance).
  Le verrou sur la facture ne protège les dossiers que contre les autres règlements. À vérifier :
  une vente dont un dossier est déjà facturé peut-elle encore être modifiée ?
- **Édition et annulation de facture** (`AbstractEditionFactureService`) écrivent aussi
  `FactureTiersPayant`. Pour sérialiser règlement et réédition, elles devraient prendre le même
  verrou (R1).
- **Ordre des verrous** : tout nouveau code qui verrouille plusieurs factures doit suivre l'ordre
  de R2, sinon des interblocages apparaîtront (PostgreSQL en tue un, qui échoue).
- **Front** : vérifier que les boutons de règlement sont désactivés pendant la requête. C'est un
  confort d'usage, pas une protection : le serveur doit rester correct sans lui.

## 6. Critères d'acceptation

- Deux `POST` de règlement simultanés sur la même facture → un seul `InvoicePayment`, l'autre
  requête reçoit un refus métier explicite.
- Un second règlement d'une facture soldée → refus explicite, aucune pièce à 0.
- Règlement de groupe et règlement individuel d'une fille en parallèle → pas d'interblocage,
  montants cohérents.
- Double annulation → une seule contrepassation, la seconde reçoit « règlement déjà annulé ».
- Un règlement saisi pendant une certification FNE planifiée n'est plus effacé.
