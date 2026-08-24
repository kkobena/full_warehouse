# Plan — Intégration de Spring AI dans PharmaSmart

> Statut : **analyse d'opportunité** — aucune ligne de code écrite à ce jour.
> Date : août 2026.
> Portée : backend `pharmaSmart-app` / `pharmaSmart-core`, front `webapp`.

---

## 0. Avertissement de vocabulaire

Le paquetage `com.kobe.warehouse.service.pharmaml` **n'a rien à voir avec le Machine Learning**.
PharmaML est le standard EDI d'échange officine ↔ grossiste (commande, disponibilité, substitution,
retour). Toute nouvelle brique d'IA doit donc être nommée sans ambiguïté — on retient le préfixe
`ai` (`service/ai/`, `web/rest/AiResource`) et **jamais** `ml`.

---

## 1. Faut-il intégrer Spring AI ? Réponse courte

**Oui, mais tardivement et sur un périmètre étroit.**

L'IA générative n'est pas le levier de valeur principal de PharmaSmart aujourd'hui. Les données de
l'officine sont **structurées, tabulaires et volumineuses** (ventes, lots, stocks, tiers-payant) :
un LLM est le mauvais outil pour prévoir une consommation ou détecter une rupture — une requête SQL
et une moyenne mobile pondérée font mieux, pour zéro euro de token et une latence de 5 ms.

En revanche, il existe **trois zones où le LLM apporte quelque chose qu'aucune requête ne sait
faire** : comprendre du langage naturel, rapprocher des libellés sales, et rédiger un commentaire.
C'est là — et seulement là — qu'il faut investir.

### 1.1 Les trois contraintes structurantes du projet

| Contrainte | Conséquence sur le choix d'IA |
|---|---|
| **Déploiement desktop Tauri, poste unique en officine** | Pas de GPU, RAM souvent 8 Go. Un modèle local type Ollama 7B est jouable mais lent ; un 3B quantisé est plus réaliste. |
| **Connectivité internet irrégulière (Côte d'Ivoire)** | Toute fonctionnalité IA doit être **dégradable** : si l'appel échoue, l'écran fonctionne sans. Jamais de chemin critique (vente, caisse, facturation) dépendant d'un LLM. |
| **Données de santé + données financières** | Envoyer des noms de patients ou des montants de CA à une API tierce US pose un problème RGPD/ARTCI et surtout un problème **commercial** de confiance. Anonymisation obligatoire ou modèle local. |

### 1.2 Compatibilité technique — levée

Le projet est sur **Spring Boot 4.1.0 / Java 25**. **Spring AI 2.0.0 supporte Spring Boot 4.1** :
l'auto-configuration se charge normalement, aucun câblage manuel des clients n'est nécessaire, et
l'alternative LangChain4j n'a plus lieu d'être envisagée.

Ce qui était le risque technique n° 1 de ce plan **est donc éliminé**. Il en résulte deux
conséquences :

1. L'étape 0 n'est plus un *go / no-go* mais une simple **vérification d'intégration** (voir §8).
2. Le risque résiduel se déplace vers l'**infrastructure de déploiement** — l'extension `pgvector`
   chez le client (§4.3) devient le principal point de fragilité opérationnelle.

Dépendances cibles :

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-bom</artifactId>
      <version>2.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

```xml
<!-- pharmaSmart-app -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-model-ollama</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
</dependency>
```

Le starter `pgvector` **crée et gère lui-même sa table** ; on désactive ce comportement
(`initialize-schema: false`) pour rester maître du schéma via Flyway, conformément à la règle du
projet qui veut que toute évolution de schéma passe par une migration versionnée.

> **Reste à vérifier en étape 0** (une demi-journée, pas un projet) : le comportement exact de
> l'auto-configuration Ollama sous Java 25, et l'absence de conflit entre le `VectorStore` de Spring
> AI et la gestion Flyway du schéma.

---

## 2. Ce que font les concurrents

L'IA en logiciel d'officine n'est plus prospective ; elle est déjà commercialisée, mais **rarement
sous forme de chatbot**.

### 2.1 Marché européen / français

| Éditeur | Capacité IA | Nature réelle |
|---|---|---|
| **Winpharma (Winsoft)** | Assistant de délivrance, alertes d'interaction | Moteur de règles + base médicamenteuse, IA marketing |
| **LGPI (Pharmagest / Equasens)** | Prédiction de commande, « Smart Rx », lecture d'ordonnance | ML tabulaire pour le réassort, OCR + NLP pour l'ordonnance |
| **Smart Rx** | Analyse d'ordonnance scannée, aide à la substitution générique | OCR + NLP spécialisé |
| **Pharmony / Alliadis** | Optimisation de rotation de stock, tableaux de bord prédictifs | Séries temporelles |
| **Nuage / Offisanté** | Reporting conversationnel | Émergent, LLM |

### 2.2 Marché nord-américain

| Éditeur | Capacité IA |
|---|---|
| **McKesson EnterpriseRx** | Prévision de demande, détection de fraude sur remboursements |
| **PioneerRx** | Scoring d'observance patient, ciblage d'entretiens pharmaceutiques |
| **BestRx / Liberty** | Assistant de saisie, complétion d'ordonnance |
| **Amazon Pharmacy** | Recommandation, vérification automatisée |

### 2.3 Enseignement à retenir

Deux constats importants pour le positionnement de PharmaSmart :

1. **La valeur reconnue par les pharmaciens est la prévision de commande et la lecture
   d'ordonnance**, pas le chatbot. Les éditeurs qui ont mis un chatbot en avant l'ont fait pour la
   communication ; ce qui se vend, c'est « je commande mieux » et « je saisis plus vite ».
2. **Aucun concurrent significatif n'est présent sur le marché ouest-africain avec ces
   fonctionnalités.** L'écart technologique est donc un argument commercial réel — mais il ne se
   monnaye que si la fonctionnalité marche hors connexion et sans coût récurrent visible pour
   l'officine.

---

## 3. Cas d'usage évalués, classés par ratio valeur / coût

Notation : valeur métier (1-5), effort (1-5), risque (faible/moyen/élevé).

### 3.1 Retenus — vague 1

#### UC-1 — Recherche produit en langage naturel au comptoir ⭐ *priorité 1*

**Valeur 5 · Effort 2 · Risque faible**

Aujourd'hui le comptoir cherche par libellé ou par code. Le client, lui, dit « le sirop pour la toux
sèche de l'enfant », « le générique du Doliprane 1000 », « la crème pour les mycoses ».

Implémentation : **RAG sur le référentiel produit local**, pas un LLM génératif.
On calcule un *embedding* par produit (libellé + DCI + forme + rayon + laboratoire), stocké dans
PostgreSQL via **pgvector**. La requête du comptoir est vectorisée et on fait une recherche par
similarité cosinus, fusionnée avec la recherche plein texte existante.

Pourquoi c'est le meilleur candidat :
- Le référentiel est **borné et stable** (quelques dizaines de milliers de produits) → l'indexation
  se fait une fois, en batch nocturne dans `pharmaSmart-batch`.
- Aucune donnée patient ne sort.
- **Le modèle d'embedding peut être local et petit** (`all-MiniLM`, 90 Mo) → fonctionne hors ligne,
  latence < 50 ms, coût nul.
- Dégradation gracieuse triviale : si l'index n'existe pas, on retombe sur la recherche actuelle.

Point d'accroche dans le code : les services de recherche produit consommés par l'écran de vente
(`service/sale/`, `ProduitService`), en ajoutant un `ai/ProduitSemanticSearchService` appelé en
complément — jamais en remplacement.

#### UC-2 — Rapprochement de libellés à l'import ⭐ *priorité 2*

**Valeur 4 · Effort 2 · Risque faible**

`ImportationProduitService`, `ImportationCustomer`, `ImportationTiersPayantService` et les imports
CSV/Excel butent tous sur le même mur : le fichier fournisseur écrit « PARACETAMOL 1G CPR BT8 »,
la base a « PARACETAMOL 1000MG COMPRIME BOITE DE 8 ». Le matching exact échoue, l'opérateur
rapproche à la main pendant des heures.

Un embedding de similarité résout ~80 % des cas et **propose** les 20 % restants avec un score de
confiance. On n'automatise jamais silencieusement : on affiche « correspondance probable à 87 % »
et l'opérateur valide.

Même infrastructure que UC-1 → **le coût marginal est quasi nul une fois UC-1 livré**. C'est
l'argument décisif pour livrer ces deux-là ensemble.

#### UC-3 — Commentaire automatique de rapport

**Valeur 3 · Effort 2 · Risque faible**

Les rapports (`service/report/`, `service/stat/`, tableaux de bord comptables) sortent des chiffres
bruts. Un LLM peut rédiger le paragraphe d'analyse : « le CA du mois recule de 4 % ; la baisse est
concentrée sur le rayon parapharmacie (-18 %) alors que le médicament progresse de 2 % ; trois
tiers-payants représentent 60 % de l'encours ».

Conditions de sécurité :
- On **n'envoie pas la base**, on envoie un **agrégat déjà calculé** (une dizaine de nombres),
  sérialisé en JSON. Aucun nom de patient, aucune ligne de vente.
- Le texte est **suggéré**, marqué comme généré, éditable avant impression.
- Génération **à la demande**, jamais automatique — pour maîtriser le coût.

C'est le seul cas où un modèle distant (API) se justifie : le volume est faible (quelques appels par
jour), la qualité rédactionnelle compte, et les données envoyées sont anonymes.

### 3.2 Retenus — vague 2, sous condition

#### UC-4 — Assistant analytique conversationnel (« text-to-report »)

**Valeur 4 · Effort 4 · Risque moyen**

« Quels produits ont le plus progressé ce trimestre ? », « Quel tiers-payant me doit le plus ? ».

**Ne jamais faire de text-to-SQL libre.** C'est la faute classique : on donne le schéma au LLM, il
génère du SQL, et un jour il génère un `DELETE` ou lit une table qu'il ne devrait pas.

Approche correcte : **function calling sur un catalogue fermé d'outils**. On expose au LLM 10 à 15
fonctions Java typées, correspondant à des méthodes de service **déjà existantes et déjà
sécurisées** :

```
chiffreAffaireParPeriode(debut, fin, storageId)
topProduits(debut, fin, limite, critere)
encoursTiersPayant(tiersPayantId)
produitsEnRupture(rayonId)
valorisationStock(storageId)
```

Le LLM ne fait que **choisir la fonction et remplir les arguments** ; l'exécution reste du Java sous
`@Transactional`, sous `@Secured`, avec le `SecurityContext` de l'utilisateur courant. Le périmètre
de données visible est donc, par construction, celui de ses droits.

Contrainte forte : cet écran doit être **derrière un `Feature` de licence** (voir §6) et
**désactivé par défaut**.

#### UC-5 — Aide à la saisie d'ordonnance (OCR + extraction)

**Valeur 5 · Effort 5 · Risque élevé**

Le plus fort potentiel commercial (c'est ce que vendent LGPI et Smart Rx), et le plus dangereux.

Il s'agit de photographier une ordonnance et d'en extraire les lignes. Techniquement c'est un modèle
multimodal ; le mobile Flutter (`mobile/`) fournirait la capture.

**Risques rédhibitoires si mal cadré :**
- Donnée de santé nominative → un envoi vers une API tierce est difficilement défendable.
- Une erreur d'extraction (dosage, posologie) engage la responsabilité du pharmacien.
- Écritures manuscrites de médecin : le taux d'erreur réel sera élevé.

**À ne lancer que si** : (a) le pharmacien valide ligne à ligne, avec l'image affichée en regard ;
(b) rien n'est jamais validé automatiquement ; (c) une clause contractuelle explicite décharge
l'éditeur ; (d) idéalement, modèle exécuté localement.

À traiter dans un plan dédié, **pas dans cette première vague**.

### 3.3 Écartés — et pourquoi

| Cas d'usage | Pourquoi on ne le fait pas avec un LLM |
|---|---|
| **Prévision de réassort** | `SuggestionReassortService` fait déjà le travail. Ce qui manque n'est pas de l'IA générative mais de la **statistique classique** : moyenne mobile, saisonnalité, délai fournisseur. Un LLM serait moins précis, plus lent et non déterministe. **À améliorer sans Spring AI.** |
| **Détection de fraude caisse** | Règles + écarts statistiques sur `cash_register`. Un LLM n'apporte rien et introduit du faux positif non explicable. |
| **Prédiction de péremption** | `product_to_destroy` + dates de lots : c'est du SQL. |
| **Support / chatbot d'aide** | Séduisant, mais suppose une documentation utilisateur riche qui n'existe pas encore. À reconsidérer après. |
| **Génération de code / migrations** | Hors produit ; relève de l'outillage de développement. |

---

## 4. Architecture cible

### 4.1 Placement dans les modules

```
pharmaSmart-core/
└── ai/
    ├── AiProperties.java          # activation, provider, modèle, timeouts
    ├── AiProvider.java            # NONE | LOCAL | REMOTE
    └── AiUnavailableException.java

pharmaSmart-app/
├── config/AiConfiguration.java    # @ConditionalOnProperty(pharma.ai.enabled)
├── service/ai/
│   ├── embedding/                 # UC-1, UC-2 : indexation + similarité
│   │   ├── ProduitEmbeddingIndexer.java
│   │   └── ProduitSemanticSearchService.java
│   ├── narrative/                 # UC-3 : commentaire de rapport
│   │   └── ReportNarrativeService.java
│   └── assistant/                 # UC-4 : function calling
│       ├── AnalyticsToolbox.java  # les @Tool exposées
│       └── AnalyticsAssistantService.java
└── web/rest/AiResource.java       # /api/ai/**

pharmaSmart-batch/
└── job/AiEmbeddingRefreshJob.java # réindexation nocturne du référentiel
```

**Règle d'or : aucun service métier existant ne dépend de `service/ai/`.** La dépendance va toujours
dans l'autre sens. On doit pouvoir retirer entièrement le paquetage `ai` sans casser la compilation
des ventes, de la caisse ou de la facturation.

### 4.2 Configuration

```yaml
pharma:
  ai:
    enabled: false            # OFF par défaut, y compris en dev
    provider: local           # local | remote | none
    timeout: 8s
    embedding:
      enabled: false
      model: all-minilm
      batch-size: 200
    chat:
      model: llama3.2:3b
      max-tokens: 800

spring:
  ai:
    ollama:
      base-url: http://localhost:11434
    vectorstore:
      pgvector:
        initialize-schema: false   # le schéma appartient à Flyway, pas à Spring AI
```

Le mode `local` pointe sur Ollama en `localhost:11434`. Le mode `remote` exige une clé, jamais
committée, lue depuis une variable d'environnement.

### 4.3 Stockage vectoriel

PostgreSQL est déjà là : **pgvector**, pas de nouvelle brique d'infrastructure.

```sql
-- V1.x.y__ai_embeddings.sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE produit_embedding (
    produit_id   INTEGER PRIMARY KEY REFERENCES produit (id) ON DELETE CASCADE,
    embedding    vector(384) NOT NULL,
    source_hash  VARCHAR(64) NOT NULL,   -- évite de recalculer si rien n'a changé
    updated_at   TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_produit_embedding_hnsw
    ON produit_embedding USING hnsw (embedding vector_cosine_ops);
```

Ne pas coder le schéma en dur : s'appuyer sur le `search_path` posé par Flyway (cf. CLAUDE.md).

**Point de vigilance déploiement** : `pgvector` n'est pas installé par défaut. Il faut l'ajouter à
`docker/init-pharma-smart.sql` et surtout **le documenter dans la procédure d'installation client**,
sinon la migration Flyway échouera au démarrage chez le pharmacien. Prévoir un
`CREATE EXTENSION IF NOT EXISTS` tolérant à l'échec, ou une migration conditionnelle.

### 4.4 Dégradation gracieuse — non négociable

Tout appel IA est encapsulé et **ne remonte jamais d'exception à l'appelant métier** :

```java
public Optional<String> narrate(ReportSummary summary) {
    if (!aiProperties.isEnabled()) return Optional.empty();
    try {
        return Optional.of(chatClient.prompt()...call().content());
    } catch (Exception e) {
        LOG.warn("Narration IA indisponible : {}", e.getMessage());
        return Optional.empty();   // le rapport sort sans commentaire
    }
}
```

Côté front, un panneau IA vide ou absent ne doit **jamais** bloquer l'écran.

---

## 5. Coût

### 5.1 Modèle local (recommandé pour UC-1 / UC-2)

| Poste | Coût |
|---|---|
| Modèle d'embedding (`all-MiniLM`, 384 dim) | ~90 Mo, CPU, gratuit |
| Indexation initiale 30 000 produits | ~10 min en batch nocturne, une fois |
| Requête | < 50 ms, 0 € |
| Empreinte RAM ajoutée | ~300 Mo |

Verdict : **compatible avec le poste type d'officine**. C'est ce qui rend UC-1 et UC-2 réalistes.

### 5.2 Modèle distant (UC-3 / UC-4)

Ordre de grandeur, tarifs 2026 pour un modèle « mini » :
- UC-3, ~20 commentaires de rapport / mois → **quelques centimes à quelques euros / officine / mois**
- UC-4, ~10 questions / jour → **1 à 3 € / officine / mois**

Négligeable en absolu, **mais** : c'est un coût récurrent en devise étrangère, à la charge de
l'éditeur, et il dépend d'une connexion. D'où le feature flag de licence.

### 5.3 Modèle local pour le chat (alternative à 5.2)

Un `llama3.2:3b` quantisé tourne sur CPU mais met 5 à 20 s pour répondre sur un poste d'officine
sans GPU. **Acceptable pour un rapport à la demande, inacceptable au comptoir.** C'est la raison de
plus pour laquelle UC-1 doit être fait en embedding et non en chat.

---

## 6. Licence et commercialisation

L'IA est un **module optionnel facturable**. Elle s'intègre dans le mécanisme existant de
`com.kobe.warehouse.license.Feature`.

Deux entrées à ajouter, **toutes deux `optional = true`** (donc accordées uniquement si
explicitement listées dans le fichier `.lic`, conformément à la règle d'octroi documentée dans
`Feature.java`) :

```java
/** Recherche sémantique produit et rapprochement de libellés à l'import. */
AI_RECHERCHE(true),
/** Assistant analytique conversationnel et commentaires de rapport générés. */
AI_ASSISTANT(true);
```

Les contrôleurs REST correspondants sont annotés `@RequiresFeature(Feature.AI_RECHERCHE)` /
`@RequiresFeature(Feature.AI_ASSISTANT)` — le mécanisme `LicenseEnforcementAspect` fait le reste.

Conséquence commerciale : **c'est un argument de montée en gamme**, pas une fonctionnalité de base.
Aucun client existant ne perd quoi que ce soit.

---

## 7. Sécurité, conformité, éthique

| Sujet | Règle |
|---|---|
| **Données patient** | Ne sortent **jamais** du poste. Aucun nom, aucun numéro d'assuré, aucune pathologie dans un prompt distant. |
| **Données financières** | Uniquement des agrégats anonymes (UC-3). Jamais de détail ligne à ligne. |
| **Prompt injection** | UC-4 : le LLM ne génère pas de SQL. Catalogue de fonctions fermé, arguments validés côté Java. |
| **Traçabilité** | Journaliser chaque appel : utilisateur, cas d'usage, horodatage, latence, tokens. Pas le contenu du prompt s'il peut contenir de la donnée sensible. |
| **Transparence** | Toute production IA est visuellement marquée (badge « généré automatiquement, à vérifier »). |
| **Responsabilité** | Aucune suggestion IA n'est appliquée sans validation humaine explicite. Vaut particulièrement pour UC-2 et UC-5. |
| **Réversibilité** | `pharma.ai.enabled: false` doit rendre le logiciel strictement identique à aujourd'hui. |

---

## 8. Feuille de route

### Étape 0 — Vérification d'intégration (0,5 à 1 jour)

Le support de Boot 4.1 par Spring AI 2.0.0 étant acquis, cette étape n'est plus bloquante ; elle
reste utile pour purger les surprises d'environnement :

- Ajout du BOM 2.0.0 + starters, démarrage de l'application, `ChatClient` qui répond.
- Vérifier qu'aucun conflit n'apparaît entre le `VectorStore` pgvector et Flyway
  (`initialize-schema: false`).
- Vérifier la disponibilité de `pgvector` sur la version PostgreSQL cible.
- **Livrable : branche de POC + note d'une page sur les points de configuration.**

### Étape 1 — Socle (1 semaine)

- `AiProperties`, `AiConfiguration`, `AiProvider`, désactivé par défaut.
- Migration Flyway `pgvector` + table `produit_embedding`.
- Enum `Feature` étendu.
- Aucune fonctionnalité visible.

### Étape 2 — UC-1 + UC-2 (2 à 3 semaines) — *le cœur de la valeur*

- Job d'indexation dans `pharmaSmart-batch` avec `source_hash` pour l'incrémental.
- `ProduitSemanticSearchService`, fusionné avec la recherche existante.
- Intégration à l'écran de vente : les résultats sémantiques complètent, ne remplacent pas.
- Assistance au rapprochement dans les écrans d'import, avec score de confiance affiché.
- **Mesure obligatoire** : taux de rapprochement automatique avant / après, sur un fichier réel.

### Étape 3 — UC-3 (1 semaine)

- `ReportNarrativeService`, agrégats anonymes uniquement.
- Bouton « Générer un commentaire » sur 2 ou 3 rapports pilotes.
- Texte éditable, marqué comme généré.

### Étape 4 — UC-4 (3 à 4 semaines, conditionnée au retour terrain)

- `AnalyticsToolbox` : 10 à 15 fonctions maximum, adossées aux services existants.
- Écran conversationnel (Design System maison, `app/features/`, signaux, `@if`/`@for`).
- Quota d'appels par utilisateur et par jour.

### Étape 5 — UC-5

Plan dédié. Ne pas démarrer avant un retour d'usage stabilisé sur les étapes 2 et 3.

---

## 9. Critères d'arrêt

Le projet IA est **arrêté ou gelé** si l'un de ces signaux apparaît :

- L'indexation dégrade le démarrage ou la consommation mémoire du poste client de façon perceptible.
- L'extension `pgvector` s'avère impossible à déployer de façon fiable sur le parc client installé.
- UC-1 n'améliore pas mesurablement la recherche au comptoir sur un pilote réel — auquel cas
  l'ensemble du pari est invalidé, puisque c'est le cas d'usage le mieux noté.
- Un incident de fuite de donnée, même mineur.
- Le coût récurrent par officine dépasse ce que la marge du module optionnel absorbe.

---

## 10. Synthèse en une page

**Ce qu'il faut retenir :**

1. **La bonne première brique n'est pas un chatbot, c'est un index vectoriel du référentiel
   produit.** Local, gratuit, hors ligne, latence négligeable, et il sert deux cas d'usage à la fois
   (recherche comptoir + import). C'est le seul endroit où le rapport valeur/effort est
   incontestable.
2. **Ne pas faire de LLM ce que fait déjà une requête SQL.** Réassort, péremption, fraude caisse :
   ce sont des chantiers statistiques, pas des chantiers IA. Les traiter comme tels.
3. **Jamais de text-to-SQL.** Function calling sur catalogue fermé, exécution en Java sous les
   `@Secured` existants.
4. **Le verrou technique est levé** : Spring AI 2.0.0 supporte Boot 4.1, l'auto-configuration est
   disponible. Le risque résiduel n'est plus le framework mais le **déploiement de `pgvector` chez
   le client**.
5. **Module optionnel de licence, désactivé par défaut, entièrement réversible.** Aucun client
   existant ne doit voir sa situation changer.
6. **Différenciation commerciale réelle** sur le marché ouest-africain, où aucun concurrent
   n'affiche ces capacités — à condition que ça marche sans connexion permanente.

---

## Annexe — Documents liés

- `docs/PLAN-GESTION-LICENCE.md` — mécanisme `Feature`, émission des licences
- `docs/ANALYSE-RAPPORTS-COMPARATIFS-EVOLUTIFS.md` — cible de UC-3
- `docs/PLAN-AMELIORATION-INVENTAIRE-OFFICINE.md` — voisinage de UC-2
- `docs/ARGUMENTAIRE-COMMERCIAL-PHARMASMART.md` — positionnement du module optionnel

