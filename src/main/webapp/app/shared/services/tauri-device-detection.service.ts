import { inject, Injectable, signal } from '@angular/core';
import { IPortConnectionStatus, ISerialPortDetail, ISystemInfo } from '../model/poste-device.model';
import { TauriPrinterService } from './tauri-printer.service';

/**
 * Service Tauri pour la détection des périphériques série (douchettes, afficheurs, imprimantes).
 * Réutilise TauriPrinterService pour la vérification Tauri (pas de duplication d'initialisation).
 * L'invoke est récupéré via window.__TAURI__ initialisé par tauri-init.ts.
 *
 * Utilisation typique :
 * 1. Appeler listSerialPorts() pour découvrir les périphériques branchés
 * 2. Comparer avec les configs sauvegardées via PosteDeviceService
 * 3. Appeler checkPortsConnection() avec les ports configurés pour savoir lesquels sont connectés
 * 4. Activer automatiquement le premier périphérique connecté si aucun n'est actif
 */
@Injectable({
  providedIn: 'root',
})
export class TauriDeviceDetectionService {
  private readonly tauriPrinterService = inject(TauriPrinterService);

  /** System info du poste courant — alimenté par loadSystemInfo(), lu par l'interceptor HTTP. */
  readonly systemInfo = signal<ISystemInfo | null>(null);

  /** Charge et met en cache le hostname + IP locale via Tauri. */
  async loadSystemInfo(): Promise<void> {
    const info = await this.getSystemInfo();
    this.systemInfo.set(info);
  }

  /**
   * Vérifie si Tauri est disponible (on est dans l'app desktop).
   */
  isTauriAvailable(): boolean {
    return this.tauriPrinterService.isRunningInTauri();
  }

  /**
   * Liste tous les ports série avec métadonnées USB détaillées.
   * Permet d'identifier automatiquement les rôles (scanner, afficheur, imprimante).
   */
  async listSerialPorts(): Promise<ISerialPortDetail[]> {
    if (!this.isTauriAvailable()) {
      return [];
    }
    try {
      return (await this.invoke('list_serial_ports_detailed')) as ISerialPortDetail[];
    } catch (error) {
      console.error('Erreur lors de la détection des ports série:', error);
      return [];
    }
  }

  /**
   * Vérifie si un port COM spécifique est actuellement connecté.
   */
  async isPortConnected(portName: string): Promise<boolean> {
    if (!this.isTauriAvailable()) {
      return false;
    }
    try {
      return (await this.invoke('is_port_connected', { portName })) as boolean;
    } catch (error) {
      console.error(`Erreur vérification port ${portName}:`, error);
      return false;
    }
  }

  /**
   * Vérifie la connectivité de plusieurs ports en un seul appel.
   * Retourne le statut de chaque port avec les infos du périphérique si connecté.
   */
  async checkPortsConnection(portNames: string[]): Promise<IPortConnectionStatus[]> {
    if (!this.isTauriAvailable() || portNames.length === 0) {
      return [];
    }
    try {
      return (await this.invoke('check_ports_connection', { portNames })) as IPortConnectionStatus[];
    } catch (error) {
      console.error('Erreur vérification batch des ports:', error);
      return [];
    }
  }

  /**
   * Démarre l'écoute du scanner sur le port donné.
   * Les codes scannés sont émis via l'événement Tauri spécifié.
   */
  async startScannerListener(portName: string, baudRate: number, eventName: string): Promise<void> {
    await this.invoke('start_scanner_listener', { portName, baudRate, eventName });
  }

  /**
   * Arrête l'écoute du scanner.
   */
  async stopScannerListener(): Promise<void> {
    if (!this.isTauriAvailable()) {
      return;
    }
    await this.invoke('stop_scanner_listener');
  }

  /**
   * Envoie un message à l'afficheur client (test ou utilisation).
   */
  async sendToDisplay(portName: string, message: string, baudRate: number): Promise<void> {
    await this.invoke('send_to_display', { portName, message, baudRate });
  }

  /**
   * Récupère les informations système du poste (hostname + IP locale).
   * Utilisé pour pré-remplir le formulaire de configuration du poste.
   */
  async getSystemInfo(): Promise<ISystemInfo | null> {
    if (!this.isTauriAvailable()) {
      return null;
    }
    try {
      return (await this.invoke('get_system_info')) as ISystemInfo;
    } catch (error) {
      console.error('Erreur lors de la détection des infos système:', error);
      return null;
    }
  }

  /**
   * Accède à l'invoke Tauri via le global __TAURI__ initialisé par tauri-init.ts.
   */
  private async invoke(cmd: string, args?: Record<string, unknown>): Promise<unknown> {
    // @ts-ignore — window.__TAURI__ est initialisé par tauri-init.ts au démarrage
    const invokeFn = window.__TAURI__?.core?.invoke;
    if (!invokeFn) {
      throw new Error('Tauri non disponible');
    }
    return invokeFn(cmd, args);
  }
}
