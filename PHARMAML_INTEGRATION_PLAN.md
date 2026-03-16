# Plan d'Intégration PharmaML — Pharma-Smart

> **Contexte** : Protocole CSRP (Commandes Standard Répartiteurs Pharmacies) v2.x
> **Standard** : PharmaML défini par CNAMTS/CSRP, utilisé par les grands répartiteurs français (OCP, Alliance Healthcare, CERP, Phoenix Pharma…)
> **Date de rédaction** : 2026-03-16
> **Dernière mise à jour** : 2026-03-16

---

## 1. État des lieux

### 1.1 Ce qui est implémenté ✅

| Composant | Localisation | Statut |
|---|---|---|
| DTOs requête XML (CSRP_ENVELOPPE) | `service/pharmaml/dto/` | ✅ Complet |
| DTOs réponse XML | `service/pharmaml/dto/response/` | ✅ Complet |
| Marshalling/Unmarshalling JAXB statique | `PharmaMlServiceImpl` | ✅ Thread-safe |
| Envoi commande NORMALE + EXCEPTIONNELLE | `PharmaMlServiceImpl` | ✅ Fonctionnel |
| Traitement réponse + mises à jour prix/qté | `traiterCommandeRepondue` | ✅ Fonctionnel |
| Ruptures EL/RL + remplacement produit | `processRemplacement` | ✅ EL+RL uniquement |
| Retry backoff exponentiel (3 tentatives) | `sendWithRetry` | ✅ Fonctionnel |
| Table de suivi `pharmaml_envoi` | `PharmaMlEnvoi` / Flyway V1.3.0 | ✅ Complet |
| Endpoint REST `/api/pharmaml/*` | `PharmaMlResource` | ✅ Complet |
| Frontend envoi / réponse / historique / polling | `features/commande/pharmaml/` | ✅ Complet |
| Configuration groupeFournisseur (idRecepteur inclus) | `GroupeFournisseur` / form | ✅ Complet |
| TypeCommande NORMALE/EXCEPTIONNELLE branchée | `buildCommande` | ✅ Complet |
| Flags CSRP partielle/reliquat/equivalent sur LigneN | `buildCommandeLigne` | ✅ Valeurs par défaut |
| Archivage commande en rupture totale | `traiterCommandeRepondue` | ✅ Complet |

### 1.2 Ce qui est absent ou incomplet

| Fonctionnalité | Nature CSRP | Priorité |
|---|---|---|
| Substitutions `TypeRemplacement.EP` (proposé non livré) | Réponse existante | 🔴 Haute |
| Demande disponibilité/prix avant commande | `REQ_INFORMATION` | 🔴 Haute |
| Commande complémentaire pour reliquats | `REQ_EMISSION` reliquat | 🔴 Haute |
| Accusé de réception entrant | `ACQ_RECEPTION` callback | 🟠 Moyenne |
| Retour marchandise vers grossiste | `REQ_RETOUR` | 🟠 Moyenne |
| Annulation de commande envoyée | `REQ_ANNULATION` | 🟠 Moyenne |
| BL dématérialisé à la réception physique | Réponse livraison | 🟡 Basse |
| Mise à jour catalogue prix automatique | Flux prix | 🟡 Basse |
| Ventilation multi-grossiste par ligne | UI commande | 🟡 Basse |

---

## 2. Problèmes Techniques Identifiés

### 2.1 Thread safety — champ `fournisseur` mutable
```java
// PharmaMlServiceImpl.java:91 — PROBLÈME
@Service  // singleton Spring
private Fournisseur fournisseur;  // état partagé entre threads !
```
**Risque** : En concurrence, deux envois simultanés peuvent se mélanger.
**Correction** : Passer `fournisseur` en paramètre local ou utiliser un `record` immuable de contexte.

### 2.2 `JAXBContext` recréé à chaque appel
```java
JAXBContext requestContext = JAXBContext.newInstance(CsrpEnveloppe.class); // ligne 125 — coûteux
```
**Correction** : `JAXBContext` est thread-safe, initialiser en `static final`.

### 2.3 `ConcurrentModificationException` potentielle
```java
for (LigneNReponse ligneNReponse : lignes) {
    items.stream()...ifPresentOrElse(orderLine -> {
        items.remove(orderLine);  // ligne 422 — modification pendant itération externe
    }, ...);
}
```
**Correction** : Collecter les items à supprimer dans un `Set`, les supprimer après la boucle.

### 2.4 Suppression de commande au lieu d'archivage
```java
if (itemSize == ruptureComplet.get()) {
    commandeRepository.delete(commande);  // ligne 441 — perte de traçabilité
}
```
**Correction** : Archiver (`setOrderStatus(ARCHIVED)`) et conserver pour audit.

### 2.5 `fournisseur` non transmis à `createRupture` pour les lignes partielles
Dans `createRupture`, `fournisseur` est le champ d'instance (voir §2.1). Pour les ruptures, le fournisseur devrait être passé explicitement.

---

## 3. Architecture Cible

```
┌─────────────────────────────────────────────────────────────┐
│                    ANGULAR FRONTEND                          │
│  ┌──────────────────┐  ┌──────────────────────────────────┐ │
│  │ PharmaML Config  │  │  Commande → Envoi PharmaML UI    │ │
│  │ (groupFournisseur│  │  (bouton envoi + suivi statut)   │ │
│  │  URL/codes)      │  └──────────────────────────────────┘ │
│  └──────────────────┘  ┌──────────────────────────────────┐ │
│                        │  Tableau ruptures + remplaçants  │ │
│                        └──────────────────────────────────┘ │
└───────────────────────────────┬─────────────────────────────┘
                                │ REST /api/pharmaml/*
┌───────────────────────────────▼─────────────────────────────┐
│                    SPRING BOOT BACKEND                       │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              PharmaMlResource (REST)                │    │
│  │  POST /api/pharmaml/envoyer-commande                │    │
│  │  POST /api/pharmaml/renvoyer-commande               │    │
│  │  GET  /api/pharmaml/ruptures/{commandeId}           │    │
│  │  PUT  /api/pharmaml/rupture/{id}/reponse            │    │
│  └──────────────────┬──────────────────────────────────┘    │
│                     │                                        │
│  ┌──────────────────▼──────────────────────────────────┐    │
│  │            PharmaMlService (refactorisé)            │    │
│  │  + PharmaMlContext (immuable, thread-safe)          │    │
│  │  + JAXBContext statique                             │    │
│  │  + gestion TypeCommande (NORMALE/EXCEPTIONNELLE)    │    │
│  │  + gestion reliquats + équivalents                  │    │
│  └──────────────────┬──────────────────────────────────┘    │
│                     │                                        │
│  ┌──────────────────▼──────────────────────────────────┐    │
│  │               CSRP HTTP Client                      │    │
│  │  + retry (3 tentatives, backoff exponentiel)        │    │
│  │  + timeout configurable                             │    │
│  │  + archivage XML (requête + réponse)                │    │
│  └──────────────────┬──────────────────────────────────┘    │
└─────────────────────┼───────────────────────────────────────┘
                      │ HTTPS POST text/xml
┌─────────────────────▼───────────────────────────────────────┐
│                  Répartiteur (OCP / Alliance / CERP…)        │
│              Serveur PharmaML / CSRP v2.x                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Plan d'Implémentation par Phases

---

### PHASE 1 — Correction des bugs critiques *(priorité immédiate)*

#### 1.1 Corriger le thread-safety de `PharmaMlServiceImpl`

Extraire un objet de contexte immuable passé aux méthodes privées :

```java
// Nouveau record interne
private record PharmaMlContext(Fournisseur fournisseur, GroupeFournisseur groupe) {}

// Dans envoiPharmaCommande — remplacer this.fournisseur par le contexte local
PharmaMlContext ctx = new PharmaMlContext(fournisseur, fournisseur.getGroupeFournisseur());
prevalidate(ctx);
CsrpEnveloppe payLoad = buildPayload(commande, envoiParamsDTO.getCommentaire(), ctx);
```

Supprimer le champ `private Fournisseur fournisseur`.

#### 1.2 `JAXBContext` statique

```java
private static final JAXBContext REQUEST_JAXB_CONTEXT;
private static final JAXBContext RESPONSE_JAXB_CONTEXT;
static {
    try {
        REQUEST_JAXB_CONTEXT  = JAXBContext.newInstance(CsrpEnveloppe.class);
        RESPONSE_JAXB_CONTEXT = JAXBContext.newInstance(CsrpEnveloppeResponse.class);
    } catch (JAXBException e) { throw new ExceptionInInitializerError(e); }
}
```

#### 1.3 Supprimer `items.remove()` dans le forEach

```java
List<OrderLine> traitees = new ArrayList<>();
for (LigneNReponse ligne : lignes) {
    items.stream()
        .filter(o -> ligne.getCodeProduit().equals(o.getFournisseurProduit().getCodeCip()))
        .findFirst()
        .ifPresentOrElse(orderLine -> {
            traitees.add(orderLine);
            // traitement...
        }, () -> LOG.warn(...));
}
items.removeAll(traitees); // après la boucle
```

#### 1.4 Archiver au lieu de supprimer en cas de rupture totale

```java
// Remplacer commandeRepository.delete(commande) par :
commande.setOrderStatus(OrderStatut.ARCHIVED);
commande.setUpdatedAt(LocalDateTime.now());
commandeRepository.save(commande);
```

---

### PHASE 2 — Exposition REST *(backend)*

#### 2.1 Créer `PharmaMlResource`

**Fichier** : `web/rest/commande/PharmaMlResource.java`

```java
@RestController
@RequestMapping("/api/pharmaml")
@Transactional
public class PharmaMlResource {

    @PostMapping("/envoyer-commande")
    public ResponseEntity<PharmamlCommandeResponse> envoyerCommande(
            @Valid @RequestBody EnvoiParamsDTO params) {
        return ResponseEntity.ok(pharmaMlService.envoiPharmaCommande(params));
    }

    @PostMapping("/renvoyer-commande")
    public ResponseEntity<Void> renvoyerCommande(@Valid @RequestBody EnvoiParamsDTO params) {
        pharmaMlService.renvoiPharmaCommande(params);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/ruptures/{commandeId}/{orderDate}")
    public ResponseEntity<VerificationResponseCommandeDTO> getRuptures(
            @PathVariable Integer commandeId, @PathVariable String orderDate) {
        return ResponseEntity.ok(
            pharmaMlService.lignesCommandeRetour(null, commandeId + "/" + orderDate));
    }

    @PutMapping("/rupture/{id}/reponse")
    public ResponseEntity<VerificationResponseCommandeDTO> repondreRupture(
            @PathVariable String id) {
        return ResponseEntity.ok(pharmaMlService.reponseRupture(id));
    }
}
```

#### 2.2 Implémenter les méthodes vides

**`renvoiPharmaCommande`** : vérifier `hasBeenSubmittedToPharmaML == true` puis rappeler `envoiPharmaCommande` en remettant le flag à false.

**`lignesCommandeRetour`** : retourner les OrderLines à quantité réduite (ruptures partielles) pour permettre à l'utilisateur de valider ou d'annuler.

**`reponseRupture`** : traiter l'arbitrage des produits en rupture (accepter/refuser le remplaçant, relancer sur un autre fournisseur).

#### 2.3 `EnvoiParamsDTO` — activer `TypeCommande` et `dateLivraison`

```java
// buildCommande() — utiliser dateLivraisonSouhaitee si fournie
LocalDate dateLivraison = envoiParamsDTO.getDateLivraisonSouhaitee() != null
    ? envoiParamsDTO.getDateLivraisonSouhaitee()
    : LocalDate.now().plusDays(1);
c.setDateLivraison(dateLivraison.toString());
```

---

### PHASE 3 — Gestion complète du protocole CSRP *(conformité)*

#### 3.1 Flags de livraison sur `LigneN`

Le protocole CSRP autorise trois flags sur chaque ligne de commande :

| Flag XML | Champ `LigneN` | Signification |
|---|---|---|
| `Partielle="O"` | `partielle` | Accepter une livraison partielle |
| `Reliquat="O"` | `reliquat` | Demander la mise en reliquat du solde |
| `Equivalent="O"` | `equivalent` | Accepter un équivalent thérapeutique |

Ces flags doivent être configurables par ligne de commande. Proposition : ajouter un champ `pharmamlOptions` sur `OrderLine` (JSON ou 3 booléens) ou les passer dans `EnvoiParamsDTO`.

#### 3.2 Gestion des reliquats

Quand `Reliquat=O` et que la réponse indique une livraison partielle :
1. Créer une nouvelle `Commande` en statut `REQUESTED` avec les quantités restantes
2. Lier à la commande originale via `originalCommandeId`
3. Notifier l'utilisateur via `StockEntryResultDTO.pendingRetourBons` ou un mécanisme similaire

#### 3.3 Gestion `NATURE_ACTION` — accusés de réception

Le protocole prévoit plusieurs valeurs pour `Nature_Action` :

| Valeur | Signification |
|---|---|
| `REQ_EMISSION` | Envoi d'une commande (implémenté) |
| `ACQ_RECEPTION` | Accusé de réception du repartiteur |
| `REP_DEMANDE` | Réponse à une demande |

L'implémentation actuelle ne gère que `REQ_EMISSION`. À terme, un endpoint de callback (webhook ou polling) devra traiter les messages entrants du répartiteur.

#### 3.4 Codification CIP — harmonisation avec les lots

La méthode `typeCodification()` détecte CIP39 (< 13 chars) vs EAN13 (13 chars). Il faut s'assurer que ce même code CIP est cohérent avec la codification utilisée dans `Lot.numLot` pour la traçabilité FEFO.

---

### PHASE 4 — Fiabilité et observabilité *(production)*

#### 4.1 Retry avec backoff exponentiel

L'appel HTTP bloquant actuel peut échouer si le serveur du répartiteur est temporairement indisponible.

```java
// Utiliser Spring Retry ou un mécanisme manuel
@Retryable(retryFor = IOException.class, maxAttempts = 3,
           backoff = @Backoff(delay = 2000, multiplier = 2))
public PharmamlCommandeResponse envoiPharmaCommande(EnvoiParamsDTO params) { ... }
```

Ou configurer un `HttpClient` avec timeout :
```java
HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build();
```

#### 4.2 Exécution asynchrone

Pour éviter de bloquer le thread HTTP de l'application pendant l'appel au répartiteur (pouvant durer plusieurs secondes) :

```java
@Async("pharmaMlTaskExecutor")
public CompletableFuture<PharmamlCommandeResponse> envoiPharmaCommandeAsync(EnvoiParamsDTO params) { ... }
```

Le frontend interroge ensuite un endpoint de statut :
```
GET /api/pharmaml/statut/{commandeId}
→ { statut: "SUBMITTED" | "ACKNOWLEDGED" | "PARTIAL" | "REJECTED" | "ERROR" }
```

#### 4.3 Table de suivi `pharmaml_envoi`

```sql
CREATE TABLE pharmaml_envoi (
    id               SERIAL PRIMARY KEY,
    commande_id      INTEGER NOT NULL,
    commande_date    DATE NOT NULL,
    fournisseur_id   INTEGER NOT NULL,
    statut           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- PENDING | SUBMITTED | ACKNOWLEDGED | PARTIAL | REJECTED | ERROR
    ref_message      VARCHAR(30),        -- RefMessage CSRP
    tentatives       INTEGER DEFAULT 0,
    derniere_tentative TIMESTAMP,
    xml_requete_path VARCHAR(255),
    xml_reponse_path VARCHAR(255),
    total_lignes     INTEGER,
    lignes_acceptees INTEGER,
    lignes_rupture   INTEGER,
    created_at       TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_pharmaml_commande FOREIGN KEY (commande_id, commande_date)
        REFERENCES commande(id, order_date)
);
```

#### 4.4 Unicité du `RefMessage`

Le `RefMessage` doit être unique et traçable. Actuellement :
```java
e.setRefMessage(LocalDateTime.now().format(...)); // risque de collision
```
**Correction** : utiliser `commande.getOrderReference() + "_" + UUID.randomUUID().toString().substring(0,8)`.

---

### PHASE 5 — Interface Angular *(frontend)*

#### 5.1 Composants à créer

```
src/main/webapp/app/features/commande/pharmaml/
├── pharmaml.service.ts               — HTTP service (/api/pharmaml/*)
├── envoi/
│   ├── envoi-pharmaml.component.ts   — formulaire d'envoi (TypeCommande, date livraison, commentaire)
│   └── envoi-pharmaml.component.html
├── suivi/
│   ├── suivi-pharmaml.component.ts   — tableau de bord statut commandes soumises
│   └── suivi-pharmaml.component.html
└── ruptures/
    ├── ruptures-pharmaml.component.ts — gestion des ruptures + remplaçants
    └── ruptures-pharmaml.component.html
```

#### 5.2 Modèles TypeScript

```typescript
// pharmaml.model.ts
export interface IEnvoiPharmaParams {
  commandeId: CommandeId;
  grossisteId?: number;
  typeCommande: 'NORMALE' | 'EXCEPTIONNELLE';
  commentaire?: string;
  dateLivraisonSouhaitee?: string;   // ISO date
}

export interface IPharmamlCommandeResponse {
  success: boolean;
  totalProduit: number;
  successCount: number;
  outOfStockCount: number;
}

export interface IPharmamlRupture {
  produitCip: string;
  produitLibelle: string;
  quantiteCommandee: number;
  quantiteLivree: number;
  codeReponse: string;
  remplacant?: IPharmamlRemplacant;
}

export interface IPharmamlRemplacant {
  cip: string;
  designation: string;
  typeRemplacement: 'RL' | 'EL' | 'EP';
}
```

#### 5.3 Intégration dans `commande-update.component.ts`

Ajouter un bouton **Envoyer via PharmaML** visible uniquement si :
- `commande.orderStatus === 'REQUESTED'`
- `commande.hasBeenSubmittedToPharmaML === false`
- `groupeFournisseur.urlPharmaMl` configuré

```html
<p-button
  *ngIf="canSubmitToPharmaML()"
  label="Envoyer PharmaML"
  icon="pi pi-send"
  severity="info"
  (onClick)="onEnvoyerPharmaML()"
/>
```

#### 5.4 Configuration PharmaML dans l'interface groupeFournisseur

Ajouter dans le formulaire `groupe-fournisseur-update` :
- Champ **URL PharmaML** (validé `@URL`)
- Champ **Code récepteur** (`code_recepteur_pharma_ml`)
- Champ **ID récepteur** (`id_recepteur_pharma_ml`)
- Champ **Code officine** (`code_office_pharma_ml`)

---

---

### PHASE 6 — Substitutions EP *(priorité haute — quotidien)*

Le type `TypeRemplacement.EP` (Equivalent Proposé) signifie que le grossiste **propose** un remplaçant mais **ne le livre pas** — la pharmacie doit valider avant qu'il soit expédié. C'est le cas le plus fréquent de remplacement.

#### 6.1 Backend — `processRemplacement`

```java
// Ajouter le cas EP dans processRemplacement :
case "EP":
    // Créer une SubstitutionProposee en BDD, notifier l'utilisateur
    substitutionService.creerProposition(origin, produitRemplacant, commande, fournisseur);
    break;
```

Nouvelle entité `SubstitutionProposee` :
```sql
CREATE TABLE warehouse.substitution_proposee (
    id              SERIAL PRIMARY KEY,
    commande_id     INTEGER NOT NULL,
    commande_date   DATE NOT NULL,
    order_line_id   INTEGER NOT NULL,  -- ligne originale (rupture)
    cip_propose     VARCHAR(13) NOT NULL,
    designation     VARCHAR(255),
    statut          VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    -- EN_ATTENTE | ACCEPTEE | REFUSEE
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

Nouveau endpoint :
```
POST /api/pharmaml/substitution/{id}/accepter  → crée OrderLine + REQ_EMISSION pour ce produit
POST /api/pharmaml/substitution/{id}/refuser   → marque REFUSEE, pas de commande
GET  /api/pharmaml/substitutions/{commandeId}/{orderDate} → liste des propositions en attente
```

#### 6.2 Frontend — badge + modal de validation

- Badge "X substitution(s) en attente" dans `pharmaml-home` si des EP existent
- Modal listant les produits proposés avec boutons Accepter / Refuser par ligne

---

### PHASE 7 — REQ_INFORMATION / Disponibilité & Prix *(priorité haute — quotidien)*

Permet de vérifier la disponibilité et le prix auprès du grossiste **avant** de passer commande, évitant les ruptures surprises.

#### 7.1 DTOs requête REQ_INFORMATION

```java
// Nouvelle classe CsrpEnveloppe avec Nature_Action = REQ_INFORMATION
// Corps : liste de codes CIP à interroger
@XmlElement(name = "DEMANDE_INFO") private DemandeInfo demandeInfo;
```

#### 7.2 Service

```java
DemandeDisponibiliteResponse demanderDisponibilite(Integer commandeId);
// Envoie les CIP des lignes de commande → reçoit stock dispo + prix unitaire
```

#### 7.3 Réponse — mise à jour des prix avant envoi

Si le répartiteur répond avec des prix mis à jour, proposer à l'utilisateur de les appliquer sur les `OrderLine` avant de confirmer l'envoi.

#### 7.4 Frontend

- Bouton "Vérifier disponibilité" dans `pharmaml-home` (avant "Envoyer")
- Tableau de résultats : produit / qté disponible / prix grossiste / statut

---

### PHASE 8 — Reliquats / Commande complémentaire *(priorité haute — quotidien)*

Quand une livraison est partielle et que `Reliquat=O`, créer automatiquement une commande complémentaire pour le solde non livré.

#### 8.1 Backend — détection et création

Dans `traiterCommandeRepondue`, après traitement :
```java
if (countRupture.get() > 0 && hasReliquatLines(lignesRupture)) {
    Commande reliquat = creerCommandeReliquat(commande, lignesRupture);
    commandeRepository.save(reliquat);
}
```

Champ à ajouter sur `Commande` :
```sql
ALTER TABLE warehouse.commande ADD COLUMN reliquat_de_commande_id INTEGER;
-- FK → commande.id (commande parente)
```

#### 8.2 Règles métier

- La commande reliquat est créée avec `OrderStatut.REQUESTED`
- Statut visible dans la liste des commandes avec badge "Reliquat de #REF"
- L'utilisateur peut modifier la quantité avant de la soumettre via PharmaML

#### 8.3 Frontend

- Badge "Reliquat créé" dans `pharmaml-home` après envoi partiel
- Lien vers la commande reliquat générée

---

### PHASE 9 — ACQ_RECEPTION callback *(priorité moyenne)*

Le grossiste envoie un accusé de réception confirmant que la commande a été intégrée dans son système.

#### 9.1 Endpoint entrant

```java
@PostMapping("/callback/acq-reception")
public ResponseEntity<Void> acqReception(@RequestBody String xmlBody) {
    pharmaMlService.traiterAcqReception(xmlBody);
    return ResponseEntity.ok().build();
}
```

- Parser le XML `ACQ_RECEPTION`
- Mettre à jour `pharmaml_envoi.statut` → `ACKNOWLEDGED`
- Ajouter `ACKNOWLEDGED` à l'enum `PharmaMlStatut`

#### 9.2 Sécurité

Le callback vient du réseau du répartiteur. Vérifier l'IP source ou un token partagé configuré dans `GroupeFournisseur`.

---

### PHASE 10 — REQ_RETOUR / Retour marchandise *(priorité moyenne)*

Retourner des produits au grossiste (périmés, surstock, rappel de lot).

#### 10.1 Nouveau flux CSRP

- `Nature_Action = REQ_RETOUR`
- Corps : liste de CIP + quantités + motif (expiration, surstock, rappel)
- Réponse : `ACQ_RETOUR` avec bon de retour du grossiste

#### 10.2 Backend

```java
void envoiRetourMarchandise(RetourParamsDTO params);
// RetourParamsDTO : fournisseurId, lignes (cip, qty, motif), dateRetour
```

Nouvelle migration :
```sql
CREATE TABLE pharmaml_retour (
    id             SERIAL PRIMARY KEY,
    fournisseur_id INTEGER NOT NULL,
    statut         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ref_message    VARCHAR(30),
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE TABLE warehouse.pharmaml_retour_ligne (
    id        SERIAL PRIMARY KEY,
    retour_id INTEGER NOT NULL REFERENCES warehouse.pharmaml_retour(id),
    cip       VARCHAR(13) NOT NULL,
    quantite  INTEGER NOT NULL,
    motif     VARCHAR(50) -- PERIME | SURSTOCK | RAPPEL_LOT | AUTRE
);
```

---

### PHASE 11 — REQ_ANNULATION *(priorité moyenne)*

Annuler une commande déjà envoyée au grossiste avant sa préparation.

#### 11.1 Règles

- Possible uniquement si `pharmaml_envoi.statut IN ('PENDING', 'SUBMITTED')`
- Non possible si `ACQ_RECEPTION` déjà reçu (statut `ACKNOWLEDGED`)

#### 11.2 Backend

```java
void annulerCommande(Integer commandeId);
// Envoie REQ_ANNULATION, met à jour pharmaml_envoi.statut → CANCELLED
// Met à jour Commande.orderStatus → CANCELED
```

#### 11.3 Frontend

- Bouton "Annuler l'envoi PharmaML" visible uniquement si statut `PENDING` ou `SUBMITTED`
- Confirmation avant envoi

---

## 5. Matrice des Priorités

| Phase | Effort | Valeur métier | Statut |
|---|---|---|---|
| P1 — Bugs critiques | Faible | Stabilité | ✅ Fait |
| P2 — REST endpoints | Moyen | Déblocage fonctionnel | ✅ Fait |
| P3 — Conformité CSRP (EXCEPTIONNELLE, reliquat flag) | Élevé | Conformité réglementaire | ✅ Fait |
| P4 — Fiabilité (retry, statut, tracking) | Moyen | Production-ready | ✅ Fait |
| P5 — Frontend Angular complet | Élevé | UX pharmacien | ✅ Fait |
| **P6 — Substitutions EP** | Moyen | Quotidien pharmacien | 🔴 À faire |
| **P7 — REQ_INFORMATION disponibilité** | Élevé | Quotidien avant commande | 🔴 À faire |
| **P8 — Reliquats auto** | Moyen | Quotidien livraison partielle | 🔴 À faire |
| P9 — ACQ_RECEPTION callback | Moyen | Traçabilité complète | 🟠 À faire |
| P10 — REQ_RETOUR marchandise | Élevé | Gestion stock retour | 🟠 À faire |
| P11 — REQ_ANNULATION | Faible | Correction erreurs | 🟠 À faire |

---

## 6. Points de Vigilance Réglementaires

### 6.1 Obligations de traçabilité
Le protocole CSRP est encadré par la réglementation pharmaceutique française. Les XML échangés constituent une preuve des commandes transmises. Le stockage actuel dans `FilePharmamlStorageLocation` doit être **pérenne et sauvegardé**.

### 6.2 Codes CIP et sécurité des médicaments
La codification CIP7/CIP13/EAN13 est sensible. Une erreur de codification peut entraîner la livraison d'un mauvais produit. La validation `typeCodification()` est correcte mais doit être couverte par des tests unitaires.

### 6.3 Produits de substitution (`TypeRemplacement.EP`)
Le type `EP` (Equivalent Proposé — non livré) signifie que le répartiteur **propose** un remplaçant sans le livrer. L'implémentation actuelle l'ignore (seuls `EL` et `RL` sont traités). Ce cas doit être géré : proposer à l'utilisateur d'accepter/refuser avant de passer commande.

### 6.4 Sécurité HTTP
Si le répartiteur requiert une authentification (certificat client, token Bearer), il faut adapter le `HttpClient`. Ne jamais stocker de credentials en clair dans `GroupeFournisseur` — utiliser `AppConfiguration` chiffré ou un coffre de secrets.

---

## 7. Fichiers Clés à Créer / Modifier

| Action | Fichier |
|---|---|
| CRÉER | `web/rest/commande/PharmaMlResource.java` |
| CRÉER | `db/migration/V1.3.3__pharmaml_envoi.sql` |
| CRÉER | `domain/PharmamlEnvoi.java` |
| CRÉER | `repository/PharmamlEnvoiRepository.java` |
| CRÉER | `webapp/.../pharmaml/pharmaml.service.ts` |
| CRÉER | `webapp/.../pharmaml/envoi/envoi-pharmaml.component.ts` |
| CRÉER | `webapp/.../pharmaml/ruptures/ruptures-pharmaml.component.ts` |
| MODIFIER | `PharmaMlServiceImpl.java` — thread-safety, JAXBContext statique, retry |
| MODIFIER | `commande-update.component.ts` — bouton envoi PharmaML |
| MODIFIER | `groupe-fournisseur-update.component` — champs config PharmaML |

---

## 8. Tests à Prévoir

| Type | Cible | Outil |
|---|---|---|
| Unitaire | `buildPayload()` — vérifier le XML produit | JUnit + JAXB assertions |
| Unitaire | `traiterCommandeRepondue()` — scénarios rupture totale / partielle / remplacement | Mockito |
| Unitaire | `typeCodification()` — CIP7, CIP13, EAN13 | JUnit |
| Intégration | Envoi vers un mock CSRP (WireMock) | WireMock + Spring Boot Test |
| E2E | Flux complet commande → envoi → réception | Playwright (frontend) |
| Charge | N envois simultanés (thread-safety) | JMeter / k6 |
