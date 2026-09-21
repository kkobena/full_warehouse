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

; Chemin de C:\ProgramData — résolu via la variable d'environnement `ProgramData`.
; ATTENTION : `$PROGRAMDATA` N'EST PAS une constante NSIS valide (warning 6000,
; évaluée à vide). On lit donc explicitement l'environnement.
Var ProgramDataDir

; Backup root directory — defaults to $PS_DataDir\backups.
Var BackupDir

; Application server port — initialized to DEFAULT_PORT in customInit.
Var BackendPort

; Service installation — resolved at install time.
Var ServiceJavaExe   ; Path to java.exe (bundled JRE or system)
Var ServiceJarPath   ; Full path to the backend JAR
Var ServiceBatchJarPath ; Full path to the pharmaSmart-batch JAR (nightly pipeline)
Var ServiceScriptDir ; Directory with PowerShell service scripts
Var ServicesWanted   ; "yes"/"no" — faut-il (ré)installer les services Windows ?
Var IsUpdate         ; "yes"/"no" — une installation existait-elle avant celle-ci ?

; Marqueur posé dans $PS_DataDir dès qu'un service backend a été installé avec
; succès. $PS_DataDir survivant aux mises à jour (cf. NSIS_HOOK_PREUNINSTALL),
; ce fichier est le seul moyen de savoir, APRÈS que le désinstalleur a supprimé
; les services, qu'il y en avait avant — et donc qu'il faut les recréer.
!define SERVICES_FLAG "services-installed.flag"

; Marqueur posé dans $PS_DataDir quand le paquet installé embarque un JRE.
; Sert à détecter le croisement de lignées : le produit serveur existe en deux
; déclinaisons (avec et sans JRE embarqué) portant le même numéro de version, et
; appliquer la déclinaison SANS JRE sur une installation AVEC JRE supprime le
; JRE (NSIS_HOOK_PREUNINSTALL efface $INSTDIR\sidecar) sur un poste qui, par
; définition, n'a pas de Java système.
!define JRE_FLAG "jre-bundled.flag"

; Database credentials — collected from the wizard page, embedded in config.json.
Var DBHost
Var DBPort
Var DBName
Var DBUser
Var DBPass
Var DBSchema

; nsDialogs control handles — required by PageConfigLeave to read entered values.
Var hTxtHost
Var hTxtPort
Var hTxtName
Var hTxtUser
Var hTxtPass
Var hTxtSchema
Var hTxtServerPort

; Configuration wizard page — handled via PowerShell in customInstall (see below).
; Page custom PageConfig PageConfigLeave  ← disabled: Tauri NSIS MUI2 does not
; reliably call PageConfigLeave, so NSD_GetText never runs and defaults are used.

; ── Initialisation ──────────────────────────────────────────────────────────
; NOTE: Tauri v2 ne dispose PAS de hook d'init équivalent à l'ancien `customInit`
; (v1). Cette macro n'est donc plus appelée automatiquement — les valeurs par
; défaut sont (ré)initialisées au début de NSIS_HOOK_POSTINSTALL (voir plus bas).
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
  ReadEnvStr $ProgramDataDir "ProgramData"
  CreateDirectory "$ProgramDataDir\PharmaSmart"
  ClearErrors
  FileOpen $9 "$ProgramDataDir\PharmaSmart\.write_test" w
  ${If} ${Errors}
    StrCpy $PS_DataDir "$APPDATA\PharmaSmart"
    DetailPrint "Per-user install: using $APPDATA\PharmaSmart"
  ${Else}
    FileClose $9
    Delete "$ProgramDataDir\PharmaSmart\.write_test"
    StrCpy $PS_DataDir "$ProgramDataDir\PharmaSmart"
    DetailPrint "All-users install: using $ProgramDataDir\PharmaSmart"
  ${EndIf}
FunctionEnd



; ── Configuration wizard page ────────────────────────────────────────────────
; Shown before installation: collects DB credentials and server port.
; Values are stored in NSIS variables and written to config.json by CreateConfigFile.
Function PageConfig
  ; Fallback: apply defaults if customInit was not called (some Tauri versions).
  ${If} $DBHost == ""
    StrCpy $DBHost      "${DB_DEFAULT_HOST}"
    StrCpy $DBPort      "${DB_DEFAULT_PORT}"
    StrCpy $DBName      "${DB_DEFAULT_NAME}"
    StrCpy $DBUser      "${DB_DEFAULT_USER}"
    StrCpy $DBSchema    "${DB_DEFAULT_SCHEMA}"
    StrCpy $BackendPort "${DEFAULT_PORT}"
  ${EndIf}

  !insertmacro MUI_HEADER_TEXT "Configuration PharmaSmart" \
    "Paramètres PostgreSQL et port du serveur applicatif"

  nsDialogs::Create 1018
  Pop $0
  ${If} $0 == error
    Abort
  ${EndIf}

  ${NSD_CreateLabel}      0    0  100% 14u "Identifiants de connexion PostgreSQL et port du serveur :"

  ${NSD_CreateLabel}      0   20u 110u 12u "Hôte :"
  ${NSD_CreateText}     115u  18u 175u 12u ""
  Pop $hTxtHost
  ${NSD_SetText} $hTxtHost $DBHost

  ${NSD_CreateLabel}      0   36u 110u 12u "Port PostgreSQL :"
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

  ${NSD_CreateLabel}      0  116u 110u 12u "Port serveur (app) :"
  ${NSD_CreateText}     115u 114u 175u 12u ""
  Pop $hTxtServerPort
  ${NSD_SetText} $hTxtServerPort $BackendPort

  ${NSD_CreateLabel}      0  132u 100% 20u \
    "Hôte, ports, base et utilisateur sont obligatoires. Schéma vide = identique à la base."

  nsDialogs::Show
FunctionEnd

; Validate and collect values when the user clicks Next.
Function PageConfigLeave
  ${NSD_GetText} $hTxtHost       $DBHost
  ${NSD_GetText} $hTxtPort       $DBPort
  ${NSD_GetText} $hTxtName       $DBName
  ${NSD_GetText} $hTxtUser       $DBUser
  ${NSD_GetText} $hTxtPass       $DBPass
  ${NSD_GetText} $hTxtSchema     $DBSchema
  ${NSD_GetText} $hTxtServerPort $BackendPort

  ${If} $DBHost == ""
  ${OrIf} $DBPort == ""
  ${OrIf} $DBName == ""
  ${OrIf} $DBUser == ""
  ${OrIf} $BackendPort == ""
    MessageBox MB_OK|MB_ICONEXCLAMATION \
      "Hôte, port PostgreSQL, base de données, utilisateur et port serveur sont obligatoires."
    Abort
  ${EndIf}

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

  ${If} $PS_DataDir == "$ProgramDataDir\PharmaSmart"
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

  ; Write config.json DIRECTLY to $INSTDIR (next to the executable). The installer
  ; runs elevated and $INSTDIR always exists at this point, so this write is
  ; guaranteed — this is the proven behaviour that always lands the file.
  DetailPrint "Writing $INSTDIR\config.json..."
  FileOpen $3 "$INSTDIR\config.json" w
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
  ${If} ${FileExists} "$INSTDIR\sidecar\jre\bin\java.exe"
    Push "$INSTDIR\sidecar\jre"
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
  FileWrite $3 '    "max_daily_dumps": 2,$\r$\n'
  FileWrite $3 '    "min_dump_interval_hours": 6,$\r$\n'
  FileWrite $3 '    "max_dump_age_hours": 48,$\r$\n'
  FileWrite $3 '    "min_daily_kept": 10,$\r$\n'
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
  DetailPrint "config.json written to $INSTDIR"

  ; Mirror config.json into $PS_DataDir (ProgramData / AppData). This is the
  ; authoritative writable location read at runtime by the Windows service and the
  ; Tauri app. $PS_DataDir was created above, so this copy is reliable.
  DetailPrint "Copying config.json to $PS_DataDir..."
  CopyFiles /SILENT "$INSTDIR\config.json" "$PS_DataDir\config.json"
FunctionEnd

; ── Resolve java.exe path ────────────────────────────────────────────────────
Function FindServiceJava
  ${If} ${FileExists} "$INSTDIR\sidecar\jre\bin\java.exe"
    StrCpy $ServiceJavaExe "$INSTDIR\sidecar\jre\bin\java.exe"
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
; Retient le DERNIER nom correspondant, pas le premier.
;
; `FindFirst` seul renvoyait le premier JAR rencontré. Si un ancien JAR survivait à
; la mise à jour (cf. le verrouillage décrit dans NSIS_HOOK_PREUNINSTALL), le service
; était reconfiguré sur l'ANCIENNE version — l'officine tournait sur le backend
; précédent sans que rien ne le signale. L'énumération NTFS étant alphabétique, le
; dernier nom est la version la plus élevée.
;
; Limite assumée, identique à celle du côté Rust (`find_jar_file`) et du catalogue de
; mise à jour : la comparaison est lexicographique, donc 1.9.0 l'emporterait sur
; 1.10.0. Le vrai garde-fou reste qu'un seul JAR doit subsister — d'où
; l'avertissement ci-dessous s'il y en a plusieurs.
Function FindSidecarJar
  StrCpy $ServiceJarPath ""
  StrCpy $R4 0 ; nombre de JAR trouvés
  FindFirst $0 $1 "$INSTDIR\sidecar\pharmaSmart-app-*.jar"
  ${Do}
    ${If} $1 == ""
      ${ExitDo}
    ${EndIf}
    IntOp $R4 $R4 + 1
    StrCpy $ServiceJarPath "$INSTDIR\sidecar\$1"
    FindNext $0 $1
  ${Loop}
  FindClose $0

  ${If} $ServiceJarPath == ""
    DetailPrint "JAR sidecar introuvable — service non disponible."
  ${Else}
    ${If} $R4 > 1
      DetailPrint "ATTENTION : $R4 JAR applicatifs presents dans sidecar — un ancien n'a pas ete supprime."
    ${EndIf}
    DetailPrint "JAR service : $ServiceJarPath"
  ${EndIf}
FunctionEnd

; ── Locate the pharmaSmart-batch sidecar JAR (nightly pipeline) ─────────────
; Même règle que FindSidecarJar : on retient le dernier nom énuméré.
Function FindSidecarBatchJar
  StrCpy $ServiceBatchJarPath ""
  StrCpy $R4 0
  FindFirst $0 $1 "$INSTDIR\sidecar\pharmaSmart-batch-*.jar"
  ${Do}
    ${If} $1 == ""
      ${ExitDo}
    ${EndIf}
    IntOp $R4 $R4 + 1
    StrCpy $ServiceBatchJarPath "$INSTDIR\sidecar\$1"
    FindNext $0 $1
  ${Loop}
  FindClose $0

  ${If} $ServiceBatchJarPath == ""
    DetailPrint "JAR sidecar batch introuvable — service pharmasmart-batch non disponible."
  ${Else}
    ${If} $R4 > 1
      DetailPrint "ATTENTION : $R4 JAR batch presents dans sidecar — un ancien n'a pas ete supprime."
    ${EndIf}
    DetailPrint "JAR batch : $ServiceBatchJarPath"
  ${EndIf}
FunctionEnd

; ── Install the backend as a Windows service via WinSW ──────────────────────
Function InstallBackendService
  Call FindServiceJava
  Call FindSidecarJar
  ${If} $ServiceJarPath == ""
    DetailPrint "JAR introuvable — le service Windows n'a pas pu etre installe."
    ${IfNot} ${Silent}
      MessageBox MB_OK|MB_ICONEXCLAMATION \
        "JAR introuvable — le service Windows n'a pas pu etre installe."
    ${EndIf}
    Return
  ${EndIf}

  StrCpy $ServiceScriptDir "$INSTDIR\service"
  CreateDirectory "$PS_DataDir\logs"
  ExecWait 'cmd.exe /c powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$ServiceScriptDir\setup-backend-service.ps1" -NsisJavaExe "$ServiceJavaExe" -NsisJarPath "$ServiceJarPath" -DataDir "$PS_DataDir" -NsisPort $BackendPort > "$PS_DataDir\logs\setup-backend-service.log" 2>&1' $0

  ${If} $0 == 0
    DetailPrint "Service pharmasmart-app installe avec succes."
    ; Marqueur de présence des services : lu par NSIS_HOOK_POSTINSTALL lors des
    ; mises à jour suivantes pour les recréer sans reposer la question.
    FileOpen $9 "$PS_DataDir\${SERVICES_FLAG}" w
    FileWrite $9 "pharmasmart-app$\r$\n"
    FileClose $9
    ExecWait 'sc start pharmasmart-app'
  ${Else}
    DetailPrint "Installation du service echouee (code $0). Detail : $PS_DataDir\logs\setup-backend-service.log"
    ${IfNot} ${Silent}
      MessageBox MB_OK|MB_ICONEXCLAMATION \
        "L'installation du service Windows a echoue (code $0).$\r$\n$\r$\n\
Detail de l'erreur : $PS_DataDir\logs\setup-backend-service.log$\r$\n$\r$\n\
Vous pouvez relancer manuellement :$\r$\n\
$ServiceScriptDir\setup-backend-service.ps1"
    ${EndIf}
  ${EndIf}
FunctionEnd

; ── Install the pharmaSmart-batch pipeline as a second Windows service ──────
; Réutilise le même java.exe (bundled JRE) et la même config.json (identifiants
; DB) que le service pharmasmart-app — pas de nouveau prompt utilisateur.
Function InstallBatchService
  Call FindServiceJava
  Call FindSidecarBatchJar
  ${If} $ServiceBatchJarPath == ""
    DetailPrint "JAR batch introuvable — service pharmasmart-batch non installe."
    Return
  ${EndIf}

  StrCpy $ServiceScriptDir "$INSTDIR\service"
  CreateDirectory "$PS_DataDir\logs"
  ExecWait 'cmd.exe /c powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$ServiceScriptDir\setup-batch-service.ps1" -NsisJavaExe "$ServiceJavaExe" -NsisJarPath "$ServiceBatchJarPath" -DataDir "$PS_DataDir" > "$PS_DataDir\logs\setup-batch-service.log" 2>&1' $0

  ${If} $0 == 0
    DetailPrint "Service pharmasmart-batch installe avec succes."
    ExecWait 'sc start pharmasmart-batch'
  ${Else}
    DetailPrint "Installation du service batch echouee (code $0). Detail : $PS_DataDir\logs\setup-batch-service.log"
    ${IfNot} ${Silent}
      MessageBox MB_OK|MB_ICONEXCLAMATION \
        "L'installation du service Windows pharmasmart-batch a echoue (code $0).$\r$\n$\r$\n\
Detail de l'erreur : $PS_DataDir\logs\setup-batch-service.log$\r$\n$\r$\n\
Vous pouvez relancer manuellement :$\r$\n\
$ServiceScriptDir\setup-batch-service.ps1"
    ${EndIf}
  ${EndIf}
FunctionEnd

; ── Stop and remove the Windows service ─────────────────────────────────────
Function RemoveBackendService
  StrCpy $ServiceScriptDir "$INSTDIR\service"
  ${If} ${FileExists} "$ServiceScriptDir\remove-backend-service.ps1"
    ExecWait 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$ServiceScriptDir\remove-backend-service.ps1"' $0
    DetailPrint "Service pharmasmart-app supprime (code $0)."
  ${Else}
    ExecWait 'sc stop pharmasmart-app'
    ExecWait 'sc delete pharmasmart-app'
    DetailPrint "Service pharmasmart-app supprime via sc.exe."
  ${EndIf}
FunctionEnd

Function RemoveBatchService
  StrCpy $ServiceScriptDir "$INSTDIR\service"
  ${If} ${FileExists} "$ServiceScriptDir\remove-batch-service.ps1"
    ExecWait 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$ServiceScriptDir\remove-batch-service.ps1"' $0
    DetailPrint "Service pharmasmart-batch supprime (code $0)."
  ${Else}
    ExecWait 'sc stop pharmasmart-batch'
    ExecWait 'sc delete pharmasmart-batch'
    DetailPrint "Service pharmasmart-batch supprime via sc.exe."
  ${EndIf}
FunctionEnd

; ── Post-install hook (Tauri v2) ─────────────────────────────────────────────
; IMPORTANT : Tauri v2 a renommé les hooks NSIS. L'ancien nom v1 `customInstall`
; n'est plus appelé (il est silencieusement ignoré) — c'est ce qui empêchait
; CreateConfigFile de s'exécuter et donc la création de config.json à l'install.
; Le nom correct en Tauri v2 est NSIS_HOOK_POSTINSTALL (exécuté après la copie
; des fichiers vers $INSTDIR, donc $INSTDIR et les resources sont disponibles).
!macro NSIS_HOOK_POSTINSTALL
  ; customInit n'étant plus invoqué en Tauri v2, on (ré)initialise ici les valeurs
  ; par défaut (générées depuis application-prod.yml) avant que CreateConfigFile
  ; ne les consomme.
  ${If} $BackendPort == ""
    StrCpy $BackendPort "${DEFAULT_PORT}"
  ${EndIf}
  ${If} $DBHost == ""
    StrCpy $DBHost   "${DB_DEFAULT_HOST}"
    StrCpy $DBPort   "${DB_DEFAULT_PORT}"
    StrCpy $DBName   "${DB_DEFAULT_NAME}"
    StrCpy $DBUser   "${DB_DEFAULT_USER}"
    StrCpy $DBPass   ""
    StrCpy $DBSchema "${DB_DEFAULT_SCHEMA}"
  ${EndIf}

  ; ── Résolution du répertoire de données et détection install/màj ───────────
  ; On résout $PS_DataDir AVANT toute écriture pour distinguer une première
  ; installation d'une mise à jour. ResolveDataDir ne fixe pas $BackupDir
  ; (normalement fait par CreateConfigFile), on l'initialise donc ici pour que
  ; la section "sauvegardes" plus bas fonctionne dans les deux branches.
  Call ResolveDataDir
  StrCpy $BackupDir "$PS_DataDir\backups"

  ; $IsUpdate doit être capturé ICI : en première installation, CreateConfigFile
  ; crée config.json juste en dessous, si bien que le même test plus bas dans le
  ; hook répondrait « mise à jour » dans les deux cas.
  StrCpy $IsUpdate "no"

  ${If} ${FileExists} "$PS_DataDir\config.json"
    StrCpy $IsUpdate "yes"
    ; ── MODE MISE À JOUR ───────────────────────────────────────────────────
    ; Une config existe déjà dans le répertoire de données (ProgramData/AppData).
    ; C'est la copie autoritative de l'utilisateur : on NE la réécrit PAS et on
    ; NE relance PAS l'assistant base de données. Seuls les binaires de $INSTDIR
    ; ont été remplacés par l'updater.
    DetailPrint "Mise à jour détectée — configuration existante préservée : $PS_DataDir\config.json"
    ; $INSTDIR ayant été reposé à neuf, on recrée uniquement la copie de
    ; "découverte" à côté de l'exe À PARTIR de la copie autoritative (sens
    ; ProgramData → INSTDIR, jamais l'inverse en mise à jour).
    CopyFiles /SILENT "$PS_DataDir\config.json" "$INSTDIR\config.json"
  ${Else}
    ; ── PREMIÈRE INSTALLATION ──────────────────────────────────────────────
    ; CreateConfigFile writes config.json with built-in defaults (from !define).
    ; configure-database.ps1 is then called to let the user override those defaults via a
    ; PowerShell Windows Forms dialog.  We use the PS dialog instead of the NSIS custom page
    ; because Tauri's MUI2 NSIS does not reliably fire PageConfigLeave callbacks.
    Call CreateConfigFile

    ; ── Launch PowerShell configuration dialog ───────────────────────────────────
    ; The script is bundled as a resource, so it is available once Tauri has
    ; extracted the payload to $INSTDIR (i.e. by the time customInstall runs).
    StrCpy $R9 "$INSTDIR\installer-hooks\configure-database.ps1"
    ; La boîte de dialogue est interactive : en mode silencieux, on conserve les
    ; valeurs par défaut écrites par CreateConfigFile plutôt que de bloquer.
    ${If} ${Silent}
      StrCpy $R9 ""
      DetailPrint "Mode silencieux — assistant base de donnees ignore, defauts conserves."
    ${EndIf}
    ${If} ${FileExists} "$R9"
      DetailPrint "Ouverture de la configuration base de données / serveur…"
      ExecWait 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$R9" -ConfigFile "$PS_DataDir\config.json"' $0
      DetailPrint "Configuration terminée (code $0)."
      ; Re-sync $INSTDIR\config.json with the (possibly updated) copy in $PS_DataDir.
      CopyFiles /SILENT "$PS_DataDir\config.json" "$INSTDIR\config.json"
    ${Else}
      DetailPrint "configure-database.ps1 introuvable — configuration par défaut conservée."
    ${EndIf}
  ${EndIf}

  ; Backup directories.
  DetailPrint "Création des répertoires de sauvegarde : $BackupDir"
  CreateDirectory "$BackupDir"
  CreateDirectory "$BackupDir\daily"
  CreateDirectory "$BackupDir\basebackup"
  CreateDirectory "$BackupDir\wal"
  CreateDirectory "$BackupDir\logs"

  ${If} $PS_DataDir == "$ProgramDataDir\PharmaSmart"
    ExecWait 'icacls "$BackupDir" /grant "*S-1-5-32-545:(OI)(CI)M" /T /Q'
  ${EndIf}

  ; Register scheduled backup tasks.
  ${If} ${FileExists} "$INSTDIR\backup\setup-backup-tasks.ps1"
    DetailPrint "Enregistrement des tâches planifiées de sauvegarde…"
    ExecWait 'cmd.exe /c powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\backup\setup-backup-tasks.ps1" -ExePath "$INSTDIR\backup\pharmasmart-backup.exe" > "$BackupDir\logs\setup-backup-tasks.log" 2>&1' $0
    ${If} $0 != 0
      DetailPrint "Avertissement : enregistrement des tâches planifiées échoué (code $0). Detail : $BackupDir\logs\setup-backup-tasks.log"
    ${Else}
      DetailPrint "Tâches planifiées PharmaSmart_Backup_* enregistrées."
    ${EndIf}
  ${Else}
    DetailPrint "setup-backup-tasks.ps1 introuvable — tâches planifiées non enregistrées."
  ${EndIf}

  ; ── Relais de mise à jour des postes clients ────────────────────────────────
  ; L'exécutable CLIENT est embarqué comme ressource de cet installeur
  ; serveur ; on le dépose dans $PS_DataDir\updates où le backend le publie via
  ; /api/updates/**. Un seul fichier transféré chez le client met ainsi à jour
  ; toute l'officine.
  ;
  ; Entièrement facultatif : si aucun artefact n'a été publié à la construction
  ; (cf. scripts/prepare-client-update.js), $INSTDIR\updates ne contient que son
  ; README, rien d'exploitable n'est déposé, le backend répond « pas de mise à
  ; jour » et le parc reste en installation manuelle.
  ${If} ${FileExists} "$INSTDIR\updates\*.exe"
    CreateDirectory "$PS_DataDir\updates"
    ; Le catalogue ne garde qu'une version : on purge avant de déposer, sinon les
    ; installeurs des versions précédentes s'y accumuleraient indéfiniment.
    Delete "$PS_DataDir\updates\*.exe"
    CopyFiles /SILENT "$INSTDIR\updates\*.exe" "$PS_DataDir\updates"
    DetailPrint "Installeur client publie pour les postes du reseau : $PS_DataDir\updates"
  ${Else}
    DetailPrint "Aucun installeur client embarque — postes clients en installation manuelle."
  ${EndIf}

  ; ── Détection du croisement de lignées (avec JRE ↔ sans JRE) ────────────────
  ; Les deux déclinaisons du produit serveur portent le même numéro de version :
  ; rien, dans le nom de fichier seul, n'empêche d'appliquer la mauvaise. On
  ; compare donc l'état précédent (marqueur) au contenu du paquet en cours.
  ;
  ; Le `config.json` conservé garde un `jvm.java_home` pointant sur le JRE
  ; supprimé, mais ce n'est pas bloquant : les trois scripts de service comme le
  ; côté Rust vérifient la présence effective de `bin\java.exe` avant de s'en
  ; servir et se rabattent sur JAVA_HOME puis PATH. Le rôle de cette garde est
  ; donc d'AVERTIR, pas de réparer — sur un poste sans Java système, ce repli ne
  ; trouvera rien et l'exploitant doit le savoir immédiatement.
  ${If} ${FileExists} "$INSTDIR\sidecar\jre\bin\java.exe"
    ; Paquet AVEC JRE : on (re)pose le marqueur.
    FileOpen $9 "$PS_DataDir\${JRE_FLAG}" w
    FileWrite $9 "bundled$\r$\n"
    FileClose $9
  ${Else}
    ; Paquet SANS JRE : si l'installation précédente en avait un, c'est un
    ; croisement de lignées.
    ${If} ${FileExists} "$PS_DataDir\${JRE_FLAG}"
      DetailPrint "ATTENTION : ce paquet n'embarque pas de JRE alors que l'installation precedente en utilisait un."
      ${IfNot} ${Silent}
        MessageBox MB_OK|MB_ICONEXCLAMATION \
          "Ce paquet d'installation n'embarque PAS de JRE, alors que l'installation$\r$\n\
precedente en utilisait un (qui vient d'etre supprime).$\r$\n$\r$\n\
Si aucun Java n'est installe sur ce poste, le backend ne demarrera pas.$\r$\n$\r$\n\
Deux solutions :$\r$\n\
 - reinstaller avec le paquet incluant le JRE (suffixe -avec-jre) ;$\r$\n\
 - ou installer un JRE sur le poste et renseigner jvm.java_home dans$\r$\n\
   $PS_DataDir\config.json"
      ${EndIf}
      Delete "$PS_DataDir\${JRE_FLAG}"
    ${EndIf}
  ${EndIf}

  ; ── Services Windows (backend + pipeline nocturne) ──────────────────────────
  ; NSIS_HOOK_PREUNINSTALL supprime les deux services au début de CHAQUE mise à
  ; jour (l'updater Tauri rejoue le désinstalleur avant de réinstaller). Leur
  ; recréation ne peut donc pas rester conditionnée à un clic : un « Non »
  ; réflexe — ou une mise à jour silencieuse, où la boîte ne peut recevoir aucune
  ; réponse — laisserait l'officine sans backend ni pipeline nocturne.
  ;
  ; Règle de décision :
  ;   1. Pas de JAR backend        → poste client : aucun service n'est concerné.
  ;   2. Marqueur présent          → des services existaient : on les recrée, sans question.
  ;   3. Mise à jour sans marqueur → installation antérieure au marqueur : on recrée
  ;                                  également (les services sont la configuration
  ;                                  recommandée, et c'est un cas de transition unique).
  ;   4. Première installation     → on demande, sauf en mode silencieux où personne
  ;                                  ne peut répondre : on installe alors par défaut.
  ${If} ${FileExists} "$INSTDIR\service\setup-backend-service.ps1"
    Call FindSidecarJar
    ${If} $ServiceJarPath == ""
      DetailPrint "Aucun JAR backend — poste client : services Windows sans objet."
    ${Else}
      StrCpy $ServicesWanted "no"

      ${If} ${FileExists} "$PS_DataDir\${SERVICES_FLAG}"
        StrCpy $ServicesWanted "yes"
        DetailPrint "Services Windows presents avant la mise a jour — recreation automatique."
      ${ElseIf} $IsUpdate == "yes"
        StrCpy $ServicesWanted "yes"
        DetailPrint "Mise a jour anterieure au marqueur — recreation des services Windows."
      ${ElseIf} ${Silent}
        StrCpy $ServicesWanted "yes"
        DetailPrint "Installation silencieuse — services Windows installes par defaut."
      ${Else}
        MessageBox MB_YESNO|MB_ICONQUESTION \
          "Installer le backend et le pipeline nocturne comme services Windows ?$\r$\n$\r$\n\
Avantage : le serveur et le pipeline (SEMOIS, Classification ABC, Stock, Avoirs)$\r$\n\
demarrent automatiquement au boot, sans avoir besoin d'ouvrir l'application PharmaSmart.$\r$\n$\r$\n\
Recommande pour les postes demarrant sans session utilisateur ouverte.$\r$\n$\r$\n\
Note : necessite WinSW dans $INSTDIR\service\WinSW.exe." \
          IDYES do_install_service IDNO skip_install_service
        do_install_service:
          StrCpy $ServicesWanted "yes"
          Goto service_choice_done
        skip_install_service:
          StrCpy $ServicesWanted "no"
        service_choice_done:
      ${EndIf}

      ${If} $ServicesWanted == "yes"
        Call InstallBackendService
        Call InstallBatchService
      ${Else}
        DetailPrint "Services Windows non installes (choix de l'utilisateur)."
      ${EndIf}
    ${EndIf}
  ${EndIf}

  ; Récapitulatif final : jamais en mode silencieux (mise à jour automatique —
  ; aucun opérateur devant l'écran, la boîte bloquerait l'installeur).
  ${IfNot} ${Silent}
    MessageBox MB_OK|MB_ICONINFORMATION \
      "Installation terminee avec succes !$\r$\n$\r$\n\
Dossier donnees  : $PS_DataDir$\r$\n\
Sauvegardes      : $BackupDir$\r$\n$\r$\n\
Configuration complete (base de donnees, port, FNE, mail…) :$\r$\n\
$PS_DataDir\config.json"
  ${EndIf}
!macroend

; ── Versions « uninstaller » des fonctions partagées ─────────────────────────
; NSIS exige que les fonctions appelées depuis le désinstalleur soient préfixées
; `un.` ; on ne peut pas réutiliser les fonctions de l'installeur. On duplique
; donc ici la logique nécessaire au hook NSIS_HOOK_PREUNINSTALL.
Function un.ResolveDataDir
  ReadEnvStr $ProgramDataDir "ProgramData"
  CreateDirectory "$ProgramDataDir\PharmaSmart"
  ClearErrors
  FileOpen $9 "$ProgramDataDir\PharmaSmart\.write_test" w
  ${If} ${Errors}
    StrCpy $PS_DataDir "$APPDATA\PharmaSmart"
    DetailPrint "Per-user install: using $APPDATA\PharmaSmart"
  ${Else}
    FileClose $9
    Delete "$ProgramDataDir\PharmaSmart\.write_test"
    StrCpy $PS_DataDir "$ProgramDataDir\PharmaSmart"
    DetailPrint "All-users install: using $ProgramDataDir\PharmaSmart"
  ${EndIf}
FunctionEnd

Function un.RemoveBackendService
  StrCpy $ServiceScriptDir "$INSTDIR\service"
  ${If} ${FileExists} "$ServiceScriptDir\remove-backend-service.ps1"
    ExecWait 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$ServiceScriptDir\remove-backend-service.ps1"' $0
    DetailPrint "Service pharmasmart-app supprime (code $0)."
  ${Else}
    ExecWait 'sc stop pharmasmart-app'
    ExecWait 'sc delete pharmasmart-app'
    DetailPrint "Service pharmasmart-app supprime via sc.exe."
  ${EndIf}
FunctionEnd

Function un.RemoveBatchService
  StrCpy $ServiceScriptDir "$INSTDIR\service"
  ${If} ${FileExists} "$ServiceScriptDir\remove-batch-service.ps1"
    ExecWait 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$ServiceScriptDir\remove-batch-service.ps1"' $0
    DetailPrint "Service pharmasmart-batch supprime (code $0)."
  ${Else}
    ExecWait 'sc stop pharmasmart-batch'
    ExecWait 'sc delete pharmasmart-batch'
    DetailPrint "Service pharmasmart-batch supprime via sc.exe."
  ${EndIf}
FunctionEnd

; ── Pre-uninstall hook (Tauri v2) ────────────────────────────────────────────
; Renommé depuis l'ancien `customUninstall` (v1, ignoré en v2). PREUNINSTALL est
; exécuté AVANT que Tauri ne supprime les fichiers installés : le backend Java est
; donc arrêté pendant qu'il tient encore les verrous sur le JAR / la JRE.
!macro NSIS_HOOK_PREUNINSTALL
  Call un.ResolveDataDir

  ; ── 1. ARRÊTER LES SERVICES WINDOWS EN PREMIER ──────────────────────────────
  ;
  ; L'ordre de ce bloc est critique et a déjà coûté cher. Auparavant, on tuait le
  ; java.exe, on supprimait le sidecar, PUIS on retirait les services. Or le
  ; service pharmasmart-app est supervisé par WinSW, configuré avec
  ; `<onfailure action="restart" delay="10 sec"/>` (cf. setup-backend-service.ps1) :
  ; tuer son processus fils ne l'arrête pas, WinSW le RELANCE au bout de 10 s.
  ;
  ; Conséquence de l'ancien ordre : au moment du `RMDir /r "$INSTDIR\sidecar"`, une
  ; JVM fraîchement relancée tenait de nouveau le JAR et les DLL de la JRE. NSIS
  ; échouait alors à supprimer le répertoire — SANS LE SIGNALER — et l'ancien JAR
  ; survivait à côté du nouveau. D'où des mises à jour en place qui « ne marchent
  ; pas », et le réflexe de désinstaller avant de réinstaller.
  ;
  ; `un.RemoveBackendService` appelle `pharmasmart-app.exe stop` puis `uninstall` :
  ; WinSW cesse de superviser, plus rien ne relance la JVM, et les verrous
  ; tombent pour de bon.
  Call un.RemoveBackendService
  Call un.RemoveBatchService

  ; ── 2. Achever les JVM résiduelles ──────────────────────────────────────────
  ;    Reste le cas d'un backend lancé par l'application Tauri elle-même (mode
  ;    sans service) : Windows ne tue pas les processus fils à la sortie du
  ;    parent, la JVM peut donc encore tenir le JAR et la JRE.
  DetailPrint "Arrêt du processus backend Java en cours..."
  ${If} ${FileExists} "$INSTDIR\installer-hooks\stop-backend.ps1"
    ExecWait 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\installer-hooks\stop-backend.ps1" -InstallDir "$INSTDIR"' $0
    DetailPrint "Processus backend arrêté (code $0)."
  ${Else}
    ; Fallback: kill java.exe with pharmaSmart in command line without the script.
    ; Note : `$$_` échappe le `$` PowerShell (sinon NSIS interprète `$_` comme une variable).
    ExecWait "powershell.exe -NoProfile -ExecutionPolicy Bypass -Command $\"Get-WmiObject Win32_Process | Where-Object { $$_.Name -like 'java*' -and $$_.CommandLine -like '*pharmaSmart*' } | ForEach-Object { Stop-Process -Id $$_.ProcessId -Force -ErrorAction SilentlyContinue }; Start-Sleep -Seconds 2$\""
    DetailPrint "Processus backend tué (fallback)."
  ${EndIf}

  ; ── 3. Supprimer le sidecar (JAR + JRE embarquée) ───────────────────────────
  ;    Les services sont arrêtés et désinstallés, les JVM résiduelles tuées : les
  ;    verrous de fichiers sont tombés et la suppression peut aboutir.
  ;    On le fait explicitement plutôt que de compter sur la passe de nettoyage de
  ;    NSIS, qui ignore silencieusement les fichiers verrouillés.
  RMDir /r "$INSTDIR\sidecar"
  ${If} ${FileExists} "$INSTDIR\sidecar\*.*"
    ; Ne doit plus arriver depuis la correction de l'ordre ci-dessus. Si cela se
    ; reproduit, c'est qu'un processus tient encore les fichiers : le signaler
    ; vaut mieux que de laisser une mise à jour se poursuivre sur un sidecar
    ; mélangeant ancienne et nouvelle version.
    DetailPrint "ATTENTION : $INSTDIR\sidecar n'a pas pu etre entierement supprime (fichiers verrouilles)."
  ${Else}
    DetailPrint "Répertoire sidecar supprimé."
  ${EndIf}

  ; Remove scheduled backup tasks.
  ${If} ${FileExists} "$INSTDIR\backup\remove-backup-tasks.ps1"
    ExecWait 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\backup\remove-backup-tasks.ps1"' $0
    DetailPrint "Nettoyage des tâches planifiées (code $0)."
  ${Else}
    ExecWait 'schtasks /Delete /TN "PharmaSmart_Backup_Dump"  /F'
    ExecWait 'schtasks /Delete /TN "PharmaSmart_Backup_Base"  /F'
    ExecWait 'schtasks /Delete /TN "PharmaSmart_Backup_Purge" /F'
    ExecWait 'schtasks /Delete /TN "PharmaSmart_Backup_Check" /F'
  ${EndIf}

  ; ── Conservation des données utilisateur ────────────────────────────────────
  ; On NE supprime JAMAIS $PS_DataDir (config.json, logs, reports, backups…).
  ; Les données doivent survivre à une désinstallation comme à une mise à jour
  ; (l'updater Tauri lance ce même désinstalleur avant de réinstaller). Seuls les
  ; binaires de $INSTDIR sont retirés par NSIS. Pour réinitialiser réellement les
  ; données, l'utilisateur supprime manuellement le dossier ci-dessous.
  DetailPrint "Données utilisateur conservées : $PS_DataDir"
!macroend
