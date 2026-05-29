# pharmaSmart-batch

Service Windows Spring Batch — pipeline nocturne (SEMOIS, Classification ABC, Inventaire).

## Prérequis

- Java 25+
- PostgreSQL 18
- Maven (via `mvnw.cmd` à la racine du repo)
- NSIS (`makensis` dans le PATH) — uniquement pour le profil `installer`

---

## Build

```bash
# JAR exécutable seul
mvnw.cmd package -pl pharmaSmart-batch -am -DskipTests

# ZIP distribuable (JAR + scripts PowerShell + config.default.json)
mvnw.cmd package -pl pharmaSmart-batch -am -P dist -DskipTests

# Installeur Windows .exe (nécessite NSIS)
mvnw.cmd package -pl pharmaSmart-batch -am -P installer -DskipTests
```

**Sorties :**

| Profil | Fichier produit |
|--------|-----------------|
| *(défaut)* | `target/pharmaSmart-batch-{version}.jar` |
| `dist` | `target/pharmasmart-batch-{version}-dist.zip` |
| `installer` | `target/installer-staging/pharmasmart-batch-{version}-setup.exe` |

---

## Déploiement — installeur .exe

1. Exécuter `pharmasmart-batch-{version}-setup.exe` en tant qu'Administrateur.
2. Choisir le répertoire d'installation (défaut : `C:\Program Files\PharmaSmart\batch`).
3. Cocher **"Installer le service Windows maintenant"** sur la page Finish — ou l'exécuter plus tard :

```powershell
# Exécuter en tant qu'Administrateur
.\service\install-batch-service.ps1 -InstallDir "C:\Program Files\PharmaSmart\batch"
```

---

## Déploiement — ZIP (sans installeur)

```powershell
# Décompresser le ZIP puis :
.\service\install-batch-service.ps1 `
    -InstallDir "C:\ProgramData\PharmaSmart\batch" `
    -DbUrl      "jdbc:postgresql://localhost:5432/pharma_smart" `
    -DbUser     "pharma_smart" `
    -DbSchema   "pharma_smart" `
    -HeapMin    "128m" `
    -HeapMax    "512m"
```

Le script demande le mot de passe PostgreSQL de façon sécurisée (pas d'argument en clair).

---

## Déploiement — depuis le repo (développement)

```powershell
# Depuis la racine du repo, en tant qu'Administrateur
.\service\install-services.ps1 -AppPort 9080
```

---

## Gestion du service

```powershell
# Démarrer
Start-Service pharmasmart-batch

# Arrêter
Stop-Service pharmasmart-batch

# Statut
Get-Service pharmasmart-batch

# Désinstaller le service
.\service\uninstall-batch-service.ps1 -InstallDir "C:\ProgramData\PharmaSmart\batch"
```

---

## Journaux

```
C:\ProgramData\PharmaSmart\batch\logs\
```

Rotation automatique : 10 Mo max, 5 fichiers conservés.

---

## Paramètres `install-batch-service.ps1`

| Paramètre | Défaut | Description |
|-----------|--------|-------------|
| `-InstallDir` | `C:\ProgramData\PharmaSmart\batch` | Répertoire de déploiement |
| `-DbUrl` | `jdbc:postgresql://localhost:5432/pharma_smart` | URL JDBC |
| `-DbUser` | `pharma_smart` | Utilisateur PostgreSQL |
| `-DbSchema` | `pharma_smart` | Schéma PostgreSQL |
| `-HeapMin` | `128m` | Mémoire JVM minimale |
| `-HeapMax` | `512m` | Mémoire JVM maximale |

Le mot de passe est toujours demandé interactivement (SecureString).
