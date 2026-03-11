!include "nsDialogs.nsh"
!include "LogicLib.nsh"

; Default port — must match config.rs default (9080)
!define DEFAULT_PORT "9080"

; Runtime variable: resolved at install time depending on install mode.
; AllUsers  → $PROGRAMDATA\PharmaSmart  (requires icacls for runtime writes)
; CurrentUser → $APPDATA\PharmaSmart     (always writable without elevation)
Var PS_DataDir

; Initialize with default port
!macro customInit
  StrCpy $BackendPort "${DEFAULT_PORT}"
!macroend

; Helper function to escape backslashes and quotes for JSON
Function EscapeBackslashes
  Exch $0 ; Input string
  Push $1 ; Output string
  Push $2 ; Current char
  Push $3 ; Length
  Push $4 ; Index

  StrCpy $1 "" ; Initialize output
  StrLen $3 $0 ; Get length
  StrCpy $4 0  ; Initialize index

  loop:
    ${If} $4 >= $3
      Goto done
    ${EndIf}

    StrCpy $2 $0 1 $4 ; Get char at index
    ${If} $2 == "\"
      StrCpy $1 "$1\\" ; Escaped backslash
    ${ElseIf} $2 == '"'
      StrCpy $1 '$1\"' ; Escaped quote (safe JSON)
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

; Grant Users modify rights on the all-users data directory.
; Uses the well-known SID *S-1-5-32-545 instead of the localised "Users" name
; so this works on French, English, and any other Windows locale.
Function GrantDataDirPermissions
  ExecWait 'icacls "$PS_DataDir" /grant "*S-1-5-32-545:(OI)(CI)M" /T /Q'
  DetailPrint "Permissions set on $PS_DataDir"
FunctionEnd

; Resolve $PS_DataDir at runtime:
;   - If $PROGRAMDATA\PharmaSmart is writable (all-users / admin install) → use it.
;   - Otherwise (per-user install without elevation) → use $APPDATA\PharmaSmart.
; This avoids relying on $MultiUser.InstallMode which may not be initialised yet.
Function ResolveDataDir
  CreateDirectory "$PROGRAMDATA\PharmaSmart"
  ClearErrors
  FileOpen $9 "$PROGRAMDATA\PharmaSmart\.write_test" w
  ${If} ${Errors}
    ; Cannot write to ProgramData — per-user install without elevation
    StrCpy $PS_DataDir "$APPDATA\PharmaSmart"
    DetailPrint "Per-user install detected: using $APPDATA\PharmaSmart"
  ${Else}
    FileClose $9
    Delete "$PROGRAMDATA\PharmaSmart\.write_test"
    StrCpy $PS_DataDir "$PROGRAMDATA\PharmaSmart"
    DetailPrint "All-users install detected: using $PROGRAMDATA\PharmaSmart"
  ${EndIf}
FunctionEnd

; Create all required subdirectories, config.json, and copy it to $INSTDIR.
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

  ; Grant Modify rights only when installed in ProgramData (protected location).
  ${If} $PS_DataDir == "$PROGRAMDATA\PharmaSmart"
    Call GrantDataDirPermissions
  ${EndIf}

  ; Escape paths for JSON
  Push "$PS_DataDir\logs"
  Call EscapeBackslashes
  Pop $0 ; logs dir (JSON-safe)

  Push "$PS_DataDir\logs\pharmasmart.log"
  Call EscapeBackslashes
  Pop $1 ; log file (JSON-safe)

  Push "$INSTDIR"
  Call EscapeBackslashes
  Pop $2 ; install dir (JSON-safe)

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

  ; Write config.json to the data directory
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

  ; jvm — defaults matching JvmConfig::default()
  FileWrite $3 '  "jvm": {$\r$\n'
  FileWrite $3 '    "heap_min": "2g",$\r$\n'
  FileWrite $3 '    "heap_max": "2g",$\r$\n'
  FileWrite $3 '    "metaspace_size": "256m",$\r$\n'
  FileWrite $3 '    "metaspace_max": "384m",$\r$\n'
  FileWrite $3 '    "direct_memory_size": "384m",$\r$\n'
  FileWrite $3 '    "max_gc_pause_millis": "200",$\r$\n'
  FileWrite $3 '    "additional_options": []$\r$\n'
  FileWrite $3 '  },$\r$\n'

  ; file paths
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

  ; fne — left empty, must be filled by the administrator
  FileWrite $3 '  "fne": {$\r$\n'
  FileWrite $3 '    "url": "",$\r$\n'
  FileWrite $3 '    "api-key": "",$\r$\n'
  FileWrite $3 '    "point-of-sale": ""$\r$\n'
  FileWrite $3 '  },$\r$\n'

  ; mail — left empty, must be filled by the administrator
  FileWrite $3 '  "mail": {$\r$\n'
  FileWrite $3 '    "username": "",$\r$\n'
  FileWrite $3 '    "email": ""$\r$\n'
  FileWrite $3 '  },$\r$\n'

  ; port-com — empty by default
  FileWrite $3 '  "port-com": ""$\r$\n'

  FileWrite $3 "}$\r$\n"
  FileClose $3
  DetailPrint "config.json written to $PS_DataDir"

  ; Copy config.json to $INSTDIR so it is visible next to the executable.
  ; config_search_dirs() (config.rs) falls back to the exe dir when the
  ; primary location is not readable.
  DetailPrint "Copying config.json to $INSTDIR..."
  CopyFiles /SILENT "$PS_DataDir\config.json" "$INSTDIR\config.json"
FunctionEnd

; Custom install section — called after files are installed
!macro customInstall
  ; ResolveDataDir is called inside CreateConfigFile
  Call CreateConfigFile

  MessageBox MB_OK|MB_ICONINFORMATION \
    "Installation terminee avec succes!$\r$\n$\r$\nDossier de donnees:$\r$\n\
$PS_DataDir$\r$\n$\r$\nConfiguration par defaut:$\r$\n\
- Port du serveur: ${DEFAULT_PORT}$\r$\n\
- Logs: $PS_DataDir\logs\pharmasmart.log$\r$\n$\r$\n\
Pour personnaliser (port, FNE, mail, port serie):$\r$\n\
Editez $PS_DataDir\config.json,$\r$\n\
puis redemarrez l'application."
!macroend

; Clean up data directory on uninstall (optional — asks user)
!macro customUninstall
  ; Re-detect data directory (same write-test logic as install time)
  Call ResolveDataDir

  MessageBox MB_YESNO|MB_ICONQUESTION \
    "Supprimer les donnees de l'application (config, logs, rapports) ?$\r$\n\
$PS_DataDir" \
    IDYES remove_data IDNO skip_data
  remove_data:
    RMDir /r "$PS_DataDir"
  skip_data:
!macroend
