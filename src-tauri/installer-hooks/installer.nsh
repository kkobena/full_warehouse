!include "nsDialogs.nsh"
!include "LogicLib.nsh"

; Default port — must match config.rs default (9080)
!define DEFAULT_PORT "9080"

; Shared data directory (writable by all users, survives app updates)
!define PHARMASMART_DATA "$PROGRAMDATA\PharmaSmart"

; Initialize with default port
!macro customInit
  StrCpy $BackendPort "${DEFAULT_PORT}"
!macroend

; Helper function to escape backslashes for JSON
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

; Grant write access to Users on the PharmaSmart data directory.
; Required so the app can write config.json at runtime without admin rights,
; even when installed in C:\Program Files.
Function GrantDataDirPermissions
  ; OI = Object Inherit, CI = Container Inherit, M = Modify
  ExecWait 'icacls "${PHARMASMART_DATA}" /grant "Users:(OI)(CI)M" /T /Q'
  DetailPrint "Permissions granted on ${PHARMASMART_DATA}"
FunctionEnd

; Function to create config file and required directories
Function CreateConfigFile
  DetailPrint "Creating data directory and configuration files..."

  ; Create shared data directory (ProgramData — accessible by all Windows users)
  CreateDirectory "${PHARMASMART_DATA}"
  CreateDirectory "${PHARMASMART_DATA}\logs"
  CreateDirectory "${PHARMASMART_DATA}\reports"
  CreateDirectory "${PHARMASMART_DATA}\images"
  CreateDirectory "${PHARMASMART_DATA}\json"
  CreateDirectory "${PHARMASMART_DATA}\csv"
  CreateDirectory "${PHARMASMART_DATA}\excel"
  CreateDirectory "${PHARMASMART_DATA}\pharmaml"

  ; Grant Users modify rights so the app can write without admin
  Call GrantDataDirPermissions

  ; Escape paths for JSON
  Push "${PHARMASMART_DATA}\logs"
  Call EscapeBackslashes
  Pop $0 ; logs dir

  Push "${PHARMASMART_DATA}\logs\pharmasmart.log"
  Call EscapeBackslashes
  Pop $1 ; log file

  Push "$INSTDIR"
  Call EscapeBackslashes
  Pop $2 ; install dir

  Push "${PHARMASMART_DATA}\reports"
  Call EscapeBackslashes
  Pop $R0

  Push "${PHARMASMART_DATA}\images"
  Call EscapeBackslashes
  Pop $R1

  Push "${PHARMASMART_DATA}\json"
  Call EscapeBackslashes
  Pop $R2

  Push "${PHARMASMART_DATA}\csv"
  Call EscapeBackslashes
  Pop $R3

  Push "${PHARMASMART_DATA}\excel"
  Call EscapeBackslashes
  Pop $R4

  Push "${PHARMASMART_DATA}\pharmaml"
  Call EscapeBackslashes
  Pop $R5

  ; Write config.json to the shared data dir (writable at runtime without admin)
  DetailPrint "Writing config.json to ${PHARMASMART_DATA}..."
  FileOpen $3 "${PHARMASMART_DATA}\config.json" w
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

  DetailPrint "Configuration created at ${PHARMASMART_DATA}\config.json"
FunctionEnd

; Custom install section — called after files are installed
!macro customInstall
  Call CreateConfigFile

  MessageBox MB_OK|MB_ICONINFORMATION \
    "Installation terminee avec succes!$\r$\n$\r$\nDonnees de l'application:$\r$\n\
${PHARMASMART_DATA}$\r$\n$\r$\nConfiguration par defaut:$\r$\n\
- Port du serveur: ${DEFAULT_PORT}$\r$\n\
- Logs: ${PHARMASMART_DATA}\logs\pharmasmart.log$\r$\n$\r$\n\
Pour personnaliser (port, FNE, mail, port serie):$\r$\n\
Editez ${PHARMASMART_DATA}\config.json,$\r$\n\
puis redemarrez l'application."
!macroend

; Clean up data directory on uninstall (optional — asks user)
!macro customUninstall
  MessageBox MB_YESNO|MB_ICONQUESTION \
    "Supprimer les donnees de l'application (config, logs, rapports) ?$\r$\n\
${PHARMASMART_DATA}" \
    IDYES remove_data IDNO skip_data
  remove_data:
    RMDir /r "${PHARMASMART_DATA}"
  skip_data:
!macroend
