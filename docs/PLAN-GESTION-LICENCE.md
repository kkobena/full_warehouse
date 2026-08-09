# PLAN — Gestion de licence / abonnement (déploiement on-premise)

> Statut : proposition technique — à valider avant implémentation
> Périmètre : `pharmaSmart-domain`, `pharmaSmart-core`, `pharmaSmart-app` (backend + webapp Angular)
> Auteur : analyse du code existant au 08/08/2026

---

## 1. Besoin exprimé

| # | Exigence | Traduction technique |
|---|---|---|
| B1 | Application installée **on-premise** chez le client | Validation **hors-ligne**, licence signée cryptographiquement, aucun appel réseau obligatoire |
| B2 | **Toast à la connexion** si expiration dans ≤ 1 mois | Statut licence renvoyé au login + `NotificationService` (ngb-toast) |
| B3 | **Bannière permanente non masquable** si ≤ 2 semaines | Composant global dans `main.component.html`, sans bouton de fermeture |
| B4 | À l'expiration : **blocage de toute modification** de données | Interception AOP/`HandlerInterceptor` sur `POST/PUT/PATCH/DELETE` → HTTP 402 + message métier |

Contrainte implicite : la lecture, l'impression et l'export **doivent rester possibles** après expiration
(obligation légale de traçabilité pharmaceutique — on ne bloque jamais la consultation de l'historique).

---

## 2. État des lieux du code (points d'ancrage)

| Élément | Chemin | Usage pour la licence |
|---|---|---|
| Modules Maven | racine → `pharmaSmart-domain`, `pharmaSmart-core`, `pharmaSmart-app`, `pharmaSmart-batch` | Entité dans `domain`, service/validation dans `core`, REST + AOP dans `app` |
| Sécurité | `pharmaSmart-app/.../config/SecurityConfiguration.java` | Ajouter `/api/license/status` en `permitAll`, `/api/license` en `ADMIN` |
| AOP | `pharmaSmart-app/.../aop/logging/LoggingAspect.java` + `config/LoggingAspectConfiguration.java` (`@EnableAspectJAutoProxy` déjà actif) | Modèle pour `LicenseEnforcementAspect` — **AOP déjà opérationnel, rien à activer** |
| Erreurs REST | `pharmaSmart-core/.../service/errors/ExceptionTranslator.java` (RFC 7807, champs `message`, `errorKey`, `payload`) | Ajouter un `@ExceptionHandler(LicenseViolationException.class)` |
| Auth | `pharmaSmart-app/.../web/rest/AuthenticationResource.java` (`/api/auth/login`, `/api/auth/refresh`) | Enrichir la réponse de login avec le bloc `license` |
| Officine | `pharmaSmart-domain/.../domain/Magasin.java` — `name` et `fullName` sont `@NotNull` **et** `unique` ; `compteContribuable` est **nullable** | `name` = pivot de liaison de la licence (§3.4) |
| Flyway | `pharmaSmart-app/src/main/resources/db/migration/` — dernière version **V1.8.7** ; schéma **`pharma_smart`** (`PHARMA_DB_SCHEMA`), historique `pharma_smart_history` | Nouvelle migration **V1.8.8** |
| Toasts | `app/shared/ui/toast-host/toast-host.component.ts` + `shared/services/notification.service.ts` | B2 sans aucun nouveau composant |
| Layout | `app/layouts/main/main.component.html` | Point d'insertion de la bannière B3 |
| Interceptors | `app/core/interceptor/{auth-jwt,auth-expired,error-handler}.interceptor.ts` | Nouveau `license.interceptor.ts` pour le 402 |
| Existant licence | **aucun** (`license` / `abonnement` absents du code) | Développement intégral |

---

## 3. Architecture cible

```
┌──────────────── ÉDITEUR (hors site client) ───────────────┐
│  pharmaSmart-license-cli  (nouveau module Maven)          │
│   • clé privée Ed25519 (coffre-fort, jamais distribuée)   │
│   • génère un fichier license.lic (JWS compact)           │
└──────────────────────────┬────────────────────────────────┘
                           │ transmission manuelle (mail / clé USB)
┌──────────────────────────▼────────────────────────────────┐
│  INSTALLATION CLIENT (on-premise)                         │
│                                                           │
│  license.lic ──► LicenseLoader ──► LicenseVerifier        │
│  (fichier)        (fs + DB)         (clé publique         │
│                                      embarquée)           │
│                        │                                  │
│                        ▼                                  │
│                 LicenseStateHolder  (cache mémoire)       │
│                   ├─► LicenseEnforcementAspect  (B4)      │
│                   ├─► LicenseResource /api/license/*      │
│                   └─► ClockTamperGuard (anti-recul date)  │
│                        │                                  │
│                  table license_state (DB)       │
└───────────────────────────────────────────────────────────┘
```

### 3.1 Format de la licence

Fichier `license.lic` = **JWS compact** (`EdDSA` / Ed25519), lisible mais infalsifiable.

```json
{
  "licenseId": "b3c1f2e0-...",
  "licenseType": "SUBSCRIPTION",
  "customerRef": "CI-ABJ-0042",
  "magasinName": "PHARMACIE DE LA PAIX",
  "magasinFullName": "PHARMACIE DE LA PAIX SARL",
  "taxId": null,
  "edition": "STANDARD",
  "issuedAt": "2026-01-15T10:00:00Z",
  "validFrom": "2026-01-15",
  "expiresAt": "2027-01-15",
  "gracePeriodDays": 7,
  "maxUsers": 10,
  "features": ["CAISSE", "FACTURATION", "COMPTABILITE", "MOBILE","CALLEBASSE","EXCLUSION_RAYON_CA","EXCLUSION_PRODUIT_CA","EXCLUSION_TP_CA"],
  "hardwareFingerprint": "sha256:9f2c...",
  "bindingPolicy": "MAGASIN_AND_HARDWARE",
  "support": {
    "resellerName": "PharmaSmart Côte d'Ivoire",
    "phones": ["+225 07 00 00 00 00", "+225 01 02 03 04 05"],
    "emails": ["support@pharmasmart.ci"],
    "whatsapp": "+225 07 00 00 00 00",
    "website": "https://pharmasmart.ci/support"
  }
}
```

**Choix techniques justifiés**

- **Ed25519** plutôt que RSA : signature 64 octets, vérification très rapide, disponible nativement
  en Java 25 (`Signature.getInstance("Ed25519")`) — **aucune dépendance supplémentaire**.
- Clé **publique** embarquée dans `pharmaSmart-core/src/main/resources/license/pharmasmart-public.pem`.
  Clé **privée** hors dépôt (variable d'env / coffre de l'éditeur).
- `licenseType` : `DEMO` | `TRIAL` | `SUBSCRIPTION` | `PARTNER` — distingue démo et licence réelle (§3.5).
- `magasinName` = `Magasin.name` : **pivot anti-partage principal** (cf. §3.4). Champ retenu car
  il est le seul à être à la fois `@NotNull` **et** `unique` dans l'entité `Magasin`, et parce qu'il
  figure déjà sur tous les documents imprimés.
- `taxId` = `Magasin.compteContribuable` : **facultatif** (colonne nullable dans `Magasin`).
  Contrôlé uniquement s'il est renseigné **des deux côtés** — sert de renfort, jamais de pivot.
- `features` : modules souscrits, réellement appliqués côté back **et** côté menus (§3.6).
- `hardwareFingerprint` : SHA-256 de `n° série carte mère + adresse MAC principale + nom d'hôte`.
- `bindingPolicy` : `NONE` | `MAGASIN` | `MAGASIN_AND_HARDWARE` — porté par la licence elle-même,
  donc ajustable client par client sans recompilation ni modification du `application.yml`.
- `support` : **objet structuré** (plusieurs téléphones, e-mails, WhatsApp, site) — repris tel quel
  dans la bannière, les toasts, l'écran d'activation et le `payload` des erreurs 402, afin que
  l'utilisateur bloqué ait toujours un moyen de contact sous les yeux.

### 3.2 Statuts de licence

| Statut | Condition | Effet |
|---|---|---|
| `VALID` | `now < expiresAt - 30j` | Aucun |
| `EXPIRING_SOON` | `expiresAt - 30j ≤ now < expiresAt - 14j` | **B2** : toast à la connexion |
| `EXPIRING_CRITICAL` | `expiresAt - 14j ≤ now < expiresAt` | **B2** + **B3** : bannière permanente |
| `GRACE` | `expiresAt ≤ now < expiresAt + gracePeriodDays` | Bannière rouge, écriture **encore** autorisée |
| `EXPIRED` | `now ≥ expiresAt + grace` | **B4** : lecture seule |
| `MISSING` | Aucun fichier trouvé | `EXPIRED` + message « licence absente » |
| `INVALID` | Signature KO / JSON corrompu / `magasinName` ≠ officine / `taxId` divergent (si renseigné) / empreinte divergente > 14 j | `EXPIRED` + message « licence invalide » |
| `CLOCK_TAMPERED` | Horloge système reculée (cf. §3.3) | `EXPIRED` + message « horloge système incohérente » |

> Les seuils **30 j / 14 j / grâce** sont externalisés dans `application.yml` (`pharma-smart.license.*`)
> afin d'être ajustables sans recompilation.

### 3.3 Protection anti-fraude (spécifique on-premise)

Le client est **administrateur de sa machine** : il peut reculer l'horloge. Parade :

1. Table `license_state` : colonne `last_seen_instant` mise à jour à chaque démarrage et toutes
   les 15 min (`@Scheduled`).
2. Au démarrage : si `now < last_seen_instant - 24h` → statut `CLOCK_TAMPERED`.
3. Recoupement secondaire : `MAX(created_at)` sur la table des ventes — si une vente est postérieure
   à `now`, l'horloge a été reculée.
4. Le fichier `license.lic` est **également stocké en base** (colonne `license_token`) : suppression
   du fichier ⇒ rechargement depuis la DB ; restauration d'un ancien backup DB ⇒ la signature reste
   valide mais `expiresAt` inchangé, donc sans effet.

> On ne cherche pas l'inviolabilité absolue (impossible en on-premise) mais un **coût de
> contournement supérieur au prix de l'abonnement**, avec traçabilité (`license_audit`).

### 3.4 Partage du fichier de licence entre officines

**Le risque.** Le fichier `.lic` est un simple fichier : rien n'empêche techniquement la
Pharmacie A de l'envoyer à la Pharmacie B. La signature Ed25519 garantit qu'il **n'a pas été
modifié**, pas qu'il est **utilisé au bon endroit**. Sans liaison, une seule licence achetée peut
équiper 10 officines — c'est le scénario de perte de revenu le plus probable, bien avant le
piratage technique (recompilation, patch du JAR).

**Réponse : 4 couches complémentaires, du plus dissuasif au plus technique.**

#### Couche 1 — Liaison au nom de l'officine (blocante, pivot principal)

Le pivot est `Magasin.name` — **seul champ à la fois `@NotNull` et `unique`** de l'entité
(`compteContribuable` est nullable, il ne peut donc pas servir de pivot). La licence embarque
`magasinName` ; à chaque vérification :

```java
if (!normalize(payload.magasinName()).equals(normalize(magasin.getName()))) {
    → statut INVALID  ("Cette licence est délivrée à l'officine « X »")
}
// Renfort facultatif : contrôlé seulement si renseigné des DEUX côtés
if (isNotBlank(payload.taxId()) && isNotBlank(magasin.getCompteContribuable())
        && !normalize(payload.taxId()).equals(normalize(magasin.getCompteContribuable()))) {
    → statut INVALID  (audit TAX_ID_MISMATCH)
}
```

`normalize()` : `trim` + majuscules + suppression des accents (`Normalizer.Form.NFD`) + réduction
des espaces multiples. Indispensable pour éviter un blocage sur « Pharmacie de la Paix » vs
« PHARMACIE DE LA PAIX ».

**Pourquoi c'est efficace :** le nom de l'officine n'est pas une donnée interne, il est **imprimé
sur tous les documents** — tickets de caisse, factures, rapports PDF, bordereaux fournisseurs.
Une officine B utilisant la licence de A produirait des documents légaux au nom de A, devant ses
propres clients, ses employés et l'administration.

**Verrou complémentaire indispensable — le renommage.** Changer `Magasin.name` est à la portée
d'un administrateur : sans garde-fou, la couche 1 se contourne en une minute. Deux mesures :

1. Toute modification de `Magasin.name` alors qu'une licence est active déclenche une
   **revérification immédiate** ⇒ statut `INVALID` si le nom ne correspond plus, avec audit
   `MAGASIN_NAME_MISMATCH`.
2. L'écran de paramétrage de l'officine affiche un avertissement explicite : « La raison sociale
   est liée à votre licence. Toute modification nécessite l'émission d'une nouvelle licence. »

Le fraudeur doit donc **conserver durablement le nom de l'officine A** pour continuer à fonctionner :
c'est précisément ce que la couche 3 rend intenable.

#### Couche 2 — Empreinte matérielle (blocante après période de tolérance)

`hardwareFingerprint` calculée sur le poste serveur. Le client obtient son empreinte dans l'écran
`admin/license` (§5.5) et la transmet au revendeur **avant** l'émission de la licence.

Pour éviter de bloquer une officine sur un simple changement de matériel, comportement **gradué** :

| Situation | Effet |
|---|---|
| Empreinte identique | OK |
| Empreinte différente, < 14 j depuis la 1ʳᵉ divergence | Bannière d'avertissement + audit `FINGERPRINT_MISMATCH`, écriture **autorisée** |
| Empreinte différente, ≥ 14 j | Statut `INVALID` → lecture seule, réémission de licence nécessaire |

La date de première divergence est persistée dans `license_state.fingerprint_mismatch_since`.
Cela laisse deux semaines pour régulariser après un changement de serveur ou une panne, sans
ouvrir la porte au partage durable.

#### Couche 3 — Le nom de l'officine sur les documents (dissuasion, coût nul)

**Aucun développement n'est nécessaire** : le nom du magasin figure déjà sur les tickets, factures
et rapports. Cette couche consiste simplement à **s'appuyer sur l'existant** et à garantir la
cohérence entre ce qui est imprimé et ce qui est licencié.

Conséquence directe pour un fraudeur : puisque la couche 1 l'oblige à conserver le nom de
l'officine A, **tous ses documents portent le nom de l'officine A**. Il ne peut ni facturer
correctement ses clients, ni justifier sa comptabilité, ni utiliser la FNE sous sa propre identité.
La licence partagée devient inexploitable en conditions réelles.

Deux ajouts mineurs pour verrouiller la cohérence :

- afficher le `magasinName` **issu de la licence** (et non de la base) dans l'écran « À propos »
  et dans l'écran `admin/license`, afin de rendre toute divergence immédiatement visible ;
- mention discrète du `licenseId` en pied des documents PDF : permet d'identifier formellement la
  source d'une licence partagée lors d'un contrôle.

#### Couche 4 — Détection côté éditeur (non blocante, optionnelle)

Ping d'activation *best-effort* (`POST https://licences.pharmasmart.ci/activation`) envoyant
`licenseId`, `hardwareFingerprint`, `magasinName`, version. **Jamais bloquant** : si le réseau est
indisponible, l'activation locale réussit quand même (contrainte on-premise respectée).

Côté éditeur, un même `licenseId` remonté avec **N empreintes distinctes** déclenche une alerte
commerciale. On détecte le partage sans jamais rendre l'application dépendante d'Internet.

#### Synthèse

| Couche | Blocante | Contourne­ment possible ? | Coût du contournement |
|---|---|---|---|
| 1 — `magasinName` | ✅ | Renommer son officine avec le nom du licencié | Documents légaux, factures et FNE au nom d'une autre pharmacie |
| 1bis — `taxId` (si renseigné) | ✅ | Falsifier son n° contribuable | Fraude fiscale caractérisée |
| 2 — Empreinte | ✅ (après 14 j) | Usurper l'empreinte matérielle (MAC, hostname) | Compétence technique + reconfiguration à chaque mise à jour |
| 3 — Nom sur documents | ❌ | Aucun (c'est la conséquence de la couche 1) | Perte de crédibilité commerciale, impossibilité de facturer |
| 4 — Télémétrie | ❌ | Bloquer le domaine au pare-feu | Détection différée mais probable |

> **Recommandation** : livrer avec `bindingPolicy = MAGASIN_AND_HARDWARE` par défaut, couches 1 à 3
> actives, `taxId` renseigné dans la licence **uniquement** si le client l'a saisi dans son officine.
> La couche 4 est un chantier séparé (infrastructure éditeur) et ne conditionne pas la v1.

**Volet contractuel indispensable** : le contrat de licence doit mentionner explicitement le
caractère **nominatif et non cessible** de la licence, le `licenseId` permettant d'identifier
formellement la source d'un fichier partagé. La technique dissuade, le contrat sanctionne.

### 3.5 Version de démonstration vs licence réelle

Le champ **`licenseType`** (signé, donc infalsifiable) porte la distinction :

| Type | Usage | Durée typique | Liaison | Données |
|---|---|---|---|---|
| `DEMO` | Démonstration commerciale, salons, formation | 30 j | `NONE` (installable partout) | Jeu de démo, sans valeur légale |
| `TRIAL` | Essai chez un prospect réel | 30–60 j | `MAGASIN` | Données réelles, **conservées** à la conversion |
| `SUBSCRIPTION` | Abonnement payant | 12 mois | `MAGASIN_AND_HARDWARE` | Production |
| `PARTNER` | Revendeur / support interne | 12 mois | `NONE` | Illimité, non facturé |

#### `DEMO` vs `TRIAL` — le critère de décision

> **La question à se poser est unique : les données saisies ont-elles vocation à devenir des
> données de production ?**
> — **Non** ⇒ `DEMO`. — **Oui** ⇒ `TRIAL`.

Tout le reste en découle :

| Critère | `DEMO` | `TRIAL` |
|---|---|---|
| **Qui l'utilise** | Le commercial de l'éditeur | Le pharmacien lui-même, dans son officine |
| **Où** | Portable du commercial, salon, centre de formation | Serveur réel de l'officine |
| **Données** | Jeu fictif, jetable | Vraies ventes, vrais stocks, vrais clients |
| **Liaison** | `NONE` — doit tourner sur n'importe quelle machine | `MAGASIN` — nominative, comme un abonnement |
| **Filigrane PDF** | ✅ Oui | ❌ Non |
| **FNE** | ❌ Désactivée | ✅ Active (le pharmacien vend réellement) |
| **Quotas de volumétrie** | ✅ 500 ventes / 1 000 produits | ❌ Aucun |
| **Bannière** | Jaune permanente « sans valeur légale » | Bannière « Période d'essai — J‑12 » (informative) |
| **À l'échéance** | Sans conséquence, données jetées | **Lecture seule** — les données réelles sont conservées et récupérées à la souscription |
| **Conversion** | Purge des données proposée | Données conservées, aucune purge |

En résumé : **un `TRIAL` est un abonnement à durée courte**, techniquement identique à un
`SUBSCRIPTION` (rien n'est bridé, tout est légalement exploitable), tandis qu'un **`DEMO` est une
vitrine délibérément stérilisée**, incapable de produire quoi que ce soit d'exploitable.

**Point de vigilance sur `TRIAL`** : puisqu'il n'est bridé en rien, c'est une licence complète
offerte pendant 30 à 60 jours. Deux garde-fous côté éditeur :

1. **un seul essai par officine** — le `licenseId` et le `magasinName` sont journalisés à l'émission ;
   un second `TRIAL` pour le même nom d'officine doit être refusé par la procédure (§10) ;
2. **pas de prolongation automatique** — la réémission d'un `TRIAL` exige une validation commerciale
   explicite, sans quoi l'essai perpétuel remplace l'abonnement.

#### Date de fin du `TRIAL` : `expiresAt` absolu, jamais un compteur

**Oui, la date de fin est dans le fichier de licence** — et c'est le champ `expiresAt` **déjà commun
à tous les types**. Un `TRIAL` n'introduit aucun mécanisme d'échéance particulier : c'est simplement
un `expiresAt` proche. Le moteur de statuts (§3.2) s'applique à l'identique.

> **Règle : `expiresAt` est obligatoire pour *tous* les types, y compris `PARTNER`.
> Aucune licence perpétuelle.** Une licence sans date de fin est une licence qu'on ne peut plus
> révoquer ni faire évoluer ; en cas de fuite, elle est définitivement exploitable.

**Le piège à éviter — le décompte « à partir de l'activation »**

La tentation est de mettre `durationDays: 30` et de faire partir le décompte à la première
activation, pour qu'un prospect ne perde pas de jours si l'installation traîne. **À proscrire en
on-premise** : la date de première activation ne peut être stockée que dans la base locale
(`license_state.activated_at`), or le client en est administrateur. Il lui suffirait de supprimer
la ligne, de restaurer un dump ou de réinstaller la base pour **relancer indéfiniment un essai de
30 jours**. Le compteur devient trivialement contournable, alors qu'`expiresAt` est protégé par la
signature Ed25519.

**Solution retenue**

- `expiresAt` **absolu et signé**, calculé par le CLI au moment de l'émission :
  `--type TRIAL --duration 30` produit `expiresAt = aujourd'hui + 30 j` ;
- l'émission étant instantanée, on génère la licence **le jour de l'installation** : le prospect ne
  perd aucun jour, sans introduire de compteur ;
- `validFrom` (déjà présent) permet, si besoin, de préparer une licence à l'avance :
  `validFrom = 2026-09-01`, `expiresAt = 2026-10-01`. Avant `validFrom`, la licence est
  `INVALID` (« licence pas encore valide ») ;
- `gracePeriodDays = 0` pour `TRIAL` : une période de grâce sur un essai gratuit n'aurait pas de
  sens commercial, et rallongerait l'essai de fait ;
- en cas de retard d'installation imprévu, la réponse est **la réémission d'un fichier** (opération
  de quelques secondes), pas un assouplissement du mécanisme.

**Pourquoi `DEMO` doit être techniquement différent, et pas seulement étiqueté**

Une démo est *faite* pour être installée n'importe où (c'est pourquoi `bindingPolicy = NONE`).
Sans garde-fous, elle deviendrait le vecteur de fraude idéal : il suffirait d'utiliser la licence
de démo en production. Les restrictions ci-dessous rendent cela inexploitable.

#### Restrictions appliquées au type `DEMO`

1. **Bannière permanente jaune**, quel que soit le nombre de jours restants :
   « VERSION DE DÉMONSTRATION — sans valeur légale ». Non masquable, même mécanisme que B3.
2. **Filigrane sur tous les documents PDF** (`DÉMONSTRATION — SANS VALEUR LÉGALE`), en diagonale,
   via une règle CSS dans les templates Thymeleaf communs — Flying Saucer gère le
   `background-image` et le positionnement absolu. Un ticket de démo n'est donc jamais présentable
   comme un justificatif.
3. **FNE désactivée d'office** : aucune certification de facture n'est transmise à l'administration
   fiscale depuis une démo. C'est un point **non négociable** — émettre des factures normalisées
   depuis un jeu de données fictif exposerait le client et l'éditeur.
4. **Quotas de volumétrie** : au-delà de `maxSales` (défaut 500 ventes) ou `maxProduits`
   (défaut 1 000), passage en lecture seule avec message « Limite de la version de démonstration
   atteinte ». Empêche l'exploitation réelle sans empêcher une démo confortable.
5. **Aucune période de grâce** : `gracePeriodDays = 0` forcé pour `DEMO`.
6. **Mention « DÉMO » dans le titre de la fenêtre** (Tauri) et dans l'écran « À propos ».

#### Conversion démo → licence réelle

À l'activation d'une licence `SUBSCRIPTION` par-dessus une `DEMO`, l'écran `admin/license`
propose explicitement :

- **conserver** les données (cas `TRIAL` chez un prospect réel) ;
- **purger** les données de démonstration (ventes, mouvements de stock, clients fictifs) via une
  procédure dédiée, avec confirmation forte.

Le filigrane et la bannière disparaissent immédiatement, la FNE est réactivée.

> **Contrôle serveur, pas seulement affichage** : `isDemo()` est évalué dans `LicenseService`
> et conditionne le filigrane PDF, l'appel FNE et les quotas côté backend. Un client qui
> masquerait la bannière via les outils de développement n'obtiendrait rien de plus.

### 3.6 Exploitation des `features` (modules souscrits)

Le champ `features` n'est utile que s'il est **réellement appliqué**. Trois points d'application,
du plus visible au plus contraignant.

#### Enum de référence

`com.kobe.warehouse.license.Feature` (dans `pharmaSmart-core`) :

| Feature | Périmètre couvert |
|---|---|
| `CAISSE` | POS / ventes / cash register (toujours incluse) |
| `FACTURATION` | Tiers-payant, assurances, factures clients |
| `COMPTABILITE` | Module comptabilité, journaux, comptes fournisseurs |
| `INVENTAIRE_AVANCE` | Inventaire tournant, planification |
| `REPORTS_AVANCES` | Rapports comparatifs et évolutifs |
| `MOBILE` | APIs des applications mobiles (`mobile`, `mobile-inventory`, `pharma-mobile-report`) |
| `FNE` | Certification des factures normalisées |
| `MULTI_DEPOT` | Gestion de plusieurs `Storage` / magasins |

#### 1. Masquage des menus (le plus visible)

`NavItem` possède déjà un `code` unique et un filtrage par rôles (`NavItemRole`) appliqué dans
`NavItemServiceImpl`. On ajoute :

- une colonne `nav_item.required_feature VARCHAR(40) NULL` (migration `V1.8.8`) ;
- dans `NavItemServiceImpl`, un filtre supplémentaire **au même endroit que le filtrage par rôles** :
  un item dont `required_feature` n'est pas dans les features de la licence est simplement retiré
  de l'arbre renvoyé.

Aucune modification du frontend n'est nécessaire : les menus non souscrits **n'existent pas** dans
la réponse. C'est le point d'application le plus rentable (une colonne + un `filter`).

#### 2. Blocage serveur (le plus contraignant)

Annotation `@RequiresFeature(Feature.COMPTABILITE)` posable sur une classe de contrôleur entière
ou sur une méthode, traitée par le **même aspect** que l'enforcement de licence :

```java
@Before("@within(rf) || @annotation(rf)")
public void checkFeature(RequiresFeature rf) {
    if (!licenseService.hasFeature(rf.value())) {
        throw new LicenseViolationException(
            "Module non inclus dans votre abonnement", "license.feature.notIncluded");
    }
}
```

Réponse **HTTP 402** avec `errorKey = license.feature.notIncluded` et, dans le `payload`, la feature
manquante et le bloc `support` — le message invite à contacter le revendeur pour souscrire.

> Contrairement au blocage d'expiration (§4.3), celui-ci s'applique **aussi aux `GET`** : un module
> non souscrit est invisible, pas seulement en lecture seule.

#### 3. Garde de route et affichage côté Angular

- `licenseService.hasFeature(f)` alimenté par `GET /api/license/status` ;
- `licenseFeatureGuard` (`CanActivateFn`) lisant `route.data.feature` → redirige vers une page
  « Module non souscrit » présentant les contacts revendeur ;
- utile même avec le masquage des menus, car une route reste atteignable par URL directe.

#### Compatibilité ascendante — règle de sécurité

> Si `features` est **absent ou vide** dans la licence, le système considère **toutes les features
> comme accordées**. Cette règle évite qu'une licence émise avant l'introduction du champ, ou par
> une version antérieure du CLI, ne bloque un client en production. L'absence de restriction ne
> doit jamais être interprétée comme une restriction totale.

`edition` (`STANDARD` / `PREMIUM`) reste un simple **libellé commercial** affiché à l'utilisateur :
la vérité technique est portée par `features`, ce qui permet de vendre des modules à la carte sans
créer une nouvelle édition à chaque combinaison.

---

## 4. Implémentation backend

### 4.1 `pharmaSmart-domain` — persistance

**Nouvelle entité** `com.kobe.warehouse.domain.LicenseState` (table `license_state`, **singleton**, `id = 1`) :

| Colonne | Type | Rôle |
|---|---|---|
| `id` | `int` PK (toujours 1) | Contrainte `CHECK (id = 1)` |
| `license_token` | `text` | JWS complet (copie de secours du fichier) |
| `license_id` | `varchar(64)` | Identifiant de la licence courante |
| `expires_at` | `date` | Dénormalisé pour requêtes/reporting |
| `activated_at` | `timestamptz` | Date d'activation |
| `activated_by` | `varchar(50)` | Login de l'admin ayant activé |
| `last_seen_instant` | `timestamptz` | Sonde anti-recul d'horloge |
| `hardware_fingerprint` | `varchar(128)` | Empreinte constatée à l'activation |
| `fingerprint_mismatch_since` | `timestamptz` NULL | Début de divergence d'empreinte (couche 2, §3.4) |

**Table `license_audit`** (append-only) : `id`, `event_type` (`ACTIVATION`, `REJECTION`,
`EXPIRATION`, `CLOCK_TAMPER`, `BLOCKED_WRITE`, `MAGASIN_NAME_MISMATCH`, `TAX_ID_MISMATCH`,
`FINGERPRINT_MISMATCH`), `detail`, `user_login`, `created_at`.

**Migration** : `V1.8.8__license_management.sql` — schéma **`pharma_smart`** (et non `warehouse`,
qui n'est que le nom du package Java `com.kobe.warehouse`) :

- `flyway.schemas: ${PHARMA_DB_SCHEMA:pharma_smart}`, table d'historique `pharma_smart_history`,
  `hibernate.default_schema: ${PHARMA_DB_SCHEMA:pharma_smart}` (cf. `config/application.yml`) ;
- **ne pas coder le schéma en dur** dans le script : le nom est surchargeable par la variable
  d'environnement `PHARMA_DB_SCHEMA` et certaines installations historiques utilisent encore
  `warehouse`. S'appuyer sur le `search_path` positionné par Flyway, ou utiliser le placeholder
  `${flyway:defaultSchema}` si une qualification explicite est indispensable ;
- contenu : tables `license_state` et `license_audit`, index sur `license_audit(created_at)`,
  colonne `nav_item.required_feature VARCHAR(40) NULL` pour le masquage des menus par feature (§3.6).

### 4.2 `pharmaSmart-core` — vérification (aucune dépendance web)

Package `com.kobe.warehouse.license` :

| Classe | Rôle |
|---|---|
| `LicensePayload` (record) | Claims désérialisés |
| `LicenseStatus` (enum) | Voir §3.2 |
| `LicenseInfo` (record) | `status`, `licenseType`, `edition`, `expiresAt`, `daysRemaining`, `magasinName`, `features`, `readOnly`, `demo`, `message`, `support` |
| `SupportContacts` (record) | `resellerName`, `phones`, `emails`, `whatsapp`, `website` |
| `Feature` (enum) | Modules souscrits — cf. §3.6 |
| `LicenseType` (enum) | `DEMO`, `TRIAL`, `SUBSCRIPTION`, `PARTNER` — cf. §3.5 |
| `LicenseVerifier` | Vérifie la signature Ed25519, parse les claims, contrôle `validFrom`/`expiresAt` |
| `LicenseProperties` | `@ConfigurationProperties(prefix = "pharma-smart.license")` — chemin du fichier, seuils, `enabled` |
| `LicenseViolationException` | `RuntimeException` avec `errorKey = "license.expired"` |

`LicenseProperties` (extrait `application.yml`) :

```yaml
pharma-smart:
  license:
    enabled: true          # false en profil dev/test
    file-path: ${user.home}/.pharmasmart/license.lic
    warning-threshold-days: 30
    critical-threshold-days: 14
    fingerprint-mismatch-tolerance-days: 14   # cf. §3.4 couche 2
    clock-tolerance-hours: 24
```

> `enabled: false` par défaut dans `application-dev.yml` et dans les tests
> (`@SpringBootTest`) pour ne pas casser la CI existante.

### 4.3 `pharmaSmart-app` — service, enforcement, REST

#### `LicenseService` (`service/license/`)

- `LicenseInfo currentStatus()` — lit un cache mémoire (`AtomicReference`), recalculé
  à chaque changement de jour ou toutes les 15 min.
- `LicenseInfo activate(MultipartFile file, String login)` — vérifie, persiste fichier + DB, audite.
- `boolean isWriteAllowed()` — `!(status.readOnly)`.
- `@Scheduled(fixedDelay = 15 min)` : rafraîchit le statut + `last_seen_instant`.
- `@EventListener(ApplicationReadyEvent.class)` : chargement initial + log `WARN`/`ERROR` explicite
  dans les logs backend (cf. `LOGS-QUICK-REFERENCE.md`).

#### `LicenseEnforcementAspect` (`aop/license/`) — exigence B4

```java
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 10)   // avant les @Transactional
public class LicenseEnforcementAspect {

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    void restControllers() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)"
        + " || @annotation(org.springframework.web.bind.annotation.PutMapping)"
        + " || @annotation(org.springframework.web.bind.annotation.PatchMapping)"
        + " || @annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    void writeMappings() {}

    @Before("restControllers() && writeMappings()")
    public void checkLicense(JoinPoint jp) { /* throw LicenseViolationException si bloqué */ }
}
```

**Points d'attention**

- L'aspect ne s'active que si `pharma-smart.license.enabled=true`
  (`@ConditionalOnProperty` sur le `@Bean` dans une `LicenseAspectConfiguration`,
  sur le modèle de `LoggingAspectConfiguration`).
- **Liste blanche obligatoire** via une annotation `@LicenseExempt` posée sur les méthodes qui
  doivent rester accessibles même expirées :
  - `AuthenticationResource#login` / `#refresh` / `logout` (sinon plus personne ne peut se connecter) ;
  - `LicenseResource#activate` (sinon impossible de renouveler !) ;
  - endpoints d'export / génération de PDF exposés en `POST` (impression de duplicata, archivage légal) ;
  - endpoints de changement de mot de passe.
- Certains `@RequestMapping(method = POST)` non annotés `@PostMapping` peuvent exister :
  ajouter un **filet de sécurité** `LicenseEnforcementInterceptor` (`HandlerInterceptor`) basé
  sur `request.getMethod()` — l'AOP couvre le cas nominal, l'interceptor couvre les cas exotiques
  (contrôleurs hérités, endpoints hors `/api/` comme `/api-user-account`). Les deux partagent
  `LicenseService.isWriteAllowed()`.

#### Réponse d'erreur (B4)

Statut HTTP **402 Payment Required** (sémantiquement exact, ne déclenche pas la déconnexion
automatique contrairement à 401/403 traités par `auth-expired.interceptor.ts`).

```json
{
  "status": 402,
  "title": "Licence expirée",
  "message": "Action non permise pour cause de non validité de licence.",
  "errorKey": "license.expired",
  "payload": { "expiresAt": "2027-01-15", "supportContact": "+225 07 00 00 00 00" }
}
```

→ ajouter dans `ExceptionTranslator` :

```java
@ExceptionHandler(LicenseViolationException.class)
public ResponseEntity<Object> handleLicenseViolation(LicenseViolationException ex) { ... }
```

#### `LicenseResource` (`web/rest/license/`)

| Méthode | Route | Sécurité | Exempt |
|---|---|---|---|
| `GET` | `/api/license/status` | authentifié | n/a (lecture) |
| `POST` | `/api/license` (upload `.lic`) | `@Secured(ADMIN)` | ✅ `@LicenseExempt` |
| `GET` | `/api/license/fingerprint` | `@Secured(ADMIN)` | n/a — sert à commander la licence |
| `GET` | `/api/license/audit` | `@Secured(ADMIN)` | n/a |

`SecurityConfiguration` : ajouter `/api/license/**` dans les règles `authenticated()` (déjà couvert
par le pattern `/api/`), et **ne pas** le placer sous `/api/admin/` pour que `GET /status` reste
accessible aux caissiers.

#### Outil de génération (éditeur)

Nouveau module `pharmaSmart-license-cli` (ou goal dans `pharmaSmart-batch`, **non livré au client**) :

```bash
java -jar pharmasmart-license-cli.jar generate \
  --type SUBSCRIPTION --customer "PHARMACIE DE LA PAIX" --ref CI-ABJ-0042 \
  --expires 2027-01-15 --edition STANDARD --max-users 10 \
  --features CAISSE,FACTURATION,COMPTABILITE,MOBILE \
  --fingerprint sha256:9f2c... \
  --support-phone "+225 07 00 00 00 00" --support-email support@pharmasmart.ci \
  --private-key ./keys/pharmasmart-ed25519.pem \
  --out ./license.lic

# Licence de démonstration : ni officine ni empreinte requises
java -jar pharmasmart-license-cli.jar generate \
  --type DEMO --expires 2026-09-30 --binding NONE --out ./demo.lic

# Essai chez un prospect : durée relative convertie en expiresAt ABSOLU à l'émission (§3.5)
java -jar pharmasmart-license-cli.jar generate \
  --type TRIAL --customer "PHARMACIE DE LA PAIX" --duration 30 --binding MAGASIN \
  --out ./trial.lic
```

> ⚠️ `keys/` doit être ajouté au `.gitignore` et le module exclu du profil `prod` de packaging client.

---

## 5. Implémentation frontend (Angular 22)

### 5.1 Service et modèle

`app/core/license/license.model.ts`

```typescript
export type LicenseStatus =
  | 'VALID' | 'EXPIRING_SOON' | 'EXPIRING_CRITICAL'
  | 'GRACE' | 'EXPIRED' | 'MISSING' | 'INVALID' | 'CLOCK_TAMPERED';

export interface SupportContacts {
  resellerName?: string;
  phones: string[];
  emails: string[];
  whatsapp?: string;
  website?: string;
}

export type LicenseType = 'DEMO' | 'TRIAL' | 'SUBSCRIPTION' | 'PARTNER';

export interface LicenseInfo {
  status: LicenseStatus;
  licenseType: LicenseType;
  edition: string;          // libellé commercial (STANDARD / PREMIUM)
  expiresAt: string;        // ISO date
  daysRemaining: number;    // négatif si expirée
  magasinName: string;      // raison sociale portée par la licence
  features: string[];       // modules souscrits — cf. §3.6
  readOnly: boolean;
  demo: boolean;            // licenseType === 'DEMO'
  message: string;
  support: SupportContacts;
}
```

`app/core/license/license.service.ts` — service `providedIn: 'root'` exposant :

- `readonly license = signal<LicenseInfo | null>(null)` ;
- `readonly isReadOnly = computed(() => this.license()?.readOnly ?? false)` ;
- `readonly isDemo = computed(() => this.license()?.demo ?? false)` ;
- `hasFeature(f: string): boolean` — `true` si `features` est vide (cf. règle de compatibilité §3.6) ;
- `readonly showBanner = computed(() => …)` (statuts `EXPIRING_CRITICAL`, `GRACE`, `EXPIRED`, `INVALID`, `MISSING`, `CLOCK_TAMPERED`, **ou** `isDemo()`) ;
- `load()` : `GET /api/license/status`, appelé après `identity()` et rafraîchi toutes les heures.

> Conventions Angular 22 : composants standalone, `inject()`, signaux (`signal` / `computed`),
> `ChangeDetectionStrategy.OnPush`, syntaxe de contrôle de flux `@if` / `@for`, routes en
> `loadComponent` avec export par défaut. Le statut étant partagé par la bannière, la directive
> et l'écran d'activation, il est porté par un **signal unique** dans le service racine —
> pas de duplication d'état ni de `BehaviorSubject`.

### 5.2 B2 — Toast à la connexion

Dans `LoginService.login()`, chaîner après `accountService.identity(true)` :

```typescript
switchMap(account => this.licenseService.load().pipe(map(() => account)))
```

puis, si `daysRemaining ≤ 30`, appeler `NotificationService.warn(...)` (déjà rendu par
`ToastHostComponent`, présent une fois dans `main.component.html:51`) avec `life` allongé (≈ 15 s) :

> « Votre abonnement PharmaSmart expire dans **18 jours** (15/01/2027).
>   Contactez votre revendeur au +225 07 00 00 00 00 pour le renouveler. »

Sévérité : `warn` si `> 0` jour, `error` si expirée.

### 5.3 B3 — Bannière permanente non masquable

Nouveau composant standalone `app/layouts/license-banner/license-banner.component.ts` :

- **aucun bouton de fermeture**, aucun `localStorage` de masquage ;
- `position: fixed; bottom: 0; left: 0; right: 0; z-index: 1080;` (sous les toasts à 1090, au-dessus des modales ngb à 1055) ;
- classes Bootstrap : `alert alert-warning` (critique) / `alert-danger` (expirée), `role="alert"`, `aria-live="assertive"` ;
- ajouter `padding-bottom` sur `.main-content` quand la bannière est visible pour ne pas masquer le contenu ;
- insertion dans `main.component.html`, juste avant `<app-toast-host />` :

```html
@if (licenseService.showBanner()) {
  <app-license-banner />
}
<app-toast-host />
```

> Conforme aux règles projet : pas de `*ngIf` (syntaxe `@if`), pas de `p-dialog`, pas de `styleClass`.

### 5.4 B4 — Comportement lecture seule côté UI

Le blocage **fait autorité côté serveur** ; l'UI ne fait qu'améliorer l'expérience :

1. **`license.interceptor.ts`** (à enregistrer dans la chaîne d'intercepteurs, après `auth-jwt`) :
   intercepte les réponses `402` avec `errorKey = 'license.expired'` → `NotificationService.error()`
   avec le message serveur, et **ne déclenche pas** de déconnexion.
2. **Directive `appLicenseReadOnly`** (`shared/directives/`) : désactive un bouton/formulaire
   (`disabled` + `title` explicatif) lorsque `licenseService.isReadOnly()` est vrai — à poser
   progressivement sur les écrans d'écriture (ventes, commandes, inventaire).
3. **Garde de route** `licenseWriteGuard` (`CanActivateFn`) pour les routes de création/édition
   (`*-update`, POS `cash-register`) : redirige vers l'écran de licence (§5.5) avec les
   coordonnées du support. **Exception** : la route d'activation n'est jamais gardée.

### 5.5 Écran d'activation de la licence (interface dédiée)

L'activation se fait **exclusivement** depuis une interface applicative dédiée, accessible à tout
moment — et **non** depuis le wizard d'installation Tauri. C'est le même écran qui sert à la
première activation, au renouvellement annuel et au diagnostic.

**Route** : `admin/license` → `app/admin/license/license.routes.ts` (export par défaut, lazy
`loadComponent`), `data: { authorities: [Authority.ADMIN] }`, `canActivate: [UserRouteAccessService]`.

**Composant** `LicenseAdminComponent` (standalone, `ChangeDetectionStrategy.OnPush`) — 4 blocs :

| Bloc | Contenu |
|---|---|
| **Statut** | Badge coloré (`VALID` / `EXPIRING_*` / `EXPIRED` / `INVALID`…), **type de licence** (`DÉMONSTRATION` / `ESSAI` / `ABONNEMENT`), raison sociale, date d'expiration, jours restants, édition, nombre d'utilisateurs autorisés |
| **Modules souscrits** | Liste des `features` avec pictogramme actif/inactif — permet au client de voir ce qu'il peut souscrire en plus |
| **Identification du poste** | Empreinte matérielle (`GET /api/license/fingerprint`) avec bouton « Copier » — à transmettre au revendeur pour obtenir le fichier |
| **Activation** | Zone de dépôt du fichier `.lic` (drag & drop + `<input type="file" accept=".lic">`), bouton « Activer », retour d'erreur explicite (signature invalide, licence expirée, mauvaise officine, empreinte différente). En cas de conversion depuis une `DEMO`, choix **conserver / purger** les données (§3.5) |
| **Contacts** | Bloc revendeur issu du champ `support` : téléphones cliquables (`tel:`), e-mails (`mailto:`), WhatsApp, site web |
| **Historique** | Table des événements `license_audit` (`GET /api/license/audit`) : activations, rejets, tentatives d'écriture bloquées |

**Contraintes fonctionnelles**

- L'écran doit rester **atteignable licence expirée, invalide ou absente** :
  - `POST /api/license` porte `@LicenseExempt` (backend) ;
  - la route n'est pas soumise à `licenseWriteGuard` (frontend).
- Après activation réussie : `licenseService.load()` est rejoué, la bannière disparaît
  **sans rechargement de page ni redémarrage du backend** (le cache serveur est invalidé par
  `LicenseService.activate()`).
- Accès rapide : lien « Gérer ma licence » directement dans la bannière (B3) et dans le menu admin,
  pour que l'utilisateur bloqué trouve la sortie en un clic.
- Upload : `multipart/form-data`, taille limitée (< 16 Ko), extension et signature validées côté serveur.

**Cas « première installation »** : après le tout premier login admin, statut `MISSING` ⇒ bannière
rouge + redirection proposée vers `admin/license`. Le wizard Tauri (`setup-wizard`) reste concentré
sur la configuration du backend et **n'embarque aucune logique de licence**.

### 5.6 i18n

Ajouter les clés dans `src/main/webapp/i18n/fr/` (nouveau `license.json`) :
`license.expired`, `license.expiringSoon`, `license.banner.critical`, `license.readOnly.tooltip`,
`license.activate.success`, `license.activate.invalidSignature`…

---

## 6. Cas particuliers à traiter

| Cas | Décision |
|---|---|
| Application desktop **Tauri** | Le backend embarqué applique les mêmes règles. Le `setup-wizard` **ne gère pas la licence** (il reste dédié à la configuration du backend) : l'activation passe par l'écran `admin/license` (§5.5), y compris à la première installation |
| **Batch** `pharmaSmart-batch` (jobs planifiés) | Les jobs d'écriture (facturation automatique, clôtures) doivent aussi vérifier `isWriteAllowed()` — l'AOP REST ne les couvre pas |
| **API mobile** (`mobile/`, `mobile-inventory/`, `pharma-mobile-report/`) | Passent par `/api/**` → couvertes par l'aspect ; adapter les apps pour afficher le message du 402 |
| **Migrations Flyway** au démarrage | Jamais bloquées (exécutées hors contexte web) |
| Licence expirée + inventaire en cours | Autoriser explicitement la clôture d'inventaire ? → **non** : lecture seule stricte, l'inventaire reste en base et sera clôturé après renouvellement |
| Profils `dev` / tests | `pharma-smart.license.enabled=false` ⇒ aucun impact sur la CI existante |
| Décalage de fuseau | Toutes les comparaisons en `Instant` UTC (cohérent avec `hibernate.jdbc.time_zone=UTC`) |

---

## 7. Découpage en lots

| Lot | Contenu | Charge estimée |
|---|---|---|
| **L0 — Socle crypto** | Génération de la paire Ed25519, `LicenseVerifier`, `LicensePayload`, CLI de génération, tests unitaires (signature valide / altérée / expirée) | 2 j |
| **L1 — Persistance** | Entité `LicenseState`, `LicenseAudit`, repositories, migration `V1.8.8` | 1 j |
| **L2 — Service** | `LicenseService`, `LicenseProperties`, chargement au démarrage, `@Scheduled`, `ClockTamperGuard`, **liaison `taxId` + empreinte graduée (§3.4)** | 3 j |
| **L3 — REST + statut** | `LicenseResource`, DTO `LicenseInfo`, ajustements `SecurityConfiguration` | 1 j |
| **L4 — Enforcement (B4)** | `@LicenseExempt`, `LicenseEnforcementAspect`, `LicenseEnforcementInterceptor`, handler dans `ExceptionTranslator`, recensement complet des exemptions | 3 j |
| **L5 — Front B2/B3** | `LicenseService` Angular, toast au login, `LicenseBannerComponent`, i18n | 2 j |
| **L6 — Front B4** | `license.interceptor.ts`, directive read-only, `licenseWriteGuard` | 2 j |
| **L6bis — Écran d'activation** | Route `admin/license`, `LicenseAdminComponent` (statut, empreinte, dépôt du `.lic`, historique), lien depuis la bannière | 2 j |
| **L7 — Recette & doc** | Scénarios de recette (§8), `HOW-TO-ACTIVER-LICENCE.md` pour le client, procédure interne de génération | 2 j |
| **L8 — Cohérence documents** | `licenseId` en pied des PDF, affichage du `magasinName` **issu de la licence** dans « À propos » et `admin/license` — couche 3 §3.4 (le nom de l'officine est déjà imprimé, rien à développer côté templates) | 0,5 j |
| **L9 — Mode démonstration** | `licenseType`, filigrane PDF, désactivation FNE, quotas de volumétrie, bannière démo, purge des données à la conversion — §3.5 | 3 j |
| **L10 — Features** | Enum `Feature`, `@RequiresFeature` + aspect, colonne `nav_item.required_feature` et filtrage dans `NavItemServiceImpl`, `licenseFeatureGuard`, page « Module non souscrit » — §3.6 | 3 j |

**Total ≈ 24 jours‑homme.** L0→L4 livrables indépendamment (backend seul = protection effective),
mais **L6bis est indissociable de L4** : ne jamais livrer le blocage sans l'écran d'activation.
L9 et L10 sont des chantiers autonomes, activables dans un second temps sans remettre en cause
le socle (`licenseType` et `features` étant déjà présents dans le payload signé dès la v1).

---

## 8. Scénarios de recette

1. Licence valide (> 30 j) → aucun toast, aucune bannière, écriture OK.
2. Licence à J‑25 → toast au login uniquement, pas de bannière.
3. Licence à J‑10 → toast **et** bannière permanente, bannière non fermable (vérifier absence de bouton et de contournement DOM/CSS).
4. Licence expirée → `POST /api/sales` renvoie 402 + message ; `GET /api/sales` renvoie 200.
5. Licence expirée → login toujours possible, écran `admin/license` accessible, dépôt d'une nouvelle licence par un ADMIN → statut `VALID` immédiat, bannière disparue, écriture rétablie **sans redémarrage ni rechargement de page**.
6. Première installation sans licence → statut `MISSING`, bannière rouge avec lien « Gérer ma licence », activation possible dès le premier login admin.
7. Fichier `.lic` modifié à la main (1 octet) → statut `INVALID`, message d'erreur explicite dans l'écran d'activation, entrée dans `license_audit`.
8. Fichier `.lic` supprimé, licence présente en DB → statut inchangé (fallback DB).
9. Horloge système reculée de 6 mois → statut `CLOCK_TAMPERED`, lecture seule.
10. **Partage de licence** : licence de la Pharmacie A déposée sur l'installation de la Pharmacie B
    (nom d'officine différent) → statut `INVALID`, message « Cette licence est délivrée à l'officine
    "PHARMACIE A" », entrée `MAGASIN_NAME_MISMATCH` dans `license_audit`, aucune écriture possible.
10bis. **Renommage de l'officine** pour contourner la couche 1 → revérification immédiate, statut
    `INVALID` dès que `Magasin.name` ne correspond plus à la licence.
10ter. **Casse et accents** : licence « PHARMACIE DE LA PAIX » vs officine « Pharmacie de la Paix »
    → statut `VALID` (normalisation `normalize()`), aucun faux positif.
10quater. **`taxId` absent** de la licence ou de l'officine → contrôle ignoré, aucun blocage.
11. **Changement de serveur légitime** : même `taxId`, empreinte différente → avertissement pendant
    14 jours (écriture autorisée), puis `INVALID` au 15ᵉ jour ; réémission de la licence avec la
    nouvelle empreinte → retour à `VALID`.
12. Restauration d'un dump DB antérieur → la licence reste expirée.
13. `pharma-smart.license.enabled=false` → comportement historique intégral (non-régression CI).
14. **Licence `DEMO`** → bannière jaune permanente, filigrane sur tous les PDF, appel FNE refusé,
    passage en lecture seule au-delà de 500 ventes.
15. **Conversion `DEMO` → `SUBSCRIPTION`** → filigrane et bannière disparaissent immédiatement,
    FNE réactivée, choix conserver/purger proposé.
15bis. **Licence `TRIAL`** → aucun filigrane, FNE active, aucun quota, bannière informative
    « Période d'essai — J‑12 » ; à l'échéance, passage en lecture seule **sans perte de données**,
    puis conversion en `SUBSCRIPTION` → reprise immédiate, aucune purge proposée.
15ter. **Tentative de relance d'essai** : purge de `license_state` / restauration d'un dump / base
    réinstallée avec le même fichier `TRIAL` → la licence reste expirée (`expiresAt` absolu et signé,
    aucun compteur local à réinitialiser).
15quater. **Licence avec `validFrom` futur** → statut `INVALID` avec message « licence pas encore
    valide », puis `VALID` automatiquement à la date prévue.
16. **Feature non souscrite** (`COMPTABILITE` absente) → menu comptabilité **absent** de la réponse
    `NavItem`, accès direct par URL → page « Module non souscrit », appel API → 402
    `license.feature.notIncluded`.
17. **Licence sans champ `features`** (ancienne licence) → toutes les features accordées, aucun
    menu masqué (non-régression).
18. **Contacts support** → téléphones et e-mails affichés dans la bannière, l'écran d'activation et
    le `payload` des erreurs 402.

---

## 9. Risques & points de vigilance

| Risque | Impact | Mitigation |
|---|---|---|
| Exemption oubliée sur un endpoint critique (login, renouvellement) | Client **définitivement bloqué**, incident majeur | Test d'intégration dédié « parcours de renouvellement licence expirée » (login → `admin/license` → dépôt du `.lic` → écriture rétablie) + revue exhaustive des `@PostMapping` en L4 |
| Blocage d'une officine en pleine journée d'exploitation | Impact commercial et image | Période de grâce configurable (recommandé : **7 jours**) + alertes à J‑30 / J‑14 / J‑7 |
| Perte de la clé privée de l'éditeur | Impossible d'émettre de nouvelles licences | Sauvegarde chiffrée hors ligne, procédure de rotation prévoyant deux clés publiques acceptées |
| Contournement par recompilation (JAR non obfusqué) | Perte de revenu | Accepté : dissuasion contractuelle + audit `license_audit`. Obfuscation possible en option (ProGuard) |
| **Partage du fichier `.lic` entre officines** | **Perte de revenu la plus probable** | 4 couches §3.4 : liaison `magasinName` (bloquante) + verrou sur le renommage, `taxId` en renfort optionnel, empreinte matérielle graduée, nom de l'officine déjà présent sur les documents, détection éditeur |
| Faux positif sur le nom d'officine (casse, accents, espaces) | Blocage injustifié | Comparaison via `normalize()` (majuscules, sans accents, espaces réduits) + tests unitaires dédiés |
| Faux positif `hardwareFingerprint` (changement de carte réseau, migration de serveur) | Blocage injustifié | Tolérance graduée de 14 j (avertissement avant blocage) + réémission de licence en libre-service via le revendeur |
| Changement légitime de raison sociale (rachat d'officine, changement de forme juridique) | Blocage injustifié | Avertissement explicite dans l'écran officine + procédure de réémission documentée, délai < 24 h |
| Charge AOP sur chaque écriture | Négligeable | Statut en cache mémoire, aucune requête DB dans le chemin critique |

---

## 10. Livrables documentaires

- `docs/PLAN-GESTION-LICENCE.md` (ce document)
- `HOW-TO-ACTIVER-LICENCE.md` — guide client non technique (sur le modèle de `HOW-TO-CONFIGURE-BACKEND.md`)
- `docs/PROCEDURE-EMISSION-LICENCE.md` — procédure interne éditeur (usage du CLI, gestion des clés)

---

## 11. Décisions à valider avant démarrage

1. Durée de la **période de grâce** après expiration (proposition : **7 jours**).
2. **Politique de liaison** par défaut (proposition : `MAGASIN_AND_HARDWARE`, avec tolérance
   graduée de 14 j sur l'empreinte — cf. §3.4).
3. **Verrou sur le renommage de l'officine** : invalidation immédiate de la licence si
   `Magasin.name` change (proposition : **oui**, sinon la couche 1 se contourne trivialement).
4. **Télémétrie d'activation** (couche 4, §3.4) : chantier v1 ou v2 ? (proposition : **v2**,
   nécessite une infrastructure éditeur).
5. Le module `pharmaSmart-license-cli` vit-il dans ce dépôt (exclu du packaging) ou dans un dépôt privé séparé ? (proposition : **dépôt séparé**).
6. Périmètre exact des actions restant autorisées après expiration (proposition : lecture, impression/duplicata, exports, changement de mot de passe).
7. Notion de **features** par édition (STANDARD / PREMIUM) : appliquée dès la v1 (§3.6) ou en v2 ?
   (proposition : **masquage des menus en v1** — coût très faible, gain commercial immédiat — et
   `@RequiresFeature` sur les modules sensibles dans la foulée).
8. **Quotas de la version `DEMO`** : 500 ventes / 1 000 produits / 30 jours — à confirmer avec le
   commerce.
8bis. **Durée du `TRIAL`** (30 ou 60 j) et règle **« un seul essai par officine »** : à valider
   avec le commerce, avec tenue d'un registre des essais émis côté éditeur. La durée est convertie
   en `expiresAt` **absolu** à l'émission — aucun décompte à l'activation (§3.5).
9. **Purge automatique des données de démonstration** à la conversion : proposée par défaut ou
   toujours à l'initiative du client ? (proposition : **proposée, jamais automatique**).
10. **Clause contractuelle** de non-cessibilité de la licence : à faire valider par le juridique.


