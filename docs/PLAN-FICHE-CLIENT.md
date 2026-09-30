# Plan — Fiche client

> Analyse du 2026-09-29 : `entities/customer/customer.component.html` (liste et ligne dépliée),
> `customer-detail.component.html` (fiche), formulaires associés, `CustomerDataService`,
> `AssuredCustomerServiceImpl`, entités `Customer`, `AssuredCustomer`, `ClientTiersPayant`.

## En bref

L'écran client est aujourd'hui un **fichier administratif** : il sait créer un assuré, ses ayants
droit et ses tiers payants. Il ne répond pas aux questions qu'on se pose au comptoir : *qu'a-t-il
pris la dernière fois ? combien me doit-il ? sa carte est-elle encore valable ? que lui reste-t-il
sur son plafond ? est-il allergique à quelque chose ?*

L'encours affiché est **faux**, et le formulaire du tiers payant propose des plafonds par client
qu'il n'enregistre pas. Le plan commence par les corriger, puis construit une fiche « 360° » et
ajoute la sécurité du patient.

> **Règle de gestion (décision du 2026-09-29)** : les plafonds sont définis **sur le tiers payant**
> et s'appliquent à chacun de ses adhérents. Il n'y a pas de plafond propre à un client, et il
> n'y en aura pas : cela éviterait d'avoir à les définir client par client.

## 1. Constats

### 1.1 Défauts (données fausses ou perdues)

| # | Constat | Preuve |
|---|---|---|
| D1 | **La colonne « Encours » vaut toujours 0.** Le champ n'est jamais renseigné. | `CustomerDTO.encours` : initialisé à 0, `setEncours` n'est appelé nulle part. Vérifié sur l'API : `encours: 0` pour les assurés de la démo. |
| D2 | Le tableau des tiers payants affiche les plafonds de l'organisme (`TiersPayant.plafondConsoClient` / `plafondJournalierClient`) — c'est la règle — mais sans le dire : ils passent pour des plafonds propres au client. | `ClientTiersPayantDTO(ClientTiersPayant)`, l. 40-41. |
| D3 | **Le formulaire du tiers payant client propose des plafonds qu'il n'enregistre pas** (« Plafond de vente », « Plafond consommation », « Plafond absolu ») : la saisie est perdue sans message. Contraire à la règle, ces champs n'ont pas lieu d'être. | Aucune occurrence de `plafond` dans `ClientTiersPayant` ni dans `AssuredCustomerServiceImpl`. |
| D4 | La colonne « Catégorie » du tableau des tiers payants affiche la **priorité** (R0, R1…). | `customer.component.html`, l. 329/352. |
| D5 | **Pas de réactivation.** En filtre « Désactivés », la ligne propose encore « Désactiver », jamais « Réactiver ». | l. 188-197. |
| D6 | L'import JSON existe (`openJsonImport`) mais **aucun bouton ne l'appelle**. | `customer.component.ts`, l. 193. |
| D7 | Le filtre de type ne propose que TOUT / ASSURÉ / STANDARD : les clients **carnet** et **dépôt** sont mêlés aux assurés. | `types`, l. 77. |
| D8 | « Supprimer » et « Désactiver » s'affichent pour tous les rôles ; le serveur refuse désormais en 403 (`customer` / `DELETE`). Le bouton devrait suivre le droit. | — |
| D9 | La fiche charge **tous les achats clôturés** du client, sans pagination ni filtre de date, alors que l'API accepte `fromDate` / `toDate`. | `SaleDataService.customerPurchases`. |
| D10 | Devise « CFA » écrite en dur dans la fiche (avoirs), au lieu de `APP_DEVISE`. | `customer-detail.component.html`, l. 158, 201. |
| D11 | **« Désactiver » n'a jamais fonctionné** : le front appelait `DELETE /api/customers/lock/{id}`, qui n'existe pas côté serveur. | Relevé pendant le lot 0. |
| D12 | Pour les clients standard, **une recherche remplaçait le filtre de statut** au lieu de s'y ajouter : les désactivés remontaient. | `CustomerDataService.loadAllUninsuredCustomers`. |
| D13 | Supprimer un client qui a des ventes sortait en **500** : la suppression partait au commit, hors du `try` qui devait afficher « Il existe des ventes… ». | `UninsuredCustomerService` / `AssuredCustomerServiceImpl.deleteCustomerById`. |

### 1.2 Lacunes d'usage (ce que la fiche ne permet pas)

| Besoin au comptoir | Aujourd'hui |
|---|---|
| Voir d'un coup d'œil **qui** est le client, **sa couverture** et **sa situation financière** | Nom seul en titre de fiche ; couverture et ayants droit uniquement dans la ligne dépliée de la liste |
| « **Le même que la dernière fois** » : historique par **produit** (quoi, quand, combien, prescrit ou non) | Historique par vente seulement, sans recherche de produit ni revente |
| **Crédit** : encours réel, limite de crédit, différés et règlements, relance | Encours faux (D1) ; les différés vivent dans un autre écran |
| **Couverture** : validité de la carte d'assuré, plafond de l'organisme consommé / restant, historique des taux | Ni date de validité, ni consommation ; plafonds de l'organisme non signalés comme tels (D2) |
| **Sécurité du patient** : allergies, pathologies chroniques, grossesse / allaitement, poids de l'enfant | Rien n'est saisi, rien n'alerte à la vente |
| **Traitements chroniques** : rappel de renouvellement, rupture de traitement | Rien |
| **Contact** : adresse, second téléphone, consentement SMS / WhatsApp | Téléphone et e-mail seulement ; aucun consentement tracé |
| **Doublons** : fusionner deux fiches du même patient | Impossible (le produit a une fusion, le client non) |
| **Documents** : relevé de compte, **attestation de dépenses** (remboursement employeur / mutuelle) | Rien, hors réimpression de facture vente par vente |
| **Remise client** attribuée (`RemiseClient` existe) | Non visible sur la fiche |

## 2. Étude comparative

Repère : fonctions **courantes** des logiciels d'officine établis (LGO français : LGPI, Winpharma,
Smart Rx, Léo, et leurs équivalents en Afrique de l'Ouest), d'après leur périmètre public
habituel — pas une vérification version par version.

| Fonction | Standard du marché | Pharma-Smart | Écart |
|---|---|---|---|
| Recherche client (nom, téléphone, n° assuré) | Instantanée, lecture de carte | Nom, code, téléphone, n° assuré, **en début de chaîne**, sur Entrée | Faible |
| Fiche de synthèse (identité, couverture, solde) | En-tête permanent | Absente | **Fort** |
| Historique de délivrance par produit | Standard, avec re-délivrance | Par vente seulement | **Fort** |
| Dossier de sécurité (allergies, pathologies, grossesse) | Standard, alerte à la délivrance | Absent | **Fort** |
| Alerte d'interaction / contre-indication | Via base médicamenteuse | Absent — la base DCI (`produit_dci`) permettrait un premier niveau par DCI | Moyen |
| Suivi des traitements chroniques / renouvellements | Courant | Absent | Moyen |
| Compte client / crédit avec limite et relance | Standard | Différés présents, mais hors fiche ; encours faux | **Fort** |
| Couverture : validité, plafond consommé / restant | Standard (tiers payant) | Taux et priorité ; plafond de l'organisme affiché sans consommation | **Fort** |
| Ayants droit | Standard | Présent | — |
| Avoirs clients | Courant | Présent (onglet Avoirs) | — |
| Fidélité / remise client | Courant | `RemiseClient` en base, invisible sur la fiche | Moyen |
| Communication (SMS disponibilité, rappel) | Courant | `SmsService` existe (avoirs) ; pas de consentement | Moyen |
| Fusion de doublons | Courant | Absent | Moyen |
| Relevé de compte, attestation de dépenses | Standard | Absent | Moyen |
| Protection des données (consentement, export, effacement) | Exigé (loi ivoirienne n° 2013-450) | Suppression / désactivation seulement | Moyen |

## 3. Plan

### Lot 0 — Corrections (≈ 2 j) — **réalisé le 2026-09-29**

> D1 à D13 corrigés. Encours : reste dû sur les ventes différées (clôturées, non annulées), la
> même règle que l'écran des différés ; le solde `CustomerAccount` du carnet n'y est pas ajouté.
> Nouveau point d'entrée `PUT /api/customers/{id}/status?status=ENABLE|DISABLE` (droit `customer`
> / `EDIT`) ; `GET /api/customers/purchases` est paginé et trié du plus récent au plus ancien
> (douze derniers mois par défaut sur la fiche). Tests : `FicheClientIntegrationTest`.

- **D1** : calculer l'encours réel (différés non réglés, et solde `CustomerAccount` pour le carnet)
  dans `CustomerDataService`, en une requête agrégée par page, pas par ligne.
- **D2 / D3** (plafonds sur le tiers payant, cf. règle en tête) : retirer du formulaire client les
  champs de plafond, jamais enregistrés ; libeller les colonnes « Plafond de l'organisme » et
  renvoyer vers la fiche du tiers payant pour les modifier.
- D4 à D10 : libellés, bouton « Réactiver », bouton d'import (ou suppression du code mort),
  filtre carnet / dépôt, boutons conditionnés par `AbilityService`, pagination et période des
  achats, devise par `APP_DEVISE`.

### Lot 1 — Fiche client 360° (≈ 4 j) — **réalisé le 2026-09-29**

> En-tête permanent (identité, âge, couverture principale, encours, avoirs, dernière visite,
> achats sur douze mois) et onglets Achats, Produits délivrés, Couverture (assurés : tiers payants,
> historique du taux, ayants droit), Crédit, Avoirs ; « Modifier » depuis la fiche. Lectures
> bornées au client : `GET /api/customers/{id}/synthese`, `/produits-delivres`, `/differes`,
> `/reglements-differes` (codes du client, donc ouvertes au caissier sans lui ouvrir l'écran
> des différés). **Reste à faire** : l'action « ajouter à une vente » depuis Produits délivrés,
> qui suppose que l'écran de vente accepte un client et des produits présélectionnés.

- **En-tête permanent** : identité, âge, téléphone ; couverture principale (organisme, taux,
  validité) ; encours et avoirs disponibles ; pastilles d'alerte (lot 2).
- **Onglets** : *Achats* (paginé, période) · *Produits délivrés* (agrégé par produit : dernière
  date, quantités, prescrit ; action « ajouter à une vente ») · *Couverture* (tiers payants,
  ayants droit, historique des taux) · *Crédit* (différés, règlements, relevé) · *Avoirs*.
- Édition depuis la fiche ; la liste garde un rôle de recherche.

### Lot 2 — Sécurité du patient (≈ 3 j) — **réalisé le 2026-09-29**

> Migration `V2.1.13` : `customer_dossier_sante`, `customer_allergie`, `alerte_sante_derogation`
> et l'`ACTION` `pr-forcer-alerte-sante` (admin, pharmacien). Onglet « Santé » et pastilles
> d'alerte dans l'en-tête de la fiche. À la vente, les cinq écrans (comptant, assurance, carnet,
> devis, dépôt) passent par le même contrôle, sur le patient réel (l'ayant droit s'il y en a un) :
> une allergie à une molécule du produit — y compris dans une association encore saisie comme
> une seule DCI — ouvre une modale bloquante ; la délivrance exige un motif, et le droit ou la
> clé de sécurité d'un collègue qui le détient ; elle est tracée (qui a délivré, qui a autorisé,
> pourquoi). Grossesse et allaitement ne font qu'un rappel. Tests : `DossierSanteIntegrationTest`.

- Dossier : allergies (par **DCI**, via `dci`), pathologies chroniques, grossesse / allaitement
  avec date, poids pour l'enfant, note libre.
- **Alerte à la vente** : un produit dont la DCI figure dans les allergies du client déclenche
  un avertissement bloquant, levable par un droit (`ACTION` dédiée), tracé.

### Lot 3 — Couverture et crédit (≈ 3 j) — **réalisé le 2026-09-29**

> Migration `V2.1.14` : `client_tiers_payant.date_fin_validite`, paramètre
> `APP_LIMITE_CREDIT_CLIENT` (0 = pas de limite), tables `limite_credit_derogation` et
> `relance_differe`, `ACTION` `pr-depasser-limite-credit` (admin, pharmacien).
>
> - **Carte d'assuré** : date de fin de validité saisie par tiers payant (le champ « Numéro »
>   dupliqué du formulaire a laissé sa place) ; carte expirée signalée dans l'en-tête de la
>   fiche et dans la barre assurance de la vente.
> - **Consommation face au plafond** (règle du 2026-09-29, migration `V2.1.15`) : plafond
>   **non absolu** → la facture **définitive** remet le compteur `conso_mensuelle` à zéro (la
>   provisoire n'y touche pas), et son annulation rend ce qu'elle avait effacé (table
>   `reinitialisation_consommation`) ; plafond **absolu** → chaque **règlement** réduit le compteur
>   du montant réglé, et son annulation le rend. Adhérent régi par « plafond des clients absolu »,
>   organisme par « plafond absolu ». Colonne « Consommation / plafond » dans l'onglet Couverture.
> - **Limite de crédit globale**, contrôlée à la finalisation d'une vente dont une part reste
>   due : encours hors vente + reste de la vente > limite ⇒ modale bloquante ; la vente ne passe
>   qu'avec un motif et le droit, ou la clé d'un collègue qui le détient, et la dérogation est
>   tracée. Onglet Crédit : limite et disponible.
> - **Relance SMS** des différés depuis l'onglet Crédit, historisée (date, téléphone, montant,
>   auteur). ⚠ `SmsService` reste un bouchon qui journalise sans envoyer : aucun fournisseur SMS
>   n'est branché.
>
> Tests : `CreditClientIntegrationTest`, `ConsommationPlafondFacturationIntegrationTest`,
> `ConsommationPlafondReglementIntegrationTest`. Vérifié en réel : vente différée au-delà de la limite, dérogation,
> relance.

- Date de validité de la carte d'assuré, alerte à l'expiration ; consommation du client par
  période face au plafond **de l'organisme** (`ClientTiersPayant.consoMensuelle` existe déjà).
- Limite de crédit par client, contrôlée à la vente à crédit ; relance SMS des différés échus.

### Fiche client depuis la vente — **réalisé le 2026-09-29**

> Panneau latéral (`features/sales/ui/fiche-client-panel`), ouvert par l'icône de la carte client
> (comptant, assurance, carnet, devis) ou **Alt+V**, sans quitter la vente : alertes (allergies,
> grossesse, allaitement, carte expirée, encours et limite, avoirs), couverture et consommation,
> dix dernières délivrances. « Re-délivrer » présélectionne le produit au prix et au stock du
> jour ; la quantité, le stock et l'alerte santé suivent le circuit habituel. « Fiche complète »
> ouvre la fiche dans un nouvel onglet, pour qui a le droit `customer`. Aucun endpoint nouveau :
> la lecture passe par les codes `ventes` / `nouvelle-vente` déjà ouverts sur `CustomerResource`.

### Lot 4 — Suivi et communication (≈ 2 j) — **réalisé le 2026-09-30**

> Migration `V2.1.17` : `customer_traitement_chronique`, `customer_consentement`, paramètres
> `APP_RENOUVELLEMENT_EXCEPTIONNEL` (0 par défaut), `APP_RENOUVELLEMENT_EXCEPTIONNEL_MOIS` (3) et
> `APP_RAPPEL_RENOUVELLEMENT_JOURS` (5).
>
> - **Traitements chroniques** (onglet Santé) : déclarés par molécule, car la prescription se fait en
>   DCI et la substitution par un générique est la règle ; un **produit imposé** restreint le
>   traitement à lui seul (patient non substituable, médicaments à marge thérapeutique étroite).
>   Arrêter un traitement le garde visible, sans rappel.
> - **Suivi calculé à la lecture** : dernière délivrance au patient réel (l'ayant droit s'il y en a
>   un), déconditionnés compris, plus la durée couverte → à jour, à renouveler (dans les
>   `APP_RAPPEL_RENOUVELLEMENT_JOURS`), en retard, en rupture (plus d'un cycle manqué). Pastilles
>   dans l'en-tête de la fiche, section « Traitements à renouveler » dans le panneau client de la
>   vente avec « Re-délivrer » (produit imposé, sinon celui délivré la dernière fois), et liste
>   « Renouvellements » des patients à relancer depuis la liste des clients.
> - **Renouvellement exceptionnel** (repris de l'article L. 5125-23-1 du code de la santé publique
>   français, désactivé par défaut) : ordonnance expirée d'au moins trois mois, hors stupéfiants et
>   assimilés (statut légal `STUPEFIANTS` ou `PSO`), première délivrance dans le mois qui suit
>   l'expiration ; la fiche indique jusqu'à quand, ou la condition qui manque. À valider par un
>   pharmacien au regard de la réglementation ivoirienne avant de l'activer.
> - **Consentement** SMS, WhatsApp et e-mail, historisé (qui, quand), dans l'en-tête de la fiche.
>   Seul un **refus explicite** bloque un envoi : relance des différés et avis d'avoir disponible ;
>   un client jamais interrogé reste joignable, sans quoi toutes les fiches existantes seraient
>   coupées. Au passage, la devise de l'avis SMS d'avoir suit `APP_DEVISE` au lieu de « CFA ».
>
> Tests : `TraitementChroniqueIntegrationTest`, `CreditClientIntegrationTest` (relance et
> consentement), `AvoirClientNotificationServiceTest`. **Reste** : aucun envoi réel tant que
> `SmsService` n'a pas de fournisseur ; WhatsApp n'a pas de canal d'envoi.

- Traitements chroniques déclarés **par molécule (DCI)**, avec un produit imposé en option
  (médicament non substituable) ; rappel de renouvellement.
- Consentement SMS / WhatsApp tracé.

> « Produit disponible » : **déjà couvert** par le circuit des avoirs. Un produit en rupture à la
> vente part en avoir ; à la clôture de l'avoir en mode retour produit,
> `AvoirClientNotificationService` prévient le client par e-mail et par SMS, selon les paramètres
> de l'officine. Le consentement tracé devra s'y appliquer. Le SMS passe par le même `SmsService`
> que les relances, qui n'envoie encore rien (aucun fournisseur branché).

### Lot 5 — Qualité du fichier et documents (≈ 3 j) — **réalisé le 2026-09-30**

> Migration `V2.1.16` : `ACTION` `pr-fusion-client` et `pr-donnees-personnelles-client` (admin,
> pharmacien), types de journal `MERGE_CUSTOMER` et `ANONYMISATION_CLIENT`.
>
> - **Fusion**, sur le modèle de la fusion produit : cases à cocher dans la liste des clients ;
>   dès deux lignes cochées, la barre d'actions groupées propose « Fusionner », qui ouvre la
>   comparaison des fiches — choix de la cible, analyse, confirmation. Ventes, ventes en tant qu'ayant droit,
>   règlements de différés, avoirs, retours, ayants droit, dérogations et relances passent sur la
>   fiche conservée ; un tiers payant d'un organisme déjà présent s'y fond (lignes de vente,
>   consommation additionnée, validité la plus lointaine), un autre change de fiche à la première
>   priorité libre (plus de quatre organismes : refus) ; allergies réunies, dossier santé fusionné,
>   comptes carnet additionnés ; les champs vides de la cible sont complétés. Les sources sont
>   **désactivées**, jamais supprimées. Aperçu obligatoire avant fusion.
> - **Relevé de compte** (onglet Crédit) : solde de début, ventes différées au débit (ce qui
>   restait dû à la vente), règlements au crédit, solde courant ; le solde à aujourd'hui est
>   l'encours de la fiche. **Attestation de dépenses** (onglet Achats, sur la période choisie) :
>   achats clôturés du client et de ses ayants droit, produits, part tiers payant et part à charge.
> - **Export** des données (JSON : identité, ayants droit, tiers payants, dossier santé, achats,
>   compte, relances) et **effacement** par anonymisation : identité, contacts, numéros d'assuré et
>   dossier santé effacés, fiche désactivée ; les délivrances restent tracées. Refusé tant qu'un
>   différé reste dû.
>
> Tests : `QualiteFichierClientIntegrationTest`. **Non traité** : les retours et avoirs ne
> viennent pas en déduction de l'attestation ; `npm run e2e:droits` n'a pas été rejoué.

- Détection des doublons (nom + téléphone + date de naissance), fusion sur le modèle de
  `ProduitMergeResource`.
- PDF : relevé de compte, attestation de dépenses sur une période.
- Export des données d'un client et effacement (droit d'accès et d'opposition).

**Total : ≈ 18 jours.** Les lots 0 et 1 apportent l'essentiel du gain au comptoir ; le lot 2
est le plus important pour la sécurité.

Chaque lot suit les conventions en place : endpoints annotés `@RequiresNavAccess` (codes
`customer`, ou nouvelles `SECTION` / `ACTION` par migration), Design System maison, migrations
Flyway, tests d'intégration Testcontainers, et `npm run e2e:droits` pour vérifier que les rôles
de comptoir gardent l'accès.

## 4. Décisions à prendre

1. ~~Plafonds par adhérent~~ — **tranché** : les plafonds restent sur le tiers payant.
2. ~~Allergie : blocage ou avertissement~~ — **tranché par défaut au lot 2** : blocage levable par le droit `pr-forcer-alerte-sante` (admin, pharmacien) ou la clé d'un collègue qui le détient, avec motif tracé.
3. ~~Limite de crédit~~ — **tranché au lot 3** : globale (paramètre de l'officine), blocage levable.
4. ~~Canal de relance~~ — **tranché au lot 3** : SMS seul.
5. ~~Suppression d'un client qui a un historique~~ — **tranché le 2026-09-30** : interdite au profit
   de la désactivation (traçabilité des délivrances). `HistoriqueClientService` refuse la
   suppression dès qu'il existe une vente (comme client ou comme ayant droit), un règlement de
   différé, un avoir ou un retour, pour le client ou l'un de ses ayants droit ; la liste propose
   alors de le désactiver. Seule une fiche sans historique — créée par erreur — se supprime.
