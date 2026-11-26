/**
 * Vrai dans l'application de bureau Tauri, faux dans un navigateur.
 *
 * Fonction libre, sans dépendance, à dessein : `tauriHeadersInterceptor` en a besoin pour chaque
 * requête HTTP. La passer par un service qui appelle lui-même l'API (comme `TauriPrinterService`)
 * refermait une boucle d'injection — NG0200 au démarrage sous Tauri.
 */
export function isTauriRuntime(): boolean {
  return typeof window !== 'undefined' && !!(window as { __TAURI_INTERNALS__?: unknown }).__TAURI_INTERNALS__;
}
