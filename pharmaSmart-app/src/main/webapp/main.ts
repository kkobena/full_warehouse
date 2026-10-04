// Couleur de l'application posée avant l'amorçage : pas de flash de la couleur d'avant au chargement.
import { applyChrome, readStoredChrome } from './app/core/theme/chrome-theme';

applyChrome(readStoredChrome());

// Initialize Tauri APIs globally (runs in background, doesn't block bootstrap)
void import('./app/tauri-init').catch(() => {});

// Bootstrap Angular application
import('./bootstrap').catch((err: unknown) => console.error(err));
