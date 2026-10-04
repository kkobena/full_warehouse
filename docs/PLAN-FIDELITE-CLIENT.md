# Plan — Programme de fidélité client

> Statut : **plan** — aucune ligne de code écrite. Rédigé le 2026-10-03.
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

## 2. Ce que le code offre déjà (relevé le 2026-10-03)

Aucune notion de fidélité n'existe (ni entité, ni table, ni écran). Le socle, lui, est en partie là :

| Existant | Utilité pour la fidélité |
|---|---|
| `Customer`, `UninsuredCustomer`, `AssuredCustomer` | le porteur des points ; la création express d'un client existe déjà à l'écran de vente |
| `RemiseClient` (rattachée au client) et `GrilleRemise` | une remise automatique **par client**, avec taux : le mécanisme d'un « palier » à la place des points |
| `AvoirClient` et `AvoirClientUtilisation` | un crédit utilisable en plusieurs fois (montant utilisé, par qui, quand) : le mécanisme d'un **bon d'achat** |
| `CustomerConsentement` | accord ou refus **par canal de message** (la ligne la plus récente fait foi) : à réutiliser pour les messages du programme, **pas** pour l'inscription elle-même |
| `fiche-client-panel` | l'endroit où afficher solde et historique, comme les avoirs ou le crédit |
| `payment_mode` (référentiel) | où **ne pas** mettre la fidélité (voir §1.2) |
| `mv_customer_rfm` et le tableau de bord | de quoi mesurer l'effet du programme (récence, fréquence, montant) |
| Module batch | de quoi expirer des points la nuit |

## 3. Décisions à prendre

| | Question | Piste |
|---|---|---|
| **F1** | Quel mécanisme : **points** (grand livre), **palier** (remise automatique selon le cumul), ou **bon d'achat** (cashback en avoir) ? | **Points** avec conversion en **remise** à l'encaissement : le plus lisible pour le client, et le seul qui tienne un historique ligne à ligne. Le palier peut s'y greffer plus tard. |
| **F2** | Sur quoi gagne-t-on des points ? | À valider (§1.1). Pistes à examiner : exclure les produits remboursés et la part prise en charge par le tiers payant ; ne compter que la **part client** ; exclure stupéfiants et produits déjà remisés. |
| **F3** | Quel barème ? | Un taux d'acquisition (points pour 1 000 F) et une valeur d'usage (F par point), tous deux **réglables** (`app_configuration`), pas codés. |
| **F4** | Quand les points expirent-ils ? | Une durée (par exemple 12 mois après le gain), ou jamais. Une expiration exige un message au client et un job nocturne. |
| **F5** | Qui peut ajuster un solde à la main ? | Un privilège dédié (`pr-ajuster-points-fidelite`), avec motif obligatoire, journalisé. |
| **F6** | Les ventes sans client gagnent-elles quelque chose ? | Non, par construction : pas de client, pas de compte. L'écran doit alors **proposer** d'associer un client avant le règlement. |
| **F7** | Inscription : le client doit-il consentir ? | Oui, en conservant la preuve (date, utilisateur) : même principe que `CustomerConsentement`, avec un canal « programme de fidélité » à ajouter. |

## 4. Modèle de données

Un **grand livre en écriture seule** : le solde est la somme des mouvements, jamais une colonne qu'on modifie. C'est ce qui permet d'expliquer chaque point.

```
fidelite_compte      customer_id (PK, FK), inscrit_le, inscrit_par, consentement_le, actif
fidelite_mouvement   id, customer_id, type, points, montant_base, sale_id, sale_date, cree_le, cree_par, commentaire, mouvement_annule_id
                     type : GAIN | UTILISATION | EXPIRATION | AJUSTEMENT | ANNULATION
```
- `points` signé (gain positif, utilisation et expiration négatives) ; contrainte : le solde d'un compte ne devient jamais négatif.
- `sale_id` + `sale_date` : la table `sales` est **partitionnée** par date (identifiant composé) ; comme pour les autres tables qui la référencent, pas de clé
  étrangère simple, à trancher à l'écriture de la migration.
- `mouvement_annule_id` : une annulation **référence** le mouvement qu'elle défait ; on n'efface jamais une ligne.
- Une vue du solde par client (somme des mouvements) pour l'écran et les rapports ; pas de colonne `solde` à tenir à jour.
- Migrations **nouvelles** (jamais une migration existante modifiée) ; aucun nom de schéma en dur.

## 5. Parcours

### 5.1 Gain
À la **finalisation** d'une vente comptant ou assurance avec client inscrit : un mouvement `GAIN` calculé sur la base retenue (F2) au barème (F3). Idempotent :
un même couple vente/date ne gagne qu'une fois, même si l'enregistrement est rejoué.

### 5.2 Usage
À l'étape de règlement : « Utiliser N points (= X F) » proposé quand le solde le permet. L'usage **réduit le montant à payer** comme une remise globale, et crée un
mouvement `UTILISATION`. Il ne passe **pas** par `payment_mode` : aucun encaissement fictif, la caisse reste juste.

### 5.3 Annulations (le point délicat)
Tout ce qui défait une vente doit défaire ses points :
- annulation d'une vente finalisée, **retour client** (avoir), régularisation après coup : un mouvement `ANNULATION` qui référence le gain d'origine ;
- si les points gagnés ont déjà été dépensés, le solde passerait sous zéro : **règle à fixer** (laisser un solde négatif, ou bloquer l'annulation de l'usage correspondant).
C'est le principal risque du chantier : une annulation oubliée fabrique des points gratuits.

### 5.4 Expiration
Job nocturne (module batch) : un mouvement `EXPIRATION` par lot de points échu (F4), après le message d'avertissement au client si le consentement le permet.

## 6. Écrans

| Écran | Ajout |
|---|---|
| `fiche-client-panel` | solde, niveau éventuel, historique des mouvements ; bouton « Inscrire au programme » (avec consentement) |
| Vente (récapitulatif des montants) | « points à gagner sur cette vente » et solde du client |
| Règlement (`payment-mode`) | « Utiliser N points » ; le montant à payer se met à jour |
| Ticket de caisse | points gagnés, solde après la vente |
| Catalogue ou paramètres | barème (F3), durée (F4), activation du programme |
| Tableau de bord | points émis, utilisés, expirés ; coût du programme ; clients actifs |

## 7. Droits, audit et garde-fous
- Nouveaux privilèges (lignes `nav_item` de niveau 3, rôles posés par migration, modèle V2.1.13) : inscription, ajustement manuel (F5).
- Tout endpoint `/api/**` porte `@RequiresNavAccess` ou `@NavAccessExempt`.
- Le grand livre est **écrit par le serveur seul** ; l'écran ne calcule pas de points, il affiche ce que le serveur lui donne.
- Un plafond d'usage par vente (par exemple pas plus de X % du montant) évite qu'un solde élevé vide une vente.
- Test d'intégration obligatoire sur la **réversibilité** : gagner, annuler, vérifier que le solde revient exactement.

## 8. Phases

| Phase | Contenu | Critère de sortie | Effort |
|---|---|---|---|
| **0** | Valider F1 à F7 avec le pharmacien titulaire ; avis réglementaire écrit sur F2 | décisions notées ici | 0,5 j (+ délai de l'avis) |
| **1** | Tables, entités, service de calcul (gain, solde), barème en configuration, tests d'intégration | un gain et un solde justes, rejouables sans doublon | 3 j |
| **2** | Usage des points au règlement (remise globale, mouvement) | un règlement avec points réduit le montant, la caisse reste juste | 2 j |
| **3** | Annulations : vente annulée, retour client, avoir | le solde revient exactement à son état d'avant, dans tous les cas du §5.3 | 2 j |
| **4** | Écrans : fiche client, récapitulatif, règlement, ticket | parcours complet à l'écran, tests Jest | 3 j |
| **5** | Expiration (batch), message d'avertissement, tableau de bord | un lot échu expire une fois | 2 j |

Total : **environ 12,5 jours**, hors délai de l'avis réglementaire, dont 2 consacrés aux annulations.

## 9. Hors périmètre
- Cartes physiques, QR codes et application mobile du client (l'identification se fait par la fiche client existante : nom, téléphone, code).
- Partage de points entre officines d'un même groupe.
- Offres ciblées et campagnes (le plan sert le socle ; `mv_customer_rfm` permettra d'y venir).
