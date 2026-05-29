!include "nsDialogs.nsh"
!include "LogicLib.nsh"
!include "FileFunc.nsh"

; Default application server port — must match config.rs default (9080).
!define DEFAULT_PORT "9080"

; ── Build-time DB defaults ───────────────────────────────────────────────────
; Update these when application-prod.yml defaults change.
; generate-db-defaults.ps1 keeps db-defaults.json in sync for the PS helper script.
!define DB_DEFAULT_HOST   "localhost"
!define DB_DEFAULT_PORT   "5432"
!define DB_DEFAULT_NAME   "pharma_smart"
!define DB_DEFAULT_USER   "pharma_smart"
!define DB_DEFAULT_SCHEMA "pharma_smart"

; ── Runtime variables ───────────────────────────────────────────────────────
; Data directory: resolved at install time depending on install mode.
;   AllUsers   → $PROGRAMDATA\PharmaSmart  (requires icacls for runtime writes)
;   CurrentUser → $APPDATA\PharmaSmart     (always writable without elevation)
Var PS_DataDir

; Backup root directory — defaults to $PS_DataDir\backups.
Var BackupDir

; Service installation — resolved at install time.
Var ServiceJavaExe   ; Path to java.exe (bundled JRE or system)
Var ServiceJarPath   ; Full path to the backend JAR
Var ServiceScriptDir ; Directory with PowerShell service scripts

; Database credentials — collected from the wizard page, embedded in config.json.
Var DBHost
Var DBPort
Var DBName
Var DBUser
Var DBPass
Var DBSchema

; nsDialogs control handles — required by PageDBConfigLeave to read values.
Var hTxtHost
Var hTxtPort
Var hTxtName
Var hTxtUser
Var hTxtPass
Var hTxtSchema

; Database configuration wizard page — shown as the first installer page so
; credentials are available when the install section runs.
Page custom PageDBConfig PageDBConfigLeave

; ── Initialisation ──────────────────────────────────────────────────────────
!macro customInit
  StrCpy $BackendPort "${DEFAULT_PORT}"
  ; Pre-fill DB fields with build-time defaults (generated from application-prod.yml).
  StrCpy $DBHost   "${DB_DEFAULT_HOST}"
  StrCpy $DBPort   "${DB_DEFAULT_PORT}"
  StrCpy $DBName   "${DB_DEFAULT_NAME}"
  StrCpy $DBUser   "${DB_DEFAULT_USER}"
  StrCpy $DBPass   ""
  StrCpy $DBSchema "${DB_DEFAULT_SCHEMA}"
!macroend

; ── Helper: escape backslashes and double-quotes for JSON strings ────────────
Function EscapeBackslashes
  Exch $0
  Push $1
  Push $2
  Push $3
  Push $4

  StrCpy $1 ""
  StrLen $3 $0
  StrCpy $4 0

  loop:
    ${If} $4 >= $3
      Goto done
    ${EndIf}
    StrCpy $2 $0 1 $4
    ${If} $2 == "\"
      StrCpy $1 "$1\\"
    ${ElseIf} $2 == '"'
      StrCpy $1 '$1\"'
    ${Else}
      StrCpy $1 "$1$2"
    ${EndIf}
    IntOp $4 $4 + 1
    Goto loop

  done:
  Pop $4
  Pop $3
  Pop $2
  StrCpy $0 $1
  Pop $1
  Exch $0
FunctionEnd

; ── Grant Users modify rights on the all-users data directory ────────────────
; Uses the well-known SID *S-1-5-32-545 to work on any Windows locale.
Function GrantDataDirPermissions
  ExecWait 'icacls "$PS_DataDir" /grant "*S-1-5-32-545:(OI)(CI)M" /T /Q'
  DetailPrint "Permissions set on $PS_DataDir"
FunctionEnd

; ── Resolve $PS_DataDir at runtime ──────────────────────────────────────────
Function ResolveDataDir
  CreateDirectory "$PROGRAMDATA\PharmaSmart"
  ClearErrors
  FileOpen $9 "$PROGRAMDATA\PharmaSmart\.write_test" w
  ${If} ${Errors}
    StrCpy $PS_DataDir "$APPDATA\PharmaSmart"
    DetailPrint "Per-user install: using $APPDATA\PharmaSmart"
  ${Else}
    FileClose $9
    Delete "$PROGRAMDATA\PharmaSmart\.write_test"
    StrCpy $PS_DataDir "$PROGRAMDATA\PharmaSmart"
    DetailPrint "All-users install: using $PROGRAMDATA\PharmaSmart"
  ${EndIf}
FunctionEnd

; ── Database configuration wizard page ──────────────────────────────────────
Function PageDBConfig
  ; Apply defaults the first time (customInit may not be called in all Tauri versions).
  ${If} $DBHost == ""
    StrCpy $DBHost   "${DB_DEFAULT_HOST}"
    StrCpy $DBPort   "${DB_DEFAULT_PORT}"
    StrCpy $DBName   "${DB_DEFAULT_NAME}"
    StrCpy $DBUser   "${DB_DEFAULT_USER}"
    StrCpy $DBSchema "${DB_DEFAULT_SCHEMA}"
  ${EndIf}

  !insertmacro MUI_HEADER_TEXT "Configuration base de données" "Paramètres de connexion PostgreSQL"

  nsDialogs::Create 1018
  Pop $0
  ${If} $0 == error
    Abort
  ${EndIf}

  ${NSD_CreateLabel}      0    0  100% 14u "Renseignez les identifiants de connexion PostgreSQL :"

  ${NSD_CreateLabel}      0   20u 110u 12u "Hôte :"
  ${NSD_CreateText}     115u  18u 175u 12u ""
  Pop $hTxtHost
  ${NSD_SetText} $hTxtHost $DBHost

  ${NSD_CreateLabel}      0   36u 110u 12u "Port :"
  ${NSD_CreateText}     115u  34u 175u 12u ""
  Pop $hTxtPort
  ${NSD_SetText} $hTxtPort $DBPort

  ${NSD_CreateLabel}      0   52u 110u 12u "Base de données :"
  ${NSD_CreateText}     115u  50u 175u 12u ""
  Pop $hTxtName
  ${NSD_SetText} $hTxtName $DBName

  ${NSD_CreateLabel}      0   68u 110u 12u "Utilisateur :"
  ${NSD_CreateText}     115u  66u 175u 12u ""
  Pop $hTxtUser
  ${NSD_SetText} $hTxtUser $DBUser

  ${NSD_CreateLabel}      0   84u 110u 12u "Mot de passe :"
  ${NSD_CreatePassword} 115u  82u 175u 12u ""
  Pop $hTxtPass

  ${NSD_CreateLabel}      0  100u 110u 12u "Schéma (optionnel) :"
  ${NSD_CreateText}     115u  98u 175u 12u ""
  Pop $hTxtSchema
  ${NSD_SetText} $hTxtSchema $DBSchema

  ${NSD_CreateLabel}      0  118u 100% 20u "Hôte, port, base et utilisateur sont obligatoires. Schéma vide = identique à la base."

  nsDialogs::Show
FunctionEnd

; Validate and collect DB values when the user clicks Next.
Function PageDBConfigLeave
  ${NSD_GetText} $hTxtHost   $DBHost
  ${NSD_GetText} $hTxtPort   $DBPort
  ${NSD_GetText} $hTxtName   $DBName
  ${NSD_GetText} $hTxtUser   $DBUser
  ${NSD_GetText} $hTxtPass   $DBPass
  ${NSD_GetText} $hTxtSchema $DBSchema

  ${If} $DBHost == ""
  ${OrIf} $DBPort == ""
  ${OrIf} $DBName == ""
  ${OrIf} $DBUser == ""
    MessageBox MB_OK|MB_ICONEXCLAMATION \
      "Hôte, port, base de données et utilisateur sont obligatoires.$\r$\nVeuillez compléter tous les champs requis."
    Abort
  ${EndIf}

  ; Default schema to DB name when left empty.
  ${If} $DBSchema == ""
    StrCpy $DBSchema $DBName
  ${EndIf}
FunctionEnd

; ── Create all required directories, config.json, and copy to $INSTDIR ──────
Function CreateConfigFile
  Call ResolveDataDir
  DetailPrint "Creating data directory: $PS_DataDir"

  CreateDirectory "$PS_DataDir"
  CreateDirectory "$PS_DataDir\logs"
  CreateDirectory "$PS_DataDir\reports"
  CreateDirectory "$PS_DataDir\images"
  CreateDirectory "$PS_DataDir\json"
  CreateDirectory "$PS_DataDir\csv"
  CreateDirectory "$PS_DataDir\excel"
  CreateDirectory "$PS_DataDir\pharmaml"

  ${If} $PS_DataDir == "$PROGRAMDATA\PharmaSmart"
    Call GrantDataDirPermissions
  ${EndIf}

  ; Escape path values for JSON.
  Push "$PS_DataDir\logs"
  Call EscapeBackslashes
  Pop $0 ; logs dir

  Push "$PS_DataDir\logs\pharmasmart.log"
  Call EscapeBackslashes
  Pop $1 ; log file

  Push "$INSTDIR"
  Call EscapeBackslashes
  Pop $2 ; install dir

  Push "$PS_DataDir\reports"
  Call EscapeBackslashes
  Pop $R0

  Push "$PS_DataDir\images"
  Call EscapeBackslashes
  Pop $R1

  Push "$PS_DataDir\json"
  Call EscapeBackslashes
  Pop $R2

  Push "$PS_DataDir\csv"
  Call EscapeBackslashes
  Pop $R3

  Push "$PS_DataDir\excel"
  Call EscapeBackslashes
  Pop $R4

  Push "$PS_DataDir\pharmaml"
  Call EscapeBackslashes
  Pop $R5

  DetailPrint "Writing $PS_DataDir\config.json..."
  FileOpen $3 "$PS_DataDir\config.json" w
  FileWrite $3 "{$\r$\n"

  ; server
  FileWrite $3 '  "server": {$\r$\n'
  FileWrite $3 '    "port": $BackendPort$\r$\n'
  FileWrite $3 '  },$\r$\n'

  ; logging
  FileWrite $3 '  "logging": {$\r$\n'
  FileWrite $3 '    "directory": "$0",$\r$\n'
  FileWrite $3 '    "file": "$1"$\r$\n'
  FileWrite $3 '  },$\r$\n'

  ; installation
  FileWrite $3 '  "installation": {$\r$\n'
  FileWrite $3 '    "directory": "$2"$\r$\n'
  FileWrite $3 '  },$\r$\n'

  ; jvm — java_home: sidecar bundled JRE if present, otherwise empty (→ JAVA_HOME / PATH).
  ${If} ${FileExists} "$INSTDIR\resources\sidecar\jre\bin\java.exe"
    Push "$INSTDIR\resources\sidecar\jre"
    Call EscapeBackslashes
    Pop $R7
  ${Else}
    StrCpy $R7 ""
  ${EndIf}

  FileWrite $3 '  "jvm": {$\r$\n'
  FileWrite $3 '    "java_home": "$R7",$\r$\n'
  FileWrite $3 '    "app": {$\r$\n'
  FileWrite $3 '      "heap_min": "2g",$\r$\n'
  FileWrite $3 '      "heap_max": "2g",$\r$\n'
  FileWrite $3 '      "metaspace_size": "256m",$\r$\n'
  FileWrite $3 '      "metaspace_max": "384m",$\r$\n'
  FileWrite $3 '      "direct_memory_size": "384m",$\r$\n'
  FileWrite $3 '      "max_gc_pause_millis": "200",$\r$\n'
  FileWrite $3 '      "additional_options": []$\r$\n'
  FileWrite $3 '    },$\r$\n'
  FileWrite $3 '    "batch": {$\r$\n'
  FileWrite $3 '      "heap_min": "128m",$\r$\n'
  FileWrite $3 '      "heap_max": "512m",$\r$\n'
  FileWrite $3 '      "additional_options": []$\r$\n'
  FileWrite $3 '    }$\r$\n'
  FileWrite $3 '  },$\r$\n'

  ; file paths ($R0-$R5 used above are written — safe to read, values already in file)
  FileWrite $3 '  "file": {$\r$\n'
  FileWrite $3 '    "report": "$R0",$\r$\n'
  FileWrite $3 '    "images": "$R1",$\r$\n'
  FileWrite $3 '    "import": {$\r$\n'
  FileWrite $3 '      "json": "$R2",$\r$\n'
  FileWrite $3 '      "csv": "$R3",$\r$\n'
  FileWrite $3 '      "excel": "$R4"$\r$\n'
  FileWrite $3 '    },$\r$\n'
  FileWrite $3 '    "pharmaml": "$R5"$\r$\n'
  FileWrite $3 '  },$\r$\n'

  ; fne — left empty, must be filled by the administrator.
  FileWrite $3 '  "fne": {$\r$\n'
  FileWrite $3 '    "url": "",$\r$\n'
  FileWrite $3 '    "api-key": "",$\r$\n'
  FileWrite $3 '    "point-of-sale": ""$\r$\n'
  FileWrite $3 '  },$\r$\n'

  ; mail — left empty, must be filled by the administrator.
  FileWrite $3 '  "mail": {$\r$\n'
  FileWrite $3 '    "username": "",$\r$\n'
  FileWrite $3 '    "email": ""$\r$\n'
  FileWrite $3 '  },$\r$\n'

  ; port-com — empty by default.
  FileWrite $3 '  "port-com": "",$\r$\n'

  ; ── Escape DB values — reuse $R0-$R5 (file section is fully written above) ──
  Push "$DBHost"
  Call EscapeBackslashes
  Pop $R0 ; host (JSON-safe)

  Push "$DBPort"
  Call EscapeBackslashes
  Pop $R1 ; port (digits only — written without quotes as a JSON number)

  Push "$DBName"
  Call EscapeBackslashes
  Pop $R2 ; db name (JSON-safe)

  Push "$DBUser"
  Call EscapeBackslashes
  Pop $R3 ; username (JSON-safe)

  Push "$DBPass"
  Call EscapeBackslashes
  Pop $R4 ; password (JSON-safe)

  Push "$DBSchema"
  Call EscapeBackslashes
  Pop $R5 ; schema (JSON-safe)

  ; JDBC URL built from host / port / db name.
  StrCpy $R8 "jdbc:postgresql://$R0:$R1/$R2"

  ; ── backup ───────────────────────────────────────────────────────────────
  StrCpy $BackupDir "$PS_DataDir\backups"
  Push "$BackupDir"
  Call EscapeBackslashes
  Pop $R6

  FileWrite $3 '  "backup": {$\r$\n'
  FileWrite $3 '    "directory": "$R6",$\r$\n'
  FileWrite $3 '    "db": "$R2",$\r$\n'
  FileWrite $3 '    "host": "$R0",$\r$\n'
  FileWrite $3 '    "port": $R1,$\r$\n'
  FileWrite $3 '    "user": "$R3",$\r$\n'
  FileWrite $3 '    "retention_daily_days": 30,$\r$\n'
  FileWrite $3 '    "retention_base_weeks": 4,$\r$\n'
  FileWrite $3 '    "wal_archiving": false,$\r$\n'
  FileWrite $3 '    "wal_directory": ""$\r$\n'
  FileWrite $3 '  },$\r$\n'

  ; ── database ─────────────────────────────────────────────────────────────
  FileWrite $3 '  "database": {$\r$\n'
  FileWrite $3 '    "url": "$R8",$\r$\n'
  FileWrite $3 '    "username": "$R3",$\r$\n'
  FileWrite $3 '    "password": "$R4",$\r$\n'
  FileWrite $3 '    "schema": "$R5"$\r$\n'
  FileWrite $3 '  }$\r$\n'

  FileWrite $3 "}$\r$\n"
  FileClose $3
  DetailPrint "config.json written to $PS_DataDir"

  ; Copy config.json to $INSTDIR so it is visible next to the executable.
  ; config_search_dirs() (config.rs) falls back to the exe dir when the
  ; primary location is not readable.
  DetailPrint "Copying config.json to $INSTDIR..."
  CopyFiles /SILENT "$PS_DataDir\config.json" "$INSTDIR\config.json"
FunctionEnd

; ── Resolve java.exe path ────────────────────────────────────────────────────
Function FindServiceJava
  ${If} ${FileExists} "$INSTDIR\resources\sidecar\jre\bin\java.exe"
    StrCpy $ServiceJavaExe "$INSTDIR\resources\sidecar\jre\bin\java.exe"
    DetailPrint "Java (JRE embarqué) : $ServiceJavaExe"
  ${Else}
    ReadEnvStr $0 "JAVA_HOME"
    ${If} $0 != ""
      StrCpy $ServiceJavaExe "$0\bin\java.exe"
      DetailPrint "Java (JAVA_HOME) : $ServiceJavaExe"
    ${Else}
      StrCpy $ServiceJavaExe "java"
      DetailPrint "Java (PATH) : java"
    ${EndIf}
  ${EndIf}
FunctionEnd

; ── Locate the backend sidecar JAR ──────────────────────────────────────────
Function FindSidecarJar
  StrCpy $ServiceJarPath ""
  FindFirst $0 $1 "$INSTDIR\resources\sidecar\pharmaSmart-app-*.jar"
  FindClose $0
  ${If} $1 != ""
    StrCpy $ServiceJarPath "$INSTDIR\resources\sidecar\$1"
    DetailPrint "JAR service : $ServiceJarPath"
  ${Else}
    DetailPrint "JAR sidecar introuvable — service non disponible."
  ${EndIf}
FunctionEnd

; ── Install the backend as a Windows service via WinSW ──────────────────────
Function InstallBackendService
  Call FindServiceJava
  Call FindSidecarJar
  ${If} $ServiceJarPath == ""
    MessageBox MB_OK|MB_ICONEXCLAMATION \
      "JAR introuvable — le service Windows n'a pas pu etre installe."
    Return
  ${EndIf}

  StrCpy $ServiceScriptDir "$INSTDIR\resources\service"
  ExecWait 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$ServiceScriptDir\setup-backend-service.ps1" -JavaExe "$ServiceJavaExe" -JarPath "$ServiceJarPath" -DataDir "$PS_DataDir" -Port $BackendPort' $0

  ${If} $0 == 0
    DetailPrint "Service pharmasmart-app installe avec succes."
    ExecWait 'sc start pharmasmart-app'
  ${Else}
    DetailPrint "Installation du service echouee (code $0)."
    MessageBox MB_OK|MB_ICONEXCLAMATION \
      "L'installation du service Windows a echoue (code $0).$\r$\n$\r$\n\
Vous pouvez relancer manuellement :$\r$\n\
$ServiceScriptDir\setup-backend-service.ps1"
  ${EndIf}
FunctionEnd

; ── Stop and remove the Windows service ─────────────────────────────────────
Function RemoveBackendService
  StrCpy $ServiceScriptDir "$INSTDIR\resources\service"
  ${If} ${FileExists} "$ServiceScriptDir\remove-backend-service.ps1"
    ExecWait 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$ServiceScriptDir\remove-backend-service.ps1"' $0
    DetailPrint "Service pharmasmart-app supprime (code $0)."
  ${Else}
    ExecWait 'sc stop pharmasmart-app'
    ExecWait 'sc delete pharmasmart-app'
    DetailPrint "Service pharmasmart-app supprime via sc.exe."
  ${EndIf}
FunctionEnd

; ── Custom install section ───────────────────────────────────────────────────
!macro customInstall
  ; CreateConfigFile calls ResolveDataDir (sets $PS_DataDir) and sets $BackupDir.
  ; It embeds the DB credentials collected by the PageDBConfig wizard page.
  Call CreateConfigFile

  ; Backup directories.
  DetailPrint "Création des répertoires de sauvegarde : $BackupDir"
  CreateDirectory "$BackupDir"
  CreateDirectory "$BackupDir\daily"
  CreateDirectory "$BackupDir\basebackup"
  CreateDirectory "$BackupDir\wal"
  CreateDirectory "$BackupDir\logs"

  ${If} $PS_DataDir == "$PROGRAMDATA\PharmaSmart"
    ExecWait 'icacls "$BackupDir" /grant "*S-1-5-32-545:(OI)(CI)M" /T /Q'
  ${EndIf}

  ; Register scheduled backup tasks.
  ${If} ${FileExists} "$INSTDIR\resources\backup\setup-backup-tasks.ps1"
    DetailPrint "Enregistrement des tâches planifiées de sauvegarde…"
    ExecWait 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\resources\backup\setup-backup-tasks.ps1" -ExePath "$INSTDIR\resources\backup\pharmasmart-backup.exe"' $0
    ${If} $0 != 0
      DetailPrint "Avertissement : enregistrement des tâches planifiées échoué (code $0)."
    ${Else}
      DetailPrint "Tâches planifiées PharmaSmart_Backup_* enregistrées."
    ${EndIf}
  ${Else}
    DetailPrint "setup-backup-tasks.ps1 introuvable — tâches planifiées non enregistrées."
  ${EndIf}

  ; Optional: install backend as Windows service.
  ${If} ${FileExists} "$INSTDIR\resources\service\setup-backend-service.ps1"
    MessageBox MB_YESNO|MB_ICONQUESTION \
      "Installer le backend comme service Windows ?$\r$\n$\r$\n\
Avantage : le serveur demarre automatiquement au boot,$\r$\n\
sans avoir besoin d'ouvrir l'application PharmaSmart.$\r$\n$\r$\n\
Recommande pour les postes demarrant sans session utilisateur ouverte.$\r$\n$\r$\n\
Note : necessite WinSW dans $INSTDIR\resources\service\WinSW.exe." \
      IDYES do_install_service IDNO skip_install_service
    do_install_service:
      Call InstallBackendService
    skip_install_service:
  ${EndIf}

  MessageBox MB_OK|MB_ICONINFORMATION \
    "Installation terminee avec succes !$\r$\n$\r$\n\
Base de donnees : $DBHost:$DBPort/$DBName$\r$\n\
Dossier de donnees : $PS_DataDir$\r$\n\
Sauvegardes      : $BackupDir$\r$\n$\r$\n\
Pour modifier la configuration (port, FNE, mail, port serie) :$\r$\n\
$PS_DataDir\config.json"
!macroend

; ── Custom uninstall section ─────────────────────────────────────────────────
!macro customUninstall
  Call ResolveDataDir

  ; Stop and remove the Windows service.
  Call RemoveBackendService

  ; Remove scheduled backup tasks.
  ${If} ${FileExists} "$INSTDIR\resources\backup\remove-backup-tasks.ps1"
    ExecWait 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\resources\backup\remove-backup-tasks.ps1"' $0
    DetailPrint "Nettoyage des tâches planifiées (code $0)."
  ${Else}
    ExecWait 'schtasks /Delete /TN "PharmaSmart_Backup_Dump"  /F'
    ExecWait 'schtasks /Delete /TN "PharmaSmart_Backup_Base"  /F'
    ExecWait 'schtasks /Delete /TN "PharmaSmart_Backup_Purge" /F'
    ExecWait 'schtasks /Delete /TN "PharmaSmart_Backup_Check" /F'
  ${EndIf}

  MessageBox MB_YESNO|MB_ICONQUESTION \
    "Supprimer les donnees de l'application (config, logs, rapports) ?$\r$\n\
$PS_DataDir" \
    IDYES remove_data IDNO skip_data
  remove_data:
    RMDir /r "$PS_DataDir"
  skip_data:
!macroend
