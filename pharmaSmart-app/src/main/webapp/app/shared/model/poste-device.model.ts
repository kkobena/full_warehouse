export type DeviceType = 'SCANNER' | 'DISPLAY' | 'PRINTER';

/**
 * Mode de connexion USB d'une douchette code-barres.
 * Retourné par la commande Tauri `detect_scanner_usb_mode`.
 *
 * - `"CDC"` → scanner en mode COM série (USB CDC-ACM) : port COM créé par Windows.
 * - `"HID"` → scanner en mode clavier virtuel (USB HID) : frappes clavier standard.
 * - `"NOT_CONNECTED"` → aucun scanner reconnu dans le système.
 *
 * Un scanner CDC ne peut PAS envoyer de frappes clavier sans reconfiguration
 * manuelle (codes-barres dans le manuel d'utilisation) — et vice-versa pour HID.
 */
export type ScannerUsbModeValue = 'CDC' | 'HID' | 'NOT_CONNECTED';

export interface IScannerUsbMode {
  /** `"CDC"` | `"HID"` | `"NOT_CONNECTED"` */
  mode: ScannerUsbModeValue;
  /** Ex: `"COM7"` — rempli uniquement si `mode === "CDC"`. */
  portName?: string;
  /** VID USB du device détecté (ex: `0x05E0` pour Zebra). */
  vid?: number;
  /** PID USB du device détecté. */
  pid?: number;
  /** Fabricant déduit du VID (ex: `"Zebra/Symbol"`). */
  manufacturer?: string;
}

export interface IPosteDevice {
  id?: number;
  posteId?: number;
  deviceType?: DeviceType;
  portName?: string;
  label?: string;
  baudRate?: number;
  vid?: number;
  pid?: number;
  manufacturer?: string;
  productName?: string;
  serialNumber?: string;
  active?: boolean;
  lastConnectedAt?: string;
  connected?: boolean; // calculé côté Tauri (non persisté)
}

/**
 * Détail d'un port série détecté par Tauri (list_serial_ports_detailed).
 */
export interface ISerialPortDetail {
  portName: string;
  vid?: number;
  pid?: number;
  manufacturer?: string;
  product?: string;
  serialNumber?: string;
  suggestedRole?: 'scanner' | 'display' | 'printer' | null;
  genericAdapter: boolean; // true si adaptateur USB-série générique (FTDI, CH340, etc.)
  chipset?: string; // Nom du chipset déduit du VID (ex: "CH340", "FTDI", "CP210x")
}

/**
 * Statut de connexion d'un port (check_ports_connection).
 */
export interface IPortConnectionStatus {
  portName: string;
  connected: boolean;
  deviceInfo?: ISerialPortDetail;
}

/**
 * Informations système du poste détectées par Tauri (get_system_info).
 */
export interface ISystemInfo {
  hostname: string;
  localIp: string;
}

