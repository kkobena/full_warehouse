use serde::{Deserialize, Serialize};
use tauri::command;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PrinterInfo {
    pub name: String,
    #[serde(rename = "isDefault")]
    pub is_default: bool,
}

/// Get list of available printers on the system
#[command]
pub async fn get_printers() -> Result<Vec<PrinterInfo>, String> {
    #[cfg(target_os = "windows")]
    {
        windows_printer::get_printers_windows()
    }

    #[cfg(not(target_os = "windows"))]
    {
        Err("Printer detection not implemented for this platform".to_string())
    }
}

/// Print base64-encoded PNG image to specified printer
#[command]
pub async fn print_image(image_data: String, printer_name: String) -> Result<(), String> {
    println!("Printing to printer: {}", printer_name);

    // Decode base64 image data using the new API
    use base64::Engine;
    let image_bytes = base64::engine::general_purpose::STANDARD
        .decode(&image_data)
        .map_err(|e| format!("Failed to decode base64 image: {}", e))?;

    println!("Decoded {} bytes of image data", image_bytes.len());

    #[cfg(target_os = "windows")]
    {
        windows_printer::print_image_windows(&image_bytes, &printer_name)
    }

    #[cfg(not(target_os = "windows"))]
    {
        Err("Printing not implemented for this platform".to_string())
    }
}

/// Print raw ESC/POS commands directly to thermal printer
/// This is much more efficient than PNG printing - smaller payload, faster execution
#[command]
pub async fn print_escpos(escpos_data: String, printer_name: String) -> Result<(), String> {
    println!("Printing ESC/POS data to printer: {}", printer_name);

    // Decode base64 ESC/POS data
    use base64::Engine;
    let escpos_bytes = base64::engine::general_purpose::STANDARD
        .decode(&escpos_data)
        .map_err(|e| format!("Failed to decode base64 ESC/POS data: {}", e))?;

    println!("Decoded {} bytes of ESC/POS data", escpos_bytes.len());

    #[cfg(target_os = "windows")]
    {
        windows_printer::send_raw_to_printer(&escpos_bytes, &printer_name)
    }

    #[cfg(not(target_os = "windows"))]
    {
        Err("ESC/POS printing not implemented for this platform".to_string())
    }
}

#[cfg(target_os = "windows")]
mod windows_printer {
    use super::PrinterInfo;
    use std::ffi::OsStr;
    use std::os::windows::ffi::OsStrExt;
    use windows::core::{PCWSTR, PWSTR};
    use windows::Win32::Graphics::Printing::{
        EnumPrintersW, GetDefaultPrinterW,
        OpenPrinterW, ClosePrinter, StartDocPrinterW, EndDocPrinter,
        StartPagePrinter, EndPagePrinter, WritePrinter, DOC_INFO_1W,
        PRINTER_ENUM_LOCAL, PRINTER_INFO_2W, PRINTER_HANDLE,
    };

    /// Get list of available printers on Windows
    pub fn get_printers_windows() -> Result<Vec<PrinterInfo>, String> {
        unsafe {
            let mut printers = Vec::new();
            let mut bytes_needed: u32 = 0;
            let mut printer_count: u32 = 0;

            // First call to get required buffer size
            let _ = EnumPrintersW(
                PRINTER_ENUM_LOCAL,
                PCWSTR::null(),
                2, // PRINTER_INFO_2
                None,
                &mut bytes_needed,
                &mut printer_count,
            );

            if bytes_needed == 0 {
                return Ok(Vec::new());
            }

            // Allocate buffer and get printer info
            let mut buffer = vec![0u8; bytes_needed as usize];
            let result = EnumPrintersW(
                PRINTER_ENUM_LOCAL,
                PCWSTR::null(),
                2,
                Some(buffer.as_mut_slice()),
                &mut bytes_needed,
                &mut printer_count,
            );

            if result.is_err() {
                return Err("Failed to enumerate printers".to_string());
            }

            // Get default printer name
            let default_printer = get_default_printer_name();

            // Parse printer info
            let printer_info_array = buffer.as_ptr() as *const PRINTER_INFO_2W;

            for i in 0..printer_count {
                let printer_info = &*printer_info_array.add(i as usize);
                let printer_name = pwstr_to_string_from_pwstr(printer_info.pPrinterName);

                let is_default = if let Some(ref default) = default_printer {
                    &printer_name == default
                } else {
                    false
                };

                printers.push(PrinterInfo {
                    name: printer_name,
                    is_default,
                });
            }

            Ok(printers)
        }
    }

    /// Get the default printer name
    fn get_default_printer_name() -> Option<String> {
        unsafe {
            let mut size: u32 = 0;

            // Get required buffer size
            let _ = GetDefaultPrinterW(None, &mut size);

            if size == 0 {
                return None;
            }

            // Allocate buffer and get default printer
            let mut buffer = vec![0u16; size as usize];
            let pwstr = PWSTR(buffer.as_mut_ptr());
            let result = GetDefaultPrinterW(Some(pwstr), &mut size);

            if result.as_bool() {
                Some(pwstr_to_string(PCWSTR(buffer.as_ptr())))
            } else {
                None
            }
        }
    }

    /// Print PNG image to Windows printer
    /// Optimized for memory usage with background cleanup
    pub fn print_image_windows(image_bytes: &[u8], printer_name: &str) -> Result<(), String> {
        println!("Preparing to print to: {}", printer_name);

        // Check if this is a thermal POS printer (common thermal printer names)
        let printer_lower = printer_name.to_lowercase();
        let is_thermal_printer = printer_lower.contains("tm-")      // EPSON TM series
            || printer_lower.contains("rp-")                         // STAR RP series
            || printer_lower.contains("thermal")
            || printer_lower.contains("pos")
            || printer_lower.contains("receipt");

        if is_thermal_printer {
            println!("Detected thermal POS printer, using direct printing");
            return print_to_thermal_printer(image_bytes, printer_name);
        }

        // Use PowerShell method for regular printers (PDF, laser, inkjet)
        print_via_powershell(image_bytes, printer_name)
    }

    /// Print to thermal POS printer using direct byte transfer
    /// Memory-efficient: no temp files, streams data directly
    fn print_to_thermal_printer(image_bytes: &[u8], printer_name: &str) -> Result<(), String> {
        use image::ImageReader;
        use std::io::Cursor;

        println!("Converting PNG to monochrome bitmap for thermal printer");

        // Decode PNG image in memory (no temp file)
        let img = ImageReader::new(Cursor::new(image_bytes))
            .with_guessed_format()
            .map_err(|e| format!("Failed to read image format: {}", e))?
            .decode()
            .map_err(|e| format!("Failed to decode PNG: {}", e))?;

        // Convert to grayscale and then to monochrome
        let gray_img = img.to_luma8();
        let width = gray_img.width() as usize;
        let height = gray_img.height() as usize;

        println!("Image dimensions: {}x{}", width, height);

        // Build ESC/POS bitmap commands
        let mut esc_pos_data = Vec::new();

        // Initialize printer
        esc_pos_data.extend_from_slice(&[0x1B, 0x40]); // ESC @ - Initialize

        // Set line spacing to minimize gaps
        esc_pos_data.extend_from_slice(&[0x1B, 0x33, 0x00]); // ESC 3 n - Set line spacing to 0

        // Convert image to ESC/POS bitmap format
        // Process in horizontal slices of 24 pixels height (24-dot thermal printer mode)
        for y_slice in (0..height).step_by(24) {
            // ESC * command for bit image
            esc_pos_data.extend_from_slice(&[0x1B, 0x2A, 0x21]); // ESC * ! (24-dot double-density)

            // Width in bytes (little-endian)
            esc_pos_data.push((width & 0xFF) as u8);
            esc_pos_data.push(((width >> 8) & 0xFF) as u8);

            // Convert pixels to bitmap bytes
            for x in 0..width {
                for byte_idx in 0..3 { // 3 bytes for 24 dots
                    let mut byte_val = 0u8;
                    for bit in 0..8 {
                        let y = y_slice + byte_idx * 8 + bit;
                        if y < height {
                            let pixel = gray_img.get_pixel(x as u32, y as u32).0[0];
                            // Threshold: dark pixels (< 128) become 1 (black)
                            if pixel < 128 {
                                byte_val |= 1 << (7 - bit);
                            }
                        }
                    }
                    esc_pos_data.push(byte_val);
                }
            }

            // Line feed after each slice
            esc_pos_data.push(0x0A);
        }

        // Feed paper and cut
        esc_pos_data.extend_from_slice(&[0x1B, 0x64, 0x02]); // ESC d 2 - Feed 2 lines
        esc_pos_data.extend_from_slice(&[0x1D, 0x56, 0x41, 0x00]); // GS V A 0 - Partial cut

        println!("Generated {} bytes of ESC/POS data", esc_pos_data.len());

        // Send to printer using Windows RAW printing
        send_raw_to_printer(&esc_pos_data, printer_name)
    }

    /// Send raw bytes directly to printer (for ESC/POS commands)
    pub fn send_raw_to_printer(data: &[u8], printer_name: &str) -> Result<(), String> {
        unsafe {
            let printer_name_wide = string_to_wide(printer_name);
            let mut printer_handle = PRINTER_HANDLE::default();

            // Open printer
            let result = OpenPrinterW(
                PWSTR(printer_name_wide.as_ptr() as *mut u16),
                &mut printer_handle as *mut _,
                None,
            );

            if result.is_err() {
                return Err(format!("Failed to open printer: {}", printer_name));
            }

            // Start print job
            let doc_name = string_to_wide("POS Receipt");
            let datatype = string_to_wide("RAW");
            let doc_info = DOC_INFO_1W {
                pDocName: PWSTR(doc_name.as_ptr() as *mut u16),
                pOutputFile: PWSTR::null(),
                pDatatype: PWSTR(datatype.as_ptr() as *mut u16),
            };

            let job_id = StartDocPrinterW(printer_handle, 1, &doc_info);
            if job_id == 0 {
                let _ = ClosePrinter(printer_handle);
                return Err("Failed to start print job".to_string());
            }

            // Start page
            if !StartPagePrinter(printer_handle).as_bool() {
                let _ = EndDocPrinter(printer_handle);
                let _ = ClosePrinter(printer_handle);
                return Err("Failed to start page".to_string());
            }

            // Write data
            let mut bytes_written = 0u32;
            let write_result = WritePrinter(
                printer_handle,
                data.as_ptr() as *const _,
                data.len() as u32,
                &mut bytes_written,
            );

            // End page and document
            let _ = EndPagePrinter(printer_handle);
            let _ = EndDocPrinter(printer_handle);
            let _ = ClosePrinter(printer_handle);

            if !write_result.as_bool() {
                return Err("Failed to write data to printer".to_string());
            }

            println!("Wrote {} bytes to thermal printer", bytes_written);
            Ok(())
        }
    }

    /// Print via PowerShell (for regular printers: PDF, laser, inkjet)
    /// Memory optimization: cleanup in background thread
    fn print_via_powershell(image_bytes: &[u8], printer_name: &str) -> Result<(), String> {
        // Create temp file
        let temp_dir = std::env::temp_dir();
        let timestamp = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis();
        let temp_file = temp_dir.join(format!("pharmasmart_receipt_{}.png", timestamp));

        // Write PNG to temp file
        std::fs::write(&temp_file, image_bytes)
            .map_err(|e| format!("Failed to write temp file: {}", e))?;

        println!("Temp file created: {:?}", temp_file);

        // Build PowerShell script (memory-efficient: use String::with_capacity)
        let temp_file_path = temp_file.to_str().unwrap().replace("\\", "\\\\");
        let mut ps_script = String::with_capacity(1500); // Pre-allocate to avoid reallocations

        ps_script.push_str(&format!(
            r#"$file = '{}'
$printer = '{}'
Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile($file)
$printDoc = New-Object System.Drawing.Printing.PrintDocument
$printDoc.PrinterSettings.PrinterName = $printer
$printHandler = {{
    param($sender, $ev)
    $ev.Graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $ev.Graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $ev.Graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $ev.Graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $pageWidth = $ev.MarginBounds.Width
    $pageHeight = $ev.MarginBounds.Height
    $scale = $pageWidth / $img.Width
    $scaledWidth = $pageWidth
    $scaledHeight = [int]($img.Height * $scale)
    if ($scaledHeight -gt $pageHeight) {{
        $scale = $pageHeight / $img.Height
        $scaledHeight = $pageHeight
        $scaledWidth = [int]($img.Width * $scale)
    }}
    $destRect = New-Object System.Drawing.Rectangle $ev.MarginBounds.Left, $ev.MarginBounds.Top, $scaledWidth, $scaledHeight
    $ev.Graphics.DrawImage($img, $destRect)
    $ev.HasMorePages = $false
}}
$printDoc.Add_PrintPage($printHandler)
$printDoc.Print()
$img.Dispose()
"#,
            temp_file_path, printer_name
        ));

        // Execute PowerShell command
        let output = std::process::Command::new("powershell")
            .args(&["-NoProfile", "-NonInteractive", "-Command", &ps_script])
            .output()
            .map_err(|e| format!("Failed to execute PowerShell: {}", e))?;

        if !output.status.success() {
            let error_msg = String::from_utf8_lossy(&output.stderr);
            let _ = std::fs::remove_file(&temp_file);
            return Err(format!("PowerShell printing failed: {}", error_msg));
        }

        println!("Print command executed successfully");

        // Memory optimization: cleanup in background thread instead of blocking
        let temp_file_clone = temp_file.clone();
        std::thread::spawn(move || {
            // Wait for print job to complete
            std::thread::sleep(std::time::Duration::from_secs(1));

            // Try cleanup multiple times if file is locked
            for attempt in 0..5 {
                match std::fs::remove_file(&temp_file_clone) {
                    Ok(_) => {
                        println!("Temp file cleaned up successfully");
                        break;
                    }
                    Err(_) if attempt < 4 => {
                        std::thread::sleep(std::time::Duration::from_millis(500));
                    }
                    Err(e) => {
                        eprintln!("Warning: Failed to cleanup temp file: {}", e);
                    }
                }
            }
        });

        println!("Successfully printed to {}", printer_name);
        Ok(())
    }

    /// Convert PCWSTR to String
    ///
    /// # Safety
    /// This function dereferences a raw pointer and must be called in an unsafe context
    unsafe fn pwstr_to_string(pwstr: PCWSTR) -> String {
        if pwstr.is_null() {
            return String::new();
        }

        // Use as_wide() method which is available in newer Windows crate versions
        pwstr.to_string().unwrap_or_default()
    }

    /// Convert PWSTR to String (for mutable wide strings)
    ///
    /// # Safety
    /// This function dereferences a raw pointer and must be called in an unsafe context
    unsafe fn pwstr_to_string_from_pwstr(pwstr: PWSTR) -> String {
        if pwstr.is_null() {
            return String::new();
        }

        // Convert PWSTR to PCWSTR and use the same conversion
        pwstr_to_string(PCWSTR(pwstr.0 as *const u16))
    }

    /// Convert String to wide string (UTF-16)
    fn string_to_wide(s: &str) -> Vec<u16> {
        OsStr::new(s)
            .encode_wide()
            .chain(std::iter::once(0))
            .collect()
    }
}
