# Plan — Programme de fidélité client

> Statut : **plan** — aucune ligne de code écrite. Rédigé le 2026-10-03, révisé le 2026-10-08 (relevé du code, décisions F8, F9, F11, phases réordonnées).
> Origine : écart 8.5 de [PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md](PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md) §8
> (« pas de programme de fidélité : points, remise palier »), sorti dans un plan dédié : c'est un chantier métier avant d'être un chantier technique.

## 1. Pourquoi un plan à part, et ce qu'il faut décider d'abord

Un programme de fidélité engage l'officine sur trois terrains que le code ne tranche pas :

1. **Réglementaire et déontologique** : accorder un avantage à l'achat de médicaments est encadré, et pas de la même façon selon le pays et selon la nature du
   produit (remboursable ou non, sur ordonnance ou non, stupéfiant). **Je ne tranche pas ici ce qui est permis** : c'est à valider avec le pharmacien titulaire
   et, au besoin, l'Ordre ou l'autorité locale **avant** de chiffrer quoi que ce soit.
2. **Comptable** : un point consommé est une remise (il réduit le chiffre d'affaires) et non un encaissement. Le traiter comme un mode de règlement fausserait la caisse.
3. **Commercial** : le barème (combien de points, pour quelle dépense, et quelle valeur à l'usage) fixe le coût réel du programme.

Le plan commence donc par des décisions (§3), pas par du développement.

## 2. Ce que le code offre déjà (relevé le 2026-10-08)

Aucune notion de fidélité n'existe (ni entité, ni table, ni écran). Le socle, lui, est en partie là :

| Existant | Utilité pour la fidélité |
|---|---|
| `Customer`, `UninsuredCustomer`, `AssuredCustomer` | le porteur des points ; la création express d'un client existe déjà à l'écran de vente |
| `AvoirClient` et `AvoirClientUtilisation` | un crédit utilisable en plusieurs fois : modèle de traçabilité d'un usage (montant, par qui, quand) |
| `CustomerConsentement` + `CanalConsentement` (`SMS`, `WHATSAPP`, `EMAIL`) | accord ou refus par canal (la ligne la plus récente fait foi) : ajouter un canal `FIDELITE` pour la preuve d'inscription |
| `Produit.statutLegal`, `CodeRemise.NONE`, ligne « non remboursé » (`V2.1.23`) | les critères d'exclusion de la base de gain (F2) existent déjà |
| `fiche-client-panel` | l'endroit où afficher solde et historique, comme les avoirs ou le crédit |
| `payment_mode` (référentiel) | où **ne pas** mettre la fidélité (voir §1.2) |
| `mv_customer_rfm` et le tableau de bord | de quoi mesurer l'effet du programme (récence, fréquence, montant) |
| Pipeline nocturne (`NightlyPipelineJobConfig`, lancé par `JobOrchestrationService`) | où ajouter l'étape d'expiration des points, plutôt qu'un `@Scheduled` isolé |
| `app_configuration` (modèle `V2.1.30`, `INSERT … ON CONFLICT DO NOTHING`) | barème, durée, plafond, activation |

**À ne pas réutiliser** :
- `RemiseClient` / `GrilleRemise` : **à décommissionner**, plus utilisés. Pas de « palier » bâti dessus. Son décommissionnement est un chantier à part,
  hors de ce plan.
- Vente simplifiée (`SimplifiedSaleServiceImpl`) : **à décommissionner**, aucune accroche fidélité.

### Points d'accroche dans le code

Toute clôture ou tout retour en arrière sur une vente doit passer par le service de fidélité :

| Événement | Où | Effet fidélité |
|---|---|---|
| Clôture comptant et assurance | `SaleCommonService.finalizeSale` (commun aux deux) | `GAIN`, et `UTILISATION` si des points sont utilisés |
| Annulation comptant | `SaleServiceImpl.cancelCashSale` | `ANNULATION` du gain, restitution de l'utilisation |
| Annulation assurance | `ThirdPartySaleServiceImpl.cancelSale` | idem |
| Régularisation d'une vente assurance | `ThirdPartySaleServiceImpl.copiePourEdition` puis `editSale` | annulation sur l'originale, nouveau gain sur la copie : sans cela, double gain |
| Retour client | `RetourClientServiceImpl.validerRetour` (ligne par ligne) | annulation **au prorata** des lignes retournées |
| Fusion de clients | `FusionClientService.fusionner` | déplacer les mouvements, fusionner les comptes |
| Vente dépôt | `SaleDepotExtensionImpl` | exclue |

## 3. Décisions

| | Question | Décision / piste |
|---|---|---|
| **F1** | Quel mécanisme : points, palier ou bon d'achat ? | Piste : **points** convertis en **remise** à l'encaissement. Le palier ne s'appuiera pas sur `RemiseClient` (décommissionnée). *À valider.* |
| **F2** | Sur quoi gagne-t-on des points ? | À valider (§1.1). Pistes : ne compter que la **part client** ; exclure produits remboursés, stupéfiants (`statutLegal`), produits non remisables (`CodeRemise.NONE`). *À valider.* |
| **F3** | Quel barème ? | Taux d'acquisition (points pour 1 000 F) et valeur d'usage (F par point), **réglables** dans `app_configuration`. *À chiffrer.* |
| **F4** | Quand les points expirent-ils ? | Une durée (par exemple 12 mois après le gain), ou jamais. *À valider.* |
| **F5** | Qui peut ajuster un solde à la main ? | Privilège dédié (`pr-ajuster-points-fidelite`), motif obligatoire, journalisé. *À valider.* |
| **F6** | Les ventes sans client gagnent-elles quelque chose ? | Non : pas de client, pas de compte. L'écran **propose** d'associer un client avant le règlement. |
| **F7** | Inscription : consentement ? | Oui, avec preuve (date, utilisateur) : canal `FIDELITE` dans `CustomerConsentement`. *À valider.* |
| **F8** | Vente différée : gain à la clôture ou au règlement ? | **Décidé : à la clôture.** |
| **F9** | Annulation d'une vente sur laquelle des points ont été utilisés | **Décidé : les points utilisés sont repositionnés sur le compte du client** (mouvement `ANNULATION` positif qui référence l'`UTILISATION`). |
| **F10** | La remise fidélité entre-t-elle dans l'assiette du CA déclaré (ponction, `declarationCaService.appliquerExclusions`) ? | *À trancher.* |
| **F11** | La vente simplifiée fait-elle gagner des points ? | **Décidé : hors périmètre**, service à décommissionner. |

**Reste ouvert sous F9** : une vente annulée dont le **gain** a déjà été dépensé sur une autre vente. Retirer ce gain ferait passer le solde sous zéro.
Il faut choisir entre autoriser un solde négatif (qui se rembourse sur les gains suivants) et ne retirer que ce qui reste. *À trancher.*

## 4. Modèle de données

Un **grand livre en écriture seule** : le solde est la somme des mouvements, jamais une colonne qu'on modifie. C'est ce qui permet d'expliquer chaque point.

```
fidelite_compte      customer_id (PK, FK), inscrit_le, inscrit_par, consentement_le, actif
fidelite_mouvement   id, customer_id, type, points, montant_base, sale_id, sale_date, cree_le, cree_par, commentaire, mouvement_annule_id
                     type : GAIN | UTILISATION | EXPIRATION | AJUSTEMENT | ANNULATION
sales                + remise_fidelite (montant en F déduit grâce aux points, 0 par défaut)
```
- `points` signé (gain positif, utilisation et expiration négatives).
- Idempotence : index unique sur `(sale_id, sale_date, type)` pour `GAIN` et `UTILISATION` ; un même enregistrement rejoué ne gagne qu'une fois.
- `sale_id` + `sale_date` : la table `sales` est **partitionnée** par date (identifiant composé) ; comme pour les autres tables qui la référencent, pas de clé
  étrangère simple, à trancher à l'écriture de la migration.
- `mouvement_annule_id` : une annulation **référence** le mouvement qu'elle défait ; on n'efface jamais une ligne. Sur une annulation de vente, le mouvement
  porte aussi l'identifiant de la copie négative créée par `copySale`, pour retrouver la contrepassation dans l'historique.
- `sales.remise_fidelite` est une **colonne distincte** de `discountAmount`, que `SaleAmountCalculator` recalcule à partir des remises produit : la fidélité
  y serait écrasée, et les rapports ne sauraient plus la distinguer d'une remise commerciale.
- Une vue du solde par client (somme des mouvements) pour l'écran et les rapports ; pas de colonne `solde` à tenir à jour.
- Migrations **nouvelles** à partir de `V2.1.32` (jamais une migration existante modifiée) ; aucun nom de schéma en dur.

## 5. Parcours

### 5.1 Gain
À la **clôture** (`finalizeSale`) d'une vente comptant ou assurance avec client inscrit, différée comprise (F8) : un mouvement `GAIN` calculé sur la base
retenue (F2) au barème (F3). Écrit **dans la même transaction que la vente** (appel direct, pas d'événement après validation) : une vente ne peut pas exister
sans son gain, ni l'inverse.

### 5.2 Usage
À l'étape de règlement : « Utiliser N points (= X F) » proposé quand le solde le permet, dans la limite du plafond par vente. L'usage alimente
`sales.remise_fidelite`, que `SaleAmountCalculator` déduit **après** les remises produit et **avant** l'arrondi de caisse au multiple de 5 ; il crée un mouvement
`UTILISATION`. Il ne passe **pas** par `payment_mode` : aucun encaissement fictif, la caisse reste juste.

### 5.3 Annulations (le point délicat)
Tout ce qui défait une vente doit défaire ses points (accroches au §2) :
- **annulation** d'une vente comptant ou assurance : `ANNULATION` du gain, et les points utilisés sont **repositionnés** sur le compte (F9) ;
- **régularisation** d'une vente assurance (`copiePourEdition`) : annulation complète sur la vente d'origine, puis gain et usage recalculés à la clôture de la copie ;
- **retour client** : annulation du gain **au prorata** du montant des lignes retournées (la base de gain de ces lignes) ;
- **fusion de clients** : les mouvements suivent le client conservé ; deux comptes fusionnés n'en font qu'un (date d'inscription la plus ancienne).

C'est le principal risque du chantier : une annulation oubliée fabrique des points gratuits. C'est pourquoi toutes ces accroches sont livrées **avec** le gain
(phase 1), et non après.

### 5.4 Expiration
Étape du pipeline nocturne : un mouvement `EXPIRATION` par lot de points échu (F4), idempotent par lot, après le message d'avertissement au client si le
consentement le permet.

## 6. Écrans

| Écran | Ajout |
|---|---|
| `fiche-client-panel` | solde, historique des mouvements ; bouton « Inscrire au programme » (avec consentement) |
| Vente (récapitulatif des montants) | « points à gagner sur cette vente » et solde du client |
| Règlement (`payment-mode`) | « Utiliser N points » ; le montant à payer se met à jour |
| Tickets comptant et assurance (`CashSaleReceiptService`, `AssuranceSaleReceiptService`) | points gagnés, points utilisés, solde après la vente |
| Paramètres | barème (F3), durée (F4), plafond d'usage, activation du programme |
| Tableau de bord | points émis, utilisés, expirés ; coût du programme ; clients actifs |

## 7. Droits, audit et garde-fous
- Nouveaux privilèges (lignes `nav_item` de niveau 3, rôles posés par migration, modèle `V2.1.27`) : inscription, ajustement manuel (F5).
- Tout endpoint `/api/**` porte `@RequiresNavAccess` ou `@NavAccessExempt`.
- Le grand livre est **écrit par le serveur seul** ; l'écran ne calcule pas de points, il affiche ce que le serveur lui donne.
- Paramètre `APP_FIDELITE_ACTIVE` à `0` par défaut : le code peut partir en production avant la validation du programme.
- Un plafond d'usage par vente (par exemple pas plus de X % du montant) évite qu'un solde élevé vide une vente.
- Tests d'intégration obligatoires sur la **réversibilité** : gagner, annuler, vérifier que le solde revient exactement, pour chaque accroche du §2.

## 8. Phases

Tranches verticales : rien n'est activable sans sa réversibilité.

| Phase | Contenu | Critère de sortie | Effort |
|---|---|---|---|
| **0** | Valider F1 à F7, F10 et le cas ouvert de F9 avec le pharmacien titulaire ; avis réglementaire écrit sur F2 | décisions notées au §3 | 0,5 j (+ délai de l'avis) |
| **1** | Socle et réversibilité : migration (tables, vue du solde, paramètres, canal `FIDELITE`), entités, `FideliteService` (gain, annulation au prorata, solde) ; **toutes** les accroches du §2 (clôture, annulations, régularisation, retour, fusion) ; tests d'intégration | un gain et un solde justes, rejouables sans doublon, qui reviennent exactement à zéro après chaque type d'annulation | 4 j |
| **2** | Usage au règlement : `sales.remise_fidelite`, déduction dans `SaleAmountCalculator` avant l'arrondi, plafond, mouvement `UTILISATION`, repositionnement à l'annulation (F9) | un règlement avec points réduit le montant, la caisse reste juste, l'annulation rend les points | 2,5 j |
| **3** | Écrans : fiche client, récapitulatif, règlement, tickets, paramètres ; tests Jest | parcours complet à l'écran | 3 j |
| **4** | Droits : migration `nav_item` (inscription, ajustement), `@RequiresNavAccess`, ajustement manuel avec motif | un utilisateur sans droit reçoit un 403 | 0,5 j |
| **5** | Expiration (pipeline nocturne), message d'avertissement, tableau de bord | un lot échu expire une seule fois | 2 j |

Total : **environ 12,5 jours**, hors délai de l'avis réglementaire, dont 4 pour le socle avec toutes ses annulations.

## 9. Hors périmètre
- Cartes physiques, QR codes et application mobile du client (l'identification se fait par la fiche client existante : nom, téléphone, code).
- Partage de points entre officines d'un même groupe.
- Offres ciblées et campagnes (le plan sert le socle ; `mv_customer_rfm` permettra d'y venir).
- Vente simplifiée et ventes dépôt.
- Décommissionnement de `RemiseClient` / `GrilleRemise` et de la vente simplifiée (chantiers distincts).
