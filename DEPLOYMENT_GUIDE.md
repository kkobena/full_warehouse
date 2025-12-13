# Guide de Déploiement Complet - Pharma-Smart Mobile Report

**Date**: 10 décembre 2025
**Version**: 1.0

---

## 📋 Table des Matières

1. [Prérequis](#prérequis)
2. [Configuration Initiale](#configuration-initiale)
3. [Génération Keystore](#génération-keystore)
4. [Build & Signature APK](#build--signature-apk)
5. [Configuration Backend](#configuration-backend)
6. [Migration Base de Données](#migration-base-de-données)
7. [Tests de Production](#tests-de-production)
8. [Déploiement Play Store](#déploiement-play-store)
9. [Monitoring & Maintenance](#monitoring--maintenance)

---

## 1. Prérequis

### Environnement de Développement

- ✅ **Android Studio**: Hedgehog | 2023.1.1+ ou plus récent
- ✅ **JDK 17**: Pour build Android
- ✅ **JDK 25**: Pour backend Spring Boot
- ✅ **Gradle**: 8.2+ (inclus avec Android Studio)
- ✅ **Maven**: 3.9+ (pour backend)
- ✅ **Git**: Pour version control

### Serveurs & Services

- ✅ **PostgreSQL 16**: Base de données
- ✅ **Firebase Project**: Pour notifications push
- ✅ **Serveur Production**: Pour héberger le backend

### Accès Requis

- ✅ Compte développeur Google Play Store ($25 one-time)
- ✅ Accès Firebase Console
- ✅ Accès serveur de production (SSH, FTP, etc.)
- ✅ Accès base de données production

---

## 2. Configuration Initiale

### 2.1 Cloner le Projet

```bash
git clone https://github.com/votre-org/full_warehouse.git
cd full_warehouse
```

### 2.2 Configuration Firebase

**Suivre le guide détaillé:** `FIREBASE_SETUP_GUIDE.md`

**Résumé rapide:**

1. Créer projet Firebase
2. Télécharger `google-services.json` → placer dans `pharma-mobile-report/`
3. Télécharger `firebase-service-account.json` → placer dans `src/main/resources/`

### 2.3 Configuration Base de Données

**Créer la base de données:**

```sql
CREATE DATABASE warehouse;
CREATE USER warehouse WITH PASSWORD 'votre_mot_de_passe';
GRANT ALL PRIVILEGES ON DATABASE warehouse TO warehouse;
```

**application-prod.yml:**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/warehouse
    username: warehouse
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: none
  flyway:
    enabled: true
    schemas: warehouse
```

---

## 3. Génération Keystore

### 3.1 Créer le Répertoire Keystore

```bash
cd pharma-mobile-report
mkdir -p keystore
```

### 3.2 Générer le Keystore de Release

**Commande:**

```bash
keytool -genkey -v \
  -storetype JKS \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore keystore/release.jks \
  -alias pharma-report
```

**Informations à fournir:**

```
Enter keystore password: [créer un mot de passe fort]
Re-enter new password: [confirmer]

What is your first and last name?
  [Unknown]: Pharma Smart

What is the name of your organizational unit?
  [Unknown]: IT Department

What is the name of your organization?
  [Unknown]: Pharma Smart SARL

What is the name of your City or Locality?
  [Unknown]: Abidjan

What is the name of your State or Province?
  [Unknown]: Abidjan

What is the two-letter country code for this unit?
  [Unknown]: CI

Is CN=Pharma Smart, OU=IT Department, O=Pharma Smart SARL, L=Abidjan, ST=Abidjan, C=CI correct?
  [no]: yes

Enter key password for <pharma-report>
        (RETURN if same as keystore password): [ENTER]
```

### 3.3 Sauvegarder les Informations

**Créer un fichier sécurisé:**

`keystore/keystore.properties` (⚠️ NE PAS COMMIT):

```properties
storeFile=keystore/release.jks
storePassword=votre_mot_de_passe_keystore
keyAlias=pharma-report
keyPassword=votre_mot_de_passe_key
```

**Ajouter au .gitignore:**

```gitignore
# Keystore
keystore/
keystore.properties
*.jks
*.keystore
```

### 3.4 Sauvegarder le Keystore (IMPORTANT!)

⚠️ **TRÈS IMPORTANT**:
- Sauvegarder `release.jks` dans un endroit sûr (cloud chiffré, coffre-fort physique)
- Ne JAMAIS perdre ce fichier (impossible de mettre à jour l'app sans lui)
- Noter le mot de passe dans un gestionnaire de mots de passe
- Créer une copie de sauvegarde

---

## 4. Build & Signature APK

### 4.1 Configuration des Variables d'Environnement

**Windows (PowerShell):**

```powershell
$env:KEYSTORE_FILE="keystore/release.jks"
$env:KEYSTORE_PASSWORD="votre_mot_de_passe"
$env:KEY_ALIAS="pharma-report"
$env:KEY_PASSWORD="votre_mot_de_passe"
```

**Linux/Mac:**

```bash
export KEYSTORE_FILE="keystore/release.jks"
export KEYSTORE_PASSWORD="votre_mot_de_passe"
export KEY_ALIAS="pharma-report"
export KEY_PASSWORD="votre_mot_de_passe"
```

### 4.2 Build Release APK

**Commande:**

```bash
cd pharma-mobile-report
./gradlew assembleRelease
```

**Sortie attendue:**

```
BUILD SUCCESSFUL in 2m 45s
58 actionable tasks: 58 executed
```

**Localisation de l'APK:**

```
pharma-mobile-report/build/outputs/apk/release/
└── pharma-mobile-report-release.apk
```

### 4.3 Vérifier la Signature

```bash
# Vérifier que l'APK est signé
jarsigner -verify -verbose -certs \
  build/outputs/apk/release/pharma-mobile-report-release.apk
```

**Sortie attendue:**

```
jar verified.
```

### 4.4 Build Android App Bundle (AAB) pour Play Store

**Commande:**

```bash
./gradlew bundleRelease
```

**Localisation de l'AAB:**

```
pharma-mobile-report/build/outputs/bundle/release/
└── pharma-mobile-report-release.aab
```

---

## 5. Configuration Backend

### 5.1 Build Backend

**Commande:**

```bash
cd ..  # Retour à la racine du projet
./mvnw clean package -Pprod -DskipTests
```

**Sortie:**

```
target/
└── warehouse-0.0.1-SNAPSHOT.jar
```

### 5.2 Configuration Production

**application-prod.yml:**

```yaml
server:
  port: 9080

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/warehouse
    username: ${DB_USER:warehouse}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false

  flyway:
    enabled: true
    schemas: warehouse
    baseline-on-migrate: true

logging:
  level:
    root: INFO
    com.kobe.warehouse: INFO
  file:
    name: logs/pharma-smart.log
```

### 5.3 Déployer sur le Serveur

**Via SCP:**

```bash
scp target/warehouse-0.0.1-SNAPSHOT.jar \
  user@serveur.com:/opt/pharma-smart/

scp src/main/resources/firebase-service-account.json \
  user@serveur.com:/opt/pharma-smart/config/
```

**Via SSH:**

```bash
ssh user@serveur.com
cd /opt/pharma-smart

# Créer le script de démarrage
cat > start.sh <<'EOF'
#!/bin/bash
export DB_HOST=localhost
export DB_USER=warehouse
export DB_PASSWORD=votre_mot_de_passe

java -Xms512m -Xmx2g \
  -Dspring.profiles.active=prod \
  -Dfirebase.service-account=/opt/pharma-smart/config/firebase-service-account.json \
  -jar warehouse-0.0.1-SNAPSHOT.jar
EOF

chmod +x start.sh
```

### 5.4 Créer un Service Systemd

**Créer le fichier service:**

```bash
sudo nano /etc/systemd/system/pharma-smart.service
```

**Contenu:**

```ini
[Unit]
Description=Pharma Smart Backend
After=postgresql.service

[Service]
Type=simple
User=pharma
WorkingDirectory=/opt/pharma-smart
ExecStart=/opt/pharma-smart/start.sh
Restart=always
RestartSec=10

Environment="DB_HOST=localhost"
Environment="DB_USER=warehouse"
Environment="DB_PASSWORD=votre_mot_de_passe"

[Install]
WantedBy=multi-user.target
```

**Activer et démarrer:**

```bash
sudo systemctl daemon-reload
sudo systemctl enable pharma-smart
sudo systemctl start pharma-smart
sudo systemctl status pharma-smart
```

---

## 6. Migration Base de Données

### 6.1 Vérifier les Migrations Disponibles

```bash
./mvnw flyway:info -Pprod
```

**Sortie attendue:**

```
+------------+---------+---------------------+------+---------------------+---------+
| Category   | Version | Description         | Type | Installed On        | State   |
+------------+---------+---------------------+------+---------------------+---------+
| Versioned  | 1.0.1   | init                | SQL  | 2025-01-15 10:00:00 | Success |
| Versioned  | 1.0.2   | referentiels        | SQL  | 2025-01-15 10:00:05 | Success |
| Versioned  | 1.0.3   | procedures          | SQL  | 2025-01-15 10:00:10 | Success |
| Versioned  | 1.0.4   | menus               | SQL  | 2025-01-15 10:00:15 | Success |
| Versioned  | 1.0.5   | id_generator        | SQL  | 2025-01-15 10:00:20 | Success |
| Versioned  | 1.0.6   | user_devices        | SQL  |                     | Pending |
+------------+---------+---------------------+------+---------------------+---------+
```

### 6.2 Exécuter les Migrations

**Dry-run (test):**

```bash
./mvnw flyway:validate -Pprod
```

**Exécution:**

```bash
./mvnw flyway:migrate -Pprod
```

**Vérifier:**

```bash
./mvnw flyway:info -Pprod
```

### 6.3 Vérifier la Table user_device

```sql
-- Connexion à PostgreSQL
psql -U warehouse -d warehouse

-- Vérifier la structure
\d user_device

-- Vérifier les index
\di user_device*

-- Compter les enregistrements
SELECT COUNT(*) FROM user_device;
```

---

## 7. Tests de Production

### 7.1 Tests Backend

**Health Check:**

```bash
curl http://localhost:9080/actuator/health
```

**Réponse attendue:**

```json
{
  "status": "UP"
}
```

**Test API:**

```bash
curl -X POST http://localhost:9080/api/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

### 7.2 Tests Mobile

**Installer l'APK sur un device de test:**

```bash
adb install pharma-mobile-report/build/outputs/apk/release/pharma-mobile-report-release.apk
```

**Checklist:**

1. ✅ Login fonctionne
2. ✅ Dashboard charge les données
3. ✅ Notifications push reçues
4. ✅ Mode offline fonctionne
5. ✅ Synchronisation automatique
6. ✅ Widget s'affiche correctement
7. ✅ Graphiques s'affichent
8. ✅ Scanner code-barre fonctionne

### 7.3 Tests de Performance

**Load test backend:**

```bash
# Installer Apache Bench
sudo apt install apache2-utils

# Test de charge
ab -n 1000 -c 10 http://localhost:9080/api/mobile/dashboard
```

**Métriques attendues:**
- Response time: < 500ms (95th percentile)
- Throughput: > 100 req/s
- Error rate: < 1%

---

## 8. Déploiement Play Store

### 8.1 Créer un Compte Développeur

1. Aller sur https://play.google.com/console
2. S'inscrire (25$ one-time fee)
3. Accepter les termes et conditions

### 8.2 Créer l'Application

1. Dans Play Console, cliquer "Create app"
2. Remplir les informations:
   - **App name**: Pharma Smart Report
   - **Default language**: Français
   - **App type**: Application
   - **Category**: Business
   - **Free or Paid**: Free

### 8.3 Préparer les Assets

**Icône de l'app (512×512 px):**
- Format: PNG 32-bit
- Transparence: Non

**Screenshots (min 2 par type):**
- Téléphone: 1080×1920 px ou 1920×1080 px
- Tablette 7": 1200×1920 px (optionnel)
- Tablette 10": 1600×2560 px (optionnel)

**Feature Graphic:**
- Taille: 1024×500 px
- Format: JPG ou PNG 24-bit

**Description courte (80 caractères max):**
```
Gestion mobile de pharmacie - Tableaux de bord et alertes
```

**Description complète (4000 caractères max):**
```
Pharma Smart Report est l'application mobile de gestion pour les pharmacies.

Fonctionnalités principales:
• Tableau de bord quotidien avec CA et objectifs
• Alertes en temps réel (ruptures, péremptions)
• Rapports de performance détaillés
• Mode offline complet
• Notifications push intelligentes
• Scanner de code-barre intégré
• Widget home screen pour suivi rapide

Conçu spécifiquement pour les gérants et le personnel de pharmacie en Côte d'Ivoire et dans la zone UEMOA.

Nécessite un compte Pharma Smart actif.
```

### 8.4 Upload de l'AAB

1. Dans Play Console → "Release" → "Production"
2. Cliquer "Create new release"
3. Upload `pharma-mobile-report-release.aab`
4. Remplir le changelog:
   ```
   Version 1.0.0
   • Première version de production
   • Dashboard avec métriques clés
   • Alertes et notifications
   • Mode offline
   • Widget Android
   ```

### 8.5 Remplir le Contenu

**Questionnaire:**
- Privacy Policy: [Obligatoire] → Fournir une URL
- Ads: Contient des publicités? → Non
- Target audience: Adults
- Data safety: Remplir selon les données collectées

### 8.6 Tester avec Internal Testing

**Avant la production:**

1. "Testing" → "Internal testing"
2. Create release
3. Upload AAB
4. Add testers (emails)
5. Distribute
6. Tester pendant 1-2 semaines

### 8.7 Soumettre pour Review

1. "Release" → "Production"
2. "Submit for review"
3. Attendre l'approbation (généralement 1-3 jours)

---

## 9. Monitoring & Maintenance

### 9.1 Firebase Analytics

**Dans Firebase Console:**
- "Analytics" → "Dashboard"
- Métriques à surveiller:
  - DAU (Daily Active Users)
  - Sessions par utilisateur
  - Taux de crash
  - Événements personnalisés

### 9.2 Crashlytics

**Activer Crashlytics:**

```kotlin
// build.gradle.kts
implementation("com.google.firebase:firebase-crashlytics")
```

**Dans Firebase Console:**
- "Build" → "Crashlytics"
- Surveiller les crash reports
- Fixer les bugs critiques

### 9.3 Backend Monitoring

**Logs:**

```bash
# Voir les logs en temps réel
sudo journalctl -u pharma-smart -f

# Voir les derniers logs
tail -f /opt/pharma-smart/logs/pharma-smart.log
```

**Métriques:**

```bash
# CPU et mémoire
top -p $(pgrep -f pharma-smart)

# Connexions réseau
netstat -anp | grep 9080
```

### 9.4 Base de Données

**Backup quotidien:**

```bash
# Créer le script de backup
cat > /opt/pharma-smart/backup.sh <<'EOF'
#!/bin/bash
BACKUP_DIR=/opt/pharma-smart/backups
DATE=$(date +%Y%m%d_%H%M%S)

pg_dump -U warehouse warehouse | gzip > $BACKUP_DIR/warehouse_$DATE.sql.gz

# Garder seulement les 7 derniers jours
find $BACKUP_DIR -name "warehouse_*.sql.gz" -mtime +7 -delete
EOF

chmod +x /opt/pharma-smart/backup.sh

# Ajouter au crontab (tous les jours à 2h du matin)
crontab -e
# Ajouter:
0 2 * * * /opt/pharma-smart/backup.sh
```

### 9.5 Mise à Jour de l'Application

**Version mobile (vX.Y.Z):**

1. Incrémenter `versionCode` et `versionName` dans `build.gradle.kts`
2. Build release APK/AAB
3. Upload dans Play Console
4. Déploiement progressif (10% → 50% → 100%)

**Version backend:**

1. Build nouveau JAR
2. Tester en staging
3. Backup de la base de données
4. Déployer en production
5. Exécuter migrations Flyway
6. Redémarrer le service

---

## ✅ Checklist de Déploiement

### Avant le Déploiement

- [ ] Tests unitaires passent (mobile + backend)
- [ ] Tests d'intégration passent
- [ ] Firebase configuré
- [ ] Keystore généré et sauvegardé
- [ ] Base de données de production créée
- [ ] Migrations testées
- [ ] Documentation à jour

### Déploiement Mobile

- [ ] APK/AAB signé correctement
- [ ] Testé sur plusieurs devices
- [ ] Screenshots préparés
- [ ] Description rédigée
- [ ] Privacy Policy disponible
- [ ] Internal testing complété
- [ ] Soumis au Play Store

### Déploiement Backend

- [ ] JAR compilé en mode prod
- [ ] Configurations prod créées
- [ ] Firebase service account configuré
- [ ] Service systemd créé
- [ ] Reverse proxy configuré (nginx/apache)
- [ ] SSL/TLS configuré
- [ ] Backup automatique configuré

### Post-Déploiement

- [ ] Health checks OK
- [ ] Logs surveillés
- [ ] Crashlytics activé
- [ ] Analytics activé
- [ ] Notifications testées
- [ ] Documentation remise aux utilisateurs
- [ ] Formation équipe effectuée

---

## 🆘 Support & Troubleshooting

### Problèmes Courants

**1. Build échoue:**
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

**2. Signature APK échoue:**
- Vérifier les chemins de keystore
- Vérifier les mots de passe
- Recréer le keystore si nécessaire

**3. Backend ne démarre pas:**
```bash
# Vérifier les logs
sudo journalctl -u pharma-smart -n 100

# Vérifier la connexion DB
psql -U warehouse -d warehouse -c "SELECT 1"
```

**4. Notifications ne marchent pas:**
- Vérifier firebase-service-account.json
- Vérifier les logs Firebase Console
- Vérifier que les tokens sont enregistrés

---

## 📚 Ressources

**Documentation:**
- Play Console: https://play.google.com/console
- Firebase: https://console.firebase.google.com
- Android Developers: https://developer.android.com

**Support:**
- Stack Overflow
- GitHub Issues
- Documentation interne

---

**Document créé le :** 10 décembre 2025
**Version :** 1.0
**Statut :** ✅ Guide Complet de Déploiement
