# Firebase Setup Guide - Pharma-Smart Mobile Report

**Date**: 10 décembre 2025
**Version**: 1.0

---

## 📋 Prérequis

- Compte Google (pour Firebase Console)
- Accès à Firebase Console: https://console.firebase.google.com
- Android Studio installé
- Backend Spring Boot configuré

---

## 🔥 Étape 1: Créer le Projet Firebase

### 1.1 Accéder à Firebase Console

1. Ouvrir https://console.firebase.google.com
2. Cliquer sur "Add project" ou "Ajouter un projet"

### 1.2 Configurer le Projet

**Nom du projet**: `pharma-smart-report`

**Étapes:**
1. Entrer le nom: `pharma-smart-report`
2. Cliquer "Continue"
3. **Google Analytics**: Activé (recommandé)
   - Sélectionner un compte Analytics existant ou en créer un
4. Cliquer "Create project"
5. Attendre la création (30-60 secondes)
6. Cliquer "Continue"

---

## 📱 Étape 2: Ajouter l'Application Android

### 2.1 Ajouter une App Android

1. Dans la console Firebase, cliquer sur l'icône Android
2. Remplir les informations:

**Package name**: `com.kobe.warehouse.reports`
- ⚠️ IMPORTANT: Doit correspondre exactement au `applicationId` dans `build.gradle.kts`

**App nickname (optional)**: `Pharma Report`

**Debug signing certificate SHA-1 (optional)**: Laisser vide pour le moment

3. Cliquer "Register app"

### 2.2 Télécharger google-services.json

1. Cliquer sur "Download google-services.json"
2. **IMPORTANT**: Placer le fichier dans:
   ```
   pharma-mobile-report/
   └── google-services.json  (racine du module)
   ```
3. Vérifier l'emplacement:
   ```
   pharma-mobile-report/
   ├── build.gradle.kts
   ├── google-services.json  ← ICI
   └── src/
   ```

### 2.3 Configuration Gradle (Déjà fait)

Le plugin Google Services est déjà configuré dans `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.google.services)
}
```

4. Cliquer "Next"
5. Cliquer "Continue to console"

---

## 🔔 Étape 3: Activer Cloud Messaging (FCM)

### 3.1 Vérifier FCM

1. Dans Firebase Console, aller dans "Build" → "Cloud Messaging"
2. Cloud Messaging devrait être automatiquement activé
3. Noter le **Server Key** (sera utilisé si besoin)

### 3.2 Obtenir le Server Key (optionnel)

1. Cliquer sur l'icône "Settings" (⚙️) → "Project settings"
2. Onglet "Cloud Messaging"
3. Copier le **Server key** (si besoin pour intégration tierce)

---

## 🔑 Étape 4: Créer le Service Account (Backend)

### 4.1 Générer la Clé Privée

1. Dans Firebase Console → Settings (⚙️) → "Project settings"
2. Onglet "Service accounts"
3. Cliquer sur "Generate new private key"
4. Confirmer en cliquant "Generate key"
5. Un fichier JSON sera téléchargé (ex: `pharma-smart-report-xxxxx.json`)

### 4.2 Renommer et Placer le Fichier

**Backend Spring Boot:**

1. Renommer le fichier en: `firebase-service-account.json`
2. Placer dans:
   ```
   src/main/resources/
   └── firebase-service-account.json
   ```

⚠️ **IMPORTANT SÉCURITÉ**:
- ✅ Ajouter à `.gitignore`:
  ```
  # Firebase credentials
  **/firebase-service-account.json
  ```
- ❌ Ne JAMAIS commit ce fichier dans Git
- ✅ Utiliser des variables d'environnement en production

### 4.3 Vérifier la Configuration Backend

Le fichier `FirebaseConfig.java` est déjà créé:

```java
@Configuration
public class FirebaseConfig {
    private static final String FIREBASE_CONFIG_PATH = "firebase-service-account.json";

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        ClassPathResource resource = new ClassPathResource(FIREBASE_CONFIG_PATH);
        InputStream serviceAccount = resource.getInputStream();

        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build();

        return FirebaseApp.initializeApp(options);
    }
}
```

---

## 🧪 Étape 5: Tester la Configuration

### 5.1 Tester l'Application Mobile

**1. Build et run l'app:**
```bash
cd pharma-mobile-report
./gradlew installDebug
```

**2. Vérifier les logs:**
```
Logcat → Filter: "FCM"
```

**Attendu:**
```
FirebaseMessaging: Token: eyJhbGciOiJSUz...
```

**3. Tester l'enregistrement du token:**
- Lancer l'app
- Se connecter
- Le token FCM devrait être enregistré automatiquement

### 5.2 Tester le Backend

**1. Démarrer le backend:**
```bash
./mvnw
```

**2. Vérifier les logs au démarrage:**
```
FirebaseConfig: Firebase app initialized successfully
FirebaseConfig: FirebaseMessaging bean created successfully
```

**3. Si erreur:**
```
ERROR: Firebase service account file not found
```
→ Vérifier que `firebase-service-account.json` est bien dans `src/main/resources/`

### 5.3 Envoyer une Notification de Test

**Via Firebase Console:**

1. Aller dans "Engage" → "Messaging"
2. Cliquer "Create your first campaign"
3. Sélectionner "Firebase Notification messages"
4. Remplir:
   - **Notification title**: Test notification
   - **Notification text**: Ceci est un test
5. Cliquer "Next"
6. **Target**: Sélectionner l'app "Pharma Report"
7. Cliquer "Review" → "Publish"

**Vérifier sur le device:**
- La notification devrait apparaître
- Tap sur la notification devrait ouvrir l'app

---

## 🔐 Configuration de Production

### Étape 6: Sécurisation

#### 6.1 Ajouter au .gitignore

**Mobile:**
```gitignore
# Firebase
google-services.json
```

**Backend:**
```gitignore
# Firebase credentials
firebase-service-account.json
```

#### 6.2 Variables d'Environnement (Production)

**Backend - application-prod.yml:**
```yaml
firebase:
  service-account: ${FIREBASE_SERVICE_ACCOUNT_PATH}
```

**Modifier FirebaseConfig.java:**
```java
@Value("${firebase.service-account:firebase-service-account.json}")
private String firebaseConfigPath;
```

**Déploiement:**
```bash
export FIREBASE_SERVICE_ACCOUNT_PATH=/path/to/secure/firebase-service-account.json
./mvnw -Pprod
```

#### 6.3 Secrets Manager (Cloud)

**Pour AWS:**
```bash
aws secretsmanager create-secret \
  --name pharma-smart-firebase-credentials \
  --secret-string file://firebase-service-account.json
```

**Pour Google Cloud:**
```bash
gcloud secrets create firebase-credentials \
  --data-file=firebase-service-account.json
```

---

## 📊 Monitoring et Analytics

### Étape 7: Activer Analytics

#### 7.1 Firebase Analytics

Déjà activé via:
```kotlin
implementation(libs.firebase.analytics)
```

#### 7.2 Événements Personnalisés

**Dans l'app mobile:**
```kotlin
// Log custom event
FirebaseAnalytics.getInstance(context).logEvent("dashboard_viewed") {
    param("user_role", "admin")
    param("timestamp", System.currentTimeMillis())
}
```

#### 7.3 Crashlytics (Optionnel)

**Ajouter dans build.gradle.kts:**
```kotlin
implementation("com.google.firebase:firebase-crashlytics")
```

**Dans Firebase Console:**
- Activer Crashlytics dans "Build" → "Crashlytics"

---

## 🐛 Troubleshooting

### Problème 1: "google-services.json not found"

**Solution:**
```bash
cd pharma-mobile-report
ls google-services.json  # Vérifier présence
```

Si absent:
1. Retélécharger depuis Firebase Console
2. Placer à la racine du module (pas dans src/)

### Problème 2: "Firebase app initialization failed"

**Causes possibles:**
- ❌ Mauvais package name dans google-services.json
- ❌ Plugin google-services non appliqué
- ❌ Fichier mal placé

**Solution:**
1. Vérifier le package name:
   ```json
   // google-services.json
   {
     "client": [{
       "client_info": {
         "android_client_info": {
           "package_name": "com.kobe.warehouse.reports"  ← Vérifier
         }
       }
     }]
   }
   ```

2. Rebuild:
   ```bash
   ./gradlew clean build
   ```

### Problème 3: Backend - "firebase-service-account.json not found"

**Solution:**
```bash
cd src/main/resources
ls firebase-service-account.json  # Vérifier présence
```

Si absent:
1. Retélécharger depuis Firebase Console
2. Renommer en `firebase-service-account.json`
3. Placer dans `src/main/resources/`

### Problème 4: Notifications non reçues

**Checklist:**
1. ✅ App en foreground? (notifications différentes)
2. ✅ Token FCM enregistré? (check logs)
3. ✅ Backend envoie bien? (check logs backend)
4. ✅ Notifications activées sur le device?
5. ✅ Channels de notification créés? (Android 8+)

---

## ✅ Checklist de Configuration

### Mobile

- [ ] Projet Firebase créé
- [ ] App Android ajoutée
- [ ] `google-services.json` téléchargé
- [ ] `google-services.json` placé dans `pharma-mobile-report/`
- [ ] Plugin google-services activé dans `build.gradle.kts`
- [ ] App build et run avec succès
- [ ] Token FCM généré (visible dans logs)
- [ ] Ajouté à `.gitignore`

### Backend

- [ ] Service Account créé
- [ ] Clé privée téléchargée
- [ ] Fichier renommé en `firebase-service-account.json`
- [ ] Fichier placé dans `src/main/resources/`
- [ ] `FirebaseConfig.java` créé
- [ ] Backend démarre sans erreur Firebase
- [ ] Ajouté à `.gitignore`
- [ ] Variables d'environnement configurées (prod)

### Tests

- [ ] Notification de test envoyée depuis console
- [ ] Notification reçue sur device
- [ ] Token enregistré en base de données
- [ ] Backend peut envoyer notifications
- [ ] Notifications groupées fonctionnent
- [ ] Action buttons fonctionnent

---

## 📚 Ressources

**Documentation officielle:**
- Firebase Console: https://console.firebase.google.com
- Firebase Android Setup: https://firebase.google.com/docs/android/setup
- Firebase Cloud Messaging: https://firebase.google.com/docs/cloud-messaging
- Firebase Admin SDK: https://firebase.google.com/docs/admin/setup

**Support:**
- Stack Overflow: [firebase] tag
- Firebase GitHub: https://github.com/firebase
- Community: https://firebase.google.com/community

---

## 🎓 Bonnes Pratiques

### Sécurité

1. ✅ Ne jamais commit `firebase-service-account.json`
2. ✅ Ne jamais commit `google-services.json` (contient des IDs)
3. ✅ Utiliser des variables d'environnement en production
4. ✅ Limiter les permissions du Service Account
5. ✅ Rotation régulière des clés (tous les 90 jours)

### Performance

1. ✅ Grouper les notifications (anti-spam)
2. ✅ Utiliser topics pour envois groupés
3. ✅ Payload léger (< 4 KB)
4. ✅ Cleanup tokens invalides
5. ✅ Monitoring des erreurs FCM

### Maintenance

1. ✅ Logger tous les envois de notifications
2. ✅ Monitorer le taux de delivery
3. ✅ Tracker les erreurs FCM
4. ✅ Dashboard de monitoring
5. ✅ Alertes sur échecs répétés

---

**Document créé le :** 10 décembre 2025
**Version :** 1.0
**Statut :** ✅ Guide Complet
