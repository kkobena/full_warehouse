# Initialisation Base de Données Pharma

Application desktop Windows pour créer automatiquement une base de données PostgreSQL avec utilisateur et schéma pour le système Pharma Smart.

## Prérequis

- Rust (version 1.70 ou supérieure)
- PostgreSQL installé et en cours d'exécution sur `localhost`
- Accès administrateur PostgreSQL

## Installation

Cloner le projet et se déplacer dans le répertoire :

```powershell
cd c:\Users\k.kobena\Documents\dev\full_warehouse\scripts\init_pharma_db
```

## Compilation

### Mode Debug (développement)

```powershell
cargo build
```

### Mode Release (production)

```powershell
cargo build --release
```

L'exécutable sera généré dans `target\release\init_pharma_db.exe`

## Exécution

### Exécuter directement avec Cargo

```powershell
cargo run
```

### Exécuter l'exécutable compilé

```powershell
.\target\release\init_pharma_db.exe
```

## Utilisation

1. Lancer l'application
2. Remplir les champs requis :
   - **Utilisateur admin** : nom d'utilisateur PostgreSQL administrateur (par défaut: `postgres`)
   - **Mot de passe admin** : mot de passe de l'administrateur PostgreSQL
   - **Mot de passe** : mot de passe pour le nouvel utilisateur `pharma_smart`
3. Cliquer sur le bouton **"Créer la base de données"**
4. Attendre la confirmation de succès

## Configuration Créée

L'application crée automatiquement :

- **Base de données** : `pharma_smart`
- **Utilisateur** : `pharma_smart` (avec le mot de passe fourni)
- **Schéma** : `pharma_smart`
- **Privilèges** : Tous les privilèges sur le schéma et les objets par défaut

## Nettoyage

Pour nettoyer les fichiers de compilation :

```powershell
cargo clean
```

## Dépendances

- `eframe` : Framework pour l'interface graphique
- `egui` : Bibliothèque GUI immédiate
- `tokio` : Runtime asynchrone
- `tokio-postgres` : Client PostgreSQL asynchrone

## Dépannage

### PostgreSQL n'est pas accessible

Vérifier que PostgreSQL est en cours d'exécution :

```powershell
Get-Service postgresql*
```

### Erreur de connexion

- Vérifier que PostgreSQL écoute sur `localhost`
- Vérifier les identifiants administrateur
- Vérifier le fichier `pg_hba.conf` pour autoriser les connexions locales

### Base de données ou utilisateur existe déjà

Supprimer manuellement la base de données et l'utilisateur existants :

```sql
DROP DATABASE IF EXISTS pharma_smart;
DROP USER IF EXISTS pharma_smart;
```
