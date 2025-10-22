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

#[cfg(target_os = "windows")]
mod windows_printer {
    use super::PrinterInfo;
    use std::ffi::OsStr;
    use std::os::windows::ffi::OsStrExt;
    use windows::core::{PCWSTR, PWSTR};
    use windows::Win32::Graphics::Printing::{
        EnumPrintersW, GetDefaultPrinterW,
        PRINTER_ENUM_LOCAL, PRINTER_INFO_2W,
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

    /// Print PNG image to Windows printer using PowerShell
    pub fn print_image_windows(image_bytes: &[u8], printer_name: &str) -> Result<(), String> {
        println!("Preparing to print to: {}", printer_name);

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

        // Use PowerShell to print the file directly to the specified printer
        let temp_file_path = temp_file.to_str().unwrap().replace("\\", "\\\\");
        let ps_script = format!(
            r#"
            $file = '{}'
            $printer = '{}'

            # Load System.Drawing assembly
            Add-Type -AssemblyName System.Drawing

            # Open the image
            $img = [System.Drawing.Image]::FromFile($file)

            # Create PrintDocument
            $printDoc = New-Object System.Drawing.Printing.PrintDocument
            $printDoc.PrinterSettings.PrinterName = $printer

            # Define print handler with high quality settings
            $printHandler = {{
                param($sender, $ev)

                # Set high quality rendering
                $ev.Graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $ev.Graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                $ev.Graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $ev.Graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

                # Calculate scaling to fit page width while maintaining aspect ratio
                $pageWidth = $ev.MarginBounds.Width
                $pageHeight = $ev.MarginBounds.Height
                $imgWidth = $img.Width
                $imgHeight = $img.Height

                # Scale to fit width
                $scale = $pageWidth / $imgWidth
                $scaledWidth = $pageWidth
                $scaledHeight = [int]($imgHeight * $scale)

                # If scaled height exceeds page, scale to fit height instead
                if ($scaledHeight -gt $pageHeight) {{
                    $scale = $pageHeight / $imgHeight
                    $scaledHeight = $pageHeight
                    $scaledWidth = [int]($imgWidth * $scale)
                }}

                # Draw at top-left of printable area, scaled to fit
                $destRect = New-Object System.Drawing.Rectangle $ev.MarginBounds.Left, $ev.MarginBounds.Top, $scaledWidth, $scaledHeight

                # Draw the entire image
                $ev.Graphics.DrawImage($img, $destRect)

                $ev.HasMorePages = $false
            }}

            # Attach handler and print
            $printDoc.Add_PrintPage($printHandler)
            $printDoc.Print()

            # Cleanup
            $img.Dispose()
            "#,
            temp_file_path,
            printer_name
        );

        // Execute PowerShell command
        let output = std::process::Command::new("powershell")
            .args(&["-NoProfile", "-NonInteractive", "-Command", &ps_script])
            .output()
            .map_err(|e| format!("Failed to execute PowerShell: {}", e))?;

        if !output.status.success() {
            let error_msg = String::from_utf8_lossy(&output.stderr);
            // Clean up temp file
            let _ = std::fs::remove_file(&temp_file);
            return Err(format!("PowerShell printing failed: {}", error_msg));
        }

        println!("Print command executed successfully");

        // Wait a bit for printing to complete, then clean up
        std::thread::sleep(std::time::Duration::from_secs(3));
        let _ = std::fs::remove_file(&temp_file);

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
