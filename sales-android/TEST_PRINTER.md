# 🧪 Test Rapide de l'Imprimante Mock

## Test en 3 Étapes

### 1️⃣ Lancer l'App

```bash
cd C:\Users\k.kobena\Documents\full_warehouse\sales-android
.\gradlew.bat installDebug
```

### 2️⃣ Faire une Vente de Test

1. Ouvrir l'app sur émulateur/téléphone
2. Se connecter
3. Aller dans "Nouvelle vente"
4. Ajouter un produit au panier
5. Cliquer sur **"Payer"**
6. Sélectionner un mode de paiement (ex: Espèces)
7. Cliquer **"Valider"**
8. Quand demandé "Voulez-vous imprimer le reçu?", cliquer **"Oui"**

### 3️⃣ Voir le Reçu dans Logcat

**Option A: Android Studio**
```
1. Ouvrir l'onglet "Logcat" en bas
2. Dans la barre de filtre, taper: MockSunmiPrinter
3. Vous verrez toutes les lignes du reçu!
```

**Option B: Ligne de Commande**
```bash
adb logcat -s MockSunmiPrinter:I
```

**Option C: Voir TOUT le log (incluant le reçu complet)**
```bash
adb logcat -s MockSunmiPrinter:I | findstr /C:"MOCK RECEIPT"
```

## 📸 Exemple de Résultat

Vous devriez voir quelque chose comme:

```
01-15 14:30:00.123  1234  1234 D MockSunmiPrinter: Mock printer connected
01-15 14:30:00.125  1234  1234 D MockSunmiPrinter: printLine: [CENTER] [BOLD] [XLARGE] Pharma Smart
01-15 14:30:00.127  1234  1234 D MockSunmiPrinter: printSeparator: --------------------------------
01-15 14:30:00.129  1234  1234 D MockSunmiPrinter: printLine: TICKET: VNO-2024-001
...
01-15 14:30:00.200  1234  1234 I MockSunmiPrinter: ========== MOCK RECEIPT START ==========
                                                     [CENTER] [BOLD] [XLARGE] Pharma Smart

                                                     [CENTER] Adresse de la pharmacie
                                                     ...
                                                     ========== MOCK RECEIPT END ==========
01-15 14:30:00.205  1234  1234 D MockSunmiPrinter: cutPaper: Receipt cut
```

## ✅ Vérifications

- [ ] Toast "Mock Printer: Connected" apparaît
- [ ] Toast "Mock Print: Receipt logged to console" apparaît
- [ ] Logcat affiche le reçu complet
- [ ] Toutes les lignes du reçu sont formatées correctement
- [ ] Aucune erreur dans Logcat

## 🔍 Si Ça Ne Marche Pas

### 1. Vérifier que c'est bien le mock qui est utilisé

```bash
adb logcat -s UnifiedPrinter:D
```

Devrait afficher:
```
D/UnifiedPrinter: Using: Mock Printer
```

### 2. Vérifier la connexion ADB

```bash
adb devices
```

Devrait lister votre émulateur/appareil.

### 3. Relancer l'app

```bash
adb shell am force-stop com.kobe.warehouse.sales
.\gradlew.bat installDebug
```

### 4. Vérifier les logs d'erreur

```bash
adb logcat *:E
```

## 🎯 Test Sur Appareil Sunmi Réel

Si vous testez sur un vrai appareil Sunmi:

1. L'app détectera automatiquement l'appareil
2. Utilisera la vraie imprimante
3. Le reçu s'imprimera sur papier thermique
4. Logcat affichera: `D/UnifiedPrinter: Using: Real Sunmi Printer`

## 🆘 Besoin d'Aide?

Consultez:
- `PRINTER_SETUP_COMPLETE.md` - Vue d'ensemble
- `PRINTER_SIMULATION.md` - Documentation complète
- Code source dans `src/main/java/.../printer/`

## 📝 Notes

- Le mock printer est **automatiquement** sélectionné sur non-Sunmi
- Pas besoin de configuration spéciale
- Fonctionne immédiatement après installation
- Les logs sont détaillés pour le débogage

**Bon test! 🚀**
