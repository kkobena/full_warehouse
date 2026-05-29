Unicode True
!include "MUI2.nsh"
!include "LogicLib.nsh"

; ── Métadonnées ──────────────────────────────────────────────────────────────
; APP_VERSION est injecté par Maven : makensis /DAPP_VERSION=x.y.z
!ifndef APP_VERSION
  !define APP_VERSION "1.0.0"
!endif

!define APP_NAME    "PharmaSmart Batch"
!define APP_SLUG    "pharmasmart-batch"
!define PUBLISHER   "PharmaSmart"
!define REG_KEY     "Software\Microsoft\Windows\CurrentVersion\Uninstall\PharmaSmart-Batch"

Name            "${APP_NAME} ${APP_VERSION}"
OutFile         "${APP_SLUG}-${APP_VERSION}-setup.exe"
InstallDir      "$PROGRAMFILES64\PharmaSmart\batch"
InstallDirRegKey HKLM "${REG_KEY}" "InstallLocation"
RequestExecutionLevel admin
ShowInstDetails show
SetCompressor   /SOLID lzma

; ── MUI ──────────────────────────────────────────────────────────────────────
!define MUI_ABORTWARNING

; Icône — optionnelle (présente seulement dans le build complet)
!if /FileExists "icon.ico"
  !define MUI_ICON   "icon.ico"
  !define MUI_UNICON "icon.ico"
!endif

!define MUI_WELCOMEPAGE_TITLE  "Installation de ${APP_NAME}"
!define MUI_WELCOMEPAGE_TEXT   "Ce programme va installer PharmaSmart Batch ${APP_VERSION} \
sur votre serveur.$\r$\n$\r$\nPharmaSmart Batch est un service Windows qui exécute \
automatiquement le pipeline nocturne (SEMOIS, Classification, Inventaire).$\r$\n$\r$\n\
Prérequis : Java 25+, PostgreSQL 18, droits Administrateur."

!define MUI_FINISHPAGE_TITLE   "Installation terminée"
!define MUI_FINISHPAGE_TEXT    "PharmaSmart Batch a été copié dans :$\r$\n$INSTDIR$\r$\n$\r$\n\
Pour enregistrer le service Windows, cochez l'option ci-dessous.$\r$\n\
Vous pouvez aussi l'exécuter plus tard avec :$\r$\n\
$INSTDIR\service\install-batch-service.ps1"

!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_TEXT    "Installer le service Windows maintenant"
!define MUI_FINISHPAGE_RUN_FUNCTION "RunServiceInstaller"
!define MUI_FINISHPAGE_RUN_NOTCHECKED   ; décoché par défaut — l'admin choisit

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "French"

; ── Section principale ───────────────────────────────────────────────────────
Section "PharmaSmart Batch" SecMain
  SectionIn RO

  SetOutPath "$INSTDIR"
  File "pharmaSmart-batch.jar"
  File "config.default.json"
  !if /FileExists "WinSW.exe"
    File "WinSW.exe"
  !endif

  SetOutPath "$INSTDIR\service"
  File "service\install-batch-service.ps1"
  File "service\uninstall-batch-service.ps1"

  CreateDirectory "$INSTDIR\logs"

  ; Menu Démarrer
  CreateDirectory "$SMPROGRAMS\PharmaSmart"
  CreateShortcut "$SMPROGRAMS\PharmaSmart\Installer service Batch.lnk" \
    "powershell.exe" \
    '-NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\service\install-batch-service.ps1" -InstallDir "$INSTDIR"' \
    "" 0 SW_SHOWNORMAL
  CreateShortcut "$SMPROGRAMS\PharmaSmart\Désinstaller service Batch.lnk" \
    "powershell.exe" \
    '-NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\service\uninstall-batch-service.ps1" -InstallDir "$INSTDIR"' \
    "" 0 SW_SHOWNORMAL

  ; Ajout/Suppression de programmes
  WriteRegStr   HKLM "${REG_KEY}" "DisplayName"     "${APP_NAME}"
  WriteRegStr   HKLM "${REG_KEY}" "DisplayVersion"  "${APP_VERSION}"
  WriteRegStr   HKLM "${REG_KEY}" "Publisher"       "${PUBLISHER}"
  WriteRegStr   HKLM "${REG_KEY}" "InstallLocation" "$INSTDIR"
  WriteRegStr   HKLM "${REG_KEY}" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKLM "${REG_KEY}" "NoModify"        1
  WriteRegDWORD HKLM "${REG_KEY}" "NoRepair"        1
  WriteUninstaller "$INSTDIR\uninstall.exe"
SectionEnd

; ── Lancement du service installer depuis la page Finish ─────────────────────
Function RunServiceInstaller
  ; Lire java_home depuis config.json (ou config.default.json) via PowerShell
  ; [Console]::Write évite le \r\n final — $0 = code retour, $1 = chemin java_home
  nsExec::ExecToStack "powershell.exe -NoProfile -Command $\"try{[Console]::Write(((Get-Content (if(Test-Path '$INSTDIR\config.json'){'$INSTDIR\config.json'}else{'$INSTDIR\config.default.json'})|ConvertFrom-Json).jvm.java_home).Trim())}catch{}$\""
  Pop $0   ; code retour (0 = succès)
  Pop $1   ; valeur java_home (vide si non configuré)

  ${If} $0 == 0
  ${AndIf} $1 != ""
    ExecShell "runas" "powershell.exe" \
      '-NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\service\install-batch-service.ps1" -InstallDir "$INSTDIR" -JavaHome "$1"' \
      SW_SHOWNORMAL
  ${Else}
    ExecShell "runas" "powershell.exe" \
      '-NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\service\install-batch-service.ps1" -InstallDir "$INSTDIR"' \
      SW_SHOWNORMAL
  ${EndIf}
FunctionEnd

; ── Désinstallation ───────────────────────────────────────────────────────────
Section "Uninstall"
  ; Arrêter et désinstaller le service s'il est actif
  ExecWait 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\service\uninstall-batch-service.ps1" -InstallDir "$INSTDIR"' $0

  ; Supprimer les fichiers (les logs sont conservés)
  Delete "$INSTDIR\uninstall.exe"
  Delete "$INSTDIR\pharmaSmart-batch.jar"
  Delete "$INSTDIR\config.default.json"
  Delete "$INSTDIR\WinSW.exe"
  RMDir  /r "$INSTDIR\service"

  ; Raccourcis
  Delete "$SMPROGRAMS\PharmaSmart\Installer service Batch.lnk"
  Delete "$SMPROGRAMS\PharmaSmart\Désinstaller service Batch.lnk"
  RMDir  "$SMPROGRAMS\PharmaSmart"

  DeleteRegKey HKLM "${REG_KEY}"

  ; Supprimer $INSTDIR seulement s'il est vide (logs préservés)
  RMDir "$INSTDIR"
SectionEnd
