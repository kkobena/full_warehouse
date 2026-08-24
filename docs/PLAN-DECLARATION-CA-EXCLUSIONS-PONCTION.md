# PLAN — Déclaration du chiffre d'affaires : exclusions et ponction

> Statut : **livré** — les dix lots L0 à L9 sont implémentés (21/08/2026)
> Périmètre : `pharmaSmart-domain`, `pharmaSmart-app` (backend, migrations Flyway, webapp Angular)
> Modules de licence concernés : `EXCLUSION_RAYON`, `EXCLUSION_TP`, `EXCLUSION_UG`, `CALLEBASSE`
> Auteur : analyse du code existant au 20/08/2026

---

## 0. Avancement

| Lot | Statut | Livrables |
|---|---|---|
| **L0 — Assainissement** | ✅ livré | Voir le détail ci-dessous |
| **L1 — Socle déclaratif** | ✅ livré | `V1.9.1__socle_declaration_ca.sql` (partie socle) : colonne `sales_line.exclusion_motif`, index couvrant `sales_line_declare_idx`, et paramètre `p_mode` sur les 5 fonctions des écrans de comptabilité. Côté Java : enum `ModeChiffreAffaire`, `MvtParam.mode`, les 5 requêtes de `SalesRepository` et le paramètre `?mode=` sur `BalanceResource`, `TaxeResource`, `TableauPharmatienResource` |
| **L2 — Pipeline d'exclusion** | ✅ livré | `V1.9.1__socle_declaration_ca.sql` (partie lecture) : colonne `payment_transaction.amount_to_be_taken_into_account`, fonctions d'appui `ca_ttc_ligne` / `ca_pondere_ligne` / `ca_reglement`, et bascule du mode `DECLARE` sur les montants déclarables. Côté Java : `DeclarationCaService` + `Impl`, enum `ExclusionMotif`, champ sur `SalesLine` et `PaymentTransaction`, hook dans `SaleServiceImpl.save(CashSaleDTO)` après création des règlements |
| **L3 — Écrans d'exclusion** | ✅ livré | Menus dans `V1.9.2` (+ `required_feature` par onglet). Backend : `ExclusionReferentielService` + `Impl`, `ExclusionResource` sous `/api/declaration-ca` avec `@RequiresFeature` par méthode, DTO `ExclusionItemDTO` / `ExclusionRequestDTO`, `AppConfigurationService.setExcludeFreeUnit` avec éviction de cache. Front : module `features/declaration-ca/` (layout à onglets, `ExclusionReferentielComponent` partagé rayons/TP, `ExclusionParametresComponent`, `DeclarationCaApiService`), route déclarée dans `entity.routes.ts` |
| **L4 — Ponction (backend)** | ✅ livré | `V1.9.2__declaration_ca_ponction_et_menus.sql` (partie socle) : tables `ca_ponction` / `ca_ponction_detail`, séquence `id_ca_ponction_seq`, contrainte GiST de non-chevauchement, colonne `sales.ponction_id`. Java : `PonctionCalculator` (SQL ensembliste), `PonctionService` + `Impl` (simuler / valider / annuler / historique / détail), `PonctionResource` sous `@RequiresFeature(CALLEBASSE)`, DTO dédiés |
| **L5 — Écrans ponction + historique** | ✅ livré | Menus dans `V1.9.2`. Front : `PonctionComponent` (assiette en cascade, saisie, simulation, validation), `PonctionHistoriqueComponent` en **maître / détail** avec `app-hint`, annulation et justificatif PDF. Validation comme annulation passent par `NgbConfirmDialogService` |
| **L6 — Double vue comptabilité** | ✅ livré | Les menus de comptabilité rendent le **CA déclaré**, le module Retraitement du CA offre les mêmes états en **CA encaissé** via trois enveloppes (`vues-reelles/`). Le mode est déduit de la licence côté client (`ModeCaService`) et côté serveur (`ModeChiffreAffaireResolver`), et inscrit dans le nom des exports. Menus dans `V1.9.2` |
| **L7 — Cohérence & recette** | ✅ livré | `AuditDeclarationCaService` (6 contrôles d'invariants) et écran `AuditComponent` ; nav `declaration-ca.audit`. Les trois ressources du module sont gardées **par privilège en plus de la licence**. Justificatif PDF (`PonctionReportService` + `templates/ponction/`). Mode borné côté serveur par `ModeChiffreAffaireResolver`. Annulation bornée dans le temps à la place du verrouillage |
| **L8 — Journaux d'exclusion** | ✅ livré | `V1.9.3__declaration_ca_journaux_exclusion.sql` : trois entrées de menu et un index partiel sur `sales_line (exclusion_motif, sale_date)`. Backend : `JournalExclusionRepository` (JPQL), `CaPeriodeRepository`, `JournalExclusionService`, `JournalExclusionResource`, DTO `JournalLigneDTO` / `JournalVenteDTO` / `JournalKpiDTO` / `JournalExclusionDTO`. Front : `JournalLignesComponent` (unités gratuites, rayons) et `JournalTiersPayantComponent` en maître / détail, tous deux avec `app-toolbar` et `app-kpi-strip` |
| **L9 — Reprise d'invariants** | ✅ livré | `V1.9.4__declaration_ca_invariant_v1_et_plafond.sql` : contrainte `sales_line_declarable_ck` (V1) et paramètre `APP_PONCTION_PLAFOND_DEFAUT`. Contrôle **V9** dans `AuditDeclarationCaService`. Plafond de ponction résolu depuis la configuration, exposé par `GET /ponctions/parametres` et surchargeable à l'écran |

**Détail du lot L0**

| Correctif | Ce qui a été fait |
|---|---|
| **I6** — division entière | `V1.9.0__fix_tva_division_entiere.sql` : `tva_divisor()` et `ht_from_ttc()`, puis les 10 fonctions de rapport redéfinies par `CREATE OR REPLACE` (16 divisions corrigées, `ceiling` → `round` sur les seules expressions de HT, `ceiling` des remises inchangés). Côté Java : les trois surcharges fausses ou mortes de `ServiceUtil.computeHtaxe` supprimées au profit de `htFromTtc` en `BigDecimal`, `calculHt` déléguant à la même implémentation |
| **I6 bis** — montant pris pour un taux | `StockEntryServiceImpl` passait `Commande.taxAmount` (un montant) au paramètre attendant un pourcentage : remplacé par `grossAmount - taxAmount` |
| **I1** — UG ignorées par le rapport TVA | `TaxeServiceImpl` transmet désormais `appConfigurationService.excludeFreeUnit()`, comme la balance de caisse et le tableau pharmacien |
| **I2** — sémantique de `toIgnore` | Documentée sur les requêtes concernées dans `SalesRepository` : le paramètre filtre par égalité, il n'existe aucune valeur qui rende la totalité des lignes |
| **I3** — dérive du montant déclarable | `SaleCommonService.updateAmounts` réétablit le montant de chaque ligne à chaque recalcul, dans la boucle qui agrège déjà les totaux. L'invariant **V2** (Σ lignes = vente) devient vrai par construction |
| ~~**I4**~~ — ventes dépôt | Écarté : `ca = CA_DEPOT` les sort déjà du CA, et zéroer leurs lignes casserait le `montantDepot` de la balance en mode `DECLARE` |

**Tests ajoutés** — 15 cas, tous verts :

- `TvaCalculationMigrationTest` (8) — Testcontainers sur PostgreSQL réel, rejoue les 87 migrations,
  vérifie le diviseur, la parité SQL/Java, la ventilation par taux, le recoupement des trois écrans,
  l'absence de division entière résiduelle et de surcharge dupliquée ;
- `TaxeServiceImplExcludeFreeUnitTest` (3) — le paramètre d'officine atteint bien la fonction SQL ;
- `SaleCommonServiceAmountToDeclareTest` (4) — le montant déclarable suit quantités et prix, et la
  somme des lignes égale toujours la vente.

L'audit de l'invariant **V2**, reporté d'ici, est livré avec **L7** ; la tâche planifiée qui devait
l'accompagner a été abandonnée au même moment.
L'invariant lui-même est tenu par construction et verrouillé par les tests.

**Détail du lot L1**

Trois écarts assumés par rapport au §5 et au §6, tous documentés dans la migration :

1. **`DECLARE` ne lit pas encore `amount_to_be_taken_into_account`.** Basculer maintenant ferait
   perdre l'exclusion des UG entre L1 et L2 : la colonne ne portera les retraitements qu'une fois le
   pipeline du §5.3 branché. `DECLARE` conserve donc le comportement actuel, et c'est **`REEL`** qui
   apporte la nouveauté — il ignore `p_exclude_free_qty`, puisque le chiffre encaissé inclut les UG
   par définition. La bascule de `DECLARE` sur la colonne appartient à **L2**.
2. **`DROP` + `CREATE` au lieu de `CREATE OR REPLACE`.** Ajouter un paramètre change la signature :
   un simple `REPLACE` aurait laissé coexister l'ancienne fonction à 6 arguments et la nouvelle à 7,
   et **tout appel à 6 arguments serait devenu ambigu** — erreur à l'exécution, pas au déploiement.
   L'ancienne signature est supprimée explicitement, et un test le vérifie.
3. **`sales.ponction_id` est reporté à L4.** La colonne est une clé étrangère vers `ca_ponction`, que
   L4 crée ; rien ne l'écrit avant. La poser en L1 aurait demandé une colonne orpheline puis une
   contrainte ajoutée après coup, pour aucun bénéfice.

Un défaut latent corrigé au passage : `MvtParam.build()` renvoyait un **nouvel** objet construit à
partir des seuls champs de période et de regroupement. Tout ce qui avait été positionné avant
l'appel — `excludeFreeUnit`, `toIgnore`, et désormais `mode` — était silencieusement perdu.

**Tests ajoutés en L1** — 5 cas dans `TvaCalculationMigrationTest` (13 au total, tous verts) :
`REEL` ignore l'exclusion des UG, `DECLARE` la respecte, le mode par défaut est bien `DECLARE`, les
5 fonctions portent le paramètre, et l'ancienne signature à 6 arguments a disparu.

**Détail du lot L2**

Le pipeline s'exécute en **un seul point** : dans `SaleServiceImpl.save(CashSaleDTO)`, juste après
`paymentService.buildPaymentFromFromPaymentDTO(...)` et avant la persistance. Plus tôt, les
règlements n'existent pas et la répartition espèces-d'abord n'a rien sur quoi mordre ; à chaque
`updateAmounts`, il faudrait résoudre le rayon de chaque ligne à chaque ajout de produit.

Périmètre appliqué, conformément à **D8** : `CashSale` → UG et rayon ; `ThirdPartySales` → exclusion
tiers-payant, qui retire la vente entière ; `VenteDepot` → rien.

L'ambiguïté **I5** est tranchée par `RayonProduitRepository.findByProduitIdAndStorageId` : le rayon
retenu est celui du magasin de la vente, jamais « exclu si l'un de ses rayons l'est ».

**Tests ajoutés en L2** — 6 cas dans `DeclarationCaServiceImplTest`, dont l'exemple de référence
(1 000 F, 200 F d'UG, réglés en espèces → CA 800 **et** encaissement 800), la répartition sur deux
règlements avec reliquat, le rayon exclu, le module non souscrit et la vente sans règlement.

**Trois migrations, et non huit**

Les scripts ont été fusionnés au fil du travail, tant qu'aucun n'était appliqué en base :

| Fichier | Contenu |
|---|---|
| `V1.9.0__fix_tva_division_entiere.sql` | correctif de la division entière (I6) |
| `V1.9.1__socle_declaration_ca.sql` | colonnes déclarables, fonctions d'appui, les 5 fonctions de rapport |
| `V1.9.2__declaration_ca_ponction_et_menus.sql` | tables de ponction, contrainte GiST, navigation du module |

`V1.9.0` étant seule appliquée sur les bases de développement, les deux autres restent modifiables
à la source. **Cette latitude disparaît à la première installation client** : il faudra alors
revenir aux migrations strictement additives.

**Effets de bord hors périmètre, corrigés en chemin**

Trois défauts trouvés en travaillant sur autre chose, et réparés parce qu'ils invalidaient le
travail en cours :

1. **Le catalogue de modules du front avait divergé.** `ALL_FEATURES` ne listait que 8 modules et
   omettait les 4 optionnels, si bien que `OPTIONAL_FEATURES` était **vide** et que `hasFeature()`
   renvoyait `true` pour tout : aucune garde de route ne bloquait plus rien. Le catalogue est
   désormais servi par `GET /api/license/features`, l'enum `Feature` portant son intitulé français.
   Une seule source, plus deux.
2. **`AppConfigurationService` n'a aucun `@CacheEvict`** alors que ses lecteurs sont `@Cacheable` :
   basculer le paramètre d'unités gratuites restait sans effet jusqu'au redémarrage.
   `setExcludeFreeUnit` évince son cache ; **les autres paramètres de la classe gardent le
   problème**, à traiter séparément.
3. **Le bandeau d'aide « premier usage » est devenu `app-hint`** dans le Design System, avec la
   mémorisation prise en charge. Huit écrans redéveloppaient le même trio signal / `localStorage` /
   `dismiss`, et l'un d'eux (`bed-home`) avait oublié la persistance — son conseil revenait à chaque
   visite.

**Détail du lot L9**

Trois reprises, toutes issues d'écarts entre ce que le plan annonçait et ce que le code faisait.

**V1 passe en contrainte.** Le plan l'annonçait depuis le début ; seule la requête d'audit
l'appliquait, c'est-à-dire après coup. `sales_line_declarable_ck` est posée **`NOT VALID`** : elle
protège les écritures à venir sans imposer un balayage complet de `sales_line` au démarrage. Le
contrôle d'audit garde donc son objet — il couvre les lignes écrites avant elle. Les deux se
complètent au lieu de se doubler : l'un empêche, l'autre constate.

**V9 rapproche la TVA taux par taux.** Un rapport TVA déclaré inférieur au réel est normal ; ce qui
ne l'est pas, c'est qu'un franc d'écart ne se rattache à aucune ligne. Le contrôle compare, pour
chaque taux, l'écart total entre réel et déclaré à la part de cet écart portée par des lignes ayant
un `exclusion_motif`. La différence est l'écart **inexpliqué**, et le message la convertit en TVA
pour dire ce qu'elle coûte.

Le rapprochement porte sur le **TTC** et non sur la taxe : à taux fixe la taxe en est strictement
proportionnelle, et raisonner sur le TTC évite d'imputer à un écart réel ce qui ne serait qu'un
arrondi de division.

**Q4 est tranchée dans les deux sens.** Le plafond a désormais un défaut d'officine
(`APP_PONCTION_PLAFOND_DEFAUT`, 35 %) *et* reste surchargeable à chaque ponction. Le front ne code
plus 35 en dur : il lit `GET /ponctions/parametres` à l'ouverture, et le champ affiche ce qu'il
surcharge dès qu'il s'en écarte. La restriction à `ROLE_ADMIN` envisagée n'a pas été retenue : celui
qui décide du montant à ponctionner décide déjà de l'essentiel, lui refuser la répartition serait
une frontière sans objet.

**La route porte enfin sa garde de licence.** `licenseFeatureGuard` acceptait un module unique
(`data.feature`) ; il accepte désormais aussi une liste (`data.features`), satisfaite dès qu'**un**
des modules est souscrit. Exiger le lot complet aurait fermé la porte à une officine n'ayant acheté
que la ponction. La forme à module unique reste valide, les routes existantes ne bougent pas.

Le masquage des onglets suffisait à l'usage courant ; ce qui ne l'était pas, c'est l'URL collée ou
le favori, qui menait à un module sans aucun onglet plutôt qu'à la page expliquant quoi faire.

**Le test de V7, annoncé depuis le début, est écrit.** `ponctionNeutreEnTva` applique une vraie
ponction puis relit `sales_tva_report` : la TVA sort identique taux par taux, le TTC exonéré baisse
du montant exact ponctionné, le TTC taxé ne bouge pas. Le test annule ensuite la ponction et vérifie
que le rapport revient à son état d'origine — la neutralité et la réversibilité se prouvent dans le
même mouvement.

**Tests ajoutés en L9** — deux cas dans `AuditDeclarationCaServiceTest` : la base **refuse** une
écriture hors bornes, et le contrôle V9 détecte un écart non motivé puis le voit disparaître dès
qu'un motif le porte. Le test de V1 a dû être réécrit : il écrivait la donnée fautive, ce que la
contrainte interdit maintenant. Il la retire le temps du test, ce qui reproduit fidèlement le cas
qu'il couvre — une ligne écrite avant la contrainte.

**Détail du lot L8**

Les écrans d'exclusion disent ce qui **sera** écarté ; ces trois journaux disent ce qui l'a été. Sans
eux, un pharmacien qui coche un rayon n'a aucun moyen de vérifier ce que sa décision a produit : il
n'en voit que le solde, après coup, dans un état comptable où l'écart ne se rattache à rien.

| Journal | Grain | Ce qu'il montre en plus |
|---|---|---|
| Ventes tiers-payant exclues | la **vente**, en maître / détail | l'organisme, le client, un filtre par tiers-payant |
| Unités gratuites vendues | la ligne | la quantité offerte, distincte de la quantité vendue |
| Produits de rayons exclus | la ligne | le rayon qui a motivé l'exclusion |

Le tiers-payant se lit **par vente** parce que l'exclusion l'est aussi : elle écarte la vente
entière. Lister ses lignes à plat ferait perdre l'unité qui porte la décision — on verrait des
produits, jamais les ventes. Le détail n'est chargé qu'à la sélection : un mois de ventes assurance
représente des milliers de lignes dont l'immense majorité ne sera jamais regardée.

**La marge figure dans les trois journaux**, et c'est la colonne qui justifie l'écran. Une ligne
écartée du CA déclaré a bel et bien été vendue et encaissée ; le pharmacien a besoin de voir ce qu'il
gagne réellement sur ce qu'il ne déclare pas. Un rayon exclu très margé n'a pas la même portée qu'un
rayon écoulé à prix coûtant, et rien ailleurs dans l'application ne le lui dit.

**Les indicateurs ne sont pas la somme du tableau.** Les lignes sont plafonnées à 2 000 ; les
agréger afficherait des totaux inférieurs à la réalité sans que rien ne le signale. Le bandeau
`app-kpi-strip` est donc alimenté par une requête d'agrégation distincte, portant sur la totalité de
la période, et un avertissement apparaît quand le tableau — et lui seul — a été tronqué.

**Les requêtes sont en JPQL, dans un repository** — décision du 21/08/2026, prise après une première
écriture en SQL natif. Ces journaux suivent des associations que le modèle décrit déjà : la vente, le
produit, son fournisseur principal, le rayon, le tiers-payant. Les écrire en SQL obligeait à recopier
des noms de colonnes que rien ne vérifie, et **deux étaient faux du premier coup** —
`client_tiers_payant.tierspayant_id` (que la stratégie de nommage d'Hibernate écrit sans séparateur)
et `sales.number_transaction` (pris pour `numero_transaction`). Le second n'aurait même pas échoué au
démarrage : il ne cassait qu'à l'exécution de la requête. En JPQL, un renommage d'attribut casse la
compilation plutôt que l'écran.

La bascule a été étendue au reste du module :

| Requête | Où elle vit désormais | Forme |
|---|---|---|
| Les trois journaux et leurs indicateurs | `JournalExclusionRepository` | JPQL |
| CA réel et CA après exclusions d'une période | `CaPeriodeRepository` | JPQL |
| Alignement de l'en-tête de ponction sur son détail | `CaPonctionRepository` | natif (`update … from`) |
| Assiette, répartition, application, annulation | `PonctionCalculator` | natif (fonctions de fenêtrage) |

Les deux dernières restent en SQL faute d'équivalent : JPQL ne connaît ni les fonctions de
fenêtrage, ni la mise à jour lisant une agrégation d'une autre table dans la même instruction. Ce
sont précisément les endroits où le SQL apporte quelque chose ; ailleurs il ne faisait que retirer
des garanties.

Un défaut corrigé au passage : l'alignement de l'en-tête porte désormais
`@Modifying(clearAutomatically = true, flushAutomatically = true)`. L'en-tête vient d'être inséré,
donc il est dans le contexte de persistance avec ses montants d'avant l'alignement ; sans vidage, la
relecture qui suit immédiatement rendait ces valeurs périmées, et l'écran affichait une ponction dont
les totaux ne correspondaient pas à son propre détail.

**Tests ajoutés en L8** — 12 cas dans `JournalExclusionServiceTest`, sur PostgreSQL réel et sur un
`EntityManagerFactory` monté sur le domaine complet. C'est ce qui leur donne leur valeur : le JPQL
n'existe qu'une fois les entités assemblées, et un dépôt simulé aurait validé des signatures Java
sans jamais toucher la requête. Le jeu d'essai contient délibérément une ligne **non** retraitée —
un journal qui remonte tout est indiscernable d'un journal juste tant qu'on ne lui donne pas
l'occasion de se tromper.

**Détail du lot L7**

Le mode par défaut est passé à **`REEL`** partout — `ModeChiffreAffaire.DEFAUT`, les paramètres REST
des trois ressources — pour la même raison que le défaut SQL : un appelant qui oublie le mode obtient
le chiffre encaissé, jamais un chiffre diminué. Une sous-déclaration silencieuse ne se remarque pas.

Conséquence traitée : `ExportComptableServiceImpl` construisait son `MvtParam` sans mode et serait
passé en `REEL`. Il résout désormais son mode via `ModeChiffreAffaireResolver`, pendant serveur de
`ModeCaService` — un export comptable et l'écran qui l'a produit ne peuvent pas afficher deux
chiffres différents.

L'audit est **délibérément sans `@RequiresFeature`** : il doit rester accessible après l'expiration
d'un module, précisément parce que c'est le moment où l'on veut vérifier que les données restées en
base sont cohérentes.

**Deux verrous indépendants sur les trois ressources du module** — décision du 21/08/2026, qui
remplace une première tentative de réserver l'audit à `ROLE_ADMIN` :

| Verrou | Ce qu'il dit |
|---|---|
| `@RequiresFeature` | ce que l'officine a **acheté** |
| `@PreAuthorize("hasAuthority('pr-…')")` | **qui**, parmi ses employés, a le droit de s'en servir |

Une licence ne remplace pas une habilitation : retirer un montant du chiffre déclaré n'est pas un
geste que tout utilisateur doit pouvoir poser. Trois privilèges sur le patron de `pr-gere-licence`
(§10) — `pr-declaration-ca-exclusion`, `pr-declaration-ca-ponction`, `pr-declaration-ca-audit` — des
`nav_item` de type `ACTION` fusionnés dans les authorities à l'authentification, donc attribuables à
n'importe quel rôle depuis « Rôles & Autorisations ». **Accordés d'office à `ROLE_ADMIN`**, qui a
tout par défaut, et à `ROLE_PHARMACIEN` pour qu'aucun écran déjà visible ne devienne inutilisable.

**Pas de tâche planifiée** — décision révisée le 21/08/2026. Un contrôle nocturne était prévu ; il a
été retiré pour deux raisons. Une officine n'est pas allumée la nuit : la tâche ne se déclencherait
qu'au hasard des postes restés ouverts. Et le contrôle ne fait que **lire** — il ne répare rien, ne
notifie personne d'autre qu'un fichier de journal que nul ne consulte. Il vaut au moment où il sert :
avant de transmettre une déclaration, à la demande.

**Tests ajoutés en L7** — 6 cas dans `AuditDeclarationCaServiceTest`, sur PostgreSQL réel. Chaque
contrôle est éprouvé en **fabriquant** l'incohérence puis en la corrigeant : un contrôle qui ne
détecte rien peut vouloir dire que tout va bien, ou qu'il ne regarde pas au bon endroit.

**Le mode est borné côté serveur.** Il arrive dans la requête HTTP : un appel forgé, ou une
officine dont la licence a expiré, réclamerait sinon des montants retraités que rien n'autorise.
`ModeChiffreAffaireResolver.resoudre()` ramène tout `DECLARE` non souscrit à `REEL`, et les **trois
services** l'appliquent — pas les ressources REST, car le service est le point de passage obligé : il
couvre aussi l'export comptable et les écrans mobiles.

**Le justificatif PDF** suit le patron de `templates/tva/` : `PonctionReportService extends
CommonReportService`, fragments Thymeleaf, pagination au-delà de `Constant.PAGE_SIZE`. Il porte le
raisonnement **avant** le résultat — cascade d'assiette, base de calcul, plafond appliqué — puis le
détail vente par vente. Sans cette cascade, le lecteur voit un total sans pouvoir le reconstituer,
ce qui est précisément ce qu'un justificatif doit permettre.

**`p_exclude_free_qty` est retiré des signatures.** Après L2, sa branche était devenue inatteignable :
elle exigeait un `p_mode` valant ni `REEL` ni `DECLARE`, alors que l'enum n'a que ces deux valeurs.
L'exclusion des unités gratuites est appliquée **une fois pour toutes à la clôture** et portée par
`amount_to_be_taken_into_account` : en `DECLARE` la retrancher de nouveau la compterait deux fois, et
en `REEL` le chiffre encaissé les inclut par définition. `ca_ttc_ligne` perd du même coup
`p_quantity_ug`, devenu inutile. `rapport_activite_vente_report` garde le sien : cette fonction n'a
jamais été migrée vers le mode.

> Conséquence sur la protection contre l'expiration de licence : elle ne passe pas par ce paramètre,
> mais par deux points plus solides. **À l'écriture**, `DeclarationCaServiceImpl` vérifie
> `hasFeature(EXCLUSION_UG)` avant de retrancher quoi que ce soit. **À la lecture**, sans module
> souscrit le résolveur ramène le mode à `REEL`, donc les écrans affichent le brut même si d'anciennes
> ventes portent encore un montant réduit. Un paramètre inerte aurait donné l'illusion d'un troisième
> garde-fou sans en être un.

**Le récapitulatif des retraitements en pied d'écran est abandonné** — décision du 21/08/2026. Il
demandait une API d'agrégation dédiée pour un confort de lecture ; l'écran de contrôle de cohérence
et l'historique des ponctions donnent déjà, chacun dans son registre, de quoi expliquer un écart.

**L'annulation est bornée dans le temps, et le statut `VERROUILLEE` est retiré** — décision du
21/08/2026. Le verrou manuel prévu en **Q6** partait d'une idée juste, appliquée au mauvais geste : il
supposait qu'après avoir transmis sa déclaration, le pharmacien revienne dans le logiciel geler la
période. Personne ne le fait. Un verrou que l'on oublie de poser ne protège rien, tout en donnant
l'impression du contraire — le pire des deux mondes.

Ce qui protège réellement, c'est le temps qui passe : une ponction devient définitive d'elle-même.

| | |
|---|---|
| Paramètre | `APP_PONCTION_ANNULATION_MAX_DAYS`, en base, **1 jour** par défaut |
| Contrôle | `ChronoUnit.DAYS.between(validatedAt, now) > délai` → refus, dans `PonctionServiceImpl.annuler` |
| Statuts restants | `SIMULATION`, `VALIDEE`, `ANNULEE` — le `CHECK` de `V1.9.2` et l'enum `StatutPonction` s'alignent |

Le délai est configurable parce qu'une officine qui déclare au mois n'a pas le même besoin qu'une
autre qui déclare au trimestre ; il n'est pas illimité parce qu'un historique indéfiniment révisable
n'est plus un historique.

**La confirmation passe par `NgbConfirmDialogService`** et non par une modale maison. Le service
existant apporte trois choses qu'il aurait fallu réécrire : le message accepte du **HTML**, donc
l'avertissement garde sa mise en forme — le nombre de jours et le mot « définitive » en gras, ce qui
distingue un avertissement lu d'un avertissement survolé ; une **garde de 200 ms** contre
l'acceptation par un `Enter` résiduel, décisive sur un écran où la validation suit une saisie au
clavier ; et l'apparence des autres confirmations de l'application, au lieu d'en inventer une
neuvième. Le délai affiché vient du serveur, via `PonctionSimulationDTO.delaiAnnulationJours` : une
constante recopiée dans le front finirait par mentir sur la configuration réelle.

**Des entités JPA sont ajoutées pour les deux nouvelles tables** — `CaPonction`, `CaPonctionDetail`
et sa clé composite `CaPonctionDetailId`, avec leurs repositories. Le partage est net et tient à la
volumétrie : les **lectures** — historique, détail, contrôle de statut, justificatif — passent par les
repositories ; les **écritures** qui touchent des milliers de ventes restent ensemblistes, en SQL. Les
charger pour les modifier une à une sur une période de deux mois serait intenable. Il ne subsiste que
deux requêtes natives dans `PonctionServiceImpl` : l'agrégat du CA réel et l'alignement de l'en-tête
sur son détail.

Ce dernier point ferme la course décrite en **V4** : `enregistrerDetail` insère le détail par un
unique `INSERT … SELECT`, puis l'en-tête est **dérivé** de ce qui a été inséré. Faire calculer deux
fois la répartition — une pour l'en-tête, une pour le détail — laissait la porte ouverte à ce que les
deux exécutions ne voient pas exactement les mêmes ventes.

**Détail du lot L6**

**Pas de bascule à l'écran** — décision révisée le 21/08/2026. Un interrupteur à portée de clic rend
le chiffre affiché ambigu : en revenant sur l'écran, ou en imprimant, on ne sait plus lequel des deux
on regarde. Deux entrées de menu, deux chiffres, aucune ambiguïté :

| Module | Chiffre | Comment |
|---|---|---|
| Comptabilité | **déclaré** — c'est lui que la comptabilité exploite | mode déduit de la licence |
| Retraitement du CA | **encaissé** — la vue interne du pharmacien | trois enveloppes forçant `REEL` |

Les trois écrans des vues encaissées sont des **enveloppes** (`vues-reelles/`), pas des copies : le
composant d'origine est réutilisé tel quel, seul le mode change. Dupliquer les gabarits aurait produit
deux versions divergeant dès la première colonne ajoutée d'un côté.

Le mode des menus de comptabilité **n'est pas figé** : il vaut `DECLARE` si au moins un module de
retraitement est souscrit, `REEL` sinon. Sans souscription les deux chiffres sont identiques par
construction, et demander le déclaré ferait porter à la requête une intention que rien ne peut
honorer. D'où l'absence de qualificatif sur ces libellés de menu : un libellé figé y mentirait la
moitié du temps.

Un détail qui compte plus qu'il n'y paraît :
- le **mode figure dans le nom du fichier** exporté (`tableau_pharmacien_ca_reel.pdf` /
  `_ca_declare.pdf`). Deux PDF d'apparence identique aux totaux différents sont une source d'erreur
  garantie une fois posés sur le bureau de quelqu'un.

Le récapitulatif des retraitements en pied d'écran, prévu au §8, est **abandonné** (21/08/2026).

**Détail du lot L5**

Deux garde-fous portés par l'interface :

1. **On ne valide jamais sans simulation à jour.** Toute retouche d'un paramètre efface le résultat
   affiché et referme le bouton. Sans cela, un pharmacien pourrait valider un calcul obtenu avec
   d'autres dates — une ponction qu'il n'a jamais vue.
2. **La cascade d'assiette est affichée en entier** — CA réel, après exclusions, dont assiette à
   TVA 0, puis montant maximal ponctionnable — avec la phrase qui l'explique. Un refus sans cette
   cascade est incompréhensible : on voit un gros chiffre d'affaires et un rejet, sans voir ce qui
   borne.

L'`id` de `ca_ponction` étant un `integer` sans séquence (choix repris de la convention du domaine),
un `CaPonctionIdGeneratorService` a été ajouté sur `id_ca_ponction_seq`, comme les autres entités.

Le PDF justificatif, initialement reporté, est livré avec L7.

**Détail du lot L4 (backend)**

L'annulation est **exacte sans rejouer le calcul**, et c'est une retombée directe de **D11** : une
vente ponctionnée était intacte, donc son montant déclarable valait son montant réel et celui d'un
règlement son montant encaissé. Défaire revient à restaurer `quantity_requested × regular_unit_price`
et à remettre le règlement à `null` — il n'y a rien à reconstituer.

Le calcul est passé de l'`EntityManager` à **`JdbcClient`** (déjà employé par
`DatabasePartitionService`) : le calculateur ne fait que du SQL, sans mapping d'entité, et devient
testable avec une simple `DataSource` plutôt qu'un bootstrap JPA complet.

**Tests ajoutés en L4** — 8 cas dans `PonctionCalculatorTest`, sur un PostgreSQL réel : l'assiette
écarte la vente entièrement taxée et la vente différée, le plafond n'est jamais dépassé, une vente
mixte est bornée par sa part exonérée, la prise s'arrête exactement à l'objectif, l'ordre suit
l'assiette exonérée et non le montant total, et deux exécutions donnent le même résultat — sans quoi
la simulation ne vaudrait pas engagement.

Le PDF justificatif est livré avec **L7**. Le verrouillage de période, lui, a été **abandonné** —
voir « L'annulation est bornée dans le temps » ci-dessus.

**Détail du lot L3**

Deux écarts au §7, tous deux dans le sens de la simplification :

1. **Un seul composant pour les deux référentiels.** Le §7.1 prévoyait `exclusion-rayon/` et
   `exclusion-tiers-payant/`. Le geste étant identique — cocher des lignes, appliquer — un
   `ExclusionReferentielComponent` paramétré par `referentiel`, `titre` et `explication` évite de
   dupliquer la sélection de masse, le filtre et la recherche. Le backend suit la même logique avec
   un `ExclusionReferentielService` unique.
2. **Le badge d'impact est abandonné** — décision du 21/08/2026, après avoir été reporté. Il
   partait d'une hypothèse fausse : que le pharmacien arbitre entre rayons au vu de leur poids en
   chiffre d'affaires. Il n'arbitre pas. Un rayon s'exclut pour la **nature** de ce qu'il contient,
   et la liste est connue avant d'ouvrir l'écran. Afficher un montant par ligne aurait suggéré une
   comparaison qui n'a pas lieu, et fait payer à chaque ouverture l'agrégation d'une année de ventes
   pour un chiffre que personne ne regarde. Les champs `nombreVentes` et `montantVentes`, qui
   valaient `null` depuis L3, sont retirés d'`ExclusionItemDTO`.

Un défaut latent corrigé au passage : `AppConfigurationService` n'a **aucun `@CacheEvict`**, alors
que ses lecteurs sont `@Cacheable`. Basculer le paramètre d'unités gratuites serait resté sans effet
jusqu'au redémarrage. `setExcludeFreeUnit` évince son cache ; les autres paramètres de la classe
gardent le défaut, à traiter séparément.

**Le recalcul de période est abandonné**, et non reporté : décision D2 révisée le 21/08/2026, cocher
comme décocher n'agit que sur les ventes à venir. L'écran porte le rappel dans les deux sens.
Le `PeriodeDeclarationGuard` n'a donc plus à protéger les exclusions ; il ne sert plus qu'à la
ponction (non-chevauchement des périodes, D6) et reste rattaché à L4.

**Reste à faire sur L2** : rien. Le recalcul de période envisagé ici a été abandonné avec la
révision de **D2** — voir le détail de L3.

---

> **Hypothèse structurante — l'application n'est pas encore en production.** Aucune donnée client
> n'est à reprendre, aucune déclaration fiscale n'a été déposée depuis le logiciel, et toute base de
> développement est reconstructible. Cela supprime trois contraintes que le plan portait
> initialement : la migration de reprise (**I3**), le chiffrage préalable de l'écart TVA, et
> l'annonce client avant livraison. Les décisions qui en dépendent sont signalées par la mention
> *(hors production)*.
>
> **En revanche la règle Flyway reste entière** : les scripts déjà exécutés en base ne sont pas
> modifiés, y compris `V1.0.5__procedure.sql`. Toute correction passe par un **nouveau script
> versionné** (§15.1).

---

## 1. Besoin

Le pharmacien doit pouvoir **déclarer à la comptabilité un chiffre d'affaires inférieur au chiffre
d'affaires réellement encaissé**, selon quatre mécanismes indépendants et cumulables :

| # | Mécanisme | Donnée porteuse | Module de licence |
|---|---|---|---|
| 1 | Exclure les ventes d'un **rayon** | `Rayon.exclude` (`to_exclude`) | `EXCLUSION_RAYON` |
| 2 | Exclure les ventes d'un **tiers-payant** | `TiersPayant.beExclude` (`to_be_exclude`) | `EXCLUSION_TP` |
| 3 | Exclure les **unités gratuites** | `AppConfigurationService.excludeFreeUnit()` | `EXCLUSION_UG` |
| 4 | **Ponctionner** un montant sur une période | nouveau (voir §5) | `CALLEBASSE` |

Deux contraintes transverses :

- **le montant réel de la vente n'est jamais modifié** — le montant déclarable est porté par
  `Sales.amountToBeTakenIntoAccount` / `SalesLine.amountToBeTakenIntoAccount` ;
- **la ponction — et elle seule — ne porte que sur les lignes à TVA 0** (cf. décision D7, §4). Les
  trois exclusions (rayon, tiers-payant, UG) retirent la ligne **avec sa TVA** : le montant de taxe
  correspondant disparaît du rapport TVA, et c'est le comportement voulu.

Écrans de comptabilité concernés : `app-balance-mvt-caisse`, `app-taxe-report`,
`app-tableau-pharmacien`.

---

## 2. État des lieux — ce qui existe déjà

Le socle est **largement présent mais inerte**. Recensement précis :

### 2.1 Champs déjà en base, jamais lus

| Champ | Emplacement | Utilisation actuelle |
|---|---|---|
| `Rayon.exclude` (`rayon.to_exclude`) | `pharmaSmart-domain/.../domain/Rayon.java:52` | **aucune** — jamais lu nulle part |
| `TiersPayant.beExclude` (`tiers_payant.to_be_exclude`) | `pharmaSmart-domain/.../domain/TiersPayant.java:96` | **aucune** — jamais lu nulle part |
| `Sales.amountToBeTakenIntoAccount` | `Sales.java:122` | renseigné à la clôture (`SaleCommonService.java:140`), lu par `sales_summary_json` uniquement |
| `SalesLine.amountToBeTakenIntoAccount` | `SalesLine.java:127` | renseigné à la création de ligne, **lu par aucun rapport** |
| `Sales.toIgnore` / `SalesLine.toIgnore` | `Sales.java:138`, `SalesLine.java:126` | filtre d'**égalité** dans les fonctions SQL (`where sl.to_ignore = p_to_ignore`) |
| `Sales.categorieChiffreAffaire` (`sales.ca`) | `Sales.java:173` | filtre `ca = any(p_cas)` ; valeurs `CA`, `CA_DEPOT`, `CALLEBASE`, `TO_IGNORE`, `IMPORT` |

### 2.2 Ce qui fonctionne déjà

`AppConfigurationService.excludeFreeUnit()` (`AppConfigurationService.java:182`) est **effectivement
appliqué** par deux services :

- `BalanceCaisseServiceImpl.java:88`
- `TableauPharmacienServiceImpl.java:93`

Ils passent le drapeau à `p_exclude_free_qty`, que les fonctions SQL traduisent par
`quantity_requested - quantity_ug` au lieu de `quantity_requested`.

### 2.3 Incohérences constatées (à corriger avant toute chose)

| # | Constat | Conséquence |
|---|---|---|
| **I1** | `TaxeServiceImpl.fetchTaxe()` (`TaxeServiceImpl.java:94-118`) **ne positionne pas** `excludeFreeUnit` — il transmet le défaut `false` | Le rapport TVA déclare aujourd'hui les UG que la balance de caisse et le tableau pharmacien excluent. Les deux états ne se recoupent pas |
| **I2** | `p_to_ignore` est utilisé en **égalité** et non en exclusion : `where sl.to_ignore = p_to_ignore` | Appelé avec `false` (cas général), il masque les lignes ignorées — comportement voulu. Mais la sémantique « je veux tout sauf les ignorées » n'est pas exprimable, et un appel à `true` ne renvoie **que** les lignes ignorées. Piège à documenter ou à corriger |
| **I3** | `SalesLine.amountToBeTakenIntoAccount` est figé à la création (`SalesLineServiceImpl.java:125`) et **n'est pas recalculé** lors des changements ultérieurs de quantité ou de prix | La colonne dérive silencieusement de `quantity_requested × regular_unit_price`. Elle n'est aujourd'hui lue par aucun rapport, donc l'écart est invisible — il ne le sera plus une fois ce plan livré. *(hors production)* Pas de reprise de données à prévoir, mais **le défaut de code reste entier** : il faut recalculer le montant à chaque modification de quantité ou de prix, sans quoi la dérive recommencera dès la première vente |
| ~~**I4**~~ | ~~Le montant déclarable de la vente est mis à `0` pour les ventes dépôt alors que leurs lignes gardent le leur~~ | **Non-sujet.** Les ventes dépôt portent `ca = CA_DEPOT` et les rapports filtrent sur `ca = any(p_cas)` : elles n'entrent pas dans le CA, quelles que soient les valeurs de leurs lignes. Les zéroer serait même **nuisible** — la balance de caisse affiche un `montantDepot` séparé, qui tomberait à zéro en mode `DECLARE` (§4 D4). L'invariant **V2** est donc énoncé hors ventes dépôt |
| **I5** | Un produit peut appartenir à **plusieurs rayons** — `rayon_produit` porte `unique(produit_id, rayon_id)` et non `unique(produit_id)` (`RayonProduit.java:19`) | La règle « la ligne appartient à un rayon exclu » est ambiguë. Il faut résoudre le rayon **par storage** de la vente, et définir le comportement si plusieurs rayons du même storage sont rattachés au produit |
| **I6** | **Division entière** dans **10 fonctions SQL, 16 occurrences** (`sales_summary_json`, `sales_balance`, `sales_summary_by_type_json`, `sales_tva_report`, `sales_tva_report_journalier`, `tableau_pharmacien_report`, `tableau_pharmacien_month_report`, `rapport_activite_vente_report`, `get_historique_vente`, `get_product_sales_summary`) : `sl.tax_value` est déclarée `integer` (`V1.0.1__init.sql:2157`) et `Tva.taux` est un `Integer` valant 0, 9 ou 18. En PostgreSQL, `18 / 100 = 0` — donc `1 + (sl.tax_value / 100) = 1` **pour tous les taux** | `montantHt = montantTtc` dans tous les rapports, donc `montantTaxe = montantTtc - montantHt = 0` (`TaxeDTO.java:66`), donc `tvaNette = -tvaDeductible`. **Le rapport TVA déclare aujourd'hui zéro TVA collectée, quel que soit le taux.** Correctif : `1 + sl.tax_value / 100.0`. **Bloquant pour D7** (§4) : la neutralité TVA ne se vérifie pas sur une TVA toujours nulle |

### 2.4 Le socle de licence est prêt

`Feature.java` déclare déjà les quatre modules en `optional = true`. Les trois points d'accroche
existent et sont documentés dans `docs/PLAN-GESTION-LICENCE.md` §3.6 :

- `nav_item.required_feature` (colonne ajoutée par `V1.8.8__license_management.sql:35`) ;
- `data: { feature: '…' }` + `licenseFeatureGuard` côté route Angular ;
- `@RequiresFeature(Feature.…)` côté contrôleur REST, **qui s'applique aussi aux `GET`**.

Rien n'est à créer sur ce volet : il n'y a qu'à câbler.

---

## 3. Le point dur : les rapports agrègent par **ligne**, pas par vente

C'est la contrainte structurante de tout le plan.

Les cinq fonctions PL/pgSQL de `V1.0.5__procedure.sql` (`sales_balance` l. 1191, `sales_tva_report`
l. 1381, `sales_tva_report_journalier` l. 1455, `tableau_pharmacien_report` l. 1532,
`tableau_pharmacien_month_report` l. 1652) calculent toutes le CA de la même façon :

```sql
sum(quantity_requested * regular_unit_price)              -- montant TTC
ceiling(sum(quantity_requested * regular_unit_price
            / nullif(1 + (tax_value / 100), 0)))          -- montant HT
```

agrégé **`group by sl.tax_value`**, **`group by fs.sale_date`** ou **`group by fs.dtype`**.

Conséquence directe : **un montant déclarable stocké uniquement au niveau `Sales` est inexploitable**.

- Le rapport TVA a besoin d'une ventilation **par taux de taxe**. Retrancher un forfait au niveau de
  la vente ne dit pas quelle part relève du 0 %, du 9 % et du 18 %.
- Le tableau pharmacien a besoin du **coût d'achat** pour la marge ; il faut donc savoir *quelles
  lignes* ont été retirées.
- Les trois rapports doivent se recouper à l'unité près. S'ils appliquent chacun leur propre règle
  de soustraction, ils divergeront — c'est exactement l'incohérence **I1** déjà présente.

> **Décision D1 — le montant déclarable est matérialisé au niveau de la ligne.**
> `sales_line.amount_to_be_taken_into_account` devient la **seule source de vérité** du CA déclaré.
> `sales.amount_to_be_taken_into_account` en est la somme, conservée pour la lisibilité, l'audit et
> les écrans de vente — jamais recalculée indépendamment.

---

## 4. Principes directeurs

### D2 — Matérialisation à l'écriture, pas calcul à la lecture

Deux stratégies étaient possibles :

| | Calcul à la lecture (jointures sur `rayon.to_exclude`, `tiers_payant.to_be_exclude`) | Matérialisation |
|---|---|---|
| Rétroactivité | **Totale et subie** : cocher un rayon aujourd'hui change le CA déclaré de l'an dernier | Maîtrisée |
| Déclarations déposées | Deviennent fausses a posteriori | Figées |
| Coût de lecture | Jointures supplémentaires sur chaque rapport | Nul |
| Coût d'écriture | Nul | Un traitement de recalcul borné |
| Ponction | **Impossible** (décision non déterministe, dépend d'un objectif saisi) | Naturelle |

La ponction impose de toute façon un stockage. Faire cohabiter deux mécanismes — trois exclusions
calculées à la volée, une ponction stockée — garantit des états qui ne se recoupent pas.

> **Décision D2 — tout est matérialisé** dans `sales_line.amount_to_be_taken_into_account`, à la
> clôture de la vente pour les exclusions 1 à 3, par un traitement explicite pour la ponction.
> Un changement de drapeau (rayon, TP, UG) ne s'applique **qu'aux ventes futures**, dans les deux
> sens : cocher n'ampute pas le passé, décocher ne le restaure pas. **Aucun recalcul rétroactif
> n'est proposé**, même optionnel.

Ce dernier point mérite d'être explicite, car un recalcul « pour rattraper » paraît toujours
serviable : une vente close porte le montant déclaré **au moment de sa clôture**, et ce montant est
définitif. Le rendre réécrivable, ne serait-ce que sur demande, ferait de tout état déjà imprimé une
valeur provisoire — plus rien ne garantirait qu'un rapport ressorte identique deux mois plus tard.
Le caractère figé n'est pas une limitation du mécanisme, c'est ce qui le rend opposable.

### D3 — Ne pas détourner `categorieChiffreAffaire` pour la ponction partielle

`sales.ca` est un filtre **binaire** : les rapports interrogent `ca = any(p_cas)` avec `{CA}` par
défaut. Basculer une vente ponctionnée à 35 % vers `CALLEBASE` la retirerait **à 100 %** des
rapports — l'inverse de l'effet recherché.

> **Décision D3** — `ca` reste à `CA` pour une vente **partiellement** ponctionnée ; le montant seul
> porte l'information. `CALLEBASE` est réservé à l'exclusion **totale** d'une vente
> (`amount_to_be_taken_into_account = 0`), et `TO_IGNORE` conserve son usage actuel.
> La traçabilité de la ponction est assurée par la table de détail (§5.2), pas par `ca`.

À noter : l'exclusion par rayon opère **au niveau de la ligne** — une même vente peut mêler un rayon
exclu et un rayon normal. Aucun champ de niveau vente ne peut représenter cette situation, ce qui
confirme D1.

### D4 — Un seul jeu de fonctions SQL, deux modes de lecture

Le besoin demande « des copies des menus qui affichent les valeurs normales ». Dupliquer les cinq
fonctions PL/pgSQL, les cinq services Java et les trois composants Angular créerait **deux
implémentations à maintenir en parallèle**, qui divergeront à la première évolution.

> **Décision D4** — chaque fonction SQL reçoit un paramètre supplémentaire
> `p_mode text default 'REEL'` (`'REEL'` | `'DECLARE'`) qui bascule l'expression agrégée :
>
> ```sql
> case when p_mode = 'DECLARE'
>        then sl.amount_to_be_taken_into_account
>      when p_exclude_free_qty
>        then (sl.quantity_requested - sl.quantity_ug) * sl.regular_unit_price
>      else sl.quantity_requested * sl.regular_unit_price
> end
> ```
>
> Un seul composant Angular par écran, un sélecteur « **CA réel / CA déclaré** », et **deux entrées
> de menu** distinctes pointant sur la même route avec un `data.mode` différent — ce qui permet
> aussi de réserver le « CA réel » à certains rôles via `nav_item_role`.

Note : en mode `REEL`, `p_exclude_free_qty` doit être **forcé à `false`** — le CA réel inclut les UG
par définition. Cela corrige mécaniquement **I1**.

> **Révisé en L7** — le paramètre a été **retiré des signatures** : sa branche est inatteignable dès
> lors que `p_mode` ne vaut que `REEL` ou `DECLARE`, et l'exclusion des UG est portée une fois pour
> toutes par `amount_to_be_taken_into_account`. Le raisonnement ci-dessus reste celui qui a conduit à
> la colonne ; le paramètre n'en était que l'étape intermédiaire. Voir le détail de L7.

### D5 — Le calcul est fait en SQL ensembliste, pas en boucle Java

Une période de deux mois représente couramment 15 000 à 25 000 ventes et 80 000 à 120 000 lignes.
Un algorithme itératif en Java (charger, trier, boucler, `saveAll`) est intenable en temps comme en
mémoire. Toute la ponction — sélection, ordonnancement, calcul du point de coupure, répartition sur
les lignes — s'exprime en **deux `UPDATE … FROM (CTE)`** grâce aux fonctions de fenêtrage (§5.4).

### D6 — Les périodes de ponction ne se chevauchent pas, et c'est la base qui le garantit

Un contrôle applicatif en Java est contournable (deux onglets, un appel API direct, un rejeu). La
contrainte est posée en base avec `btree_gist` :

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE ca_ponction
  ADD CONSTRAINT ca_ponction_periode_no_overlap
  EXCLUDE USING gist (
    magasin_id WITH =,
    daterange(date_debut, date_fin, '[]') WITH &&
  ) WHERE (statut = 'VALIDEE');
```

Le prédicat retient le seul statut qui doit réserver la période : une simulation n'est jamais
persistée, et une ponction annulée la libère. Le cas cité dans
le besoin — 02/06 → 31/07 puis 30/07 → 25/10 — est rejeté par la base elle-même, avec un message
d'erreur traduit par `ExceptionTranslator`.

### D8 — Périmètre par type de vente

| Type de vente | Mécanismes applicables |
|---|---|
| `CashSale` — vente comptant | **UG, rayon, ponction** |
| `ThirdPartySales` — tiers-payant | **exclusion TP** uniquement, et elle retire la vente entière |
| `VenteDepot` | aucun — `ca = CA_DEPOT` les sort déjà du CA (cf. **I4**) |

L'exclusion tiers-payant ne peut pas viser une vente comptant : il n'y a pas de tiers-payant sur une
`CashSale`. Symétriquement, la ponction ne vise que les ventes comptant, puisqu'elle porte sur la
part espèces (D9).

### D9 — La ponction mord d'abord sur les espèces, et les règlements suivent le CA

Deux exigences distinctes, souvent confondues :

**a) L'assiette.** La ponction porte sur les ventes réglées en espèces
(`PaymentMode.code = 'CASH'`, cf. `SalePayment`). Les règlements mobile money sont hors périmètre du
premier lot — le champ `ca_ponction.modes_reglement` est prévu pour les ouvrir sans migration.

**b) Le montant encaissé affiché doit suivre.** C'est le point le plus important, et il vaut pour
**les quatre mécanismes**, pas seulement la ponction. Exemple :

```
Vente de 1 000 F, dont 200 F d'UG, réglée 1 000 F en espèces.

  Sans retraitement des règlements     Avec (attendu)
  CA déclaré        800                CA déclaré        800
  Encaissement    1 000   ← incohérent Encaissement      800
```

Afficher un CA de 800 et un encaissement de 1 000 rend l'état indéfendable : l'écart de 200 ne se
rattache à rien. **Le montant déclarable doit donc être porté aussi par les règlements**, et non par
les seules lignes de vente.

> **Décision D9** — `payment_transaction` reçoit une colonne `amount_to_be_taken_into_account`. La
> réduction de la vente (`montant réel − montant déclaré`) est répartie sur ses règlements
> **espèces d'abord**, puis sur les autres dans l'ordre de `ModePaimentCode`. Une vente peut porter
> plusieurs règlements : si la réduction dépasse la part espèces, le reliquat mord sur le règlement
> suivant.

Exemple à deux règlements — vente 1 000, réduction 700, réglée 600 espèces + 400 mobile :

```
espèces  600 → 0     (600 absorbés)
mobile   400 → 300   (100 de reliquat)
                     total déclaré 300 = 1 000 − 700  ✓
```

### D10 — Un seul ratio pilote tous les montants dérivés

Le montant déclarable d'une ligne est un montant **TTC**. Le tableau pharmacien a besoin du coût
d'achat, le rapport TVA du HT, les deux de la remise — aucun ne se déduit d'un TTC seul.

Plutôt que de matérialiser quatre colonnes qui pourraient diverger, tous les montants dérivés se
calculent à partir d'un unique ratio :

```
ratio = amount_to_be_taken_into_account / (quantity_requested × regular_unit_price)

montantTtc   = amount_to_be_taken_into_account
montantHt    = ht_from_ttc(amount_to_be_taken_into_account, tax_value)
montantAchat = round(quantity_requested × cost_amount × ratio)
montantRemise= round(quantity_requested × regular_unit_price × taux_remise × ratio)
```

Le ratio redonne exactement les bons montants dans les cas connus : exclusion d'UG →
`(qté − ugs) / qté`, donc un coût d'achat amputé des unités gratuites ; exclusion de rayon →
ratio 0, donc coût nul ; ponction partielle → coût réduit dans la même proportion, ce qui préserve le
taux de marge affiché.

Conséquence : en mode `DECLARE`, le paramètre `p_exclude_free_qty` **n'a plus d'effet** — l'exclusion
des UG est déjà dans la colonne. Le laisser agir la compterait deux fois.

### D11 — La ponction ne touche que des ventes intactes

Deux restrictions supplémentaires sur l'assiette de la ponction :

1. **Pas de ventes différées.** Une vente à crédit n'est pas encaissée à sa clôture : ses règlements
   arrivent plus tard, parfois sur une autre période. Y appliquer une ponction obligerait à reporter
   la réduction sur des règlements futurs, donc à traîner un reliquat d'une période à l'autre — et à
   faire mentir le principe de non-chevauchement (D6). Elles sont écartées de l'assiette.
2. **Pas de ventes déjà retraitées.** Une vente qui a subi une exclusion (rayon, UG ou tiers-payant)
   n'est pas ponctionnable.

La seconde règle n'est pas qu'une simplification, elle évite une incohérence : sur une vente déjà
amputée, le plafond de 35 % porterait sur quoi — le montant réel ou ce qu'il en reste ? Et la
répartition sur les règlements devrait se combiner avec une réduction déjà répartie. En réservant la
ponction aux ventes intactes, **le montant déclarable d'une vente ponctionnée vaut exactement son
montant réel avant traitement** : le plafond, l'assiette et la répartition sur les règlements n'ont
plus qu'une seule lecture possible.

> **Décision D11** — l'assiette de la ponction exclut les ventes `differe = true` et toute vente dont
> une ligne porte déjà un `exclusion_motif`. Le critère est directement testable en SQL :
>
> ```sql
> AND s.differe = false
> AND NOT EXISTS (SELECT 1 FROM sales_line l
>                  WHERE l.sales_id = s.id AND l.sales_sale_date = s.sale_date
>                    AND l.exclusion_motif IS NOT NULL)
> ```

C'est aussi ce qui donne son rôle opérationnel à `exclusion_motif` : au-delà de la traçabilité, il
sert de **critère d'éligibilité**.

### D7 — La ponction, et elle seule, ne porte que sur les lignes à TVA 0

**Périmètre de cette décision.** Elle ne concerne **que la ponction**. Les trois exclusions (rayon,
tiers-payant, UG) ne sont pas soumises à la contrainte TVA : elles retirent des lignes entières — ou,
pour les UG, une quantité identifiée — et **emportent la TVA correspondante**. C'est légitime et
cohérent, parce que le montant retiré est *identifiable* : on sait exactement quelle ligne, quel
produit, quelle quantité, donc quel taux et quel montant de taxe. Le rapport TVA reste juste, il
porte simplement sur un périmètre plus étroit.

La ponction est d'une autre nature : elle retranche un montant **arbitraire**, décidé après coup,
sans contrepartie identifiable en produits. C'est cette absence de rattachement qui interdit de la
laisser mordre sur la base taxable.

Ponctionner une ligne taxée revient à **réduire la base taxable et donc la TVA collectée déclarée**.
Or cette TVA a déjà été facturée au client, figure sur le ticket, et — pour les ventes certifiées —
a été transmise à l'administration par la FNE. Un CA déclaré amputé sur des lignes à 9 % ou 18 %
produit trois divergences simultanées :

1. la TVA déclarée ne correspond plus à la TVA facturée (écart opposable) ;
2. le rapport TVA cesse de se recouper avec les factures normalisées émises ;
3. la ventilation par taux devient impossible à justifier ligne à ligne.

Restreindre la ponction aux lignes à **`tax_value = 0`** supprime les trois : la TVA collectée est
**strictement identique avant et après ponction**, seul le chiffre d'affaires exonéré diminue.
La TVA du mode `DECLARE` peut rester inférieure à celle du mode `REEL` — mais uniquement du fait des
exclusions, dont chaque franc de taxe retiré se rattache à une ligne précise (invariant V9). C'est
cette traçabilité qui rend les états défendables ; la ponction, elle, n'en offre aucune.

> **Décision D7** — l'assiette ponctionnable est `Σ amount_to_be_taken_into_account` des lignes dont
> `tax_value = 0`, après application des exclusions rayon / TP / UG. Les lignes taxées ne sont
> **jamais** touchées par la ponction. Le taux éligible est stocké (`ca_ponction.taux_tva_eligibles`,
> défaut `'0'`) pour qu'un élargissement futur ne demande pas de migration.

Deux conséquences opérationnelles :

- Le **plafond par vente** se combine avec cette restriction. Le besoin fixe « ne pas dépasser 35 %
  du montant d'une vente » : la prise vaut donc
  `min(35 % × montant total de la vente, assiette TVA 0 de la vente)`. Une vente entièrement taxée
  est de fait inéligible ; une vente à 80 % exonérée reste plafonnée à 35 % de son total.
- Le **montant maximal ponctionnable** d'une période n'est plus `35 % × CA après exclusions` mais
  `Σ min(35 % × montant vente, assiette TVA 0 de la vente)`, nécessairement inférieur. L'écran de
  simulation doit l'afficher **avant** la saisie, sinon le pharmacien saisira des objectifs
  systématiquement refusés.

En officine, le médicament est exonéré et la parapharmacie taxée : l'assiette à TVA 0 représente
l'essentiel du chiffre d'affaires, la restriction n'est donc pas limitante en pratique. Elle le
deviendrait pour une officine à forte part parapharmacie — d'où l'affichage systématique du plafond
réel.

**Prérequis : I6 doit être corrigé d'abord.** Tant que la division entière écrase la TVA à zéro,
l'invariant « TVA collectée inchangée » se vérifie trivialement et ne prouve rien.

---

## 5. Modèle de données

### 5.1 Colonnes ajoutées à l'existant

| Table | Colonne | Type | Rôle |
|---|---|---|---|
| `sales_line` | `amount_to_be_taken_into_account` | *existe* | Devient la source de vérité du CA déclaré. Index partiel à ajouter |
| `sales_line` | `exclusion_motif` | `varchar(20) null` | `RAYON`, `TIERS_PAYANT`, `UG`, `PONCTION`, `MANUEL` — pourquoi cette ligne est réduite. Sert l'écran d'audit et l'éligibilité à la ponction (D11) |
| `sales` | `ponction_id` | `bigint null` | FK vers `ca_ponction`. Rend le rollback trivial et l'historique lisible |

Index recommandés :

```sql
CREATE INDEX sales_line_declare_idx
  ON sales_line (sale_date)
  INCLUDE (amount_to_be_taken_into_account, tax_value)
  WHERE to_ignore = false;

CREATE INDEX sales_ponction_idx ON sales (ponction_id) WHERE ponction_id IS NOT NULL;
```

### 5.2 Nouvelles tables

```sql
-- Un traitement de ponction sur une période. Une ligne = une décision du pharmacien.
CREATE TABLE ca_ponction (
  id                  integer PRIMARY KEY,             -- alimenté par id_ca_ponction_seq, cf. L5
  magasin_id          bigint        NOT NULL REFERENCES magasin (id),
  date_debut          date          NOT NULL,
  date_fin            date          NOT NULL,
  mode_calcul         varchar(15)   NOT NULL,          -- MONTANT_FIXE | POURCENTAGE
  valeur_saisie       numeric(12,2) NOT NULL,          -- montant en F CFA ou taux en %
  plafond_par_vente   numeric(5,2)  NOT NULL DEFAULT 35.00,
  strategie           varchar(15)   NOT NULL DEFAULT 'DECROISSANT',  -- DECROISSANT | UNIFORME
  modes_reglement     varchar(80)   NOT NULL DEFAULT 'CASH',         -- codes séparés par ','
  taux_tva_eligibles  varchar(40)   NOT NULL DEFAULT '0',            -- cf. D7 ; '0' = exonéré seul
  ca_reel             bigint        NOT NULL,          -- CA de la période, avant tout retraitement
  ca_apres_exclusions bigint        NOT NULL,          -- après rayon / TP / UG
  ca_assiette_tva0    bigint        NOT NULL,          -- part exonérée : l'assiette ponctionnable
  montant_ponctionnable bigint      NOT NULL,          -- Σ min(plafond × montant vente, assiette TVA 0)
  montant_objectif    bigint        NOT NULL,
  montant_ponctionne  bigint        NOT NULL,          -- ce qui a réellement pu être prélevé
  ca_declare          bigint        NOT NULL,          -- ca_apres_exclusions - montant_ponctionne
  nombre_ventes       integer       NOT NULL,
  statut              varchar(15)   NOT NULL,          -- SIMULATION | VALIDEE | ANNULEE
  features_actives    varchar(120),                    -- modules appliqués lors du calcul, pour l'audit
  commentaire         varchar(255),
  created_by          bigint        NOT NULL REFERENCES app_user (id),
  created_at          timestamp     NOT NULL,
  validated_by        bigint        REFERENCES app_user (id),
  validated_at        timestamp,
  canceled_by         bigint        REFERENCES app_user (id),
  canceled_at         timestamp,
  CONSTRAINT ca_ponction_periode_ck CHECK (date_fin >= date_debut),
  CONSTRAINT ca_ponction_plafond_ck CHECK (plafond_par_vente > 0 AND plafond_par_vente <= 100)
);

-- Le détail : ce qui a été retiré, vente par vente. Permet le rollback exact et la justification.
CREATE TABLE ca_ponction_detail (
  ponction_id        integer NOT NULL REFERENCES ca_ponction (id) ON DELETE CASCADE,
  sale_id            bigint  NOT NULL,
  sale_date          date    NOT NULL,
  numero_transaction varchar(20),        -- dénormalisé : le numéro affiché au pharmacien
  montant_vente      integer NOT NULL,   -- total de la vente après exclusions : base du plafond 35 %
  montant_base       integer NOT NULL,   -- part à TVA 0 : assiette réellement ponctionnable (D7)
  montant_ponctionne integer NOT NULL,
  rang               integer NOT NULL,
  PRIMARY KEY (ponction_id, sale_id, sale_date),
  FOREIGN KEY (sale_id, sale_date) REFERENCES sales (id, sale_date),
  CONSTRAINT ca_ponction_detail_plafond_ck
    CHECK (montant_ponctionne >= 0 AND montant_ponctionne <= montant_base)
);
```

Le double plafond de D7 (`35 % du total` **et** `assiette TVA 0`) est vérifiable a posteriori depuis
ces deux colonnes ; seul le second est exprimable en `CHECK` de ligne, le premier dépendant de
`ca_ponction.plafond_par_vente` :

```sql
ALTER TABLE ca_ponction_detail
  ADD CONSTRAINT ca_ponction_detail_plafond_vente_ck CHECK (true);  -- contrôlé par déclencheur
-- ou, plus simple, un contrôle applicatif à la validation + la requête d'audit V5.
```

`ca_ponction_detail` est ce que l'écran « Historique » affiche en détail, et ce qui rend l'annulation
exacte : `amount_to_be_taken_into_account += montant_ponctionne` réparti à l'identique.

`numero_transaction` y est **recopié** de la vente. C'est la seule dénormalisation du modèle, et elle
est délibérée : l'écran de détail et le justificatif PDF n'affichent jamais l'identifiant technique
d'une vente, toujours son numéro. Le lire par jointure obligerait le justificatif — un document qui
doit rester lisible dix ans plus tard — à dépendre de lignes de vente qui auront pu être archivées ou
purgées. Il est figé à l'insertion, avec le reste de ce qui a été décidé ce jour-là.

### 5.3 Le pipeline de calcul du montant déclarable

Un ordre unique, appliqué partout, ligne par ligne :

```
base_ligne = quantity_requested × regular_unit_price          (CA réel, jamais modifié)

  1. UG           si EXCLUSION_UG     → base -= quantity_ug × regular_unit_price
  2. RAYON        si EXCLUSION_RAYON  → si le rayon (résolu par storage) est to_exclude → base = 0
  3. TIERS-PAYANT si EXCLUSION_TP     → si la vente relève d'un TP to_be_exclude        → base = 0
  4. PONCTION     si CALLEBASSE ET tax_value = 0
                                      → base -= quote-part de la ponction de la vente

amount_to_be_taken_into_account = max(0, base)
```

L'ordre n'est pas indifférent : la ponction s'applique **sur ce qui reste**. Sans cela, l'objectif
saisi par le pharmacien porterait sur un CA qu'il ne voit pas à l'écran, et le CA déclaré pourrait
devenir négatif.

L'écran de ponction affiche donc **trois** montants, du plus large au plus étroit — c'est la seule
façon de rendre un objectif refusé compréhensible :

```
CA réel                                     12 400 000
CA après exclusions (rayon / TP / UG)       11 850 000
  dont assiette à TVA 0 (D7)                10 900 000
Montant maximal ponctionnable                3 615 000   ← Σ min(35 % × vente, assiette TVA 0)
```

**La TVA des étapes 1 à 3 se déduit toute seule — rien à construire.** Puisque le montant déclarable
est porté par la ligne (D1) et que les fonctions SQL divisent chaque ligne par *son propre*
`tax_value`, retirer une ligne ou une quantité en retire mécaniquement la taxe :

```sql
montantHt   = amount_to_be_taken_into_account / (1 + tax_value / 100.0)
montantTaxe = amount_to_be_taken_into_account - montantHt
```

Pour les **UG**, la quantité gratuite est connue (`sales_line.quantity_ug`) : le montant retiré vaut
`quantity_ug × regular_unit_price` et sa TVA s'en déduit exactement au taux de la ligne. C'est déjà
ce que fait `p_exclude_free_qty` aujourd'hui — la soustraction précède la division par le taux. Pour
les **rayons** et les **tiers-payants**, la ligne tombe à 0 : sa taxe tombe à 0 avec elle.

Aucune logique spécifique n'est donc à écrire pour la TVA des exclusions : elle découle de la
matérialisation par ligne. Ce qui se déduit du même constat, c'est que la ponction, elle, n'a pas de
ligne à laquelle se rattacher — d'où D7.

### 5.4 Algorithme de ponction — sans boucle

Assiette : ventes `CLOSED`, non annulées, `ca = 'CA'`, `to_ignore = false`, dont le règlement relève
des modes retenus (`CASH` en lot 1 ; `ModePaimentCode.PaymentGroup.MOBILE` prévu mais non demandé —
d'où le champ `modes_reglement` déjà présent en base, alimenté par une constante en lot 1).

Objectif :

```
objectif = MONTANT_FIXE  → valeur_saisie
           POURCENTAGE   → round(ca_apres_exclusions × valeur_saisie / 100)
```

Le pourcentage se calcule sur `ca_apres_exclusions` — c'est le CA que le pharmacien lit sur ses
états — et non sur l'assiette à TVA 0, qui n'est qu'une contrainte de faisabilité. L'écart entre les
deux est absorbé par le contrôle d'atteignabilité ci-dessous.

Répartition **DECROISSANT** (celle du besoin) en une passe, avec une somme cumulée glissante. Noter
les deux agrégats distincts par vente : `montant_vente` (assiette du plafond de 35 %) et `base_tva0`
(ce qui est réellement ponctionnable, cf. D7).

```sql
WITH eligible AS (
  SELECT s.id, s.sale_date,
         SUM(sl.amount_to_be_taken_into_account)                                  AS montant_vente,
         SUM(sl.amount_to_be_taken_into_account) FILTER (WHERE sl.tax_value = 0)  AS base_tva0
    FROM sales s
    JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
   WHERE s.sale_date BETWEEN :d1 AND :d2
     AND s.statut = 'CLOSED' AND s.canceled = false
     AND s.ca = 'CA' AND s.to_ignore = false AND s.imported = false
     AND s.magasin_id = :magasin
     AND sl.to_ignore = false
     AND EXISTS (SELECT 1 FROM payment_transaction p
                  WHERE p.sale_id = s.id AND p.sale_date = s.sale_date
                    AND p.dtype = 'SalePayment'
                    AND p.payment_mode_code = ANY (:modes))
   GROUP BY s.id, s.sale_date
  HAVING COALESCE(SUM(sl.amount_to_be_taken_into_account)
                    FILTER (WHERE sl.tax_value = 0), 0) > 0
),
plafonne AS (
  SELECT id, sale_date, montant_vente, base_tva0,
         LEAST(floor(montant_vente * :plafond / 100)::bigint, base_tva0) AS cap,
         row_number() OVER (ORDER BY base_tva0 DESC, sale_date, id)      AS rang,
         SUM(LEAST(floor(montant_vente * :plafond / 100)::bigint, base_tva0))
             OVER (ORDER BY base_tva0 DESC, sale_date, id
                   ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)     AS cumul
    FROM eligible
)
SELECT id, sale_date, montant_vente, base_tva0, rang,
       LEAST(cap, GREATEST(0, :objectif - (cumul - cap))) AS prise
  FROM plafonne
 WHERE cumul - cap < :objectif;
```

`LEAST(floor(montant_vente × plafond / 100), base_tva0)` est la traduction exacte du double plafond
de D7. Le tri `(base_tva0 DESC, sale_date, id)` est **total** : deux exécutions donnent le même
résultat, ce qui rend la simulation fiable et le traitement rejouable. L'ordre est établi sur
l'assiette exonérée, pas sur le montant total — c'est elle qui détermine la capacité réelle d'une
vente, et trier sur le total ferait remonter en tête des ventes majoritairement taxées, donc
quasiment inéligibles.

Répartition de la prise d'une vente sur ses lignes **exonérées uniquement**, au prorata, le reste de
l'arrondi étant porté par la plus élevée d'entre elles :

```sql
UPDATE sales_line sl
   SET amount_to_be_taken_into_account = sl.amount_to_be_taken_into_account - r.part,
       exclusion_motif = 'PONCTION'
  FROM (
    SELECT l.id, l.sale_date,
           floor(p.prise * l.amount_to_be_taken_into_account::numeric / p.base_tva0)
             + CASE WHEN row_number() OVER (PARTITION BY p.id
                                            ORDER BY l.amount_to_be_taken_into_account DESC, l.id) = 1
                    THEN p.prise - <somme des floor des lignes exonérées de la vente>
                    ELSE 0 END AS part
      FROM ponction_calc p
      JOIN sales_line l ON l.sales_id = p.id AND l.sales_sale_date = p.sale_date
     WHERE l.tax_value = 0            -- D7 : les lignes taxées ne sont jamais touchées
  ) r
 WHERE sl.id = r.id AND sl.sale_date = r.sale_date;
```

**Cas d'échec à traiter explicitement** : si `Σ cap < objectif`, l'objectif est **inatteignable**.
Le maximum n'est pas `35 % × ca_apres_exclusions` mais bien `Σ LEAST(35 % × montant_vente,
base_tva0)`, stocké dans `ca_ponction.montant_ponctionnable`. La simulation l'annonce avant la saisie
et la **validation est refusée** au-delà — surtout pas une ponction partielle silencieuse.

Deux situations produisent un plafond effondré, à signaler explicitement dans les avertissements :
une officine à forte part parapharmacie (peu de lignes exonérées), et une période où les exclusions
rayon / TP ont déjà vidé l'assiette exonérée.

### 5.5 Deux améliorations proposées

**a) Stratégie `UNIFORME` en complément de `DECROISSANT`.** Concentrer la ponction sur les plus gros
tickets produit un motif statistique très reconnaissable : les grosses ventes deviennent
systématiquement amputées de 35 %, les petites intactes. Une répartition **uniforme** (`taux =
objectif / ca_apres_exclusions` appliqué à toutes les ventes éligibles) est plus discrète et plus
simple à justifier — le taux est le même partout et reste très en deçà du plafond. Le champ
`strategie` est prévu ; `DECROISSANT` reste le défaut demandé.

**b) Arrondi des montants déclarés au multiple de 5.** Les montants en F CFA se manipulent au
multiple de 5. Un CA déclaré se terminant par 3 ou 7 attire l'œil. Arrondir chaque
`amount_to_be_taken_into_account` au multiple de 5 le plus proche (avec report du delta sur la ligne
la plus élevée pour préserver la somme) rend les états plus naturels. À proposer en option de
`ca_ponction`, désactivée par défaut.

---

## 6. Backend — services et API

### 6.1 Nouveaux services (`pharmaSmart-app/.../service/declaration_ca/`)

| Classe | Responsabilité |
|---|---|
| `DeclarationCaService` / `Impl` | Point d'entrée : application du pipeline §5.3 à la clôture, invariants |
| `PonctionService` / `Impl` | `simuler(param)`, `valider(param)`, `annuler(id)`, `historique(pageable)`, `detail(id)` |
| `PonctionCalculator` | Traduction de l'algorithme §5.4 en requêtes natives ; aucune logique métier |
| `ExclusionReferentielService` | Mise à jour **en masse** de `rayon.to_exclude` et `tiers_payant.to_be_exclude`. Aucun recalcul : l'effet est prospectif (D2) |
| `PeriodeDeclarationGuard` | Refuse une ponction sur une période déjà couverte par une ponction `VALIDEE`. Rôle tenu en dernier ressort par la contrainte GiST (D6), qui ne peut pas être contournée par un appel concurrent. Ne concerne pas les exclusions, qui ne réécrivent jamais le passé |
| `AuditDeclarationCaService` | Les six contrôles d'invariants du §9.1, à la demande |
| `ModeChiffreAffaireResolver` | Ramène à `REEL` tout `DECLARE` réclamé sans module souscrit (L7) |
| `JournalExclusionService` | Assemble les trois journaux (L8) : plafond d'affichage, rattachement des intitulés de tiers-payant. Aucune requête — elles vivent dans le repository |

**Repositories du module** (`pharmaSmart-domain/.../repository/`)

| Repository | Ce qu'il porte |
|---|---|
| `CaPonctionRepository` | Historique, chevauchement de périodes, alignement de l'en-tête sur le détail |
| `CaPonctionDetailRepository` | Le détail d'une ponction, pour l'écran et le justificatif |
| `JournalExclusionRepository` | Les trois journaux et leurs indicateurs (L8), en JPQL |
| `CaPeriodeRepository` | CA réel et CA après exclusions d'une période, en JPQL |

`SaleCommonService.java:140` est complété : à la clôture, le pipeline §5.3 étapes 1 à 3 est appliqué
au lieu de l'affectation `amountToBeTakenIntoAccount = salesAmount`. C'est le seul point de couture
dans le flux de vente — volontairement minimal, pour ne pas alourdir l'encaissement.

### 6.2 API REST (`web/rest/declaration_ca/`)

```
GET    /api/declaration-ca/rayons?exclus={bool}&search=&page=&size=  → page + X-Total-Count [EXCLUSION_RAYON]
PUT    /api/declaration-ca/rayons/exclusion              → { ids: [], exclure: bool }   [EXCLUSION_RAYON]
GET    /api/declaration-ca/tiers-payants?exclus={bool}&search=&page=&size=             [EXCLUSION_TP]
PUT    /api/declaration-ca/tiers-payants/exclusion       → { ids: [], exclure: bool }   [EXCLUSION_TP]
GET    /api/declaration-ca/parametres                    → { excludeFreeUnit, plafond } [EXCLUSION_UG]
PUT    /api/declaration-ca/parametres                                                   [EXCLUSION_UG]

GET    /api/declaration-ca/ponctions/assiette?fromDate&toDate  → CA réel + CA après excl. [CALLEBASSE]
POST   /api/declaration-ca/ponctions/simulation               → PonctionSimulationDTO    [CALLEBASSE]
POST   /api/declaration-ca/ponctions                          → valide (201)             [CALLEBASSE]
DELETE /api/declaration-ca/ponctions/{id}                     → annule + restaure        [CALLEBASSE]
GET    /api/declaration-ca/ponctions?dateDebut&dateFin        → historique de la période [CALLEBASSE]
GET    /api/declaration-ca/ponctions/{id}/detail              → détail par vente         [CALLEBASSE]
GET    /api/declaration-ca/ponctions/{id}/pdf                 → justificatif             [CALLEBASSE]

GET    /api/declaration-ca/journaux/unites-gratuites          → lignes + indicateurs     [EXCLUSION_UG]
GET    /api/declaration-ca/journaux/rayons                                               [EXCLUSION_RAYON]
GET    /api/declaration-ca/journaux/tiers-payants             → ventes + indicateurs     [EXCLUSION_TP]
GET    /api/declaration-ca/journaux/tiers-payants/lignes      → détail d'une vente       [EXCLUSION_TP]
```

Les journaux prennent tous `dateDebut`, `dateFin` et `recherche` — un fragment de code CIP, d'EAN ou
de libellé — auxquels le tiers-payant ajoute `tiersPayantId`. Le détail d'une vente exige
`saleId` **et** `saleDate` : `sales` est partitionnée par date, l'identifiant seul obligerait à
balayer toutes les partitions.

Chaque contrôleur porte `@RequiresFeature(Feature.…)` — qui filtre aussi les `GET`, cf.
`RequiresFeature.java` — **et** un `@PreAuthorize` de classe sur son privilège
(`pr-declaration-ca-exclusion`, `-ponction`, `-audit`). Les deux verrous sont indépendants : voir le
détail de L7. Les mises à jour en masse sont **idempotentes** (PUT avec l'état cible, pas
un toggle) pour supporter le rejeu sans surprise.

`MvtParam` reçoit un champ `mode` (`REEL` | `DECLARE`) ; les trois ressources existantes
(`BalanceResource`, `TaxeResource`, `TableauPharmatienResource`) exposent le paramètre
`?mode=` avec `DECLARE` par défaut — le comportement des appels existants reste inchangé une fois
les colonnes reprises (§11, lot L0).

### 6.3 Réponse de simulation

Livré dans `pharmaSmart-domain/.../service/declaration_ca/dto/`, avec les autres DTO du module :
le front et le batch le lisent, il ne peut pas vivre dans `pharmaSmart-app`.

```java
public record PonctionSimulationDTO(
    LocalDate dateDebut, LocalDate dateFin,
    long caReel, long caApresExclusions,
    long caAssietteTva0,                       // D7 : part exonérée, seule ponctionnable
    long montantObjectif, long montantPonctionnable, long montantPonctionne,
    long caDeclare,
    int nombreVentesEligibles, int nombreVentesImpactees,
    BigDecimal tauxMoyenApplique, BigDecimal tauxMaxApplique,
    boolean objectifAtteignable,
    int delaiAnnulationJours,                  // APP_PONCTION_ANNULATION_MAX_DAYS, pour la confirmation
    List<PonctionLigneDTO> apercu,             // premières ventes impactées
    List<String> avertissements                // chevauchement de période, plafond atteint…
) {}
```

Les deux champs `tvaCollecteeAvant` / `tvaCollecteeApres` envisagés ici n'ont pas été retenus : **V7**
étant tenu par construction — la ponction ne touche que des lignes à TVA 0, cf. D7 — les deux valeurs
sont égales par définition. Les afficher côte à côte aurait suggéré qu'elles peuvent différer.

---

## 7. Front — module « Retraitement du CA »

Nouveau module `pharmaSmart-app/src/main/webapp/app/features/declaration-ca/`, sur le modèle de
`features/comptabilite/` : une route, un composant *layout* à onglets verticaux `ngbNav`, des onglets
chargés en `@defer`, visibilité pilotée par `AbilityService.canSignal('display', '…')`.

```
features/declaration-ca/
├── declaration-ca.routes.ts
├── data-access/
│   ├── services/     declaration-ca-api.service.ts, ponction-api.service.ts
│   └── store/        ponction.store.ts            (signaux)
├── feature/
│   ├── declaration-ca-layout/
│   ├── exclusion-rayon/          table + cases à cocher + sélection de masse
│   ├── exclusion-tiers-payant/   idem
│   ├── exclusion-parametres/     bascule UG, plafond par défaut
│   ├── ponction/                 assiette, saisie, simulation, validation
│   └── ponction-historique/      liste, détail, annulation, PDF
└── ui/                           ponction-simulation-preview, exclusion-table
```

Conventions imposées par le dépôt : composants **standalone**, signaux, `@if` / `@for`, Design System
`app/shared/ui/` (`app-button`, `app-table`, `app-card`, `app-modal`, `app-badge`), libellés en
français en dur (convention dominante, cf. `docs/PLAN-GESTION-LICENCE.md` §L7), **aucun composant
`p-*`**, export par défaut du fichier de routes.

### 7.1 Écrans d'exclusion (rayon, tiers-payant)

- `app-table` avec colonne de cases à cocher, case d'en-tête « tout sélectionner sur la page » ;
- barre d'actions groupées : « Exclure la sélection » / « Réintégrer la sélection », compteur ;
- filtre `Tous / Exclus / Non exclus` + recherche plein texte ;
- ~~badge d'impact : nombre de ventes et CA sur les 12 derniers mois~~ — **abandonné** (21/08/2026),
  voir le détail de L3 : l'exclusion d'un rayon tient à la nature de ses produits, pas à leur volume ;
- bandeau permanent rappelant que **seules les ventes futures sont affectées**, dans les deux sens :
  cocher n'ampute pas le passé, décocher ne le restaure pas.

### 7.2 Écran de ponction

Trois blocs verticaux :

1. **Assiette** — sélection de période (`app-month-picker` ou plage ng-bootstrap), puis affichage de
   la cascade §5.3 : `CA réel`, `CA après exclusions`, **`dont assiette à TVA 0`**,
   `nombre de ventes éligibles`, `montant maximal ponctionnable`. Une note explique en une phrase
   pourquoi seule la part exonérée est ponctionnable (« la TVA facturée au client ne peut pas être
   réduite »), sans quoi l'écart entre le CA affiché et le plafond paraîtra arbitraire.
   Un bandeau rouge apparaît immédiatement si la période chevauche une ponction existante, avec le
   rappel de la période fautive — **avant** toute saisie.
2. **Paramètres** — bascule `Montant fixe` / `Pourcentage`, champ de saisie, plafond par vente
   (35 % par défaut, modifiable si le rôle le permet), stratégie, modes de règlement.
3. **Simulation** — bouton « Simuler » ; le résultat affiche objectif / ponctionné / CA déclaré,
   taux moyen et taux maximal appliqués, aperçu des 20 premières ventes, et la liste des
   avertissements — dont un contrôle visible « TVA collectée avant / après **ponction** : inchangée ✓ »
   qui rend l'invariant **V7** lisible par le pharmacien lui-même. Le libellé doit dire « ponction »
   et non « déclaration » : la TVA *a* baissé du fait des exclusions, et laisser croire l'inverse
   rendrait le récapitulatif du §8 incompréhensible. **Le bouton « Valider » reste désactivé
   tant qu'aucune simulation à jour n'existe** ou que `objectifAtteignable` est faux. Toute
   modification d'un paramètre invalide la simulation.

### 7.3 bis Journaux d'exclusion

Trois écrans de consultation, bâtis sur le même patron : une `app-toolbar` portant le titre et la
barre de filtres — période, recherche produit, et le tiers-payant pour le journal correspondant — un
`app-kpi-strip` sous la barre, puis le tableau.

Un seul composant sert les journaux d'unités gratuites et de rayons (`JournalLignesComponent`,
paramétré par `type`), comme un seul composant sert les deux écrans d'exclusion : mêmes filtres, même
tableau, mêmes indicateurs, deux colonnes de différence. Le tiers-payant a son composant propre, sa
présentation en maître / détail n'ayant rien de commun avec un tableau à plat.

La période s'ouvre sur le **mois en cours** : c'est celle sur laquelle porte la déclaration à venir.
Un intervalle vide qu'il faudrait renseigner avant de voir quoi que ce soit ferait payer un aller-
retour pour la consultation la plus fréquente.

### 7.3 Historique

Table paginée : période, mode, valeur saisie, CA réel, CA déclaré, montant ponctionné, taux effectif,
statut, auteur, date. Actions : détail (ventes impactées), export PDF et **annulation** — restauration
exacte depuis `ca_ponction_detail`, refusée passé le délai (voir L7), sous confirmation.

La confirmation d'annulation porte l'icône `pi pi-undo` et non l'avertissement : l'annulation
rétablit, elle ne détruit pas, et un triangle rouge ferait hésiter sur un geste correctif légitime.
Elle annonce le montant rétabli, le nombre de ventes, et surtout que **la période redevient
disponible** — c'est la conséquence que le pharmacien cherche quand il annule, puisque la contrainte
de non-chevauchement lui refusait jusque-là une nouvelle ponction sur ces dates.

---

## 8. Double vue en comptabilité

`ComptabiliteLayoutComponent` (`features/comptabilite/feature/comptabilite-layout/`) est enrichi :

- les trois composants `app-balance-mvt-caisse`, `app-taxe-report`, `app-tableau-pharmacien`
  reçoivent une entrée `mode = input<ModeCa | null>(null)` ; laissée vide, elle est **déduite de la
  licence** par `ModeCaService` — `DECLARE` si un module de retraitement est souscrit, `REEL` sinon ;
- **aucune bascule à l'écran** : les vues en chiffre encaissé sont des entrées de menu distinctes,
  dans le module Retraitement du CA, qui enveloppent les mêmes composants en forçant `REEL` ;
- ~~un pied d'écran récapitulant les retraitements appliqués~~ — **abandonné** : il demandait une API
  d'agrégation dédiée pour un confort de lecture, alors que le contrôle de cohérence et l'historique
  des ponctions expliquent déjà un écart, chacun dans son registre.

Nouvelles entrées `nav_item`, avec `required_feature` positionné :

```
declaration-ca                            → 'Retraitement du CA'     (parent)
declaration-ca.exclusion-rayon            → required_feature = 'EXCLUSION_RAYON'
declaration-ca.exclusion-tp               → required_feature = 'EXCLUSION_TP'
declaration-ca.parametres                 → required_feature = 'EXCLUSION_UG'
declaration-ca.ponction                   → required_feature = 'CALLEBASSE'
declaration-ca.ponction-historique        → required_feature = 'CALLEBASSE'
declaration-ca.balance-reelle             → 'Balance caisse (CA encaissé)'
declaration-ca.taxe-report-reel           → 'Rapport TVA (CA encaissé)'
declaration-ca.tableau-pharmacien-reel    → 'Tableau pharmacien (CA encaissé)'
declaration-ca.audit                      → 'Contrôle de cohérence'  (sans required_feature)
```

Le module s'intitule « **Retraitement du CA** » et non « Déclaration » : rien ne s'y déclare, on y
applique des retraitements au chiffre d'affaires avant qu'il ne parte à la comptabilité. Les
**identifiants techniques restent `declaration-ca`** — codes `nav_item`, route, package Java,
privilèges : ce sont eux qui portent les habilitations, et les renommer demanderait un script de
reprise pour aucun gain fonctionnel.

Le vocabulaire est « **CA encaissé** » et non « CA réel » : « réel » sous-entend que l'autre chiffre
serait faux, ce qui est le mot qu'on ne veut pas voir sur une capture d'écran. Les menus de
comptabilité restent **sans qualificatif**, leur mode dépendant de la licence.

Les entrées `…-reel` ne sont attribuées qu'aux rôles `ROLE_ADMIN` et `ROLE_PHARMACIEN` via
`nav_item_role` : le CA réel n'a pas à être visible d'un comptable externe.

Les exports PDF et Excel doivent porter le mode **dans le titre du document et dans le nom du
fichier** (`tableau_pharmacien_ca_reel.pdf` / `tableau_pharmacien_ca_declare.pdf`). Deux PDF
identiques en apparence avec des totaux différents sont une source d'erreur garantie.

---

## 9. Cohérence, contrôles et cas limites

### 9.1 Invariants à garantir

| # | Invariant | Contrôle |
|---|---|---|
| **V1** | `0 ≤ sl.amount_to_be_taken_into_account ≤ quantity_requested × regular_unit_price` | `CHECK` `sales_line_declarable_ck` (`V1.9.4`, posé `NOT VALID`) + contrôle d'audit pour les lignes antérieures |
| **V2** | `Σ lignes = sales.amount_to_be_taken_into_account` (hors ventes dépôt, cf. **I4**) | Requête de contrôle exposée dans un écran d'audit, à la demande — la tâche planifiée envisagée ici a été retirée en L7 |
| **V3** | En mode `REEL`, aucun retraitement n'est appliqué (montant = `quantity_requested × regular_unit_price`) | Test d'intégration comparant `REEL` à une somme calculée indépendamment |
| **V4** | `Σ ca_ponction_detail.montant_ponctionne = ca_ponction.montant_ponctionne` | Vrai **par construction** : l'en-tête est dérivé du détail après son insertion (L7), et vérifié par la requête d'audit |
| **V5** | `montant_ponctionne ≤ min(montant_vente × plafond / 100, montant_base)` | `CHECK` en base pour la borne `montant_base`, contrôle applicatif + requête d'audit pour le plafond |
| **V6** | Les périodes `VALIDEE` ne se chevauchent pas | Contrainte `EXCLUDE USING gist` (§4, D6) |
| **V7** | **La ponction est neutre en TVA** — conséquence directe de D7. L'invariant se mesure **avant / après le traitement de ponction**, à exclusions constantes ; **pas** entre `REEL` et `DECLARE`, où la TVA diminue légitimement du fait des exclusions | `PonctionCalculatorTest.ponctionNeutreEnTva` : `sales_tva_report` en `DECLARE`, avant puis après application d'une ponction → TVA strictement égale taux par taux, TTC du taux 0 en baisse du montant exact ponctionné, TTC taxé inchangé, et rapport rétabli après annulation |
| **V8** | Aucune ligne à `tax_value <> 0` ne porte `exclusion_motif = 'PONCTION'` | Requête d'audit + assertion en fin de traitement |
| **V9** | Pour chaque taux, `montantTaxe(DECLARE) ≤ montantTaxe(REEL)`, et l'écart s'explique **intégralement** par les lignes exclues (rayon / TP / UG) de ce taux | Contrôle `V9` de l'écran de cohérence : rapprochement par taux, écart total contre écart porté par des lignes motivées |

### 9.2 Cas limites à trancher et à traiter

| Cas | Traitement proposé |
|---|---|
| **Vente annulée après ponction** | La contrepassation (`SaleCommonService.java:496`) négate `amountToBeTakenIntoAccount` : le montant ponctionné s'annule mécaniquement. Mais la vente d'annulation tombe dans **sa propre** période. La ponction passée n'est pas recalculée ; l'écart est tracé dans l'écran d'audit |
| **Vente à cheval sur deux périodes** | Impossible : `sale_date` est atomique et fait partie de la clé primaire |
| **Vente réglée en espèces *et* en mobile** | Éligible dès qu'**au moins un** règlement relève des modes retenus. Variante plus stricte (assiette réduite à la part espèces) à arbitrer — voir §13 |
| **Vente multi-tiers-payants** (`ThirdPartySaleLine` est une collection) | Exclusion si **au moins un** TP de la vente est `to_be_exclude`. Alternative : n'exclure que la part du TP concerné, ce que `ThirdPartySaleLine.montant` et la ventilation `repartitions` (jsonb par taux de TVA) rendent techniquement faisable et fiscalement plus défendable — voir §13 |
| **Produit rattaché à plusieurs rayons** (**I5**) | Résoudre par le `storage` de la vente ; si plusieurs rayons du même storage subsistent, retenir le plus petit `rayon_id` et **journaliser l'ambiguïté**. Ne jamais exclure « si l'un des rayons est exclu » : la règle deviendrait imprévisible |
| **Licence perdue en cours de route** | Les montants déjà matérialisés ne bougent pas. Seuls les **nouveaux** calculs cessent d'appliquer le module. `ca_ponction.features_actives` conserve la trace de ce qui était souscrit |
| **Dérive de `amount_to_be_taken_into_account` (I3)** | *(hors production)* Aucune reprise de données. Le correctif est **dans le code** : recalculer la colonne à chaque modification de quantité, de prix ou de remise, et non seulement à la création de la ligne. L'invariant **V1** posé en `CHECK` empêche la dérive de réapparaître silencieusement |
| **Ventes importées** (`imported = true`) | Déjà exclues de toutes les fonctions SQL. Hors périmètre de la ponction |
| **Vente entièrement taxée** (parapharmacie pure) | `base_tva0 = 0` → écartée par le `HAVING`. Elle n'apparaît ni dans l'assiette, ni dans le détail. Le nombre de ventes éligibles affiché doit donc être **inférieur** au nombre de ventes de la période, sans que ce soit une anomalie |
| **Vente mixte exonéré / taxé** | Plafonnée par `LEAST(35 % × total, base exonérée)`. Une vente à 90 % taxée ne cède au plus que ses 10 % exonérés, jamais 35 % de son total |
| **Objectif atteignable en montant mais pas en assiette exonérée** | C'est le cas nominal du refus. Le message doit distinguer les deux causes : « plafond de 35 % atteint » et « assiette à TVA 0 épuisée » — les remèdes ne sont pas les mêmes (élargir la période *vs* revoir les exclusions) |
| **Ligne exonérée déjà ramenée à 0** par une exclusion rayon ou TP | Exclue du prorata (`base_tva0` la compte à sa valeur courante, soit 0). Aucun risque de montant négatif |

---

## 10. Licence

Rien à créer : le mécanisme décrit dans `docs/PLAN-GESTION-LICENCE.md` §3.6 est complet. Trois
câblages par module :

| Couche | Action |
|---|---|
| Menu | `UPDATE nav_item SET required_feature = 'CALLEBASSE' WHERE code LIKE 'declaration-ca.ponction%'` |
| Route | `data: { features: ['EXCLUSION_RAYON', 'EXCLUSION_TP', 'EXCLUSION_UG', 'CALLEBASSE'] }, canActivate: [AuthGuard, licenseFeatureGuard]` — **une seule** option suffit à ouvrir le module, chaque onglet restant filtré par son propre `required_feature` |
| API | `@RequiresFeature(Feature.CALLEBASSE)` sur `PonctionResource` |

Point d'attention : le **calcul** doit lui aussi être conditionné. `DeclarationCaService` interroge
`LicenseService.hasFeature(…)` avant d'appliquer chaque étape du pipeline §5.3 — sans quoi une vente
close pendant une coupure de licence recevrait un montant déclarable incohérent avec ses voisines.

---

## 11. Lotissement

| Lot | Contenu | Dépend de | Charge estimée |
|---|---|---|---|
| **L0 — Assainissement** | Corriger **I6** (division entière → TVA nulle partout : 10 fonctions SQL + `ServiceUtil.computeHtaxe` + `StockEntryServiceImpl:668`, par la migration `V1.9.0` — cf. §15), **I1** (`TaxeServiceImpl` ignore `excludeFreeUnit`), **I3** (recalcul de `amount_to_be_taken_into_account` sur modification de ligne), documenter **I2**, contrôle **V2**, tests sur les trois rapports | — | 3 j |
| **L1 — Socle déclaratif** | Colonnes `exclusion_motif` / `ponction_id`, index, paramètre `p_mode` dans les 5 fonctions SQL (nouvelle migration, jamais de modification de `V1.0.5`), `MvtParam.mode`, `?mode=` sur les 3 ressources | L0 | 3 j |
| **L2 — Pipeline d'exclusion** | `DeclarationCaService`, application des étapes 1–3 à la clôture, répartition de la réduction sur les règlements | L1 | 4 j |
| **L3 — Écrans d'exclusion** | Module Angular, layout, onglets rayon / TP / paramètres, actions de masse, API + `@RequiresFeature`, `nav_item` | L2 | 4 j |
| **L4 — Ponction** | Tables `ca_ponction` / `ca_ponction_detail` + contrainte `EXCLUDE`, `PonctionCalculator`, simulation / validation / annulation, PDF justificatif | L2 | 5 j |
| **L5 — Écrans ponction + historique** | Assiette, saisie, simulation, validation, historique, détail, annulation | L4 | 4 j |
| **L6 — Double vue comptabilité** | Mode déduit de la licence, vues encaissées dans le module Déclaration, entrées `nav_item` dédiées, nommage des exports | L1, L2 | 3 j |
| **L7 — Cohérence & recette** | Écran d'audit des invariants, gardes par privilège, justificatif PDF, délai d'annulation, jeu de tests §12 | tous | 3 j |
| **L8 — Journaux d'exclusion** | Trois écrans de consultation de ce qui a été écarté, avec marge et indicateurs ; bascule des requêtes du module vers des repositories JPQL | `EXCLUSION_*` | 2 j |
| **L9 — Reprise d'invariants** | `CHECK` de V1, contrôle V9, plafond de ponction configurable, garde de licence sur la route | tous | 0,5 j |

Ordre non négociable : **L0 avant tout le reste** — non plus pour protéger des données existantes,
mais parce que D7 et son invariant **V7** ne sont pas testables tant que la TVA calculée vaut zéro :
« TVA inchangée par la ponction » se vérifierait trivialement et ne prouverait rien.

*(hors production)* Le correctif **I6** peut être livré sans préavis ni chiffrage préalable : aucun
état déclaratif n'a été produit à partir des valeurs fautives.

---

## 12. Tests

**Unitaires (backend)**

- `PonctionCalculatorTest` — objectif atteignable / inatteignable ; plafond exactement atteint ;
  vente unique ; ex æquo sur le montant (déterminisme du tri) ; objectif nul ; assiette vide.
- **D7** — vente 100 % taxée (jamais retenue) ; vente 100 % exonérée (plafond = 35 % du total) ;
  vente mixte où `base_tva0 < 35 % × total` (c'est l'assiette qui plafonne) et le cas inverse ;
  vérification qu'aucune ligne à `tax_value <> 0` n'est modifiée (**V8**).
- `TaxeDTO` / fonctions SQL après correctif **I6** — `montantHt` d'une ligne à 18 % vaut bien
  `ttc / 1,18` et non `ttc` ; jeu de valeurs sur les trois taux 0 / 9 / 18.
- `DeclarationCaServiceTest` — les 16 combinaisons des 4 modules actifs/inactifs sur une même vente.
- Répartition sur les lignes : la somme des parts égale exactement la prise, quel que soit l'arrondi.
- `PeriodeDeclarationGuardTest` — chevauchement par la borne gauche, par la borne droite,
  inclusion stricte, période adjacente (autorisée), période d'une seule journée.

**Intégration**

- Les trois rapports en mode `REEL` renvoient des totaux identiques à ceux d'avant la livraison.
- En mode `DECLARE`, `balance.montantTtc` = `Σ tableau_pharmacien.montantTtc` =
  `Σ taxe_report.montantTtc` sur la même période — le test qui aurait détecté **I1**.
- Ponction puis annulation : l'état de `sales_line` est **strictement** identique à l'initial.
- Ponction de 35 % exactement sur une période à une seule vente : la contrainte **V5** tient.
- **V7 — le test central de D7** : `sales_tva_report` en mode `DECLARE`, mesuré **avant puis après**
  la validation d'une ponction, renvoie un `montantTaxe` **strictement identique** taux par taux ;
  seul le `montantTtc` du taux 0 diminue, et exactement du montant ponctionné.
- **V9 — les exclusions, elles, réduisent bien la TVA** : exclure un rayon de produits à 18 % fait
  baisser `montantTaxe` du taux 18 de la taxe des lignes retirées, ni plus ni moins. Le test doit
  échouer si un futur développement rendait les exclusions neutres en TVA « par sécurité ».
- **UG** : sur un produit à 18 % avec `quantity_ug = 2`, la TVA retirée vaut exactement
  `2 × prix / 1,18 × 0,18`, arrondis compris.

**Recette manuelle** — un scénario complet : exclure 2 rayons, exclure 1 TP, activer les UG,
ponctionner 15 % sur juin–juillet, vérifier que les trois écrans en `DECLARE` se recoupent, que le
`REEL` est inchangé, que **la baisse de TVA collectée s'explique intégralement par les lignes exclues
et pas du tout par la ponction** (V7 + V9), qu'une seconde ponction du 30/07 au 25/10 est refusée, et
que le PDF de justificatif reconstitue le montant à l'unité près.

---

## 13. Questions ouvertes — à trancher avant L2 et L4

| # | Question | Défaut proposé |
|---|---|---|
| **Q1** | Exclusion tiers-payant : la vente **entière** ou seulement la **part tiers-payant** ? | Vente entière, conformément au libellé `beExclude`. **Réserve** : la part TP est facturée à l'assureur et donc traçable hors du logiciel ; n'exclure que la part assuré serait plus défendable. `ThirdPartySaleLine.repartitions` (jsonb ventilé par taux de TVA) rend les deux options réalisables sans travail supplémentaire de modèle |
| **Q2** | Vente réglée partiellement en espèces : éligible en totalité ou à hauteur de la part espèces ? | Éligible en totalité si au moins un règlement est en espèces |
| ~~**Q3**~~ | ~~Cocher un rayon doit-il proposer un recalcul rétroactif ?~~ | **Tranchée : strictement prospectif, dans les deux sens.** Aucun recalcul n'est proposé, même optionnel : une vente close porte définitivement le montant déclaré à sa clôture. Cf. D2 |
| **Q4** | Le plafond de 35 % est-il un paramètre d'officine ou saisissable à chaque ponction ? | **Les deux, tranché le 21/08/2026** : `APP_PONCTION_PLAFOND_DEFAUT` en base (35 %), surchargeable à chaque ponction depuis l'écran. La surcharge n'est pas réservée à `ROLE_ADMIN` — le privilège `pr-declaration-ca-ponction` suffit, celui qui décide du montant décide aussi de sa répartition |
| **Q5** | Qui peut voir la vue « CA réel » ? | `ROLE_ADMIN` et `ROLE_PHARMACIEN` |
| **Q6** | Faut-il un verrouillage de période après déclaration fiscale ? | Non, révisé le 21/08/2026 — un verrou manuel que personne ne pose ne verrouille rien. Remplacé par un délai d'annulation configurable (L7) |
| **Q7** | Les règlements mobile money entrent-ils dans l'assiette ? | Non en lot 1 ; le champ `modes_reglement` est prévu pour les ouvrir sans migration |
| ~~**Q8**~~ | ~~La restriction TVA 0 doit-elle s'appliquer aussi aux exclusions rayon, TP et UG ?~~ | **Tranchée : non.** Ces trois exclusions retirent la ligne avec sa TVA. Le montant écarté étant rattaché à un produit et à une quantité identifiés (`quantity_ug` pour les UG), la taxe correspondante se déduit exactement et le rapport TVA reste juste sur un périmètre plus étroit. Seule la ponction, montant arbitraire sans contrepartie identifiable, est soumise à D7. Cf. §4 D7 et §5.3 |

---

## 14. Ce que ce plan ne fait pas

- Il ne touche pas au calcul des ventes, des règlements, des stocks ni de la facturation
  tiers-payant : seule la colonne `amount_to_be_taken_into_account` est écrite.
- Il ne modifie **jamais** `sales.sales_amount`, `sales_line.sales_amount` ni les montants réglés.
- Il ne traite pas la déclaration FNE (facture normalisée électronique), qui repose sur les montants
  réels et doit continuer à le faire.
- Il ne laisse **jamais** la ponction toucher la TVA (D7, invariant V7). En revanche les exclusions
  rayon / TP / UG réduisent bien la TVA collectée déclarée, à hauteur exacte de la taxe des lignes
  retirées : c'est voulu, et l'écart est rapproché ligne à ligne par l'invariant V9.
- Il ne change **pas le type** de `sales_line.tax_value` ni de `tva.taux` : ils restent `integer`,
  qui est le type juste pour des taux entiers. La correction du bug TVA se fait par cast au point
  d'usage (§15.1).
- Il ne modifie **aucune migration existante** — `V1.0.5__procedure.sql` a déjà été exécuté en base
  et reste intact. Les fonctions fautives sont redéfinies par `CREATE OR REPLACE` dans
  `V1.9.0__fix_tva_division_entiere.sql`, et toutes les autres évolutions du plan (colonnes, tables,
  contraintes) passent par des migrations additives versionnées.

---

## 15. Annexe — correction du bug TVA (I6) en entier

Le recensement complet donne **trois défauts distincts**, pas un seul. Les traiter ensemble sans les
distinguer conduirait à masquer le troisième.

### 15.1 Défaut A — division entière dans 10 fonctions SQL

`sales_line.tax_value` est `integer` (`V1.0.1__init.sql:2157`), `Tva.taux` est un `Integer` valant
0, 9 ou 18. En PostgreSQL, `18 / 100 = 0`.

| Fonction (`V1.0.5__procedure.sql`) | Lignes | Consommateur |
|---|---|---|
| `sales_summary_json` | 1142 | Résumé de ventes |
| `sales_balance` | 1229, 1246 | `app-balance-mvt-caisse` |
| `sales_summary_by_type_json` | 1330 | Résumé par type |
| `sales_tva_report` | 1409, 1426 | `app-taxe-report` |
| `sales_tva_report_journalier` | 1485, 1502 | `app-taxe-report` (vue journalière) |
| `tableau_pharmacien_report` | 1569, 1586 | `app-tableau-pharmacien` |
| `tableau_pharmacien_month_report` | 1690, 1707 | idem, groupement mensuel |
| `rapport_activite_vente_report` | 1882, 1899 | Rapport d'activité |
| `get_historique_vente` | 2147 | Historique produit |
| `get_product_sales_summary` | 2358 | Synthèse produit |

**Correction — une nouvelle migration `V1.9.0__fix_tva_division_entiere.sql`.**

`V1.0.5__procedure.sql` a déjà été exécuté en base : il n'est pas modifié. Le nouveau script
redéfinit les 10 fonctions par `CREATE OR REPLACE`, **signatures inchangées** — donc aucun impact sur
`SalesRepository`, aucun `DROP`, aucune somme de contrôle Flyway invalidée, et un retour arrière
possible en rejouant l'ancienne définition.

Le coût réel de cette voie est la **duplication** : environ 700 lignes de SQL existeront en deux
versions, dont celle de `V1.0.5` sera morte sans que rien ne le signale au lecteur. Deux mesures le
limitent, et elles font partie du livrable :

1. **Un en-tête explicite** dans `V1.9.0`, nommant chaque fonction reprise et indiquant qu'elle
   remplace la définition de `V1.0.5` — le seul endroit où un futur lecteur ira chercher.
2. **L'extraction de l'arithmétique dans `ht_from_ttc()`** (voir plus bas), appelée par les 10
   fonctions redéfinies. La formule cesse d'être recopiée 16 fois : les deux versions du fichier ne
   peuvent plus diverger *sur ce point précis*, et une évolution future du calcul se fait en un seul
   endroit.

> Convention à tenir désormais : `V1.9.0` devient la source de vérité des fonctions de rapport. Toute
> évolution ultérieure part de **ce** fichier, jamais de `V1.0.5`.

Le remplacement, appliqué aux 16 occurrences :

```sql
-- avant
/ nullif(1 + (sl.tax_value / 100), 0)
-- après
/ nullif(1 + sl.tax_value::numeric / 100, 0)
```

`::numeric` et non `/ 100.0` : `100.0` produit du `double precision`, donc de l'arithmétique binaire
dont la somme dépend de l'ordre d'agrégation. Sur des montants, `numeric` est le type juste — décimal
exact, résultat déterministe, `sum()` reproductible. Le `nullif` devient inutile (le diviseur ne peut
plus valoir 0 pour un taux positif) mais il est conservé : il ne coûte rien et protège d'une donnée
aberrante.

#### Pourquoi ne pas passer `tax_value` en `double` / `float`

L'idée est naturelle — le bug vient d'une division entière, donc rendons le champ décimal — mais elle
coûte beaucoup plus cher qu'elle ne rapporte, et elle introduit un défaut pire que celui qu'elle
corrige.

| | Cast au point d'usage (`::numeric`) | Changement de type en `double` / `float` |
|---|---|---|
| Portée | 16 lignes de SQL | `sales_line.tax_value`, `tva.taux`, leurs entités, DTO, modèles front, gabarits — **43 fichiers** touchent `taxValue` / `codeTva` / `getTaux()` |
| Migration de schéma | aucune | `ALTER COLUMN … TYPE` sur au moins deux tables |
| Les 16 divisions | corrigées | **toujours à revoir une par une** — le type change, pas les expressions |
| Regroupement | `group by tax_value` sûr | `group by` sur un flottant : `18.000000000000004` et `18.0` forment **deux groupes**. Le rapport TVA se scinderait en lignes fantômes |
| Agrégation | `sum(numeric)` déterministe | `sum(double)` dépend de l'ordre de lecture : deux exécutions du même rapport peuvent différer d'un franc |
| Sérialisation | `codeTva` reste un entier, affiché « 18 % » | `18.0` à afficher, à comparer, à utiliser comme clé de `Map` |

Le point décisif est le troisième : **changer le type ne corrige rien tout seul**. `18.0 / 100`
donnerait bien `0.18`, mais il faudrait quand même relire les 16 expressions pour s'en assurer — on
paierait la migration *en plus* du travail, pas *à la place*.

Et si l'on devait vraiment stocker un taux non entier, le type juste serait **`numeric(5,2)`**, pas
`float` ni `double` : sur des données qui alimentent une déclaration fiscale, on ne veut pas
d'arithmétique binaire approchée. Aujourd'hui les taux du domaine sont 0, 9 et 18 — des entiers.
`integer` est le bon type ; c'est l'expression qui était fausse, pas la colonne.

> **Décision** — le type reste `integer`. La correction se fait par cast au point d'usage.
> Si un besoin de taux fractionnaire apparaît (5,5 %, 2,5 %), ce sera `numeric(5,2)` et une décision
> métier distincte, pas un effet de bord d'une correction de bug.

**Se prémunir de la récidive — et limiter la duplication.** Le cast est une discipline, donc
oubliable, et il serait recopié 16 fois dans `V1.9.0`. Plutôt qu'un type flottant, une fonction SQL
unique, livrée en tête de `V1.9.0` et appelée par les 10 fonctions redéfinies :

```sql
CREATE FUNCTION ht_from_ttc(p_ttc numeric, p_taux integer) RETURNS numeric
  LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$ SELECT p_ttc / nullif(1 + p_taux::numeric / 100, 0) $$;
```

Un seul endroit à avoir juste, et `grep` retrouve immédiatement tout calcul qui la contourne. C'est
la réponse au risque réel — la récidive — sans toucher au schéma. `IMMUTABLE` et `PARALLEL SAFE`
permettent au planificateur de l'inliner : aucun coût mesurable par rapport à l'expression écrite en
clair.

**Décision associée à trancher : `ceiling` → `round`.** L'arrondi actuel est
`ceiling(sum(ttc / (1 + t)))`. Arrondir le HT **au-dessus** revient à arrondir la TVA (`ttc - ht`)
**en dessous** : la taxe collectée est systématiquement sous-déclarée, d'un franc par agrégat.
`round` est neutre. Recommandation : passer à `round`, mais **dans la même migration et annoncé comme
tel** — c'est un second changement de valeur, il ne doit pas se cacher derrière le premier. L'arrondi
reste appliqué **sur la somme** et non par ligne, pour ne pas accumuler les erreurs ligne à ligne.

### 15.2 Défaut B — le même bug en Java

`ServiceUtil.computeHtaxe(long ttc, int taxe)` et `computeHtaxe(int ttc, int taxe)` calculent
`ttc / (1 + (taxe / 100))` : division entière identique, donc `ht = ttc`.

Une troisième surcharge, `ServiceUtil.calculHt(int ttc, int tva)`, fait `ttc * 1.0 / (1 + (tva / 100.f))`
— **correcte**, et c'est celle qu'utilise la FNE (`FneServiceImpl.java:250`). Deux formules
concurrentes cohabitent donc, et ont divergé.

**Correction.** Supprimer les deux surcharges entières plutôt que les réparer, et faire converger
tous les appelants vers une implémentation unique en `BigDecimal`. Laisser trois méthodes qui
répondent à la même question est ce qui a produit l'écart ; le corriger sans consolider le
reproduirait.

### 15.3 Défaut C — un montant utilisé comme un taux

`StockEntryServiceImpl.java:668` :

```java
commande.setHtAmount(ServiceUtil.computeHtaxe(commande.getGrossAmount(), commande.getTaxAmount()));
```

`Commande.taxAmount` est un **montant de taxe** (`tax_amount`, `Commande.java:91`), pas un taux. Il
est passé au paramètre `taxe` qui attend un pourcentage. Sur une commande à 1 000 000 TTC dont
90 000 de taxe, la division entière donne `1 + 900 = 901`, donc `htAmount = 1 109`.

C'est un défaut **différent** — et le défaut B le masque partiellement aujourd'hui : réparer B sans
traiter C aggraverait le résultat au lieu de l'améliorer. Le calcul juste est ici trivial :
`htAmount = grossAmount - taxAmount`.

> **À traiter dans le même lot que B, obligatoirement.** Corriger `computeHtaxe` en laissant cet
> appel en place produirait des montants HT de commande faux d'une autre manière.

### 15.4 Défaut connexe D — `tvaDeductible` toujours nul

`TaxeServiceImpl.java:81-87` calcule la TVA déductible à partir de `taxe.getMontantAchat()`. Or
`sales_tva_report` ne projette **pas** `montantAchat` dans son `jsonb_build_object` : le champ reste
à 0 après désérialisation, donc `tvaDeductible = 0` et `tvaNette = montantTaxe`.

Ce n'est pas un défaut d'arithmétique mais un champ manquant dans la projection. **Hors périmètre de
la correction I6** : à traiter comme une évolution du rapport, une fois que `montantTaxe` sera non nul
et que l'écart deviendra visible.

### 15.5 Comment prouver que la correction est juste

Il n'existe aucune valeur de référence en base : le contrôle ne peut pas être « comparer à
l'existant », puisque l'existant est faux.

1. **Oracle indépendant** — `ServiceUtil.calculHt`, déjà correct et utilisé par la FNE, sert de
   référence. Test : pour un jeu de lignes aux trois taux, le `montantHt` renvoyé par
   `sales_tva_report` doit égaler la somme des `calculHt` ligne à ligne, à l'arrondi d'agrégat près.
2. **Test d'intégration sur PostgreSQL réel** — Testcontainers est déjà dans le projet
   (`pom.xml:270`, `pharmaSmart-app/pom.xml:472`). Un test insère un jeu figé de `sales` /
   `sales_line` aux taux 0 / 9 / 18 et vérifie les 10 fonctions. Un test unitaire avec
   `SalesRepository` simulé ne verrait rien : le bug est **dans le SQL**.
3. **Recoupement métier** — la TVA du rapport doit se recouper avec celle des factures normalisées
   émises sur la même période. C'est le seul contrôle que le pharmacien puisse faire lui-même, et il
   vaut aussi bien sur un jeu de recette que sur des données réelles.

*(hors production)* Le chiffrage préalable de l'écart sur une copie de production, prévu
initialement, est sans objet : il n'y a pas de base à comparer. Le jeu de recette du point 2 en tient
lieu.

### 15.6 Ce que la correction change dans les écrans

| Grandeur | Avant | Après |
|---|---|---|
| `montantHt` (les 3 écrans) | = `montantTtc` | `ttc / (1 + taux)`, donc **plus bas** |
| `montantTaxe` | **0** à tous les taux | montant réel |
| `tvaNette` | `-tvaDeductible` (nul aujourd'hui, cf. D) | `montantTaxe - tvaDeductible` |
| Marge et ratios dérivés du HT | surévalués | corrigés à la baisse |
| Lignes à taux 0 | inchangées | inchangées |

Le changement va dans le sens d'une **baisse du HT affiché** et d'une TVA collectée qui cesse d'être
nulle. *(hors production)* Aucune annonce préalable n'est requise ; en revanche les captures d'écran,
jeux de démonstration et documents commerciaux réalisés avant le correctif deviennent faux et sont à
refaire.

### 15.7 Découpage proposé

| Étape | Contenu | Livrable |
|---|---|---|
| **1** | Défauts **B** et **C** ensemble : consolidation de `computeHtaxe` en `BigDecimal`, correction de `StockEntryServiceImpl:668` | Java + tests unitaires |
| **2** | Défaut **A** : migration `V1.9.0__fix_tva_division_entiere.sql` — `ht_from_ttc()` puis `CREATE OR REPLACE` des 10 fonctions, `numeric` + `round` | Nouvelle migration + tests Testcontainers |
| *(hors lot)* | Défaut **D** : projeter `montantAchat` dans `sales_tva_report` | Évolution du rapport |

L'ordre 1 avant 2 est délibéré : le SQL et le Java doivent produire le même HT à la fin, et corriger
le SQL en premier laisserait une fenêtre où les deux divergent en sens inverse.
