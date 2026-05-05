export type DeviceType = 'SCANNER' | 'DISPLAY' | 'PRINTER';

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

