// src-tauri/src/scanner.rs
// Module scanner USB CDC (COM virtuel) — lecture douchette code-barres via serialport

use serde::Serialize;
use std::collections::HashMap;
use std::io::Read;
use std::net::UdpSocket;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, LazyLock, Mutex};
use std::thread;
use std::time::Duration;
use tauri::{AppHandle, Emitter};

/// ── Ouverture CDC brute + lecture (Windows) ──────────────────────────────────
///
/// La crate `serialport` v4 appelle GetCommState après CreateFile. Certains
/// firmwares USB CDC ne répondent pas à cet IOCTL (ERROR_GEN_FAILURE 0x1F).
///
/// Ce module implémente deux mécanismes complémentaires :
///
/// 1. `prime()` — amorçage préventif :
///    Ouvre via CreateFileW, envoie DTR/RTS pour réveiller le firmware, ferme.
///    Tente de préparer le firmware AVANT que serialport::open() appelle GetCommState.
///
/// 2. `RawCdcPort` + `open_raw()` — fallback total :
///    Si serialport::open() échoue malgré le primer, ouvre avec CreateFileW +
///    SetCommTimeouts UNIQUEMENT (sans GetCommState ni SetCommState), puis lit
///    avec ReadFile. Compatible avec tout firmware CDC qui répond à CreateFile
///    mais pas aux IOCTLs DCB.
#[cfg(target_os = "windows")]
mod cdc_primer {
    use std::thread;
    use std::time::Duration;
    use windows::Win32::Devices::Communication::{
        EscapeCommFunction, PurgeComm, SetCommTimeouts, COMMTIMEOUTS, ESCAPE_COMM_FUNCTION,
        PURGE_COMM_FLAGS,
    };
    use windows::Win32::Foundation::{CloseHandle, GENERIC_READ, GENERIC_WRITE, HANDLE};
    use windows::Win32::Storage::FileSystem::{
        CreateFileW, ReadFile, FILE_CREATION_DISPOSITION, FILE_FLAGS_AND_ATTRIBUTES, FILE_SHARE_MODE,
    };
    use windows::Win32::System::IO::{CancelIoEx, GetOverlappedResult, OVERLAPPED};
    use windows::Win32::System::Threading::{CreateEventW, ResetEvent, WaitForSingleObject};
    use windows::core::PCWSTR;

    // ── Constantes WINAPI ─────────────────────────────────────────────────
    const SETDTR: ESCAPE_COMM_FUNCTION = ESCAPE_COMM_FUNCTION(5);
    const CLRDTR: ESCAPE_COMM_FUNCTION = ESCAPE_COMM_FUNCTION(6);
    const SETRTS: ESCAPE_COMM_FUNCTION = ESCAPE_COMM_FUNCTION(3);
    const PURGE_RXCLEAR: PURGE_COMM_FLAGS = PURGE_COMM_FLAGS(0x0008);
    const PURGE_TXCLEAR: PURGE_COMM_FLAGS = PURGE_COMM_FLAGS(0x0004);
    /// FILE_FLAG_OVERLAPPED = 0x40000000 — requis par usbser.sys pour router les
    /// données USB CDC vers le buffer ReadFile.
    const FILE_FLAG_OVERLAPPED: FILE_FLAGS_AND_ATTRIBUTES = FILE_FLAGS_AND_ATTRIBUTES(0x40000000);
    /// ERROR_IO_PENDING : ReadFile overlapped en cours, pas une erreur.
    const ERROR_IO_PENDING: u32 = 997;
    /// WAIT_OBJECT_0 : WaitForSingleObject signalé (IO complète).
    const WAIT_OBJECT_0: u32 = 0;

    fn wide_path(port_name: &str) -> Vec<u16> {
        let path = format!("\\\\.\\{}", port_name);
        let mut v: Vec<u16> = path.encode_utf16().collect();
        v.push(0u16);
        v
    }

    /// Ouvre un handle COM (non-overlapped) pour le primer.
    fn open_handle_sync(port_name: &str) -> Option<HANDLE> {
        let wide = wide_path(port_name);
        unsafe {
            CreateFileW(
                PCWSTR(wide.as_ptr()),
                (GENERIC_READ.0 | GENERIC_WRITE.0) as u32,
                FILE_SHARE_MODE(0),
                None,
                FILE_CREATION_DISPOSITION(3), // OPEN_EXISTING
                FILE_FLAGS_AND_ATTRIBUTES(0), // non-overlapped pour le primer
                None,
            )
        }.ok()
    }

    /// Ouvre un handle COM overlapped (requis par usbser.sys pour la lecture).
    fn open_handle_overlapped(port_name: &str) -> Option<HANDLE> {
        let wide = wide_path(port_name);
        unsafe {
            CreateFileW(
                PCWSTR(wide.as_ptr()),
                (GENERIC_READ.0 | GENERIC_WRITE.0) as u32,
                FILE_SHARE_MODE(0),
                None,
                FILE_CREATION_DISPOSITION(3),
                FILE_FLAG_OVERLAPPED,         // OBLIGATOIRE pour usbser.sys
                None,
            )
        }.ok()
    }

    fn send_dtr_sequence(handle: HANDLE) {
        unsafe {
            EscapeCommFunction(handle, CLRDTR).ok();
            thread::sleep(Duration::from_millis(200));
            EscapeCommFunction(handle, SETDTR).ok();
            EscapeCommFunction(handle, SETRTS).ok();
            thread::sleep(Duration::from_millis(400));
            PurgeComm(handle, PURGE_RXCLEAR | PURGE_TXCLEAR).ok();
        }
    }

    /// Amorce préventive : ouvre (non-overlapped), envoie DTR/RTS, ferme.
    pub fn prime(port_name: &str) -> bool {
        let Some(handle) = open_handle_sync(port_name) else { return false };
        send_dtr_sequence(handle);
        unsafe { CloseHandle(handle).ok() };
        true
    }

    // ─────────────────────────────────────────────────────────────────────
    // RawCdcPort — overlapped IO (bypass total du DCB serialport)
    // ─────────────────────────────────────────────────────────────────────

    /// Handle série brut overlapped + event de complétion IO.
    pub struct RawCdcPort {
        pub handle: HANDLE,
        event: HANDLE, // event OVERLAPPED réutilisé sur chaque ReadFile
    }

    impl Drop for RawCdcPort {
        fn drop(&mut self) {
            unsafe {
                CloseHandle(self.handle).ok();
                CloseHandle(self.event).ok();
            }
        }
    }

    /// Ouvre le port CDC avec handle overlapped + SetCommState manuel.
    /// - FILE_FLAG_OVERLAPPED : usbser.sys route les données USB → buffer ReadFile
    /// - SetCommState sans GetCommState : configure le DCB (DTR_ENABLE, 8N1)
    pub fn open_raw(port_name: &str) -> Option<RawCdcPort> {
        use windows::Win32::Devices::Communication::{DCB, SetCommState};

        let handle = open_handle_overlapped(port_name)?;

        // Event de complétion IO (manuel-reset, non-signalé initialement)
        let event = unsafe { CreateEventW(None, true, false, None) }.ok()?;

        // SetCommTimeouts — pour overlapped IO les valeurs sont ignorées en pratique,
        // le timeout est géré par WaitForSingleObject. On les met à 0 (immédiat).
        let timeouts = COMMTIMEOUTS {
            ReadIntervalTimeout: 0xFFFF_FFFF, // MAXDWORD = retour immédiat si buffer vide
            ReadTotalTimeoutMultiplier: 0,
            ReadTotalTimeoutConstant: 0,
            WriteTotalTimeoutMultiplier: 0,
            WriteTotalTimeoutConstant: 5000,
        };
        if unsafe { SetCommTimeouts(handle, &timeouts) }.is_err() {
            unsafe { CloseHandle(handle).ok(); CloseHandle(event).ok() };
            return None;
        }

        // SetCommState manuel : configure le DCB sans passer par GetCommState.
        // _bitfield = fBinary(0x0001) | fDtrControl=ENABLE(0x0010) | fRtsControl=ENABLE(0x1000)
        let dcb = DCB {
            DCBlength: core::mem::size_of::<DCB>() as u32,
            BaudRate: 9600,
            _bitfield: 0x1011,
            wReserved: 0,
            XonLim: 0,
            XoffLim: 0,
            ByteSize: 8,
            Parity: windows::Win32::Devices::Communication::DCB_PARITY(0),
            StopBits: windows::Win32::Devices::Communication::DCB_STOP_BITS(0),
            ..Default::default()
        };
        // CRITIQUE : si SetCommState échoue, le DCB par défaut (DTR_CONTROL_HANDSHAKE
        // ou DISABLE) reste actif → DTR jamais asserté → la douchette refuse de
        // transmettre (signalé par 2 bips d'erreur côté scanner). Sur les firmwares
        // qui rejettent GetCommState (la raison même d'être de open_raw),
        // SetCommState rejette aussi très souvent. On préfère retourner None et
        // laisser l'orchestrateur retenter `serialport::open()` plus tard, plutôt
        // que de fournir un port "muet" pour toujours.
        if unsafe { SetCommState(handle, &dcb) }.is_err() {
            unsafe { CloseHandle(handle).ok(); CloseHandle(event).ok() };
            return None;
        }

        // SETDTR : sans DTR explicitement assertée, même un DCB correct ne suffit
        // pas — le scanner ne reçoit pas le signal "hôte prêt". Si l'IOCTL
        // EscapeCommFunction échoue, le port est inutilisable pour notre usage.
        if unsafe { EscapeCommFunction(handle, SETDTR) }.is_err() {
            unsafe { CloseHandle(handle).ok(); CloseHandle(event).ok() };
            return None;
        }

        unsafe {
            PurgeComm(handle, PURGE_RXCLEAR | PURGE_TXCLEAR).ok();
            // RTS plus tolérant : on le tente sans bloquer si refusé.
            EscapeCommFunction(handle, SETRTS).ok();
        }
        thread::sleep(Duration::from_millis(200));
        unsafe { PurgeComm(handle, PURGE_RXCLEAR | PURGE_TXCLEAR).ok() };

        Some(RawCdcPort { handle, event })
    }

    /// Lit un octet avec IO overlapped et timeout 100 ms :
    ///  - Ok(1)  = donnée reçue
    ///  - Ok(0)  = timeout (100 ms sans donnée)
    ///  - Err(_) = erreur I/O fatale
    pub fn read_byte(port: &RawCdcPort, buf: &mut [u8; 1]) -> std::io::Result<u32> {
        // Réinitialiser l'event avant chaque lecture
        unsafe { ResetEvent(port.event).ok() };

        let mut overlapped = OVERLAPPED {
            hEvent: port.event,
            ..Default::default()
        };

        let mut bytes_read: u32 = 0;
        match unsafe { ReadFile(port.handle, Some(buf.as_mut_slice()), Some(&mut bytes_read), Some(&mut overlapped)) } {
            Ok(()) => {
                // Complété synchroniquement (données déjà dans le buffer)
                return Ok(bytes_read);
            }
            Err(ref e) if e.code().0 as u32 == ERROR_IO_PENDING => {
                // IO en attente — normal pour overlapped, continuer
            }
            Err(e) => {
                return Err(std::io::Error::new(
                    std::io::ErrorKind::Other,
                    windows::core::Error::message(&e),
                ));
            }
        }

        // Attendre 100 ms la complétion de l'IO
        let wait = unsafe { WaitForSingleObject(port.event, 100) };

        if wait.0 == WAIT_OBJECT_0 {
            // IO complète — récupérer le résultat
            match unsafe { GetOverlappedResult(port.handle, &overlapped, &mut bytes_read, false) } {
                Ok(()) => Ok(bytes_read),
                Err(e) => Err(std::io::Error::new(
                    std::io::ErrorKind::Other,
                    windows::core::Error::message(&e),
                )),
            }
        } else {
            // Timeout — annuler l'IO pendante et attendre sa complétion
            unsafe { CancelIoEx(port.handle, Some(&overlapped)).ok() };
            let mut dummy: u32 = 0;
            unsafe { GetOverlappedResult(port.handle, &overlapped, &mut dummy, true).ok() };
            Ok(0) // 0 octet = timeout
        }
    }
}

#[cfg(not(target_os = "windows"))]
mod cdc_primer_stub_placeholder_remove_old {}

#[cfg(not(target_os = "windows"))]
mod cdc_primer {
    pub fn prime(_port_name: &str) -> bool { false }

    pub struct RawCdcPort;
    pub fn open_raw(_port_name: &str) -> Option<RawCdcPort> { None }
    pub fn read_byte(_port: &RawCdcPort, _buf: &mut [u8; 1]) -> std::io::Result<u32> {
        Err(std::io::Error::new(std::io::ErrorKind::Unsupported, "not supported"))
    }
}

/// ── Moniteur USB Windows ─────────────────────────────────────────────────────
///
/// Écoute `WM_DEVICECHANGE` pour détecter en temps réel l'arrivée/le retrait
/// d'un port COM. C'est le SEUL signal fiable indiquant que `usbser.sys` a
/// terminé l'initialisation du device et que `serialport::open()` réussira.
///
/// Sans ça, après un débranchement/rebranchement, `available_ports()` peut
/// renvoyer le port comme « présent » avant que le driver soit prêt → toute
/// tentative d'ouverture échoue avec `ERROR_GEN_FAILURE` (0x1F), et chaque
/// nouvelle tentative pollue la pile USB et empêche la récupération.
///
/// Avec WM_DEVICECHANGE : zéro tentative timée, ouverture déclenchée
/// uniquement à l'arrivée effective du device → succès au premier essai.
#[cfg(target_os = "windows")]
mod device_monitor {
    use std::sync::{Mutex, OnceLock};
    use std::thread;
    use tauri::{AppHandle, Emitter};
    use windows::Win32::Foundation::{HANDLE, HWND, LPARAM, LRESULT, WPARAM};
    use windows::Win32::System::LibraryLoader::GetModuleHandleW;
    use windows::Win32::UI::WindowsAndMessaging::{
        CreateWindowExW, DefWindowProcW, DispatchMessageW, GetMessageW, RegisterClassW,
        RegisterDeviceNotificationW, TranslateMessage, DEVICE_NOTIFY_WINDOW_HANDLE, HMENU,
        HWND_MESSAGE, MSG, WINDOW_EX_STYLE, WNDCLASSW, WS_OVERLAPPED,
    };
    use windows::core::{w, GUID};

    // Constantes Win32 — définies en local pour ne pas dépendre de la
    // version de la crate `windows` (les types DBT_* changent de module
    // selon les versions).
    const WM_DEVICECHANGE: u32 = 0x0219;
    const DBT_DEVICEARRIVAL: u32 = 0x8000;
    const DBT_DEVICEREMOVECOMPLETE: u32 = 0x8004;
    const DBT_DEVTYP_DEVICEINTERFACE: u32 = 0x0000_0005;

    /// `GUID_DEVINTERFACE_COMPORT` — classe d'interface des ports COM PnP.
    /// Filtre les notifications pour ne réagir qu'aux arrivées/retraits de
    /// ports COM (pas tous les périphériques USB du système).
    const COMPORT_GUID: GUID = GUID::from_u128(0x86E0D1E0_8089_11D0_9CE4_08003E301F73);

    #[repr(C)]
    struct DevBroadcastHdr {
        dbch_size: u32,
        dbch_devicetype: u32,
        dbch_reserved: u32,
    }

    #[repr(C)]
    struct DevBroadcastDeviceInterfaceW {
        dbcc_size: u32,
        dbcc_devicetype: u32,
        dbcc_reserved: u32,
        dbcc_classguid: GUID,
        dbcc_name: [u16; 1], // longueur variable, seul le 1er char compte pour l'enregistrement
    }

    static APP_HANDLE: OnceLock<Mutex<Option<AppHandle>>> = OnceLock::new();
    static INIT: OnceLock<()> = OnceLock::new();

    pub fn start(app: AppHandle) {
        let slot = APP_HANDLE.get_or_init(|| Mutex::new(None));
        if let Ok(mut g) = slot.lock() {
            *g = Some(app);
        }
        INIT.get_or_init(|| {
            thread::spawn(|| {
                if let Err(e) = run_message_loop() {
                    eprintln!("[device_monitor] échec boucle WM_DEVICECHANGE : {:?}", e);
                }
            });
        });
    }

    fn run_message_loop() -> windows::core::Result<()> {
        unsafe {
            let class_name = w!("PharmaSmartUsbMonitor");
            let instance = GetModuleHandleW(None)?;
            let wc = WNDCLASSW {
                lpfnWndProc: Some(wnd_proc),
                hInstance: instance.into(),
                lpszClassName: class_name,
                ..Default::default()
            };
            RegisterClassW(&wc);

            let hwnd = CreateWindowExW(
                WINDOW_EX_STYLE(0),
                class_name,
                class_name,
                WS_OVERLAPPED,
                0,
                0,
                0,
                0,
                Some(HWND_MESSAGE),
                Some(HMENU::default()),
                Some(instance.into()),
                None,
            )?;

            let filter = DevBroadcastDeviceInterfaceW {
                dbcc_size: std::mem::size_of::<DevBroadcastDeviceInterfaceW>() as u32,
                dbcc_devicetype: DBT_DEVTYP_DEVICEINTERFACE,
                dbcc_reserved: 0,
                dbcc_classguid: COMPORT_GUID,
                dbcc_name: [0; 1],
            };
            RegisterDeviceNotificationW(
                HANDLE(hwnd.0),
                &filter as *const _ as *const _,
                DEVICE_NOTIFY_WINDOW_HANDLE,
            )?;

            let mut msg = MSG::default();
            while GetMessageW(&mut msg, None, 0, 0).into() {
                let _ = TranslateMessage(&msg);
                DispatchMessageW(&msg);
            }
        }
        Ok(())
    }

    extern "system" fn wnd_proc(hwnd: HWND, msg: u32, wparam: WPARAM, lparam: LPARAM) -> LRESULT {
        if msg == WM_DEVICECHANGE && lparam.0 != 0 {
            let event_code = wparam.0 as u32;
            if event_code == DBT_DEVICEARRIVAL || event_code == DBT_DEVICEREMOVECOMPLETE {
                unsafe {
                    let header = lparam.0 as *const DevBroadcastHdr;
                    if (*header).dbch_devicetype == DBT_DEVTYP_DEVICEINTERFACE {
                        let event_name = if event_code == DBT_DEVICEARRIVAL {
                            "scan-usb-arrived"
                        } else {
                            "scan-usb-removed"
                        };
                        if let Some(slot) = APP_HANDLE.get() {
                            if let Ok(g) = slot.lock() {
                                if let Some(app) = g.as_ref() {
                                    app.emit(event_name, ()).ok();
                                }
                            }
                        }
                    }
                }
            }
        }
        unsafe { DefWindowProcW(hwnd, msg, wparam, lparam) }
    }
}

/// Démarre le moniteur Win32 `WM_DEVICECHANGE`. À appeler une fois au boot
/// dans le `setup` Tauri. No-op sur les autres plateformes.
#[cfg(target_os = "windows")]
pub fn start_device_monitor(app: tauri::AppHandle) {
    device_monitor::start(app);
}

#[cfg(not(target_os = "windows"))]
pub fn start_device_monitor(_app: tauri::AppHandle) {
    // À implémenter via udev (Linux) / IOKit (macOS) si besoin desktop multi-OS.
}

/// Drapeaux d'arrêt par port — permet plusieurs listeners simultanés et un arrêt ciblé.
static SCANNER_FLAGS: LazyLock<Mutex<HashMap<String, Arc<AtomicBool>>>> =
    LazyLock::new(|| Mutex::new(HashMap::new()));

/// Heartbeat : toutes les HEARTBEAT_TICKS × 100 ms (≈ 2 s) on vérifie l'état du
/// port — détecte un débranchement OU une ré-énumération USB silencieuse via
/// la transition absence → présence (handle Windows devenu obsolète).
const HEARTBEAT_TICKS: u32 = 20;

/// Codes d'erreur typés émis sur l'event `scan-error`.
#[derive(Serialize, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum ScanErrorCode {
    /// Le port n'a pas pu être ouvert (occupé, droits, mauvais nom).
    OpenFailed,
    /// Erreur d'I/O en cours de lecture (câble, USB, driver).
    Disconnected,
    /// Le port n'est plus listé par le système — déconnexion silencieuse.
    PortDropped,
}

/// Payload de l'event `scan-error` — remplace l'ancien message string opaque.
#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ScanErrorPayload {
    pub code: ScanErrorCode,
    pub port_name: String,
    pub details: String,
}

fn emit_scan_error(
    app: &AppHandle,
    code: ScanErrorCode,
    port_name: &str,
    details: impl Into<String>,
) {
    let payload = ScanErrorPayload {
        code,
        port_name: port_name.to_string(),
        details: details.into(),
    };
    app.emit("scan-error", payload).ok();
}

/// Vérifie l'état du port et détecte une invalidation du handle.
///
/// Logique générique (indépendante du modèle de douchette) :
///   - Sur débranchement/rebranchement, le port disparaît brièvement de
///     `available_ports()` puis réapparaît avec une nouvelle énumération USB.
///     Le handle Windows que le thread détient pointe alors sur l'ancienne
///     instance et `port.read()` ne reçoit plus jamais de données — sans
///     pour autant retourner d'erreur (`usbser.sys` retourne `TimedOut`).
///
///   - Vérifier seulement la présence du nom de port ne suffit donc pas :
///     il faut détecter la transition absence → présence.
///
/// États retournés via le tuple (was_absent_avant, présence_actuelle) :
///   - (false, false) → première absence : on attend confirmation.
///   - (true,  false) → 2 ticks d'absence : débranchement confirmé.
///   - (true,  true)  → réapparu après absence : ré-énumération, handle obsolète.
///   - (false, true)  → nominal.
#[cfg(feature = "serialport")]
fn check_port_health(port_name: &str, was_absent: &mut bool) -> Option<&'static str> {
    let present = serialport::available_ports()
        .map(|ports| ports.iter().any(|p| p.port_name == port_name))
        .unwrap_or(true); // En cas d'échec listing, on ne conclut pas
    match (*was_absent, present) {
        (false, false) => {
            *was_absent = true;
            None
        }
        (true, false) => Some("Port absent du système (débranchement)"),
        (true, true) => {
            *was_absent = false;
            Some("Ré-énumération USB détectée — handle obsolète")
        }
        (false, true) => None,
    }
}

/// Informations système du poste (hostname + IP locale).
#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct SystemInfo {
    pub hostname: String,
    pub local_ip: String,
}

/// Récupère le nom de la machine et l'adresse IP locale.
#[tauri::command]
pub fn get_system_info() -> SystemInfo {
    let hostname = hostname::get()
        .map(|h| h.to_string_lossy().to_string())
        .unwrap_or_else(|_| "UNKNOWN".to_string());

    // Ouvre une socket UDP vers une adresse publique (pas de données envoyées)
    // pour déterminer l'IP locale utilisée par la route par défaut.
    let local_ip = UdpSocket::bind("0.0.0.0:0")
        .and_then(|socket| {
            socket.connect("8.8.8.8:80")?;
            socket.local_addr()
        })
        .map(|addr| addr.ip().to_string())
        .unwrap_or_else(|_| "127.0.0.1".to_string());

    SystemInfo { hostname, local_ip }
}

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct SerialPortDetail {
    pub port_name: String,
    pub vid: Option<u16>,
    pub pid: Option<u16>,
    pub manufacturer: Option<String>,
    pub product: Option<String>,
    pub serial_number: Option<String>,
    pub suggested_role: Option<String>, // "scanner" | "display" | "printer" | null
    pub generic_adapter: bool,          // true si adaptateur USB-série générique (FTDI, CH340, etc.)
    pub chipset: Option<String>,        // Nom du chipset déduit du VID (ex: "CH340", "FTDI", "CP210x")
}

/// Liste tous les ports série avec métadonnées USB pour aider l'utilisateur
/// à attribuer les rôles (scanner, afficheur, imprimante).
#[tauri::command]
pub fn list_serial_ports_detailed() -> Vec<SerialPortDetail> {
    #[cfg(feature = "serialport")]
    {
        let Ok(ports) = serialport::available_ports() else {
            return vec![];
        };
        ports
            .iter()
            .map(|p| {
                let (vid, pid, manufacturer, product, serial_number, suggested_role) =
                    if let serialport::SerialPortType::UsbPort(info) = &p.port_type {
                        let role = suggest_role(info);
                        (
                            Some(info.vid),
                            Some(info.pid),
                            info.manufacturer.clone(),
                            clean_product_name(info.product.as_deref(), &p.port_name),
                            info.serial_number.clone(),
                            role,
                        )
                    } else {
                        (None, None, None, None, None, None)
                    };
                let generic_adapter = is_generic_adapter(vid);
                let chipset = chipset_name(vid);
                // Remplacer le fabricant si c'est le driver Windows générique ("Microsoft", "wch.cn", "(Standard port types)")
                let manufacturer = resolve_manufacturer(manufacturer, &chipset);
                SerialPortDetail {
                    port_name: p.port_name.clone(),
                    vid,
                    pid,
                    manufacturer,
                    product,
                    serial_number,
                    suggested_role,
                    generic_adapter,
                    chipset,
                }
            })
            .collect()
    }

    #[cfg(not(feature = "serialport"))]
    {
        vec![]
    }
}

/// Démarre la lecture du scanner en mode USB CDC sur le port COM donné.
/// Plusieurs listeners peuvent coexister sur des ports différents.
/// Si un listener tourne déjà sur ce port, il est arrêté avant d'en démarrer un nouveau.
///
/// Émet :
///  - `event_name` (`"scan-reception"`, `"scan-vente"`…) sur scan complet (payload : code lu)
///  - `scan-error` ([`ScanErrorPayload`]) sur erreur fatale ou port disparu
#[tauri::command]
pub async fn start_scanner_listener(
    app: AppHandle,
    port_name: String,
    baud_rate: u32,
    event_name: String,
) -> Result<(), String> {
    // Crée un nouveau drapeau pour ce port. Si un précédent existe, on le force à false
    // pour signaler à l'ancien thread de se terminer.
    let new_flag = Arc::new(AtomicBool::new(true));
    {
        let mut flags = SCANNER_FLAGS.lock().unwrap();
        if let Some(old) = flags.insert(port_name.clone(), Arc::clone(&new_flag)) {
            old.store(false, Ordering::SeqCst);
        }
    }

    #[cfg(feature = "serialport")]
    {
        let flag = Arc::clone(&new_flag);
        thread::spawn(move || {
            // ── Stratégie d'ouverture robuste — compatible toute douchette USB CDC ──
            //
            // Trois classes d'erreurs à l'ouverture (Windows) :
            //
            //  • ERROR_ACCESS_DENIED (5) : port occupé par un autre processus.
            //    Retries rapides (250 ms).
            //
            //  • ERROR_GEN_FAILURE (31) : pile CDC non initialisée ou USB Selective
            //    Suspend. Commun sur les douchettes à firmware économique (tout fabricant).
            //    Traitement :
            //      1. Primer CDC (DTR toggle via CreateFileW brut) — tente de réveiller
            //         la pile CDC sans passer par GetCommState.
            //      2. Si encore en échec : retries lents (2 s) pour laisser le temps
            //         au réveil USB (Selective Suspend ~ 1-2 s).
            //
            //  • Autres erreurs : retries rapides × 3, puis lents.
            //
            // Stratégie d'ouverture MINIMALISTE pour ne pas polluer la pile USB.
            //
            // Chaque appel à `serialport::new(...).open()` envoie au minimum :
            // CreateFileW + GetCommState (+ CloseHandle si échec). Sur un firmware
            // CDC en état GEN_FAILURE post-rebranchement, ces transferts USB de
            // contrôle entretiennent l'état bloqué du device et empêchent le
            // pilote `usbser.sys` de finaliser son initialisation.
            //
            // Donc : UN essai. Si GEN_FAILURE → primer DTR (1 toggle USB) + UN
            // dernier essai. Pas de back-off interne, pas de fallback open_raw
            // (qui ne fonctionne de toute façon pas si SetCommState échoue).
            // L'orchestrateur Angular planifie les retries avec de longs délais
            // (1-5 min) et le déclencheur primaire reste WM_DEVICECHANGE.
            const GEN_FAILURE_FRAGMENTS: &[&str] = &[
                "ne fonctionne pas",
                "not functioning",
                "general failure",
            ];

            let mut opened_port = None;
            let mut last_open_err = String::new();
            let mut hit_gen_failure = false;

            for attempt in 0..2 {
                match serialport::new(&port_name, baud_rate)
                    .data_bits(serialport::DataBits::Eight)
                    .flow_control(serialport::FlowControl::None)
                    .parity(serialport::Parity::None)
                    .stop_bits(serialport::StopBits::One)
                    .timeout(Duration::from_millis(100))
                    .open()
                {
                    Ok(p) => { opened_port = Some(p); break; }
                    Err(e) => {
                        last_open_err = e.to_string();
                        if !flag.load(Ordering::SeqCst) { break; }

                        let is_gen_failure = GEN_FAILURE_FRAGMENTS
                            .iter()
                            .any(|frag| last_open_err.to_lowercase().contains(frag));

                        // Premier essai en GEN_FAILURE → primer DTR + 1 retry.
                        // Tout autre cas (ou 2ème essai) → on abandonne immédiatement
                        // et on laisse l'orchestrateur replanifier proprement.
                        if attempt == 0 && is_gen_failure {
                            hit_gen_failure = true;
                            cdc_primer::prime(&port_name); // ~600 ms toggle DTR
                        } else {
                            break;
                        }
                    }
                }
            }
            let _ = hit_gen_failure; // garder l'info pour de futurs diagnostics

            let mut port = match opened_port {
                Some(p) => p,
                None => {
                    // ── Fallback RawCdcPort (Windows) ──────────────────────────────
                    // serialport::open() a échoué sur tous les retries avec GEN_FAILURE.
                    // GetCommState ne répond pas sur ce firmware.
                    // Tentative avec CreateFileW + SetCommTimeouts uniquement (sans DCB).
                    #[cfg(target_os = "windows")]
                    if hit_gen_failure {
                        if let Some(raw_port) = cdc_primer::open_raw(&port_name) {
                            // Mode CDC brut actif : notifier Angular
                            app.emit("scan-raw-mode", &port_name).ok();
                            // ── Boucle de lecture brute (sans serialport) ─────────
                            let mut buf = Vec::new();
                            let mut idle_ticks: u32 = 0;
                            let mut was_absent = false;
                            loop {
                                if !flag.load(Ordering::SeqCst) { break; }
                                let mut byte = [0u8; 1];
                                match cdc_primer::read_byte(&raw_port, &mut byte) {
                                    Ok(1) => {
                                        idle_ticks = 0;
                                        was_absent = false; // handle prouve sa validité
                                        // Premier octet reçu → confirmer que ReadFile fonctionne
                                        if buf.is_empty() && byte[0] != b'\r' && byte[0] != b'\n' {
                                            app.emit("scan-raw-byte", byte[0]).ok();
                                        }
                                        if byte[0] == b'\r' || byte[0] == b'\n' {
                                            if !buf.is_empty() {
                                                let code = String::from_utf8_lossy(&buf).trim().to_string();
                                                if !code.is_empty() { app.emit(&event_name, code).ok(); }
                                                buf.clear();
                                            }
                                        } else {
                                            buf.push(byte[0]);
                                        }
                                    }
                                    Ok(_) => {
                                        // Timeout 100 ms : flush buffer si données partielles
                                        if !buf.is_empty() {
                                            let code = String::from_utf8_lossy(&buf).trim().to_string();
                                            if !code.is_empty() { app.emit(&event_name, code).ok(); }
                                            buf.clear();
                                        }
                                        idle_ticks = idle_ticks.saturating_add(1);
                                        if idle_ticks >= HEARTBEAT_TICKS {
                                            idle_ticks = 0;
                                            if let Some(reason) = check_port_health(&port_name, &mut was_absent) {
                                                emit_scan_error(&app, ScanErrorCode::PortDropped, &port_name, reason);
                                                break;
                                            }
                                        }
                                    }
                                    Err(e) => {
                                        emit_scan_error(&app, ScanErrorCode::Disconnected, &port_name, e.to_string());
                                        break;
                                    }
                                }
                            }
                            flag.store(false, Ordering::SeqCst);
                            let mut flags = SCANNER_FLAGS.lock().unwrap();
                            if let Some(current) = flags.get(&port_name) {
                                if Arc::ptr_eq(current, &flag) { flags.remove(&port_name); }
                            }
                            return;
                        }
                    }
                    // Aucune méthode n'a fonctionné → signaler l'échec
                    flag.store(false, Ordering::SeqCst);
                    SCANNER_FLAGS.lock().unwrap().remove(&port_name);
                    emit_scan_error(&app, ScanErrorCode::OpenFailed, &port_name, last_open_err);
                    return;
                }
            };

            // ── Vider le buffer UART avant toute initialisation ──────────────
            port.clear(serialport::ClearBuffer::All).ok();

            // ── Séquence d'initialisation CDC (RS-232 standard) ─────────────
            // Envoyer les signaux de contrôle pour indiquer que l'hôte est prêt.
            // La plupart des douchettes USB CDC (quel que soit le fabricant)
            // attendent ces signaux avant de commencer à transmettre des données.
            //
            //   DTR=false → signal "hôte non prêt"  (réinitialise l'état du scanner)
            //   DTR=true  → signal "hôte prêt"       (déclenche la transmission)
            //   RTS=true  → flux de données autorisé
            //   Attente   → délai de stabilisation firmware (variable selon le modèle)
            //
            // Note : certains scanners ignorent DTR/RTS (ex: mode HID bridgé vers CDC).
            // Ces signaux sont envoyés de façon non bloquante (.ok()) et n'échouent
            // pas si le firmware ne les supporte pas.
            port.write_data_terminal_ready(false).ok();
            thread::sleep(Duration::from_millis(200));
            port.write_data_terminal_ready(true).ok();
            port.write_request_to_send(true).ok();
            port.clear(serialport::ClearBuffer::Input).ok();
            thread::sleep(Duration::from_millis(400));

            let mut buf = Vec::new();
            let mut idle_ticks: u32 = 0;
            // Tracker pour la détection de ré-énumération USB (cf. check_port_health).
            let mut was_absent = false;
            loop {
                if !flag.load(Ordering::SeqCst) {
                    break;
                }
                let mut byte = [0u8; 1];
                match port.read(&mut byte) {
                    Ok(1) => {
                        idle_ticks = 0;
                        was_absent = false; // Le handle prouve sa validité en lisant.
                        if byte[0] == b'\r' || byte[0] == b'\n' {
                            if !buf.is_empty() {
                                let code = String::from_utf8_lossy(&buf).trim().to_string();
                                if !code.is_empty() {
                                    app.emit(&event_name, code).ok();
                                }
                                buf.clear();
                            }
                        } else {
                            buf.push(byte[0]);
                        }
                    }
                    Err(ref e) if e.kind() == std::io::ErrorKind::TimedOut => {
                        // Silence de 100 ms : si le buffer contient des données, le scan est terminé
                        // (scanners USB COM qui n'envoient pas de terminateur CR/LF)
                        if !buf.is_empty() {
                            let code = String::from_utf8_lossy(&buf).trim().to_string();
                            if !code.is_empty() {
                                app.emit(&event_name, code).ok();
                            }
                            buf.clear();
                            idle_ticks = 0;
                        } else {
                            // Heartbeat (~2 s) : détecte un débranchement OU une
                            // ré-énumération USB silencieuse (handle obsolète).
                            idle_ticks = idle_ticks.saturating_add(1);
                            if idle_ticks >= HEARTBEAT_TICKS {
                                idle_ticks = 0;
                                if let Some(reason) = check_port_health(&port_name, &mut was_absent) {
                                    emit_scan_error(
                                        &app,
                                        ScanErrorCode::PortDropped,
                                        &port_name,
                                        reason,
                                    );
                                    break;
                                }
                            }
                        }
                    }
                    Err(e) => {
                        // Erreur fatale (déconnexion USB, etc.)
                        emit_scan_error(
                            &app,
                            ScanErrorCode::Disconnected,
                            &port_name,
                            e.to_string(),
                        );
                        break;
                    }
                    _ => {}
                }
            }
            flag.store(false, Ordering::SeqCst);
            // Ne retirer du map que si c'est toujours notre flag (un nouveau start a pu nous remplacer)
            let mut flags = SCANNER_FLAGS.lock().unwrap();
            if let Some(current) = flags.get(&port_name) {
                if Arc::ptr_eq(current, &flag) {
                    flags.remove(&port_name);
                }
            }
        });
        Ok(())
    }

    #[cfg(not(feature = "serialport"))]
    {
        let _ = (app, baud_rate, event_name);
        SCANNER_FLAGS.lock().unwrap().remove(&port_name);
        Err("Serial port support not enabled".to_string())
    }
}

/// Arrête un listener scanner.
/// Si `port_name` est fourni, stoppe seulement ce listener.
/// Sinon stoppe tous les listeners actifs (compat ascendante).
#[tauri::command]
pub fn stop_scanner_listener(port_name: Option<String>) {
    let mut flags = SCANNER_FLAGS.lock().unwrap();
    match port_name {
        Some(p) => {
            if let Some(flag) = flags.remove(&p) {
                flag.store(false, Ordering::SeqCst);
            }
        }
        None => {
            for (_, flag) in flags.drain() {
                flag.store(false, Ordering::SeqCst);
            }
        }
    }
}

/// Vérifie si un port COM spécifique est actuellement connecté (présent dans le système).
/// Utilisé par le frontend pour déterminer si le périphérique configuré est branché.
#[tauri::command]
pub fn is_port_connected(port_name: String) -> bool {
    #[cfg(feature = "serialport")]
    {
        serialport::available_ports()
            .unwrap_or_default()
            .iter()
            .any(|p| p.port_name == port_name)
    }

    #[cfg(not(feature = "serialport"))]
    {
        let _ = port_name;
        false
    }
}

/// Vérifie la connectivité de plusieurs ports en batch.
/// Retourne pour chaque port demandé s'il est connecté ou non.
#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct PortConnectionStatus {
    pub port_name: String,
    pub connected: bool,
    pub device_info: Option<SerialPortDetail>,
}

#[tauri::command]
pub fn check_ports_connection(port_names: Vec<String>) -> Vec<PortConnectionStatus> {
    #[cfg(feature = "serialport")]
    {
        let available = serialport::available_ports().unwrap_or_default();
        port_names
            .iter()
            .map(|name| {
                let found = available.iter().find(|p| &p.port_name == name);
                PortConnectionStatus {
                    port_name: name.clone(),
                    connected: found.is_some(),
                    device_info: found.map(|p| {
                        let (vid, pid, manufacturer, product, serial_number, suggested_role) =
                            if let serialport::SerialPortType::UsbPort(info) = &p.port_type {
                                let role = suggest_role(info);
                                (
                                    Some(info.vid),
                                    Some(info.pid),
                                    info.manufacturer.clone(),
                                    clean_product_name(info.product.as_deref(), &p.port_name),
                                    info.serial_number.clone(),
                                    role,
                                )
                            } else {
                                (None, None, None, None, None, None)
                            };
                        let generic_adapter = is_generic_adapter(vid);
                        let chipset = chipset_name(vid);
                        let manufacturer = resolve_manufacturer(manufacturer, &chipset);
                        SerialPortDetail {
                            port_name: p.port_name.clone(),
                            vid,
                            pid,
                            manufacturer,
                            product,
                            serial_number,
                            suggested_role,
                            generic_adapter,
                            chipset,
                        }
                    }),
                }
            })
            .collect()
    }

    #[cfg(not(feature = "serialport"))]
    {
        let _ = port_names;
        vec![]
    }
}

/// Envoie un message sur l'afficheur client (port série) — pour le test depuis SC-04.
#[tauri::command]
pub fn send_to_display(port_name: String, message: String, baud_rate: u32) -> Result<(), String> {
    #[cfg(feature = "serialport")]
    {
        use std::io::Write;
        let mut port = serialport::new(&port_name, baud_rate)
            .timeout(Duration::from_millis(500))
            .open()
            .map_err(|e| format!("Impossible d'ouvrir {} : {}", port_name, e))?;
        // Protocole afficheur standard : ESC @ (init) + texte + CR
        let payload = format!("\x1B@{}\r", message);
        port.write_all(payload.as_bytes())
            .map_err(|e| format!("Erreur écriture afficheur : {}", e))?;
        Ok(())
    }

    #[cfg(not(feature = "serialport"))]
    {
        Err("Serial port support not enabled".to_string())
    }
}

/// Détecte si le VID correspond à un adaptateur USB-série générique (FTDI, CH340, Prolific, CP210x).
/// Ces adaptateurs peuvent avoir n'importe quel périphérique derrière → rôle non déterminable automatiquement.
#[cfg(feature = "serialport")]
fn is_generic_adapter(vid: Option<u16>) -> bool {
    matches!(vid, Some(0x0403) | Some(0x067B) | Some(0x1A86) | Some(0x10C4))
}

#[cfg(not(feature = "serialport"))]
fn is_generic_adapter(_vid: Option<u16>) -> bool {
    false
}

/// Nettoie le nom du produit retourné par Windows.
/// Windows ajoute souvent "(COMx)" à la fin du nom → on le retire.
/// Exemples :
///   "Périphérique série USB (COM7)" → "Périphérique série USB"
///   "USB-SERIAL CH340 (COM3)" → "USB-SERIAL CH340"
#[cfg(feature = "serialport")]
fn clean_product_name(product: Option<&str>, port_name: &str) -> Option<String> {
    let raw = product?;
    // Retirer le suffixe "(COMx)" qui est redondant avec port_name
    let cleaned = if let Some(idx) = raw.rfind('(') {
        let before = raw[..idx].trim();
        if raw[idx..].to_uppercase().contains("COM") {
            before.to_string()
        } else {
            raw.to_string()
        }
    } else {
        raw.to_string()
    };
    // Retirer le port_name s'il apparait au début (ex: "COM7Périphérique...")
    let cleaned = if cleaned.starts_with(port_name) {
        cleaned[port_name.len()..].trim().to_string()
    } else {
        cleaned
    };
    if cleaned.is_empty() {
        None
    } else {
        Some(cleaned)
    }
}

#[cfg(not(feature = "serialport"))]
fn clean_product_name(_product: Option<&str>, _port_name: &str) -> Option<String> {
    None
}

/// Remplace le fabricant signalé par le driver Windows (ex: "Microsoft") par le vrai
/// fabricant déduit du VID USB quand il est connu.
fn resolve_manufacturer(reported: Option<String>, chipset: &Option<String>) -> Option<String> {
    let reported_lower = reported.as_deref().unwrap_or("").to_lowercase();
    // Si le fabricant reporté est le driver Windows générique ou vide, utiliser le chipset
    if reported_lower.is_empty()
        || reported_lower == "microsoft"
        || reported_lower.contains("standard")
        || reported_lower == "wch.cn"
        || reported_lower == "(standard port types)"
    {
        chipset.clone().or(reported)
    } else {
        reported
    }
}

/// Retourne le nom lisible du chipset USB-série d'après le Vendor ID.
/// Utile pour identifier un adaptateur générique.
fn chipset_name(vid: Option<u16>) -> Option<String> {
    match vid? {
        0x0403 => Some("FTDI".to_string()),
        0x067B => Some("Prolific PL2303".to_string()),
        0x1A86 => Some("CH340/CH341".to_string()),
        0x10C4 => Some("CP210x (Silicon Labs)".to_string()),
        0x28E9 => Some("Netum".to_string()),
        0x0C2E => Some("Honeywell".to_string()),
        0x05E0 | 0x0536 => Some("Zebra/Symbol".to_string()),
        0x05F9 => Some("Datalogic".to_string()),
        0x1EAB => Some("Newland".to_string()),
        0x065A => Some("Opticon".to_string()),
        0x27DD => Some("Mindeo".to_string()),
        0x04B8 => Some("Epson".to_string()),
        0x0519 => Some("Star Micronics".to_string()),
        0x1504 => Some("Bixolon".to_string()),
        0x1D90 => Some("Citizen".to_string()),
        0x0DD4 => Some("Logic Controls".to_string()),
        0x0D3A => Some("Posiflex".to_string()),
        _ => None,
    }
}

#[cfg(feature = "serialport")]
fn suggest_role(info: &serialport::UsbPortInfo) -> Option<String> {
    // ── 1. Détection par VID/PID (la plus fiable) ──────────────────────────
    if let Some(role) = suggest_role_by_vid_pid(info.vid, info.pid) {
        return Some(role);
    }

    // ── 2. Détection par nom produit / fabricant ───────────────────────────
    let product = info.product.as_deref().unwrap_or("").to_lowercase();
    let manufacturer = info.manufacturer.as_deref().unwrap_or("").to_lowercase();

    // Scanner / Douchette
    if product.contains("scan")
        || product.contains("barcode")
        || product.contains("reader")
        || product.contains("voyager")
        || product.contains("xenon")
        || product.contains("ds22")
        || product.contains("ds94")
        || product.contains("ls22")
        || product.contains("heron")
        || product.contains("gryphon")
        || product.contains("quickscan")
        || product.contains("magellan")
        || product.contains("powerscan")
        || product.contains("1900")
        || product.contains("1950")
        || product.contains("hr22")
        || product.contains("nls-")
        || product.contains("fm43")
    {
        return Some("scanner".into());
    }

    // Afficheur client
    if product.contains("display")
        || product.contains("vfd")
        || product.contains("customer")
        || product.contains("bcd")
        || product.contains("cd-")
        || product.contains("pole")
        || product.contains("ld2")
        || product.contains("dm-d")
    {
        return Some("display".into());
    }

    // Imprimante thermique
    if product.contains("printer")
        || product.contains("receipt")
        || product.contains("tm-t")
        || product.contains("tm-m")
        || product.contains("pos")
        || product.contains("rp-")
        || product.contains("ct-s")
        || product.contains("srp-")
        || product.contains("xp-")
        || product.contains("rongta")
    {
        return Some("printer".into());
    }

    // Heuristique fabricant — scanners
    if manufacturer.contains("honeywell")
        || manufacturer.contains("zebra")
        || manufacturer.contains("datalogic")
        || manufacturer.contains("newland")
        || manufacturer.contains("opticon")
        || manufacturer.contains("mindeo")
        || manufacturer.contains("unitech")
        || manufacturer.contains("cino")
        || manufacturer.contains("socket mobile")
    {
        return Some("scanner".into());
    }

    // Heuristique fabricant — afficheurs
    if manufacturer.contains("bixolon") && product.contains("display")
        || manufacturer.contains("partner tech")
        || manufacturer.contains("logic controls")
        || manufacturer.contains("posiflex") && product.is_empty()
    {
        return Some("display".into());
    }

    // Heuristique fabricant — imprimantes
    if manufacturer.contains("epson") && (product.is_empty() || product.contains("tm"))
        || manufacturer.contains("star micronics")
        || manufacturer.contains("bixolon") && !product.contains("display")
        || manufacturer.contains("citizen") && product.is_empty()
        || manufacturer.contains("rongta")
        || manufacturer.contains("xprinter")
    {
        return Some("printer".into());
    }

    // ── 3. Aucune correspondance → None (l'utilisateur assigne manuellement) ──
    None
}

/// Détection par Vendor ID / Product ID USB.
/// Plus fiable que les noms (qui peuvent être absents ou génériques).
#[cfg(feature = "serialport")]
fn suggest_role_by_vid_pid(vid: u16, pid: u16) -> Option<String> {
    match (vid, pid) {
        // ── Scanners ──────────────────────────────────────────────────────
        // Honeywell (anciennement Metrologic)
        (0x0C2E, _) => Some("scanner".into()),
        // Zebra / Symbol / Motorola Solutions
        (0x05E0, _) | (0x0536, _) => Some("scanner".into()),
        // Datalogic
        (0x05F9, _) => Some("scanner".into()),
        // Newland
        (0x1EAB, _) => Some("scanner".into()),
        // Opticon
        (0x065A, _) => Some("scanner".into()),
        // Mindeo
        (0x27DD, _) => Some("scanner".into()),
        // Netum / GD Microelectronics (GD32 MCU utilisé par Netum, TaoTronics, Inateck, etc.)
        (0x28E9, _) => Some("scanner".into()),
        // Tera / WoneNice / autres scanners chinois sur MCU HID-CDC
        (0x0483, 0x5740) => Some("scanner".into()),

        // ── Imprimantes thermiques ────────────────────────────────────────
        // Epson
        (0x04B8, _) => Some("printer".into()),
        // Star Micronics
        (0x0519, _) => Some("printer".into()),
        // Bixolon
        (0x1504, _) => Some("printer".into()),
        // Citizen
        (0x1D90, _) => Some("printer".into()),

        // ── Afficheurs ────────────────────────────────────────────────────
        // Logic Controls (Bematech)
        (0x0DD4, _) => Some("display".into()),
        // Posiflex
        (0x0D3A, _) => Some("display".into()),

        // ── Adaptateurs USB-Série génériques (rôle inconnu) ───────────────
        // FTDI, Prolific, CH340, CP210x → pas d'heuristique possible
        (0x0403, _) | (0x067B, _) | (0x1A86, _) | (0x10C4, _) => None,

        _ => None,
    }
}
