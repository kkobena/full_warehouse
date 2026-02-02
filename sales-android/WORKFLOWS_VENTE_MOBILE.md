# 📱 Workflows Fonctionnels - Module Vente Mobile

**Application :** Full Warehouse - Module de Vente  
**Version :** Angular 21  
**Date :** 30 janvier 2026  
**Objectif :** Documentation des workflows pour implémentation mobile

---

## 🎯 Vue d'Ensemble

Le module de vente gère **3 types de ventes** avec des workflows distincts :

| Type | Code | Description | Client requis |
|------|------|-------------|---------------|
| **Vente Comptant** | `COMPTANT` | Vente au comptant (cash) | ❌ Optionnel |
| **Vente Assurance** | `ASSURANCE` | Vente avec tiers payant | ✅ Obligatoire |
| **Vente Carnet** | `CARNET` | Vente à crédit | ✅ Obligatoire |

---

## 📊 Architecture des Composants

```mermaid
graph TB
    A[SellingHomeComponent<br/>Orchestrateur Principal] --> B[ComptantComponent<br/>Vente Comptant]
    A --> C[AssuranceComponent<br/>Vente Assurance]
    A --> D[CarnetComponent<br/>Vente Carnet]
    
    B --> E[ProductTableComponent<br/>Tableau Produits]
    C --> E
    D --> E
    
    B --> F[ModeReglementComponent<br/>Modes Paiement]
    C --> F
    D --> F
    
    B --> G[AmountComputingComponent<br/>Calcul Montants]
    C --> G
    D --> G
    
    C --> H[AssuranceDataComponent<br/>Gestion Tiers Payants]
    D --> H
    
    style A fill:#ff6b6b
    style B fill:#4ecdc4
    style C fill:#95e1d3
    style D fill:#f7dc6f
```

---

## 🔄 WORKFLOW 1 : Ajout de Produit

### État Global

```mermaid
stateDiagram-v2
    [*] --> Initialisation
    Initialisation --> RechercherProduit: Utilisateur recherche
    RechercherProduit --> SaisirQuantité: Produit sélectionné
    SaisirQuantité --> ValidationStock: Quantité saisie
    ValidationStock --> AjouterProduit: Stock OK
    ValidationStock --> GestionStock: Stock Insuffisant
    GestionStock --> ForceStock: Autorisation accordée
    GestionStock --> Déconditionner: Produit conditionnement
    GestionStock --> RechercherProduit: Annulation
    ForceStock --> AjouterProduit
    Déconditionner --> AjouterProduit
    Déconditionner --> RechercherProduit: Refus déconditionnement
    AjouterProduit --> AppelAPI: Créer/Ajouter ligne
    AppelAPI --> MiseAJourVente: Succès
    AppelAPI --> GestionErreur: Erreur
    MiseAJourVente --> [*]
    GestionErreur --> [*]
```

### Workflow Détaillé

#### 1.1 Recherche de Produit

**Paramètres de Recherche :**
- Requête de recherche (minimum 3 caractères)
- Nombre maximum de résultats (par défaut : 50)

**Informations Produit Retournées :**
- ID du produit
- Libellé
- Prix unitaire régulier
- Quantité en stock
- Quantité maximale autorisée
- Indicateur déconditionnement possible
- Nombre d'unités par conditionnement

**Endpoint API :** `GET /api/products/search` avec paramètres query et size

---

#### 1.2 Validation de Stock

```mermaid
flowchart TD
    A[Quantité Demandée] --> B{Stock >= Quantité ?}
    B -->|Oui| C{Quantité <= Max ?}
    B -->|Non| D{Force Stock Autorisé ?}
    
    C -->|Oui| E[✅ Validation OK]
    C -->|Non| F{Force Stock Autorisé ?}
    
    D -->|Oui| G[⚠️ Confirmation Force Stock]
    D -->|Non| H[❌ Stock Insuffisant]
    
    F -->|Oui| I[⚠️ Confirmation Quantité Max]
    F -->|Non| J[❌ Quantité Excessive]
    
    G --> K{Utilisateur Confirme ?}
    I --> K
    
    K -->|Oui| E
    K -->|Non| L[Annulation]
    
    E --> M[Ajouter au Panier]
```

**Logique de Validation :**

**Données Requises :**
- Informations du produit
- Quantité demandée
- Quantité déjà dans le panier
- Permission force stock de l'utilisateur

**Résultat de Validation :**
- Statut valide/invalide
- Raison du rejet (stock insuffisant, quantité excessive, force stock, déconditionnement)
- Nécessite confirmation utilisateur
- Message de confirmation

**Règles de Validation :**

1. **Stock Insuffisant :**
   - Si stock < quantité demandée ET force stock autorisé → Demander confirmation
   - Si stock < quantité demandée ET force stock non autorisé → Bloquer

2. **Quantité Excessive :**
   - Si quantité totale > maximum autorisé ET force stock autorisé → Demander confirmation
   - Si quantité totale > maximum autorisé ET force stock non autorisé → Bloquer

3. **Déconditionnement :**
   - Si produit déconditionnant ET quantité non multiple du conditionnement → Demander confirmation

4. **Validation Réussie :**
   - Tous les critères respectés → Autoriser ajout

---

#### 1.3 Ajout au Panier (COMPTANT)

```mermaid
sequenceDiagram
    participant U as Utilisateur
    participant M as Mobile App
    participant A as API Backend
    participant D as Base de données

    U->>M: Clique "Ajouter"
    M->>M: Valider Stock
    
    alt Première ligne
        M->>A: POST /api/sales/comptant/create
        Note over A: Créer nouvelle vente
        A->>D: INSERT sale + sale_line
        D-->>A: Sale créée (ID: 123)
        A-->>M: { sale: {...}, saleId: 123 }
    else Vente existante
        M->>A: POST /api/sales/comptant/add-item
        Note over A: Ajouter ligne à vente
        A->>D: INSERT sale_line
        D-->>A: Ligne ajoutée
        A-->>M: { salesLine: {...} }
        M->>A: GET /api/sales/123
        A-->>M: Vente mise à jour
    end
    
    M->>M: Mettre à jour UI
    M->>U: Afficher panier mis à jour
```

**API Endpoints :**

**Créer Nouvelle Vente Comptant :**
- Endpoint : `POST /api/sales/comptant/create`
- Données requises : lignes de vente (produit ID, quantité, prix), ID caissier, ID vendeur, ID client (optionnel)
- Réponse : ID de la vente créée, lignes de vente, montants totaux

**Ajouter Ligne à Vente Existante :**
- Endpoint : `POST /api/sales/comptant/add-item`
- Données requises : ID de la vente, ID produit, quantité demandée, prix unitaire, quantité vendue

---

#### 1.4 Ajout au Panier (ASSURANCE/CARNET)

```mermaid
sequenceDiagram
    participant U as Utilisateur
    participant M as Mobile App
    participant A as API Backend

    U->>M: Sélectionner Client
    M->>M: Vérifier Client Sélectionné
    
    alt Client NON sélectionné
        M->>U: ❌ Erreur: "Client requis"
        Note over M,U: Bloquer ajout produit
    else Client sélectionné
        U->>M: Ajouter Produit
        
        alt Première ligne
            M->>A: POST /api/sales/vo/create
            Note over A: natureVente = "ASSURANCE"
            A-->>M: Vente créée
        else Vente existante
            M->>A: POST /api/sales/vo/add-item
            A-->>M: Ligne ajoutée
        end
        
        M->>U: ✅ Produit ajouté
    end
```

**Différences ASSURANCE vs COMPTANT :**

| Critère | COMPTANT | ASSURANCE/CARNET |
|---------|----------|------------------|
| Client | ❌ Optionnel | ✅ **Obligatoire** |
| Tiers Payants | ❌ Non | ✅ **Obligatoire** (1-3) |
| Numéro Bon | ❌ Non | ✅ **Obligatoire** par tiers payant |
| Plafond | ❌ Non | ✅ Vérifié |
| Prescription | ❌ Non | ⚠️ Optionnel selon client |

---

## 🔄 WORKFLOW 2 : Modification de Produit

### État Global

```mermaid
stateDiagram-v2
    [*] --> ListeProduits
    ListeProduits --> SelectionLigne: Clic sur ligne
    SelectionLigne --> ModificationQuantité: Modifier Qté
    SelectionLigne --> ModificationPrix: Modifier Prix
    SelectionLigne --> SuppressionLigne: Supprimer
    
    ModificationQuantité --> ValidationStock
    ValidationStock --> AppelAPI: Stock OK
    ValidationStock --> Erreur: Stock KO
    
    ModificationPrix --> VérificationAutorisation
    VérificationAutorisation --> AppelAPI: Autorisé
    VérificationAutorisation --> DemandeAutorisation: Non autorisé
    DemandeAutorisation --> SaisieCredentials
    SaisieCredentials --> AppelAPI: Validé
    
    SuppressionLigne --> VérificationAutorisation2
    VérificationAutorisation2 --> AppelAPI2: Autorisé
    VérificationAutorisation2 --> DemandeAutorisation2: Non autorisé
    
    AppelAPI --> MiseAJour
    AppelAPI2 --> MiseAJour
    MiseAJour --> [*]
```

### Workflow Détaillé

#### 2.1 Modification Quantité

**Données Requises :**
- ID de la ligne de vente
- Nouvelle quantité
- ID de la vente

**Endpoint API :** `PUT /api/sales/comptant/update-item-quantity`

**Validation requise :** Même logique que ajout produit

---

#### 2.2 Modification Prix (Nécessite Autorisation)

```mermaid
flowchart TD
    A[Modifier Prix] --> B{Utilisateur a<br/>permission ?}
    B -->|Oui| C[Modifier Prix]
    B -->|Non| D[Demander Autorisation]
    
    D --> E[Modal Autorisation]
    E --> F[Saisir Login/Password<br/>Utilisateur Autorisé]
    F --> G{Credentials<br/>Valides ?}
    
    G -->|Oui| H[Vérifier Permission]
    G -->|Non| I[❌ Erreur Auth]
    
    H -->|OK| C
    H -->|KO| J[❌ Permission Refusée]
    
    C --> K[API Update Price]
    K --> L[✅ Prix Modifié]
```

**Endpoint API :** `POST /api/sales/comptant/update-item-price`

**Données Requises :**
- ID de la vente
- ID de la ligne de vente
- Nouveau prix unitaire
- ID du produit
- Quantité demandée

**Permission requise :** `PR_MODIFICATION_PRIX_VENTE`

---

#### 2.3 Suppression Ligne

**Endpoint API :** `DELETE /api/sales/comptant/delete-item/{saleLineId}`

**Permission requise :** `PR_SUPPRIME_PRODUIT_VENTE`

**Workflow identique à modification prix pour l'autorisation**

---

## 🔄 WORKFLOW 3 : Gestion Client

### 3.1 Ajout Client (COMPTANT - Optionnel)

```mermaid
sequenceDiagram
    participant U as Utilisateur
    participant M as Mobile
    participant A as API

    U->>M: Cliquer "Ajouter Client"
    M->>M: Ouvrir Liste Clients
    U->>M: Rechercher/Sélectionner Client
    M->>M: Client Sélectionné (ID: 789)
    
    alt Vente déjà créée
        M->>A: POST /api/sales/comptant/add-customer
        Note over A: { id: 123, value: 789 }
        A-->>M: ✅ Client ajouté
    else Pas de vente
        M->>M: Stocker client temporairement
        Note over M: Sera attaché à la création
    end
    
    M->>U: Afficher infos client
```

**API Endpoints :**

**Ajouter Client :**
- Endpoint : `POST /api/sales/comptant/add-customer`
- Données : ID de la vente, ID du client

**Retirer Client :**
- Endpoint : `POST /api/sales/comptant/remove-customer`
- Données : ID de la vente

---

### 3.2 Sélection Client (ASSURANCE/CARNET - Obligatoire)

```mermaid
flowchart TD
    A[Écran Vente] --> B{Client<br/>sélectionné ?}
    B -->|Non| C[🔒 Bloquer Ajout Produit]
    B -->|Oui| D[✅ Permettre Ajout]
    
    C --> E[Bouton "Sélectionner Client"]
    E --> F[Modal Recherche Client]
    
    F --> G{Type Client}
    G -->|Assuré| H[Liste Clients Assurés]
    G -->|Ayant-Droit| I[Liste Ayants-Droit]
    
    H --> J[Sélectionner Client]
    I --> J
    
    J --> K{Client a<br/>Tiers Payants ?}
    K -->|Oui| L[Charger Tiers Payants]
    K -->|Non| M[Créer Tiers Payants]
    
    L --> N[Client Sélectionné]
    M --> N
    N --> D
```

**Recherche Client :**

**Clients Assurés :**
- Endpoint : `GET /api/customers/search/assures`
- Paramètre : query (texte de recherche)

**Ayants-Droit d'un Client :**
- Endpoint : `GET /api/customers/{customerId}/ayants-droit`

**Informations Client Retournées :**
- ID, prénom, nom, nom complet
- Téléphone mobile, email
- Type (ASSURE ou AYANT_DROIT)
- ID du parent (si ayant-droit)
- Liste des tiers payants
- Plafonds mensuel et annuel
- Montants consommés mensuel et annuel

---

## 🔄 WORKFLOW 4 : Gestion Tiers Payants (ASSURANCE uniquement)

### Architecture

```mermaid
graph TB
    A[Client Assuré] --> B[Tiers Payant Principal<br/>Taux: 80%]
    A --> C[Tiers Payant Complémentaire 1<br/>Taux: 15%]
    A --> D[Tiers Payant Complémentaire 2<br/>Taux: 5%]
    
    B --> E[Numéro Bon Obligatoire]
    C --> E
    D --> E
    
    E --> F[Total Taux = 100%]
    
    style B fill:#4ecdc4
    style C fill:#95e1d3
    style D fill:#f7dc6f
```

### Workflow Détaillé

```mermaid
sequenceDiagram
    participant U as Utilisateur
    participant M as Mobile
    participant A as API

    U->>M: Sélectionner Client
    M->>A: GET /api/customers/{id}
    A-->>M: Client + Tiers Payants
    
    M->>M: Charger Tiers Payants Existants
    
    alt Tiers Payants Existe
        M->>U: Afficher Tiers Payants
    else Pas de Tiers Payants
        M->>U: Afficher "Ajouter Tiers Payant"
    end
    
    U->>M: Ajouter Complémentaire
    M->>M: Modal Tiers Payants
    U->>M: Sélectionner Tiers Payant
    U->>M: Saisir Taux (%)
    U->>M: Saisir Numéro Bon
    
    M->>M: Valider Total Taux ≤ 100%
    
    alt Total > 100%
        M->>U: ❌ Erreur: Taux total > 100%
        Note over M,U: Ajustement requis
    else Total ≤ 100%
        M->>M: ✅ Ajouter Tiers Payant
        M->>U: Afficher Liste Mise à Jour
    end

### Structure Tiers Payant

**Informations Tiers Payant :**
- ID (optionnel pour nouveau)
- ID du tiers payant
- Libellé du tiers payant
- Taux de prise en charge (entre 0 et 100%)
- Numéro de bon (obligatoire)
- Montant restant disponible (plafond)
- Ordre (0 = Principal, 1+ = Complémentaire)

**Validation Tiers Payants :**

**Règles de Validation :**
1. Au moins un tiers payant requis
2. Total des taux ne doit pas dépasser 100%
3. Chaque tiers payant doit avoir un numéro de bon valide

**Messages d'Erreur :**
- "Au moins un tiers payant requis"
- "Total des taux (X%) dépasse 100%"
- "Numéro de bon manquant pour [Nom Tiers Payant]"

### API Endpoints

**Récupérer Tiers Payants d'un Client :**
- Endpoint : `GET /api/customers/{customerId}/tiers-payants`

**Rechercher Tiers Payants Disponibles :**
- Endpoint : `GET /api/tiers-payants/search`
- Paramètre : query (texte de recherche)

**Ajouter Tiers Payant à un Client :**
- Endpoint : `POST /api/customers/{customerId}/tiers-payants`
- Données : ID tiers payant, taux, numéro de bon, ordre

---

## 🔄 WORKFLOW 5 : Finalisation Vente

### Vue d'Ensemble

```mermaid
stateDiagram-v2
    [*] --> VérifPanier
    VérifPanier --> VérifClient: Panier non vide
    VérifPanier --> Erreur: Panier vide
    
    VérifClient --> VérifTiersPayants: VO (Client obligatoire)
    VérifClient --> SaisiePaiement: Comptant
    
    VérifTiersPayants --> VérifBons: Tiers Payants OK
    VérifTiersPayants --> Erreur: Pas de Tiers Payants
    
    VérifBons --> VérifPlafond: Bons OK
    VérifBons --> ConfirmSansBon: Bon manquant
    
    ConfirmSansBon --> VérifPlafond: Utilisateur confirme
    ConfirmSansBon --> Annulation: Utilisateur refuse
    
    VérifPlafond --> ChoixModeRéglement: Plafond OK
    VérifPlafond --> Erreur: Plafond dépassé
    
    SaisiePaiement --> VérifMontant
    ChoixModeRéglement --> VérifMontant
    
    VérifMontant --> VérifCaisse: Montant OK
    VérifMontant --> VenteDifférée: Reste à payer
    
    VérifCaisse --> Finalisation: Caisse ouverte
    VérifCaisse --> OuvrirCaisse: Caisse fermée
    
    OuvrirCaisse --> Finalisation
    VenteDifférée --> Finalisation
    
    Finalisation --> Impression: Vente enregistrée
    Impression --> [*]: Terminé
```

---

### 5.1 Finalisation COMPTANT

```mermaid
sequenceDiagram
    participant U as Utilisateur
    participant M as Mobile
    participant A as API
    participant P as Imprimante

    U->>M: Cliquer "Finaliser"
    M->>M: Valider Panier Non Vide
    
    M->>M: Afficher Modes de Paiement
    U->>M: Sélectionner Mode(s)
    U->>M: Saisir Montant(s)
    
    M->>M: Calculer Total Versé
    M->>M: Calculer Monnaie à Rendre
    
    alt Montant < Total
        M->>U: Proposer Vente Différée
        U->>M: Confirmer Différé
        Note over M: differe = true
    end
    
    M->>A: POST /api/sales/comptant/finalize
    
    A->>A: Vérifier Caisse Ouverte
    
    alt Caisse Fermée
        A-->>M: Erreur: Caisse Fermée
        M->>U: ❌ Ouvrir Caisse d'abord
    else Caisse Ouverte
        A->>A: Enregistrer Vente
        A->>A: Mettre à jour Stock
        A->>A: Enregistrer Paiements
        A-->>M: ✅ Vente Finalisée (ID: 123)
        
        M->>P: Imprimer Ticket
        P-->>U: 🖨️ Ticket
        
        alt Client souhaite facture
            M->>A: GET /api/sales/123/invoice
            A-->>M: PDF Facture
            M->>P: Imprimer Facture
        end
        
        M->>M: Réinitialiser Vente
        M->>U: ✅ Vente Terminée
    end
```

**Données de Finalisation :**

**Informations Requises :**
- ID de la vente
- Mettre en attente (booléen)
- Montant total versé
- Commentaire (optionnel)
- Est un avoir (booléen)
- Liste des paiements

**Informations de Paiement :**
- Code mode règlement (CASH, CB, MOBILE, CHEQUE)
- Montant
- ID du mode règlement

**Exemple de Finalisation :**
- Vente ID : 123
- Pas en attente
- Montant versé : 15000 F
- Paiements : Espèces (10000 F) + Mobile Money (5000 F)

**Endpoint API :** `POST /api/sales/comptant/finalize`

**Réponse :**
- ID de la vente
- Succès (booléen)
- Imprimer ticket (booléen)
- Imprimer facture (booléen)

---

### 5.2 Finalisation ASSURANCE/CARNET

```mermaid
sequenceDiagram
    participant U as Utilisateur
    participant M as Mobile
    participant A as API

    U->>M: Cliquer "Finaliser"
    
    M->>M: Vérifier Client Sélectionné
    alt Pas de Client
        M->>U: ❌ Erreur: Client requis
    end
    
    M->>M: Vérifier Tiers Payants
    alt Pas de Tiers Payants
        M->>U: ❌ Erreur: Tiers Payants requis
    end
    
    M->>M: Vérifier Numéros de Bon
    alt Bon Manquant
        M->>U: ⚠️ Confirmer Vente Sans Bon ?
        U->>M: Oui/Non
        alt Non
            M->>U: Annulation
        end
    end
    
    M->>M: Calculer Répartition Montants
    Note over M: Part Tiers Payants<br/>Part Client
    
    M->>M: Vérifier Plafond Client
    alt Plafond Dépassé
        M->>U: ❌ Erreur: Plafond Dépassé
    end
    
    M->>A: POST /api/sales/vo/finalize
    Note over A: natureVente = ASSURANCE
    
    A->>A: Enregistrer Vente VO
    A->>A: Créer Bons Tiers Payants
    A->>A: Mettre à jour Stock
    A->>A: Mettre à jour Plafonds
    
    A-->>M: ✅ Vente Finalisée
    
    M->>U: Afficher Récapitulatif
    Note over M,U: Part Client: 2000 F<br/>Part Assurance: 8000 F
    
    alt Part Client > 0
        M->>M: Demander Paiement Client
        U->>M: Saisir Mode(s) Paiement
        M->>A: POST /api/sales/vo/add-payment
    end
    
    M->>M: Imprimer Ticket
    M->>U: ✅ Vente Terminée
```

**Données de Finalisation VO :**

**Informations Requises :**
- ID de la vente
- Liste des tiers payants avec taux et numéros de bon
- Mettre en attente (booléen)
- Montant versé par le client (part client)
- Liste des paiements effectués par le client
- Commentaire (optionnel)

**Réponse API :**
- ID de la vente
- Part client (montant à payer par le client)
- Part tiers payants (montant couvert par les assurances)
- Détails par tiers payant (libellé, montant, taux, numéro de bon)

**Calcul de Répartition :**

**Logique :**
1. Pour chaque tiers payant, calculer : (Montant Total × Taux) ÷ 100
2. Arrondir le résultat
3. Additionner tous les montants des tiers payants
4. Part client = Montant Total - Somme des parts tiers payants

**Exemple :**
- Montant total : 10000 F
- Tiers payant 1 (CNPS) : 80% → 8000 F
- Tiers payant 2 (MUTUELLE) : 15% → 1500 F
- Part client : 10000 - (8000 + 1500) = 500 F (5%)

**Endpoint API :** `POST /api/sales/vo/finalize`

---

### 5.3 Vente en Attente (Prévente)

```mermaid
flowchart TD
    A[Vente en Cours] --> B{Finaliser<br/>ou Mettre<br/>en Attente ?}
    
    B -->|Finaliser| C[Workflow Normal]
    B -->|Attente| D[Sauvegarder Prévente]
    
    D --> E[API: PUT ON STANDBY]
    E --> F[Vente Status = STANDBY]
    F --> G[Réinitialiser Écran]
    
    G --> H[Liste Ventes en Attente]
    H --> I{Reprendre<br/>Vente ?}
    
    I -->|Oui| J[Charger Vente]
    J --> K[Continuer Modification]
    K --> B
    
    I -->|Non| L[Rester en Attente]
```

**API Endpoints :**

**Mettre en Attente :**
- Endpoint : `POST /api/sales/comptant/put-on-standby`
- Données : ID de la vente

**Lister Ventes en Attente :**
- Endpoint : `GET /api/sales/pending`
- Retourne : Liste avec ID vente, date, nom client, montant total, nombre d'articles

**Reprendre une Vente :**
- Endpoint : `GET /api/sales/{saleId}`

---

## 🔄 WORKFLOW 6 : Transformation de Vente

### Cas d'Usage

Un client arrive **sans assurance** → Vente COMPTANT créée  
Client retrouve sa **carte d'assurance** → Transformer en ASSURANCE

```mermaid
sequenceDiagram
    participant U as Utilisateur
    participant M as Mobile
    participant A as API

    U->>M: Vente COMPTANT en cours
    Note over M: Produits déjà ajoutés
    
    U->>M: Cliquer "Transformer en Assurance"
    
    M->>U: Sélectionner Client Assuré
    U->>M: Client Sélectionné
    
    M->>A: POST /api/sales/transform
    Note over A: {<br/>  saleId: 123,<br/>  natureVente: "ASSURANCE"<br/>}
    
    A->>A: Changer Type Vente
    A->>A: Attacher Client
    A->>A: Préserver Produits
    
    A-->>M: Vente Transformée (ID: 123)
    
    M->>A: GET /api/sales/123
    A-->>M: Vente ASSURANCE
    
    M->>M: Charger Tiers Payants
    M->>U: Saisir Tiers Payants
    U->>M: Finaliser
```

**Endpoint API :** `POST /api/sales/transform`

**Données Requises :**
- ID de la vente
- Nouveau type de vente (ASSURANCE ou CARNET)

**Contraintes :**
- ✅ COMPTANT → ASSURANCE : OK
- ✅ COMPTANT → CARNET : OK
- ❌ ASSURANCE → COMPTANT : KO (perte tiers payants)
- ❌ CARNET → COMPTANT : KO (perte tiers payants)
- ✅ ASSURANCE ↔ CARNET : OK (même structure)

---

## 🔄 WORKFLOW 7 : Modes de Paiement

### Modes Disponibles

| Code | Libellé | Usage | Restrictions |
|------|---------|-------|--------------|
| `CASH` | Espèces | Comptant/VO | Aucune |
| `CB` | Carte Bancaire | Comptant/VO | Montant max configurable |
| `MOBILE` | Mobile Money | Comptant/VO | Vérification transaction |
| `CHEQUE` | Chèque | Comptant/VO | Numéro chèque requis |
| `VIREMENT` | Virement | VO seulement | Référence requise |

### Workflow Paiement Multiple

```mermaid
flowchart TD
    A[Montant Total: 15000] --> B{1 ou Plusieurs<br/>Modes ?}
    
    B -->|1 Mode| C[Sélectionner Mode]
    C --> D[Saisir Montant]
    D --> E{Montant = Total ?}
    E -->|Oui| F[Valider]
    E -->|Non| G[Ajuster Montant]
    G --> D
    
    B -->|Plusieurs| H[Sélectionner Mode 1]
    H --> I[Saisir Montant 1]
    I --> J{Ajouter<br/>Mode 2 ?}
    
    J -->|Oui| K[Sélectionner Mode 2]
    K --> L[Saisir Montant 2]
    L --> M{Somme = Total ?}
    
    M -->|Oui| F
    M -->|Non| G
    
    J -->|Non| N{Montant = Total ?}
    N -->|Oui| F
    N -->|Non| G
    
    F --> O[Enregistrer Paiements]
    O --> P[Finalisation]
```

| Code | Libellé | Usage | Restrictions |
| `CASH` | Espèces | Comptant/VO | Aucune |
| `CB` | Carte Bancaire | Comptant/VO | Montant max configurable |
| `MOBILE` | Mobile Money | Comptant/VO | Vérification transaction |
| `CHEQUE` | Chèque | Comptant/VO | Numéro chèque requis |
| `VIREMENT` | Virement | VO seulement | Référence requise |

### Workflow Paiement Multiple

```mermaid
flowchart TD
    A[Montant Total: 15000] --> B{1 ou Plusieurs<br/>Modes ?}
    
    B -->|1 Mode| C[Sélectionner Mode]
    C --> D[Saisir Montant]
    D --> E{Montant = Total ?}
    E -->|Oui| F[Valider]
    E -->|Non| G[Ajuster Montant]
    G --> D
    
    B -->|Plusieurs| H[Sélectionner Mode 1]
    H --> I[Saisir Montant 1]
    I --> J{Ajouter<br/>Mode 2 ?}
    
    J -->|Oui| K[Sélectionner Mode 2]
    K --> L[Saisir Montant 2]
    L --> M{Somme = Total ?}
    
    M -->|Oui| F
    M -->|Non| G
    
    J -->|Non| N{Montant = Total ?}
    N -->|Oui| F
    N -->|Non| G
    
    F --> O[Enregistrer Paiements]
    O --> P[Finalisation]
```

**Exemple Multi-Paiement :**

Montant total : 15000 F
- Paiement 1 : Espèces (CASH) → 10000 F
- Paiement 2 : Mobile Money (MOBILE) → 3000 F
- Paiement 3 : Carte Bancaire (CB) → 2000 F
- Total : 10000 + 3000 + 2000 = 15000 F ✅

**Règles de Validation :**

1. Maximum 2 modes de paiement autorisés
2. La somme des paiements doit être exactement égale au montant total


---

## 📊 Récapitulatif API Endpoints

### Ventes COMPTANT

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/sales/comptant/create` | POST | Créer nouvelle vente |
| `/api/sales/comptant/add-item` | POST | Ajouter produit |
| `/api/sales/comptant/update-item-quantity` | PUT | Modifier quantité |
| `/api/sales/comptant/update-item-price` | POST | Modifier prix |
| `/api/sales/comptant/delete-item/{id}` | DELETE | Supprimer ligne |
| `/api/sales/comptant/add-customer` | POST | Ajouter client |
| `/api/sales/comptant/remove-customer` | POST | Retirer client |
| `/api/sales/comptant/finalize` | POST | Finaliser vente |
| `/api/sales/comptant/put-on-standby` | POST | Mettre en attente |

### Ventes ASSURANCE/CARNET (VO)

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/sales/vo/create` | POST | Créer nouvelle vente VO |
| `/api/sales/vo/add-item` | POST | Ajouter produit |
| `/api/sales/vo/update-item-quantity` | PUT | Modifier quantité |
| `/api/sales/vo/update-item-price` | POST | Modifier prix |
| `/api/sales/vo/delete-item/{id}` | DELETE | Supprimer ligne |
| `/api/sales/vo/change-customer` | POST | Changer client |
| `/api/sales/vo/finalize` | POST | Finaliser vente |
| `/api/sales/vo/put-on-standby` | POST | Mettre en attente |
| `/api/sales/transform` | POST | Transformer type vente |

### Clients & Tiers Payants

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/customers/search/assures` | GET | Rechercher clients assurés |
| `/api/customers/{id}` | GET | Détails client |
| `/api/customers/{id}/ayants-droit` | GET | Ayants-droit |
| `/api/customers/{id}/tiers-payants` | GET | Tiers payants client |
| `/api/tiers-payants/search` | GET | Rechercher tiers payants |

### Produits

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/products/search` | GET | Rechercher produits |
| `/api/products/{id}` | GET | Détails produit |

### Ventes en Attente

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/sales/pending` | GET | Liste préventes |
| `/api/sales/{id}` | GET | Charger vente |
| `/api/sales/{id}/delete` | DELETE | Supprimer prévente |

---




## 🔧 Gestion d'Erreurs

### Codes d'Erreur Courants

| Code | Message | Action Mobile |
|------|---------|---------------|
| `stock` | Stock insuffisant | Afficher stock disponible + proposer force |
| `stockChInsufisant` | Stock insuffisant | Bloquer ou proposer force stock |
| `plafondAtteint` | Plafond dépassé | Bloquer vente, afficher plafond restant |
| `bonManquant` | Numéro bon requis | Demander confirmation vente sans bon |
| `clientRequis` | Client obligatoire | Bloquer ajout produit, forcer sélection client |
| `tiersPayantRequis` | Tiers payant manquant | Demander ajout tiers payants |
| `caisseNonOuverte` | Caisse fermée | Proposer ouverture caisse |
| `unauthorized` | Autorisation refusée | Demander credentials utilisateur autorisé |

### Exemple Gestion Erreur

**Processus de Gestion :**

1. **Stock Insuffisant (stock / stockChInsufisant) :**
   - Afficher alerte avec stock disponible
   - Proposer boutons : "Annuler" et "Forcer" (si autorisé)

2. **Plafond Dépassé (plafondAtteint) :**
   - Afficher alerte avec plafond mensuel et montant déjà consommé
   - Bouton : "OK" (blocage)

3. **Client Requis (clientRequis) :**
   - Afficher alerte demandant la sélection d'un client assuré
   - Bouton : "Sélectionner" qui ouvre le sélecteur de client

4. **Erreur Générique :**
   - Afficher message d'erreur générique
   - Bouton : "OK"

---

## 📱 Checklist Implémentation Mobile

### Phase 1 : Fonctionnalités de Base
- [ ] Recherche produits
- [ ] Ajout produit au panier
- [ ] Modification quantité
- [ ] Suppression ligne
- [ ] Affichage total
- [ ] Validation stock basique

### Phase 2 : Gestion Clients
- [ ] Recherche clients
- [ ] Sélection client (COMPTANT)
- [ ] Sélection client assuré (ASSURANCE)
- [ ] Affichage infos client
- [ ] Vérification plafonds

### Phase 3 : Tiers Payants (ASSURANCE)
- [ ] Affichage tiers payants client
- [ ] Ajout tiers payant complémentaire
- [ ] Saisie numéros de bon
- [ ] Validation taux (≤ 100%)
- [ ] Calcul répartition montants

### Phase 4 : Paiement
- [ ] Sélection mode(s) paiement
- [ ] Paiement multiple (max 2)
- [ ] Calcul monnaie à rendre
- [ ] Validation montants

### Phase 5 : Finalisation
- [ ] Finalisation COMPTANT
- [ ] Finalisation ASSURANCE/CARNET
- [ ] Impression ticket (si possible)
- [ ] Gestion vente différée
- [ ] Mise en attente (prévente)

### Phase 6 : Fonctionnalités Avancées
- [ ] Modification prix (avec autorisation)
- [ ] Force stock (avec autorisation)
- [ ] Déconditionnement
- [ ] Transformation type vente
- [ ] Ventes en attente (liste)
- [ ] Reprise vente en attente
- [ ] Suppression vente en attente

### Phase 7 : Hors-Ligne & Sync
- [ ] Stockage local ventes
- [ ] Cache produits
- [ ] Cache clients
- [ ] Synchronisation auto
- [ ] Gestion conflits

### Phase 8 : Optimisations
- [ ] Scan code-barres
- [ ] Raccourcis clavier
- [ ] Recherche rapide
- [ ] Historique recherches
- [ ] Favoris produits

---

## 🎯 Conclusion

Ce document fournit une **vue complète des workflows fonctionnels** du module de vente pour une implémentation mobile efficace. 

### Points Clés à Retenir

1. **3 Types de Ventes** avec logiques distinctes
2. **Client Obligatoire** pour ASSURANCE/CARNET
3. **Tiers Payants** critiques pour ASSURANCE
4. **Validation Stock** multi-niveaux
5. **Paiements Multiples** (max 2 modes)
6. **Autorisations** pour actions sensibles
7. **Préventes** pour workflow interrompu
8. **Transformation** entre types de vente

### Ressources Supplémentaires


- **Environnements** : Dev, Staging, Prod

**Bonne implémentation ! 📱✨**


