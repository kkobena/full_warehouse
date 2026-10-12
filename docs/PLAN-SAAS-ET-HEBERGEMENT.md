# Plan SaaS et hébergement — Pharma-Smart

> Document d'architecture et de dimensionnement pour une offre SaaS hébergée.
> Cible initiale : **300 officines** simultanées.

---

## 1. Modes de déploiement

Trois modes coexistent dans la roadmap. Chaque officine choisit le sien à la souscription.

| Mode | Description | Connexion requise | Sync cloud |
|------|-------------|-------------------|------------|
| **Cloud pur** | Frontend Angular servi depuis le CDN, backend sur le serveur hébergé | Permanente | N/A — source de vérité = cloud |
| **On-premise** | Installation locale (Tauri + Spring Boot + PostgreSQL) | Non | Optionnelle (si module activé) |
| **Hybride** | On-premise + agent de synchronisation vers le cloud | Intermittente | Oui — sync asynchrone |

---

## 2. Dimensionnement pour 300 officines

### 2.1 Hypothèses de charge

| Paramètre | Par officine | 300 officines |
|-----------|-------------|---------------|
| Produits catalogue | 18 000 | 5 400 000 (1 schéma chacun) |
| Ventes / jour | 1 000 | 300 000 |
| Lignes de vente / an | ~2 000 000 | ~600 000 000 |
| Taille BDD (5 ans) | ~8 Go | ~2,4 To |
| Connexions actives (heures de pointe) | 5–15 | ~3 000 |
| Transactions / s (pointe, ×10 moyenne) | ~12 | ~1 200 |

### 2.2 Serveur de base de données — PostgreSQL

**Configuration recommandée pour 300 officines (serveur dédié) :**

```
CPU   : 32 cœurs (AMD EPYC ou Intel Xeon Scalable)
RAM   : 128 Go (shared_buffers = 32 Go, work_mem = 64 Mo)
NVMe  : 4 × 1,92 To en RAID 10 → ~3,8 To utiles
Réseau: 10 Gbps
OS    : Ubuntu 24.04 LTS + PostgreSQL 18
```

**Paramètres PostgreSQL 18 clés (`postgresql.conf`) :**

```ini
# ── I/O asynchrone (nouveauté PG 18 majeure) ──────────────────────────────
# Sur Linux + NVMe : io_uring multiplie le débit en écriture par 2–4×
io_method               = io_uring  # valeur par défaut sur Linux PG18 ; 'sync' sur Windows
io_combine_limit        = 128kB     # taille max des I/O fusionnées (default PG18)

# ── Mémoire ───────────────────────────────────────────────────────────────
shared_buffers          = 32GB      # 25 % de la RAM
effective_cache_size    = 96GB      # 75 % de la RAM
work_mem                = 64MB      # par opération de tri/hash (×2 vs PG16 conseillé)
maintenance_work_mem    = 2GB

# ── Connexions — PgBouncer gère le pool, PG ne voit que ~300 connexions réelles
max_connections         = 400

# ── WAL et performance ────────────────────────────────────────────────────
wal_level               = replica   # pour la réplication streaming
synchronous_commit      = off       # +30 % perf, risque 1 commit en cas de crash
checkpoint_timeout      = 15min
max_wal_size            = 8GB

# ── PG 18 : vacuum en arrière-plan amélioré ───────────────────────────────
autovacuum_max_workers  = 6         # augmenter pour 300 schémas actifs
autovacuum_vacuum_cost_delay = 2ms  # moins agressif que le défaut (20ms)

# ── Partitionnement des tables de ventes (si > 200 M lignes) ─────────────
# → partition par schéma tenant, puis par année sur table sale_line
```

**Points saillants de PostgreSQL 18 par rapport aux versions précédentes :**

| Fonctionnalité | Impact pour Pharma-Smart |
|----------------|--------------------------|
| **E/S asynchrones (io_uring)** | +2 à 4× débit sur NVMe — lecture de rapports lourds nettement plus rapide |
| **Vacuum incrémental** | Moins de pics de latence pendant les heures de pointe (300 schémas × autovacuum) |
| **`MERGE` amélioré** | Simplifie les upserts dans les imports BDPM |
| **Statistiques étendues v2** | Plans de requêtes plus précis sur les colonnes corrélées (ex. `product_id` + `lot_number`) |
| **Authentification OAuth2 native** | Pourra remplacer le JWT applicatif à terme si le fournisseur d'identité est externe |
| **`pg_stat_io` affiné** | Meilleur diagnostic des goulots I/O par tenant |

> **Attention** : `io_method = io_uring` requiert le noyau Linux ≥ 5.1 et liburing installée.
> Sur Windows (développement local), laisser la valeur par défaut (`worker` ou `sync`).

**PgBouncer (obligatoire à cette échelle) :**

```ini
pool_mode               = transaction   # le plus efficace
max_client_conn         = 5000          # connexions des apps
default_pool_size       = 20            # par base de données
```

### 2.3 Serveur applicatif — JVM (Spring Boot)

Pour 300 officines actives en simultané, **3 instances Spring Boot** en actif-actif derrière un
load balancer (HAProxy ou Nginx upstream).

**Par instance JVM :**

```
CPU  : 8 vCPU dédiés (ou 16 partagés)
RAM  : 8 Go heap + 2 Go overhead = 10 Go alloués
JVM flags:
  -Xms6g -Xmx6g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:G1HeapRegionSize=16m
  -XX:+HeapDumpOnOutOfMemoryError
```

**Nombre d'instances selon la charge :**

| Officines actives | Instances Spring Boot | Heap total |
|-------------------|-----------------------|-----------|
| ≤ 100 | 2 (1 actif + 1 standby) | 12 Go |
| ≤ 300 | 3 actif-actif | 18 Go |
| ≤ 700 | 5 actif-actif | 30 Go |
| > 700 | Scale horizontal + Kubernetes | À la demande |

### 2.4 Architecture complète pour 300 officines

```
Internet / officines
        │
  ┌─────▼──────┐
  │  Cloudflare │  CDN + WAF + DDoS protection (free tier suffisant)
  └─────┬───────┘
        │
  ┌─────▼──────────────────────────────────┐
  │  Serveur applicatif (OVH Advance-6)    │
  │  Ubuntu 24.04 · 32 vCPU · 128 Go RAM  │
  │  ┌──────────┐ ┌──────────┐ ┌────────┐ │
  │  │ JVM 1    │ │ JVM 2    │ │ JVM 3  │ │  Spring Boot ×3 (Xmx6g)
  │  └──────────┘ └──────────┘ └────────┘ │
  │  ┌─────────────────────────────────┐   │
  │  │  HAProxy (load balancer L7)     │   │
  │  └─────────────────────────────────┘   │
  │  ┌─────────────────────────────────┐   │
  │  │  MinIO (rapports PDF, exports)  │   │  Stockage objet S3-compatible
  │  └─────────────────────────────────┘   │
  └──────────────────┬─────────────────────┘
                     │ réseau privé / VLan
  ┌──────────────────▼─────────────────────┐
  │  Serveur BDD (OVH Infra-3)             │
  │  Ubuntu 24.04 · 32 vCPU · 256 Go RAM  │
  │  4 × 1,92 To NVMe RAID 10             │
  │  ┌────────────────────────────────┐    │
  │  │  PostgreSQL 18 (io_uring)      │    │
  │  │  300+ schémas : pharma_t_001…  │    │
  │  └────────────────────────────────┘    │
  │  ┌────────────────────────────────┐    │
  │  │  PgBouncer (pooler)            │    │
  │  └────────────────────────────────┘    │
  │  ┌────────────────────────────────┐    │
  │  │  Réplique lecture (PostgreSQL  │    │  Rapports lourds, exports
  │  │  streaming replication)        │    │
  │  └────────────────────────────────┘    │
  └────────────────────────────────────────┘
```

---

## 3. Tarifs d'hébergement (OVH — estimations Oct. 2026)

| Serveur | Référence OVH | Config | Prix / mois |
|---------|---------------|--------|-------------|
| Applicatif | Advance-6 | 32 vCPU · 128 Go · 2×960 Go NVMe | ~350 € |
| Base de données | Infra-3 | 32 vCPU · 256 Go · 4×1,92 To NVMe | ~620 € |
| Réplique lecture | Advance-4 | 16 vCPU · 64 Go · 2×960 Go NVMe | ~180 € |
| Backup externe | Object Storage | 3 To | ~30 € |
| **Total infrastructure** | | | **~1 180 €/mois** |

**Modèle de rentabilité à 300 officines :**

| Abonnement / officine | Revenu mensuel | Marge infrastructure |
|-----------------------|----------------|----------------------|
| 15 000 FCFA (~23 €) | ~6 900 € | Déficitaire |
| 25 000 FCFA (~38 €) | ~11 400 € | **Marge ~90 % infra** |
| 40 000 FCFA (~61 €) | ~18 300 € | Confortable |

> Le seuil d'équilibre infrastructure seul est atteint à ~50 officines à 25 000 FCFA/mois.

---

## 4. Espaces de travail accessibles en ligne

**Oui**, en mode cloud pur, l'interface Angular est accessible depuis n'importe quel navigateur
sans installation :

- L'URL est servie par Cloudflare CDN → chargement < 1 s même en CI
- L'API Spring Boot répond en JSON/REST, accessible depuis n'importe quelle connexion internet
- Chaque officine a son propre schéma, ses propres utilisateurs et ses propres données
- Multiposte natif : caissier 1 et caissier 2 ouverts simultanément sur la même officine

**Ce qui ne fonctionne pas en mode cloud pur :**
- Impression thermique (le driver Tauri est local) → voir alternative WebUSB/IP-printing
- Lecture de code-barres USB (nécessite un accès navigateur WebHID, expérimental)
- Fonctionnement hors ligne

---

## 5. Synchronisation hybride on-premise ↔ cloud

### 5.1 Architecture de synchronisation

```
Officine (on-premise)                    Cloud
─────────────────────                    ─────────────────────────────

Tauri (Rust)                             API Sync (endpoint dédié)
  └─ Spring Boot (port 8080)               └─ Spring Boot (cloud)
       └─ PostgreSQL local                      └─ PostgreSQL (schéma officine)
            (pharma_smart)
                │
         SyncAgent (service Java)
         ─────────────────────
         1. Écoute la table sync_outbox
         2. Toutes les X minutes (configurable),
            si internet disponible :
            POST /api/sync/push  ──────────────────────►  SyncController
         3. Récupère les deltas cloud (catalogue
            BDPM, mises à jour tarifaires…)
            GET /api/sync/pull   ◄──────────────────────  SyncController
```

### 5.2 Pattern Outbox (transactional outbox)

Toute écriture critique génère une ligne dans `sync_outbox` dans la même transaction :

```sql
CREATE TABLE sync_outbox (
    id          BIGSERIAL PRIMARY KEY,
    event_type  VARCHAR(64) NOT NULL,   -- 'SALE_CREATED', 'STOCK_ADJUSTED'…
    entity_id   BIGINT,
    payload     JSONB NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now(),
    synced_at   TIMESTAMPTZ,            -- null = en attente
    attempts    INT DEFAULT 0
);
```

Le `SyncAgent` lit les lignes `synced_at IS NULL`, les envoie par batch au cloud, et marque
`synced_at = now()` une fois l'accusé reçu.

### 5.3 Types d'événements synchronisés

| Direction | Événements | Fréquence |
|-----------|------------|-----------|
| Local → Cloud | Ventes, mouvements de stock, encaissements | Toutes les 5–15 min |
| Local → Cloud | Clients créés/modifiés, ordonnances | Toutes les 30 min |
| Cloud → Local | Mises à jour catalogue BDPM | 1×/jour |
| Cloud → Local | Mises à jour tarifaires (prix remboursement) | 1×/semaine |
| Cloud → Local | Messages éditeur, alertes sécurité | À la connexion |

### 5.4 Gestion des conflits

Les conflits sont rares car les ventes sont append-only. Les cas à gérer :

| Conflit | Règle |
|---------|-------|
| Stock modifié offline ET online | Dernier écrit gagne, alerte dans journal |
| Client modifié des deux côtés | Timestamp + merge des champs non conflictuels |
| Vente créée offline avec ID local | Réattribution d'ID cloud à la sync, mapping conservé |
| Migration Flyway cloud plus récente | Blocage de la sync + notification à l'officine |

### 5.5 Détection de la connectivité (côté Tauri)

```rust
// src-tauri/src/sync.rs
use tauri::Manager;

#[tauri::command]
pub async fn check_connectivity() -> bool {
    reqwest::get("https://pharma-smart.ci/api/health")
        .await
        .map(|r| r.status().is_success())
        .unwrap_or(false)
}
```

Le backend Java expose `GET /api/sync/status` qui retourne la dernière sync réussie,
l'heure du prochain envoi planifié, et les éventuelles erreurs en attente.

---

## 6. Checklist technique avant le passage en SaaS

### Obligatoires (bloquants)

- [ ] **TenantResolver Hibernate** — résolution du schéma depuis le JWT (2–3 semaines)
- [ ] **Claim `tenantId` dans le JWT** — émis à la connexion, validé sur chaque requête (3–5 jours)
- [ ] **Job Flyway multi-tenant** — migration de tous les schémas au démarrage (1 semaine)
- [ ] **MinIO / S3 pour les PDFs** — remplacer le chemin local `reports/` (1–2 semaines)
- [ ] **PgBouncer** — pooler de connexions en frontal de PostgreSQL (2 jours)

### Importants (à faire avant ouverture commerciale)

- [ ] **Module facturation** — création de tenant, abonnement, suspension (4–8 semaines)
- [ ] **Table `sync_outbox` + SyncAgent** — pour le mode hybride (3–4 semaines)
- [ ] **Impression cloud** — remplacement du driver Tauri local pour les clients cloud pur (3–6 semaines)
- [ ] **Monitoring multi-tenant** — Grafana + alertes par tenant (1–2 semaines)
- [ ] **Backup automatisé par tenant** — pg_dump journalier + rétention 30 jours (1 semaine)

### Souhaitables

- [ ] Mode offline-first complet (synchronisation bidirectionnelle avec résolution de conflits)
- [ ] Application mobile de supervision (sales-android en lecture)
- [ ] Dashboard éditeur (santé de chaque tenant, consommation, alertes)

---

## 8. Configuration multi-tenant PostgreSQL (on-premise ou cloud)

Cette configuration s'applique **identiquement** en on-premise (un groupe d'officines,
un réseau de pharmacies) et en SaaS hébergé. La seule différence est qui gère le serveur.

### 8.1 PostgreSQL — création d'un tenant

```sql
-- Schéma d'administration (créé une seule fois)
CREATE SCHEMA IF NOT EXISTS saas_admin;

CREATE TABLE IF NOT EXISTS saas_admin.tenant (
    id          BIGSERIAL PRIMARY KEY,
    slug        VARCHAR(63)  UNIQUE NOT NULL,   -- identifiant lisible, ex. "pharmacie-cocody"
    schema_name VARCHAR(63)  UNIQUE NOT NULL,   -- ex. "pharma_t_0042"
    plan        VARCHAR(32)  NOT NULL DEFAULT 'STARTER',
    actif       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Créer un tenant
INSERT INTO saas_admin.tenant (slug, schema_name, plan)
VALUES ('pharmacie-cocody', 'pharma_t_0042', 'PRO');

-- Créer le schéma correspondant
CREATE SCHEMA pharma_t_0042;

-- Flyway s'occupe des tables — voir section 8.3
```

### 8.2 Spring Boot — résolution du tenant depuis le JWT

Deux fichiers à ajouter dans `pharmaSmart-app` (ou `pharmaSmart-core`).

**`TenantContext.java`** — ThreadLocal simple :

```java
// config/multitenancy/TenantContext.java
public final class TenantContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public static void set(String schema) { CURRENT.set(schema); }
    public static String get()            { return CURRENT.get(); }
    public static void clear()            { CURRENT.remove(); }
}
```

**`TenantFilter.java`** — extrait `tenantId` du JWT et positionne le contexte :

```java
// config/multitenancy/TenantFilter.java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            // Le claim "tenant" est ajouté à l'émission du JWT (voir JwtAuthenticationConverter)
            var auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                String tenantId = auth.getToken().getClaimAsString("tenant");
                String schema   = tenantRepository.findSchemaBySlug(tenantId)
                                                  .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
                TenantContext.set(schema);
            }
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();   // OBLIGATOIRE — évite les fuites de thread pool
        }
    }
}
```

**`MultiTenantConnectionProvider.java`** — commute le `search_path` à chaque connexion :

```java
// config/multitenancy/SchemaMultiTenantConnectionProvider.java
@Component
public class SchemaMultiTenantConnectionProvider
        implements MultiTenantConnectionProvider<String> {

    @Autowired
    private DataSource dataSource;          // DataSource Hikari habituel

    @Override
    public Connection getConnection(String schema) throws SQLException {
        Connection conn = dataSource.getConnection();
        // Positionner search_path = schema,public  (public pour les extensions)
        try (Statement st = conn.createStatement()) {
            st.execute("SET search_path = " + schema + ", public");
        }
        return conn;
    }

    @Override
    public void releaseConnection(String schema, Connection conn) throws SQLException {
        // Réinitialiser avant de rendre la connexion au pool
        try (Statement st = conn.createStatement()) {
            st.execute("SET search_path = public");
        }
        conn.close();
    }

    // ... autres méthodes de l'interface (supportsAggressiveRelease → false)
}
```

**`CurrentTenantIdentifierResolver.java`** — lit le ThreadLocal :

```java
// config/multitenancy/ThreadLocalTenantResolver.java
@Component
public class ThreadLocalTenantResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        String schema = TenantContext.get();
        return schema != null ? schema : "pharma_smart";  // fallback pour les jobs batch
    }

    @Override
    public boolean validateExistingCurrentSessions() { return false; }
}
```

**`HibernateProperties.java`** — activer le multi-tenancy dans la config :

```java
// config/DatabaseConfiguration.java  (ou CacheConfiguration)
@Bean
public HibernatePropertiesCustomizer hibernateMultiTenancy(
        SchemaMultiTenantConnectionProvider provider,
        ThreadLocalTenantResolver resolver) {
    return props -> {
        props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, provider);
        props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
        // SCHEMA = search_path par connexion (pas de colonne discriminant)
        props.put(AvailableSettings.JAKARTA_HBM2DDL_DB_MAJOR_VERSION, "schema");
    };
}
```

### 8.3 Flyway — migrer tous les tenants au démarrage

```java
// service/FlywayMigrationService.java
@Service
@Slf4j
public class FlywayMigrationService implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private DataSource dataSource;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        tenantRepository.findAllActifs().forEach(tenant -> {
            log.info("Migration Flyway → schéma {}", tenant.getSchemaName());
            Flyway.configure()
                  .dataSource(dataSource)
                  .schemas(tenant.getSchemaName())
                  .locations("classpath:db/migration")
                  .table("pharma_smart_history")   // nom de la table d'historique Flyway
                  .baselineOnMigrate(true)
                  .load()
                  .migrate();
        });
    }
}
```

### 8.4 Ajouter le claim `tenant` dans le JWT

Dans `SecurityConfiguration.java`, l'`AuthenticationManager` ou le service de token
doit inclure le slug du tenant parmi les claims :

```java
// Dans le service qui génère le JWT (ex. TokenProvider ou UserDetailsServiceImpl)
Map<String, Object> claims = new HashMap<>();
claims.put("tenant", user.getOfficine().getSlug());   // ex. "pharmacie-cocody"
claims.put("tenantSchema", user.getOfficine().getSchemaName());
// ... autres claims habituels (sub, roles…)
```

### 8.5 Cas on-premise — groupe de pharmacies

Pour un réseau de pharmacies (groupe pharmaceutique, franchiseur) installé sur un
seul serveur local, la même approche s'applique avec quelques adaptations :

```
Un serveur PostgreSQL sur le réseau LAN du groupe
├── schéma saas_admin        ← annuaire des officines du groupe
├── schéma pharma_t_pharmacie_a   ← Officine A (Abidjan)
├── schéma pharma_t_pharmacie_b   ← Officine B (Bouaké)
└── schéma pharma_t_pharmacie_c   ← Officine C (San-Pédro)
```

- Un seul backend Spring Boot tourne sur le serveur du groupe
- Chaque pharmacien se connecte via son navigateur (ou Tauri) avec ses identifiants
- Le JWT identifie son schéma → isolation totale des données
- Les rapports consolidés groupe interrogent les **N schémas** via des vues ou des
  fonctions PL/pgSQL qui font `SET search_path` dynamiquement

```sql
-- Exemple : CA consolidé du groupe sur les 30 derniers jours
CREATE OR REPLACE FUNCTION saas_admin.ca_groupe(depuis DATE)
RETURNS TABLE(officine TEXT, ca NUMERIC) AS $$
DECLARE
    t RECORD;
BEGIN
    FOR t IN SELECT schema_name, slug FROM saas_admin.tenant WHERE actif = TRUE LOOP
        RETURN QUERY EXECUTE format(
            'SELECT %L, SUM(amount_paid) FROM %I.sale
             WHERE sale_date >= $1 AND sale_status = ''CLOSED''',
            t.slug, t.schema_name
        ) USING depuis;
    END LOOP;
END;
$$ LANGUAGE plpgsql;
```

---

## 7. Références

- PostgreSQL multi-tenant : https://www.postgresql.org/docs/current/ddl-schemas.html
- Flyway tenant migration pattern : https://flywaydb.org/documentation/learnmore/multi-tenant
- Transactional outbox pattern : https://microservices.io/patterns/data/transactional-outbox.html
- OVH tarifs serveurs dédiés : https://www.ovhcloud.com/fr/bare-metal/infra/
