# Plan — Passage en HTTPS et stratégie de certificats

> Rédigé le 2026-08-01. Fait suite à la phase 4 du
> [plan inventaire](PLAN-AMELIORATION-INVENTAIRE-OFFICINE.md) (chantier 4.1).
> Périmètre : backend Spring Boot, application web Angular, application de bureau Tauri,
> application mobile d'inventaire.

## 1. Qu'est-ce qui bloque aujourd'hui, concrètement ?

**Aucune fonctionnalité.** Vérifié : l'application web n'utilise aucune API à contexte
sécurisé (`getUserMedia`, Web Serial, Web Bluetooth, Service Worker, Clipboard,
Geolocation). Le scan caméra est natif Android, l'impression passe par Tauri. Rester en
HTTP n'empêche donc rien de fonctionner — la question est uniquement celle du risque.

### Ce que le HTTP coûte réellement sur un LAN d'officine

| Risque | Réalité en officine |
|---|---|
| **Wi-Fi partagé** | C'est le point faible principal. En WPA2-PSK (cas courant), **toute personne connaissant le mot de passe Wi-Fi peut déchiffrer le trafic des autres postes** après capture du handshake. Or le mot de passe circule entre employés, parfois fournisseurs. Et le comptage d'inventaire se fait **par définition en Wi-Fi**. |
| **Mot de passe en clair** | Il transite à chaque connexion sur `/api/auth/login`. Les mots de passe sont souvent réutilisés ailleurs. |
| **Jeton JWT en clair** | Il transite à **chaque requête**. Sa capture donne l'identité complète de l'utilisateur. |
| **Refresh token** | La phase 4 a introduit un refresh token, qui prolonge la valeur d'un jeton intercepté au-delà des 8 h de l'access token. **Durée ramenée à 48 h le 2026-08-01** précisément pour limiter cette exposition tant que le trafic circule en clair. |
| **MITM actif** (ARP spoofing) | Un seul poste infecté sur le LAN permet non seulement de lire mais de **modifier** les réponses : prix, quantités, écarts d'inventaire. |
| **Données de santé** | Données patients/clients d'officine, avec les obligations réglementaires afférentes. |

### L'argument inverse, qui est légitime

Un TLS mal déployé coûte plus cher que le risque qu'il couvre : certificat expiré qui
bloque le comptoir un lundi matin, erreurs « certificat non valide » que le personnel ne
sait pas interpréter, application mobile qui cesse de fonctionner après une rotation.
**Le critère de choix doit être la robustesse opérationnelle**, pas la pureté théorique.

### Conclusion

Rien n'oblige à basculer demain. L'élément déterminant reste que **le comptage mobile se
fait en Wi-Fi**, là où l'interception est la plus accessible. La réduction du refresh
token à 48 h a été appliquée comme mesure d'atténuation immédiate, sans infrastructure.
**Priorité : moyenne-haute, sans urgence bloquante.**

---

## 2. Ce que le passage en HTTPS implique, composant par composant

### Backend Spring — le mécanisme existe déjà

`config/application-tls.yml` est présent, avec `server.ssl.key-store:
classpath:config/tls/keystore.p12`. Activation : `--spring.profiles.active=prod,tls`.

À reprendre :
- **Régénérer le keystore** : l'actuel est le matériel de démo *, auto-signé pour
  `localhost`, mot de passe `password` en clair dans le YAML.0
- **Le SAN doit contenir l'adresse réellement utilisée** par les clients (IP ou nom
  d'hôte). Android **ignore le CN**, seul le SAN compte.
- Sortir le mot de passe du YAML (via `config.json` → argument poussé par
  `backend_manager.rs`, convention du projet).
- Corriger l'issuer JWT codé en dur (`http://localhost:8080` dans `JwtService`).

### Application web Angular — rien à faire

En production, Angular est **servi par Spring Boot** (`frontendDist` →
`target/classes/static`) et `apiServerUrl` vaut `''` : les appels sont en URLs relatives.
Le front hérite donc automatiquement du schéma qui l'a servi. Basculer Spring en HTTPS
bascule l'application web, **sans une ligne de code**.

**En développement, c'est déjà prêt.** Le proxy n'est pas un `proxy.conf.json` mais
`webpack/proxy.conf.js`, branché par `webpack.custom.js` :

```js
function setupProxy({ tls }) {
  const serverResources = ['/api', '/services', '/management', '/v3/api-docs', '/h2-console', '/health'];
  const target = `http${tls ? 's' : ''}://localhost:9080`;
  return serverResources.map(path => ({ context: path, target, secure: false, changeOrigin: true }));
}
```

Deux points en découlent :

- Le drapeau `tls` vient de `config.devServer?.server?.type === 'https'` : lancer
  **`ng serve --ssl`** fait automatiquement pointer le proxy vers `https://localhost:9080`.
- `secure: false` est déjà positionné : le certificat auto-signé du poste de
  développement est accepté sans configuration supplémentaire.

**Aucune modification n'est donc nécessaire côté développement.** Seule contrainte à
connaître : le même drapeau pilote le TLS du serveur de développement (navigateur ↔
`ng serve`) *et* celui de la cible du proxy (`ng serve` ↔ Spring). Si Spring tourne en
TLS, il faut lancer `ng serve --ssl`, sinon le proxy visera `http://localhost:9080` et
les appels échoueront.

### Application Tauri

- `environment.tauri.ts` : `apiServerUrl: 'http://localhost:9080'` en dur → à passer en
  `https`.
- `tauri.conf.json` : la CSP `connect-src` liste `http://localhost:*` et `http://*:*` →
  ajouter les équivalents `https`.
- WebView2 utilise le magasin de certificats Windows : une autorité installée sur le
  poste est donc reconnue automatiquement.

### Application mobile — le point le plus délicat

- `BASE_URL` en `http://` et `usesCleartextTraffic="true"` dans le manifeste.
- **Android refuse un certificat auto-signé** par défaut.
- **Depuis Android 7 (API 24), une autorité installée manuellement par l'utilisateur est
  ignorée par les applications.** L'app doit la déclarer explicitement dans un
  `network_security_config.xml`, soit en embarquant le certificat
  (`<certificates src="@raw/ca_officine"/>`), soit en acceptant les autorités
  utilisateur (`<certificates src="user"/>`).

> **Tentative écartée le 2026-08-01** — restreindre le trafic en clair au LAN via
> `network_security_config.xml` en attendant le TLS. Impossible : l'URL du serveur est
> configurable à l'exécution (`ServerConfigDialog` / `TokenManager.getBaseUrl()`) et
> Android **n'accepte pas de notation CIDR** dans `<domain>` — il faut lister les
> adresses exactes. Figer une IP casserait l'application dans toutes les officines sauf
> une ; tout autoriser ne changerait rien tout en donnant l'illusion d'un durcissement.

---

## 3. Stratégies de certificat

### Option A — Autorité interne par installation + confiance à la première connexion

L'installeur génère, **sur la machine de l'officine**, une autorité et un certificat
serveur ; la clé privée ne quitte jamais le poste. À la première connexion, l'app mobile
affiche l'**empreinte** du certificat et demande confirmation au pharmacien, puis la
mémorise et la vérifie ensuite (modèle SSH, *trust on first use*).

- ✅ Chiffrement réel, aucun secret partagé entre officines, aucune installation
  manuelle de certificat sur les terminaux, aucun domaine à acheter.
- ❌ Un écran de confirmation à développer dans l'app ; re-confirmation à chaque
  rotation du certificat serveur ; la confiance initiale repose sur le fait que la
  première connexion se fasse bien sur le réseau de l'officine.

> ⚠️ Ne **jamais** embarquer une autorité commune dans l'installeur : un installeur est
> une archive, la clé privée en serait extraite, et elle permettrait de forger un
> certificat valide pour l'ensemble du parc.

### Option B — Certificat public par officine

Un domaine (`pharmasmart.ci`), un sous-domaine par client, un certificat Let's Encrypt
obtenu par **challenge DNS-01**.

**Une IP publique n'est pas nécessaire** : un enregistrement DNS public peut pointer vers
une adresse privée (`192.168.1.188`), et le challenge DNS-01 prouve la maîtrise du
*domaine*, pas la joignabilité du serveur — l'autorité ne contacte jamais celui-ci.

- ✅ Aucune configuration chez le client, fonctionne sur tous les appareils, renouvellement
  standard, aucun secret partagé entre officines.
- ❌ Vos clients deviennent **publiquement énumérables** (DNS, et de toute façon via les
  registres Certificate Transparency) ; administration DNS par client ; risque de blocage
  par les protections **anti-rebinding DNS** de certains routeurs/résolveurs, qui
  refusent une réponse publique pointant vers une plage RFC 1918.

### Option C — Certificat joker `*.local.pharmasmart.ci` + résolution locale

Un seul certificat joker obtenu par DNS-01, noms résolus uniquement en local.

- ✅ Clients non énumérables, aucune administration DNS par installation, un seul
  renouvellement centralisé, pas de rebinding.
- ❌ **La clé privée est déployée dans toutes les officines** : la compromission d'un
  serveur permet d'usurper n'importe quel `*.local.pharmasmart.ci` sur les autres réseaux.

### Repère

Une dizaine d'officines → **option B** (aucun secret partagé). Au-delà, l'administration
DNS par client devient lourde et l'option C s'impose en pratique. L'option A reste la
seule qui ne demande ni domaine ni administration externe.

Les options ci-dessus sont détaillées, avec toutes les autres voies envisageables, dans
la section 3 bis.

---

## 3 bis. Toutes les options d'intégration HTTPS — comparatif détaillé

### Vue d'ensemble

| # | Option | Prérequis externe | Config sur les terminaux Android | Renouvellement | Complexité mise en place | Complexité exploitation |
|---|---|---|---|---|---|---|
| 0 | Rester en HTTP + atténuations | aucun | aucune | — | ○ nulle | ○ nulle |
| 1 | Auto-signé simple | aucun | ❌ ne fonctionne pas | manuel | ○ faible | ✗ inutilisable |
| 2 | Auto-signé + épinglage dans l'app | aucun | embarqué dans l'APK | **rebuild + redéploiement de l'app** | ●○ faible | ●●● élevée |
| 3 | CA interne unique pour tout le parc | aucun | CA embarquée dans l'APK | serveur seul | ●● moyenne | ●● moyenne |
| 4 | CA interne par installation + TOFU | aucun | 1 confirmation d'empreinte | serveur seul (+ reconfirmation) | ●●● élevée (dev app) | ● faible |
| 5 | Certificat public par officine (DNS-01) | domaine + zone DNS | **aucune** | automatisable | ●● moyenne | ●● moyenne (par client) |
| 6 | Certificat joker public + résolution locale | domaine + zone DNS | **aucune** | centralisé, 1 fois | ●● moyenne | ● faible |
| 7 | Reverse proxy TLS (Caddy/nginx) | selon le certificat choisi | selon le certificat choisi | automatisable | ●● moyenne | ● faible |
| 8 | VPN / tunnel (WireGuard, Tailscale) | service tiers ou serveur VPN | client VPN sur chaque terminal | transparent | ●●● élevée | ●●● élevée |
| 9 | mTLS (certificat client) | infrastructure de l'option 3 ou 4 | certificat client par terminal | par terminal | ●●●● très élevée | ●●●● très élevée |

---

### Option 0 — Rester en HTTP, avec atténuations

**Principe.** Ne pas chiffrer, mais réduire la surface et la valeur de ce qui circule.

**Prérequis.** Aucun.

**Intégration.**
- Refresh token court — *déjà fait, 48 h* (`AuthenticationResource.REFRESH_TOKEN_HOURS`).
- Passer le Wi-Fi de l'officine en **WPA3** (supprime le déchiffrement entre clients
  connu de WPA2-PSK), ou à défaut activer l'**isolation des clients** sur la borne.
- SSID dédié aux terminaux d'inventaire, séparé du Wi-Fi visiteurs.
- Réseau filaire pour les postes fixes.

**Limites.** Le mot de passe et le JWT restent en clair ; un poste compromis sur le LAN
peut toujours faire du MITM actif. Ne répond à aucune exigence de conformité.

**Complexité.** Nulle côté logiciel — ce sont des réglages réseau, pas du code.

**Quand la choisir.** Comme palier assumé et documenté, le temps de décider. C'est la
situation actuelle ; le mérite des atténuations est de la rendre volontaire plutôt que
subie.

---

### Option 1 — Certificat auto-signé simple

**Principe.** Le serveur présente un certificat qu'il a signé lui-même, sans autorité.

**Prérequis.** Aucun.

**Intégration.** `keytool -genkeypair` avec le bon SAN, puis profil `tls` de Spring.

**Limites — rédhibitoires.** Android refuse la connexion. Le navigateur affiche un
avertissement que le personnel apprendra à contourner par réflexe, ce qui **détruit la
valeur du dispositif** : un vrai MITM produirait le même écran, ignoré de la même façon.

**Verdict.** ✗ À écarter. Mentionnée ici parce que c'est la première idée qui vient, et
la plus coûteuse en faux sentiment de sécurité.

---

### Option 2 — Auto-signé + épinglage dans l'application mobile

**Principe.** L'app n'accepte qu'un certificat précis, identifié par l'empreinte de sa
clé publique, quelle que soit la chaîne d'autorité.

**Prérequis.** Aucun externe.

**Intégration.**
- `network_security_config.xml` avec `<pin-set>` contenant l'empreinte SHA-256.
- Ou côté OkHttp : `CertificatePinner` dans `ApiClient.createOkHttpClient()`.
- Prévoir **au moins deux empreintes** (courante + prochaine), sinon la rotation coupe
  l'accès sans recours.

**Limites.** L'empreinte est **liée à un certificat**, donc à une officine : soit une
build par client, soit toutes les empreintes du parc dans chaque APK. Toute rotation
impose de reconstruire et redéployer l'application sur tous les terminaux. Le navigateur
n'est pas couvert (l'épinglage HTTP est mort avec HPKP).

**Complexité.** Faible à écrire, **élevée à exploiter** — c'est un piège à retardement :
le jour où le certificat expire, l'app cesse de fonctionner et seule une mise à jour la
répare.

**Quand la choisir.** Parc très restreint, certificat de longue durée, et process de mise
à jour d'app maîtrisé.

---

### Option 3 — Autorité interne unique pour tout le parc

**Principe.** Une seule autorité, la vôtre. Vous signez un certificat par officine ; les
clients font confiance à l'autorité.

**Prérequis.** Un endroit sûr pour conserver la clé privée de l'autorité (elle ne doit
**jamais** entrer dans l'installeur, cf. §3 option A).

**Intégration.**
- CA générée une fois, hors ligne, durée ~10 ans.
- Certificat serveur généré **chez vous** par officine, livré avec l'installation.
- Mobile : CA embarquée dans `res/raw/` + `<trust-anchors>` dans
  `network_security_config.xml` — une seule APK pour tout le parc.
- Windows : CA dans le magasin « Autorités racines de confiance » (script d'installation).

**Limites.** Chaque nouvelle officine exige une émission de certificat de votre part. La
compromission de la CA compromet tout le parc. Vous devenez opérateur d'une PKI, avec ce
que ça implique de rigueur.

**Renouvellement.** Certificat serveur seul, sans action sur les clients — c'est
l'avantage décisif sur l'épinglage.

**Quand la choisir.** Parc homogène que vous installez vous-même, et volonté d'éviter
tout domaine public.

---

### Option 4 — Autorité interne par installation + confiance à la première connexion

*(détaillée en §3, option A)*

**Complément d'intégration.**
- L'installeur (PowerShell, déjà présent) génère CA + certificat serveur localement.
- Mobile : écran affichant l'empreinte à la première connexion, mémorisation dans
  `TokenManager`, vérification via un `X509TrustManager` personnalisé.
- Prévoir un message explicite si l'empreinte change (certificat renouvelé **ou** MITM —
  l'utilisateur doit pouvoir distinguer les deux avec l'aide du support).

**Complexité.** C'est la seule option qui demande un **développement applicatif réel**
côté mobile. En contrepartie, l'exploitation est la plus légère : rien à distribuer,
rien à administrer.

---

### Option 5 — Certificat public par officine (DNS-01)

*(détaillée en §3, option B)*

**Complément d'intégration.**
- Zone DNS pilotable par API (Cloudflare, OVH, Route 53…).
- Renouvellement **centralisé chez vous** puis poussé vers les officines — éviter de
  déposer un jeton d'API DNS sur les machines clientes.
- Prévoir la mécanique de livraison du certificat renouvelé (canal de mise à jour de
  l'app, ou dépôt sécurisé interrogé par le serveur).

**Limite spécifique.** Certificats Let's Encrypt valides **90 jours** : le renouvellement
doit être automatisé, sinon c'est une panne trimestrielle programmée.

---

### Option 6 — Certificat joker public + résolution locale

*(détaillée en §3, option C)*

**Complément d'intégration.** Un joker ne couvre **qu'un seul niveau** :
`*.local.pharmasmart.ci` couvre `officine-durand.local.pharmasmart.ci` mais pas
`a.b.local.pharmasmart.ci`. Planifier la convention de nommage en conséquence.

**Limite spécifique.** Clé privée commune à tout le parc — la compromission d'un serveur
permet l'usurpation sur les autres réseaux. À mettre en regard du fait que la clé se
trouve de toute façon sur des machines que vous ne contrôlez pas physiquement.

---

### Option 7 — Reverse proxy TLS devant Spring

**Principe.** Spring continue d'écouter en HTTP sur `localhost`, et un proxy
(Caddy, nginx) porte le TLS.

**Prérequis.** Un binaire supplémentaire à embarquer dans le bundle Tauri et à piloter au
démarrage (`backend_manager.rs` gère déjà un processus, il en gérerait deux).

**Intégration.**
- Caddy est ici l'outil pertinent : il gère **ACME automatiquement**, y compris le
  challenge DNS-01 via plugin, et renouvelle sans intervention.
- Spring reste inchangé — pas de keystore à gérer côté Java, pas de profil `tls`.

**Limites.** Un composant de plus à empaqueter, superviser et arrêter proprement.
Redondant avec ce que Spring sait déjà faire, sauf pour l'automatisation ACME — qui est
précisément l'intérêt.

**Quand la choisir.** Si vous partez sur l'option 5 ou 6 : Caddy supprime toute la
logistique de renouvellement, ce qui est le point faible de ces options.

---

### Option 8 — VPN / tunnel chiffré

**Principe.** Ne pas faire de TLS applicatif ; chiffrer tout le transport entre les
terminaux et le serveur (WireGuard, Tailscale, ZeroTier).

**Prérequis.** Un client VPN installé et configuré sur **chaque terminal**, y compris les
tablettes. Pour Tailscale : dépendance à un service tiers et à une identité externe.

**Intégration.** Aucune modification applicative — c'est l'attrait principal.

**Limites.** Configuration par appareil, dépendance à un tiers, et un VPN qui tombe rend
l'application **totalement** inaccessible — alors qu'un problème de certificat laisse au
moins un diagnostic. Surdimensionné pour un LAN unique.

**Quand la choisir.** Si un besoin d'accès **distant** apparaît (multi-sites, télétravail
du pharmacien titulaire). Pour le seul chiffrement du LAN, le rapport bénéfice/complexité
est défavorable.

---

### Option 9 — mTLS (authentification par certificat client)

**Principe.** Le serveur exige aussi un certificat de la part du client : seuls les
terminaux munis d'un certificat émis par vous peuvent se connecter.

**Prérequis.** L'infrastructure de l'option 3 ou 4, plus une émission et une révocation
**par terminal**.

**Intégration.** `server.ssl.client-auth: need` côté Spring, keystore client côté Android.

**Limites.** Cycle de vie par appareil (perte, vol, remplacement d'une tablette →
révocation), et la révocation est le point notoirement fragile de toute PKI. Ne remplace
pas l'authentification utilisateur, elle s'y **ajoute**.

**Quand la choisir.** Uniquement si une exigence explicite impose d'authentifier
l'appareil et pas seulement l'utilisateur. Hors périmètre à ce stade.

---

### Recommandation

**Chemin le plus court vers un chiffrement réel et durable : option 6 + option 7.** Un
certificat joker public renouvelé automatiquement par Caddy, résolu localement. Aucune
configuration sur les terminaux, aucun avertissement pour le personnel, renouvellement
invisible. La contrepartie — clé privée partagée dans le parc — est explicite et
acceptable au regard du fait que ces machines sont déjà hors de votre contrôle physique.

**Si vous refusez tout domaine public : option 4**, au prix d'un développement mobile
(écran d'empreinte) mais avec l'exploitation la plus légère ensuite.

**À écarter :** options 1 et 2 (fausse sécurité et pannes différées), 8 et 9 (hors
proportion pour un LAN d'officine).

---

## 4. Résolution de noms : ce qui marche selon l'appareil

| Méthode | Windows / Tauri | **Android** | Remarque |
|---|---|---|---|
| Fichier hosts | ✅ (droits admin, par machine) | ❌ **Impossible** — `/system/etc/hosts` en lecture seule, root requis | Dépannage ponctuel uniquement |
| DNS du routeur | ✅ | ✅ | Une entrée, tous les appareils via DHCP. Dépend du matériel présent chez le client |
| DNS public | ✅ | ✅ | Aucune configuration client ; seule option indépendante du matériel réseau |
| mDNS (`.local`) | partiel | partiel | À éviter : **aucune autorité n'émet de certificat pour un TLD `.local`** |

> **Correction** : `pharmasmart.local`, employé comme exemple lors des échanges
> préparatoires, n'est **pas** utilisable avec un certificat public. Le nom doit être sous
> un domaine possédé — par exemple `officine-durand.local.pharmasmart.ci`, où `local`
> n'est qu'un sous-domaine ordinaire.

Le comptage se faisant au mobile, **le fichier hosts ne peut pas être la solution
principale**.

---

## 5. Découpage proposé

### Étape 0 — Décision (préalable, non technique)
Choisir l'option A, B ou C. Tout le reste en découle ; la configuration Spring est
identique dans les trois cas, seule la façon dont les clients accordent leur confiance
change.

### Étape 1 — Serveur
| # | Tâche |
|---|---|
| 1.1 | Génération du certificat serveur avec le bon SAN (script d'installation ou procédure) |
| 1.2 | Mot de passe du keystore via `config.json` → `backend_manager.rs`, plus de valeur en clair dans le YAML |
| 1.3 | Activation du profil `tls` dans la chaîne Tauri (`--spring.profiles.active=standalone,tauri,prod,tls`) |
| 1.4 | Correction de l'issuer JWT |
| 1.5 | Redirection HTTP → HTTPS, ou fermeture du port HTTP |

### Étape 2 — Clients de bureau
| # | Tâche |
|---|---|
| 2.1 | `environment.tauri.ts` : `apiServerUrl` en `https` |
| 2.2 | CSP `connect-src` de `tauri.conf.json` |
| 2.3 | Installation de l'autorité dans le magasin Windows (options A et C) |
| 2.4 | ~~Proxy `ng serve`~~ — **rien à faire** : `webpack/proxy.conf.js` bascule déjà sur `https://localhost:9080` avec `secure: false` quand on lance `ng serve --ssl` (cf. §2). Documenter la commande auprès de l'équipe |

### Étape 3 — Mobile
| # | Tâche |
|---|---|
| 3.1 | `network_security_config.xml` avec `trust-anchors` (ou `pin-set`), suppression de `usesCleartextTraffic` |
| 3.2 | `BASE_URL` et validation de l'URL saisie dans `ServerConfigDialog` (imposer `https`) |
| 3.3 | Option A uniquement : écran de confirmation d'empreinte à la première connexion + mémorisation |
| 3.4 | Message d'erreur explicite en cas de certificat refusé — sinon le terrain ne voit qu'un « échec réseau » |

### Étape 4 — Exploitation
| # | Tâche |
|---|---|
| 4.1 | Procédure de rotation documentée (fréquence, qui, impact sur les clients) |
| 4.2 | Alerte avant expiration — un certificat expiré bloque le comptoir |
| 4.3 | Procédure de repli documentée en cas d'échec TLS en production |

---

## 6. Critères d'acceptation

- Une capture réseau sur le Wi-Fi de l'officine ne laisse apparaître ni mot de passe, ni
  JWT, ni données de stock.
- Une tablette d'inventaire neuve se connecte **sans manipulation de certificat** sur
  l'appareil (options B et C), ou avec une **seule confirmation d'empreinte** (option A).
- Le renouvellement du certificat serveur n'impose aucune action sur les clients
  (options B et C).
- Un certificat expiré produit un message compréhensible par le personnel, pas un échec
  réseau opaque.
- Le poste de développement continue de fonctionner sans certificat valide
  (`ng serve --ssl` + `secure: false` du proxy webpack).
