# PharmaSmart Desktop - Guide Utilisateur

## Configuration du Serveur Backend

L'application PharmaSmart Desktop nécessite une connexion au serveur backend pour fonctionner. Ce guide vous explique comment configurer cette connexion.

## Démarrage Rapide

### 1. Démarrer le Serveur Backend

**Avant de lancer l'application desktop**, vous devez démarrer le serveur backend:

```bash
# Dans le dossier du projet
./mvnw
```

Le serveur démarrera sur `http://localhost:8080`

### 2. Lancer l'Application Desktop

```bash
# Double-cliquez sur l'icône PharmaSmart
# Ou exécutez:
pharmasmart.exe
```

### 3. Première Configuration (Si Nécessaire)

Si le serveur backend n'est pas sur `localhost:8080`, configurez l'adresse:

1. **Ouvrez les Paramètres**
   - Cliquez sur l'icône des paramètres dans la barre de navigation (en haut à droite)

2. **Entrez l'Adresse du Serveur**
   - Adresse par défaut: `http://localhost:8080`
   - Exemples d'autres adresses:
     - `http://192.168.1.100:8080` (serveur sur le réseau local)
     - `http://localhost:9090` (port personnalisé)
     - `http://172.23.162.15:8080` (adresse IP spécifique)

3. **Testez la Connexion**
   - Cliquez sur "Tester la connexion"
   - Attendez le résultat:
     - ✅ Vert = Connexion réussie
     - ❌ Rouge = Échec de la connexion

4. **Enregistrez**
   - Cliquez sur "Enregistrer et Redémarrer"
   - L'application redémarrera automatiquement avec les nouveaux paramètres

## Exemples de Configuration

### Serveur sur la Même Machine (Défaut)

```
Adresse: http://localhost:8080
```

Utilisez cette configuration si:
- Le backend est installé sur le même ordinateur que l'application desktop
- C'est la configuration la plus courante pour un poste unique

### Serveur sur le Réseau Local

```
Adresse: http://192.168.1.100:8080
```

Utilisez cette configuration si:
- Le backend est installé sur un serveur dédié
- Plusieurs postes se connectent au même backend
- Remplacez `192.168.1.100` par l'adresse IP réelle de votre serveur

### Port Personnalisé

```
Adresse: http://localhost:9090
```

Utilisez cette configuration si:
- Votre backend utilise un port différent de 8080
- Remplacez `9090` par le port configuré

## Résolution des Problèmes

### Problème: "localhost refused to connect"

**Cause**: Le serveur backend n'est pas démarré

**Solution**:
1. Ouvrez un terminal/invite de commandes
2. Accédez au dossier du projet
3. Exécutez: `./mvnw`
4. Attendez le message "Started WarehouseApplication"
5. Relancez l'application desktop

### Problème: "401 Unauthorized" ou "403 Forbidden"

**Cause**: Problème d'authentification ou de configuration

**Solutions**:
1. Vérifiez que vous utilisez la bonne adresse du serveur
2. Essayez de vous connecter via un navigateur web: `http://localhost:8080`
3. Si vous voyez la page de connexion, le serveur fonctionne correctement
4. Réinitialisez les paramètres de l'application (bouton "Réinitialiser" dans Paramètres)

### Problème: "ERR_CONNECTION_REFUSED"

**Cause**: Le serveur n'est pas accessible à l'adresse indiquée

**Solutions**:
1. **Vérifiez l'adresse IP**:
   - Ouvrez cmd/terminal sur le serveur
   - Exécutez: `ipconfig` (Windows) ou `ifconfig` (Linux/Mac)
   - Notez l'adresse IPv4

2. **Vérifiez le port**:
   - Par défaut: 8080
   - Vérifiez dans `application.yml`: `server.port`

3. **Testez avec un navigateur**:
   - Ouvrez: `http://[ADRESSE_IP]:8080`
   - Si ça ne fonctionne pas, le problème vient du serveur

4. **Vérifiez le pare-feu**:
   - Windows: Autorisez le port 8080 dans le pare-feu
   - Antivirus: Vérifiez qu'il ne bloque pas les connexions

### Problème: Le test de connexion échoue

**Checklist**:
- [ ] Le serveur backend est-il démarré?
- [ ] L'adresse est-elle correcte (http://, pas https://)?
- [ ] Le port est-il correct (généralement 8080)?
- [ ] Pouvez-vous accéder au serveur via un navigateur?
- [ ] Le pare-feu autorise-t-il les connexions sur ce port?

## Conseils

### Enregistrer les Paramètres

Les paramètres de connexion sont automatiquement sauvegardés et persistent entre les redémarrages de l'application.

### Réinitialiser les Paramètres

Si vous avez des problèmes:
1. Ouvrez les Paramètres
2. Cliquez sur "Réinitialiser"
3. Cela restaurera l'adresse par défaut: `http://localhost:8080`

### Tester Avant d'Enregistrer

Utilisez toujours le bouton "Tester la connexion" avant d'enregistrer pour éviter de sauvegarder une mauvaise configuration.

## Configurations Recommandées

### Installation Mono-Poste (1 ordinateur)

```
Backend: Installé localement
Adresse: http://localhost:8080
```

**Avantages**:
- Simple à configurer
- Pas besoin de réseau
- Performances maximales

### Installation Multi-Postes (Plusieurs ordinateurs)

```
Backend: Serveur dédié (192.168.1.100)
Adresse: http://192.168.1.100:8080
```

**Avantages**:
- Données centralisées
- Plusieurs utilisateurs simultanés
- Sauvegardes centralisées

**Prérequis**:
- Tous les postes sur le même réseau
- Serveur backend accessible
- Port 8080 ouvert sur le serveur

## Support

Pour plus d'aide, consultez:
- `TAURI_BACKEND_CONNECTION.md` - Guide technique détaillé
- Logs du serveur backend
- Console développeur de l'application (F12)

---

**Important**: L'application desktop est le frontend - elle ne peut pas fonctionner sans le serveur backend. Assurez-vous toujours que le backend est démarré avant de lancer l'application.
