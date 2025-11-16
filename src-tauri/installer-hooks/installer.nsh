!include "nsDialogs.nsh"
!include "LogicLib.nsh"

; Custom page variables
Var ConfigDialog
Var PortInput
Var PortLabel
Var InfoLabel
Var BackendPort

; Default port value
!define DEFAULT_PORT "8080"

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
  StrCpy $4 0 ; Initialize index

  loop:
    ${If} $4 >= $3
      Goto done
    ${EndIf}

    StrCpy $2 $0 1 $4 ; Get char at index
    ${If} $2 == "\"
      StrCpy $1 "$1\\" ; Add escaped backslash
    ${Else}
      StrCpy $1 "$1$2" ; Add char as-is
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

; Function to create config file after installation
Function CreateConfigFile
  DetailPrint "Creating configuration files..."

  ; Create logs directory
  CreateDirectory "$INSTDIR\logs"

  ; Escape backslashes for JSON
  Push "$INSTDIR\logs"
  Call EscapeBackslashes
  Pop $0

  Push "$INSTDIR\logs\pharmasmart.log"
  Call EscapeBackslashes
  Pop $1

  Push "$INSTDIR"
  Call EscapeBackslashes
  Pop $2

  ; Create config.json file with installation settings
  DetailPrint "Writing config.json..."
  FileOpen $3 "$INSTDIR\config.json" w
  FileWrite $3 "{$\r$\n"
  FileWrite $3 '  "server": {$\r$\n'
  FileWrite $3 '    "port": $BackendPort$\r$\n'
  FileWrite $3 '  },$\r$\n'
  FileWrite $3 '  "logging": {$\r$\n'
  FileWrite $3 '    "directory": "$0",$\r$\n'
  FileWrite $3 '    "file": "$1"$\r$\n'
  FileWrite $3 '  },$\r$\n'
  FileWrite $3 '  "installation": {$\r$\n'
  FileWrite $3 '    "directory": "$2"$\r$\n'
  FileWrite $3 '  }$\r$\n'
  FileWrite $3 "}$\r$\n"
  FileClose $3

  ; Create a README file explaining the configuration
  DetailPrint "Writing CONFIGURATION.txt..."
  FileOpen $3 "$INSTDIR\CONFIGURATION.txt" w
  FileWrite $3 "==================================================================$\r$\n"
  FileWrite $3 "PharmaSmart - Configuration$\r$\n"
  FileWrite $3 "==================================================================$\r$\n"
  FileWrite $3 "$\r$\n"
  FileWrite $3 "Repertoire d'installation: $INSTDIR$\r$\n"
  FileWrite $3 "Port du backend: $BackendPort$\r$\n"
  FileWrite $3 "Repertoire des logs: $INSTDIR\logs$\r$\n"
  FileWrite $3 "Fichier de log: $INSTDIR\logs\pharmasmart.log$\r$\n"
  FileWrite $3 "$\r$\n"
  FileWrite $3 "==================================================================$\r$\n"
  FileWrite $3 "Comment modifier la configuration:$\r$\n"
  FileWrite $3 "==================================================================$\r$\n"
  FileWrite $3 "$\r$\n"
  FileWrite $3 "Vous pouvez modifier la configuration en editant le fichier$\r$\n"
  FileWrite $3 "config.json dans le repertoire d'installation.$\r$\n"
  FileWrite $3 "$\r$\n"
  FileWrite $3 "Pour changer le port du backend:$\r$\n"
  FileWrite $3 "1. Ouvrez config.json avec un editeur de texte$\r$\n"
  FileWrite $3 "2. Modifiez la valeur 'port' sous 'server'$\r$\n"
  FileWrite $3 "3. Enregistrez le fichier et redemarrez l'application$\r$\n"
  FileWrite $3 "$\r$\n"
  FileWrite $3 "Pour changer le repertoire des logs:$\r$\n"
  FileWrite $3 "1. Ouvrez config.json avec un editeur de texte$\r$\n"
  FileWrite $3 "2. Modifiez 'directory' et 'file' sous 'logging'$\r$\n"
  FileWrite $3 "3. Assurez-vous que le repertoire existe$\r$\n"
  FileWrite $3 "4. Enregistrez le fichier et redemarrez l'application$\r$\n"
  FileWrite $3 "$\r$\n"
  FileWrite $3 "==================================================================$\r$\n"
  FileWrite $3 "Pour plus d'informations:$\r$\n"
  FileWrite $3 "==================================================================$\r$\n"
  FileWrite $3 "$\r$\n"
  FileWrite $3 "- Les logs sont automatiquement crees au demarrage$\r$\n"
  FileWrite $3 "- Le port peut etre modifie entre 1024 et 65535$\r$\n"
  FileWrite $3 "- Le fichier config.json est au format JSON standard$\r$\n"
  FileWrite $3 "$\r$\n"
  FileClose $3

  DetailPrint "Configuration files created successfully"
FunctionEnd

; Custom install section - called after files are installed
!macro customInstall
  Call CreateConfigFile

  ; Show configuration information
  MessageBox MB_OK|MB_ICONINFORMATION "Installation terminée avec succès!$\r$\n$\r$\nConfiguration par défaut:$\r$\n- Port du serveur: ${DEFAULT_PORT}$\r$\n- Logs: $$INSTDIR\logs\pharmasmart.log$\r$\n$\r$\nPour modifier la configuration:$\r$\n1. Ouvrez le fichier config.json dans le répertoire d'installation$\r$\n2. Modifiez le port ou le chemin des logs selon vos besoins$\r$\n3. Enregistrez et redémarrez l'application$\r$\n$\r$\nConsultez CONFIGURATION.txt pour plus de détails."
!macroend
