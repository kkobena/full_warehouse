# Plan d'amélioration — Avoir Client & Retour Client

## Contexte

Analyse comparative des fonctionnalités **Avoir Client** et **Retour Client** par rapport aux
logiciels experts (Winpharma, Pharmagest, LGPI, Caducée) et aux exigences réglementaires des
officines françaises.

---

## État actuel

### Avoir Client
- Liste paginée avec filtres statut (OUVERT/CLOTURE), date, recherche
- 5 modes de clôture : espèces, CB, bon d'avoir, retour produit, compensation vente
- Création automatique (via retour client ou quantité avoir sur ligne de vente)
- Clôture manuelle via modal

### Retour Client
- Liste paginée avec filtres date et recherche par référence
- 5 motifs : erreur dispensation, produit défectueux, erreur quantité, insatisfaction, autre
- 3 modes de règlement : espèces, CB, avoir client
- Création via modal (retrouver vente par référence, sélectionner lignes, valider)
- Remise en stock automatique + création d'avoir si mode = AVOIR_CLIENT

---

## Prérequis — Refonte de la classification légale produit

### 0.1 Remplacement de `scheduled` par l'enum `StatutLegal` sur `Produit`

**Problème actuel :** `scheduled: boolean` (ligne 117 de `Produit.java`) est un flag vague qui
signifie uniquement "ce produit nécessite une ordonnance", sans distinguer le type de prescription.
Il est impossible de différencier un médicament liste I (retour possible avec précautions) d'un
stupéfiant (retour interdit, destruction obligatoire). `FamilleProduit` et `Categorie` sont
purement organisationnels et ne portent aucun comportement légal — c'est la bonne approche.

**Solution :** Créer l'enum `StatutLegal` et le poser sur `Produit`. `scheduled` est supprimé,
son comportement est dérivé de `statutLegal != SANS_LISTE`.

**Définition de l'enum :**

```java
// domain/enumeration/StatutLegal.java
public enum StatutLegal {

    SANS_LISTE("Médicament ou produit disponible sans ordonnance. "
        + "Aucune restriction de dispensation, retour possible sous conditions."),

    LISTE_I("Substance vénéneuse de liste I (arrêté). "
        + "Ordonnance obligatoire, renouvelable sauf mention contraire. "
        + "Retour client possible avec vérification état produit."),

    LISTE_II("Substance vénéneuse de liste II (arrêté). "
        + "Ordonnance obligatoire, non renouvelable par défaut. "
        + "Retour client possible avec vérification état produit."),

    STUPEFIANTS("Stupéfiant soumis à ordonnance sécurisée (carnet à souche). "
        + "Tracabilité lot obligatoire, quantité maximale délivrable réglementée. "
        + "Retour client interdit — destruction réglementaire obligatoire."),

    PSO("Prescription Sécurisée Obligatoire : psychotropes et autres substances "
        + "à prescription sécurisée sans être classés stupéfiants. "
        + "Ordonnance sécurisée obligatoire. Retour client interdit.");

    private final String description;

    StatutLegal(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOrdonnanceObligatoire() {
        return this != SANS_LISTE;
    }

    public boolean isRetourInterdit() {
        return this == STUPEFIANTS || this == PSO;
    }

    public boolean isTracabilityLotObligatoire() {
        return this == STUPEFIANTS || this == PSO;
    }
}
```

**Migration sur `Produit` :**
- Ajouter le champ `statutLegal: StatutLegal` (colonne `statut_legal`, défaut `SANS_LISTE`)
- Supprimer le champ `scheduled` (devenu inutile)
- Partout où `scheduled` est lu, remplacer par `!statutLegal.isOrdonnanceObligatoire()`
- Migration Flyway : `ALTER TABLE produit ADD COLUMN statut_legal VARCHAR(20) DEFAULT 'SANS_LISTE'`
  puis `UPDATE produit SET statut_legal = 'LISTE_I' WHERE scheduled = true`
  puis `ALTER TABLE produit DROP COLUMN scheduled`

**Sur `FamilleProduit` (optionnel) :**
- Ajouter `statutLegalDefaut: StatutLegal` uniquement comme valeur de pré-remplissage
  lors de la création manuelle d'un produit — jamais utilisé comme règle métier

---

## Priorité 1 — Conformité légale et réglementaire

### 1.1 Blocage retour stupéfiants / PSO
**Réalité terrain :** Le pharmacien refuse le retour. Si le produit est intact, il le prend en
destruction (circuit réglementaire), sans rembourser. Zéro remise en stock, zéro remboursement.

**Principe clé :** remboursement financier et remise en stock sont deux décisions indépendantes.
Le logiciel les traitait comme une seule opération — c'est le problème de fond.

**Implémentation :**
- `SaleLineForRetourDTO` : ajout de `retourInterdit`, `thermosensible`, `statutLegal`
  pour que le frontend affiche les contraintes avant même la validation
- `RetourClientServiceImpl.validerRetour()` : pour chaque ligne, vérifier
  `produit.getStatutLegal().isRetourInterdit()` → lever `GenericError` avec le nom du produit
- Le frontend désactive la ligne non retournable (checkbox grisée + tooltip explicatif)

### 1.2 Blocage remise en stock produits thermosensibles
**Besoin :** Les produits à chaîne du froid ne peuvent pas être remis en stock après retour
(rupture de chaîne froide non vérifiable).  
**Note :** Le champ `thermosensible` existe déjà sur l'entité `Produit` (ligne 104).  
**Implémentation :**
- Dans `RetourClientServiceImpl.validerRetour()`, vérifier `produit.isThermosensible()`
- Si vrai : ne pas incrémenter `StockProduit.qtyStock`, créer un mouvement de stock de type
  DESTRUCTION ou QUARANTAINE
- Afficher une alerte à l'écran "Produit thermosensible — remise en stock impossible"
- Permettre quand même le remboursement financier au client

### 1.3 Gestion tiers-payant sur retour
**Besoin :** Si une vente a été payée partiellement par une mutuelle (tiers-payant), le
remboursement au client doit être limité à sa part réelle (ticket modérateur).  
**Implémentation :**
- Lire la ventilation tiers-payant de la vente d'origine (`SaleLine.montantTp`,
  `SaleLine.montantPatient`)
- Calculer automatiquement : montant remboursable = part patient uniquement
- Afficher les deux montants dans le modal de retour
- Générer si nécessaire un avoir ou une note de régularisation côté mutuelle

---

## Priorité 2 — Impact métier fort

### 2.1 Bon d'avoir imprimable
**Besoin :** Remettre au client un document physique ou numérique attestant de son avoir.  
**Implémentation :**
- Créer un template Thymeleaf `avoir-client-voucher.html` (Flying Saucer)
- Inclure : numéro d'avoir, montant, date de création, date d'expiration, nom client,
  code-barres ou QR code scannable à la caisse
- Ajouter endpoint `GET /api/sales/avoirs/documents/{id}/pdf`
- Bouton "Imprimer" dans la liste des avoirs ouverts

### 2.2 Notification email/SMS à la réception des produits en avoir
**Besoin :** Informer le client au moment où il peut venir récupérer ses produits, c'est-à-dire
quand l'avoir est clôturé en mode `RETOUR_PRODUIT` — les produits sont disponibles au comptoir.
La notification n'a pas de sens à la création de l'avoir (le client est encore là) ni pour
les modes espèces/CB (remboursement immédiat).  
**Implémentation :**
- Déclencher la notification dans `AvoirClientDocumentServiceImpl.cloturerAvoir()` uniquement
  quand `modeCloture == RETOUR_PRODUIT` et que les produits sont confirmés disponibles
- Email : template "Vos produits sont prêts — numéro avoir, liste produits, montant"
- SMS : message court "Vos produits sont disponibles à la pharmacie [nom officine]"
- Conditionné à la présence d'un email/téléphone sur la fiche client
- Paramétrable dans la configuration officine (activer/désactiver email, SMS)

### 2.3 Validité d'un avoir
**Besoin :** Un avoir non utilisé doit expirer après un délai configurable.  
**Implémentation :**
- Ajouter `dateExpiration` sur l'entité `AvoirClientDocument`
- Paramètre officine : `avoir.delaiValiditeJours` (défaut : 90 jours)
- Calcul automatique à la création : `dateExpiration = dateCreation + delaiValiditeJours`
- Afficher la date d'expiration dans la liste
- Tâche planifiée (`@Scheduled`) pour clôturer automatiquement les avoirs expirés
- Alerte visuelle (badge rouge) pour les avoirs proches de l'expiration (< 7 jours)

### 2.4 Application automatique de l'avoir à la caisse
**Besoin :** Lors d'une nouvelle vente, si le client a un avoir ouvert, le proposer automatiquement.  
**Implémentation :**
- Dans le flux de vente (`SalesHomeComponent`), après sélection du client, appeler
  `GET /api/sales/avoirs?clientId=X&statut=OUVERT`
- Si avoir(s) trouvé(s) : afficher une bannière "Ce client a un avoir de XX€ — Appliquer ?"
- Mode de clôture = COMPENSATION_VENTE, montant de la vente réduit en conséquence
- Gérer le cas avoir < montant vente (complément) et avoir > montant vente (reliquat)

### 2.5 Délai de retour configurable avec alerte
**Besoin :** Bloquer ou alerter si la vente est trop ancienne (pratique standard officine).  
**Implémentation — suivre le pattern existant de `AppConfigurationService` :**

1. Dans `EntityConstant.java`, ajouter :
```java
// Délai max (jours) entre la vente et le retour client avant avertissement
public static final String APP_DELAI_RETOUR_CLIENT = "APP_DELAI_RETOUR_CLIENT";
public static final String APP_DELAI_RETOUR_CLIENT_CACHE = "APP_DELAI_RETOUR_CLIENT_CACHE";
```

2. Dans `AppConfigurationService.java`, ajouter (calqué sur `getDelaiRetourFournisseur()`) :
```java
@Cacheable(EntityConstant.APP_DELAI_RETOUR_CLIENT_CACHE)
public int getDelaiRetourClient() {
    return appConfigurationRepository
        .findById(EntityConstant.APP_DELAI_RETOUR_CLIENT)
        .map(c -> Integer.parseInt(c.getValue()))
        .orElse(30); // défaut 30 jours
}
```

3. Migration Flyway : insérer la ligne dans `app_configuration`
   (valeur par défaut `30`, type `NUMBER`, description "Délai max en jours entre la vente et
   le retour client autorisé sans avertissement")

4. Dans `RetourClientServiceImpl.findSaleByRef()` :
   - Calculer l'ancienneté de la vente vs `appConfigurationService.getDelaiRetourClient()`
   - Si dépassé : ne pas bloquer — retourner un objet `ISaleForRetour` avec un flag `warning`
     et un message "Vente datée de N jours (délai max : M jours)"
   - Décision finale laissée au pharmacien (confirmation dans le modal)

### 2.6 Remise en stock conditionnelle
**Besoin :** Vérifier l'état du produit avant de le remettre en stock.  
**Implémentation :**
- Ajouter un formulaire intermédiaire dans le modal de retour : "État du produit retourné"
  - Emballage intact ? (oui/non)
  - Numéro de lot lisible ? (oui/non)
  - Date de péremption valide ? (oui/non)
- Si toutes les conditions = oui ET produit non thermosensible → remise en stock
- Sinon → mouvement QUARANTAINE ou DESTRUCTION
- Stocker le résultat de ce contrôle sur `RetourClientLine`

---

## Priorité 3 — Différenciant et expérience avancée

### 3.1 ~~Flux CYCLAMED~~ — hors périmètre
**Raison d'exclusion :** Le dispositif CYCLAMED concerne les médicaments non utilisés déposés par
des patients, qui ne sont pas nécessairement des produits achetés dans cette officine. Ce flux
n'entre donc pas dans le périmètre du retour client tel que défini ici (produit acheté dans
l'officine, retourné par l'acheteur). CYCLAMED est un sujet distinct qui mérite son propre plan
si l'officine souhaite le tracer numériquement.

### 3.2 Retour avec échange
**Besoin :** Permettre en une seule opération : retour + sélection du produit de remplacement.  
**Implémentation :**
- Option "Retour avec échange" dans le modal de retour
- Après validation du retour, ouvrir directement une nouvelle vente pré-remplie avec le client
- Règlement = différence entre valeur retournée et valeur du produit échangé
- Lier les deux opérations (référence croisée retour ↔ nouvelle vente)

### 3.3 Utilisation partielle d'un avoir
**Besoin :** Permettre de solder un avoir en plusieurs fois.  
**Implémentation :**
- Ajouter `montantUtilise` et `montantRestant` sur `AvoirClientDocument`
- Lors de la clôture en mode COMPENSATION_VENTE, ne clôturer que si `montantRestant = 0`
- Sinon : laisser le statut OUVERT avec le montant restant
- Historique des utilisations partielles (table `AvoirClientUtilisation`)

### 3.4 Tableau de bord retours/avoirs
**Besoin :** Vue synthétique pour détecter des anomalies (fraude, problèmes qualité fournisseur).  
**Implémentation :**
- Widget sur le dashboard officine : retours du mois (nombre, montant total, top motifs)
- Alerte si un produit dépasse un seuil de retour configurable (ex : > 5 retours/mois)
- Alerte si un client dépasse un seuil de retours (ex : > 3 retours/mois)
- Export mensuel PDF/Excel pour la comptabilité

### 3.5 Historique avoirs par client
**Besoin :** Vue consolidée des avoirs sur la fiche client.  
**Implémentation :**
- Onglet "Avoirs" sur la fiche client (`customer-detail`)
- Afficher : avoirs ouverts (solde disponible), avoirs clôturés (historique), total utilisé
- Lien rapide vers chaque bon d'avoir imprimable

---

## Récapitulatif des priorités

| # | Fonctionnalité | Priorité | Complexité | Réglementation |
|---|---|---|---|---|
| 0.1 | Enum `StatutLegal` — suppression `scheduled` | Haute | Faible | Prérequis |
| 1.1 | Blocage retour stupéfiants | Haute | Faible | Obligatoire |
| 1.2 | Blocage remise en stock thermosensibles | Haute | Faible | Obligatoire |
| 1.3 | Gestion tiers-payant sur retour | Haute | Élevée | Obligatoire |
| 2.1 | Bon d'avoir imprimable | Haute | Moyenne | Recommandé |
| 2.2 | Notification email/SMS à réception produits en avoir | Haute | Faible | Recommandé |
| 2.3 | Validité d'un avoir | Haute | Faible | Recommandé |
| 2.4 | Application avoir à la caisse | Haute | Élevée | Non |
| 2.5 | Délai retour client via AppConfiguration | Moyenne | Faible | Non |
| 2.6 | Remise en stock conditionnelle | Moyenne | Moyenne | Non |
| 3.1 | CYCLAMED — hors périmètre (plan séparé) | — | — | — |
| 3.2 | Retour avec échange | Moyenne | Élevée | Non |
| 3.3 | Utilisation partielle d'un avoir | Faible | Moyenne | Non |
| 3.4 | Tableau de bord retours/avoirs | Faible | Moyenne | Non |
| 3.5 | Historique avoirs par client | Faible | Faible | Non |
