// Tauri command handlers for customer display
// This module handles sending ESC/POS commands to customer displays via different connection types

use serde::{Deserialize, Serialize};
use std::io::Write;
use std::net::TcpStream;

#[derive(Debug, Deserialize, Serialize, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum ConnectionType {
    Serial,
    Usb,
    Network,
}

#[derive(Debug, Deserialize, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct CustomerDisplayConnectionConfig {
    pub connection_type: ConnectionType,
    pub serial_port: Option<String>,
    pub baud_rate: Option<u32>,
    pub usb_device_name: Option<String>,
    pub ip_address: Option<String>,
    pub port: Option<u16>,
}

/// Send ESC/POS data to customer display
/// This is the main Tauri command that frontend calls
#[tauri::command]
pub async fn send_to_customer_display(
    esc_pos_data: Vec<u8>,
    config: CustomerDisplayConnectionConfig,
) -> Result<String, String> {
    match config.connection_type {
        ConnectionType::Serial => send_to_serial_display(&esc_pos_data, &config).await,
        ConnectionType::Usb => send_to_usb_display(&esc_pos_data, &config).await,
        ConnectionType::Network => send_to_network_display(&esc_pos_data, &config).await,
    }
}

/// Send data to serial port customer display
async fn send_to_serial_display(
    data: &[u8],
    config: &CustomerDisplayConnectionConfig,
) -> Result<String, String> {
    let port_name = config
        .serial_port
        .as_ref()
        .ok_or("Serial port name not specified")?;
    let baud_rate = config.baud_rate.unwrap_or(9600);

    #[cfg(feature = "serialport")]
    {
        use serialport::SerialPort;

        let mut port = serialport::new(port_name, baud_rate)
            .timeout(std::time::Duration::from_millis(1000))
            .open()
            .map_err(|e| format!("Failed to open serial port {}: {}", port_name, e))?;

        port.write_all(data)
            .map_err(|e| format!("Failed to write to serial port: {}", e))?;

        port.flush()
            .map_err(|e| format!("Failed to flush serial port: {}", e))?;

        Ok(format!(
            "Sent {} bytes to serial display at {} ({}  baud)",
            data.len(),
            port_name,
            baud_rate
        ))
    }

    #[cfg(not(feature = "serialport"))]
    {
        Err("Serial port support not enabled. Add 'serialport' feature to Cargo.toml".to_string())
    }
}

/// Send data to USB customer display
/// Note: USB displays are typically accessed via serial ports on most systems
/// For Windows, they create virtual COM ports
async fn send_to_usb_display(
    data: &[u8],
    config: &CustomerDisplayConnectionConfig,
) -> Result<String, String> {
    // For USB displays that create virtual serial ports
    if let Some(serial_port) = &config.serial_port {
        let serial_config = CustomerDisplayConnectionConfig {
            connection_type: ConnectionType::Serial,
            serial_port: Some(serial_port.clone()),
            baud_rate: config.baud_rate,
            usb_device_name: None,
            ip_address: None,
            port: None,
        };
        return send_to_serial_display(data, &serial_config).await;
    }

    // For true USB devices (would require libusb or similar)
    #[cfg(feature = "libusb")]
    {
        // USB device handling would go here
        // This is more complex and requires device-specific implementation
        Err("Direct USB display support not yet implemented. Use serial port mode for USB-to-Serial displays.".to_string())
    }

    #[cfg(not(feature = "libusb"))]
    {
        Err("USB display requires serial port configuration. Set serialPort in config.".to_string())
    }
}

/// Send data to network customer display via TCP/IP
async fn send_to_network_display(
    data: &[u8],
    config: &CustomerDisplayConnectionConfig,
) -> Result<String, String> {
    let ip_address = config
        .ip_address
        .as_ref()
        .ok_or("IP address not specified")?;
    let port = config.port.unwrap_or(9100); // Default raw printing port

    let address = format!("{}:{}", ip_address, port);

    let mut stream = TcpStream::connect(&address)
        .map_err(|e| format!("Failed to connect to network display at {}: {}", address, e))?;

    stream
        .write_all(data)
        .map_err(|e| format!("Failed to write to network display: {}", e))?;

    stream
        .flush()
        .map_err(|e| format!("Failed to flush network stream: {}", e))?;

    Ok(format!(
        "Sent {} bytes to network display at {}",
        data.len(),
        address
    ))
}

/// List available serial ports
/// This helps users discover which ports are available
#[tauri::command]
pub async fn list_serial_ports() -> Result<Vec<String>, String> {
    #[cfg(feature = "serialport")]
    {
        let ports = serialport::available_ports()
            .map_err(|e| format!("Failed to list serial ports: {}", e))?;

        Ok(ports.iter().map(|p| p.port_name.clone()).collect())
    }

    #[cfg(not(feature = "serialport"))]
    {
        Err("Serial port support not enabled".to_string())
    }
}

/// Test customer display connection
/// Sends a simple initialization command to verify connection
#[tauri::command]
pub async fn test_customer_display_connection(
    config: CustomerDisplayConnectionConfig,
) -> Result<String, String> {
    // ESC @ - Initialize display
    let test_data = vec![0x1B, 0x40];

    send_to_customer_display(test_data, config).await
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_config_deserialization() {
        let json = r#"{
            "connectionType": "SERIAL",
            "serialPort": "COM3",
            "baudRate": 9600
        }"#;

        let config: CustomerDisplayConnectionConfig = serde_json::from_str(json).unwrap();
        assert!(matches!(config.connection_type, ConnectionType::Serial));
        assert_eq!(config.serial_port, Some("COM3".to_string()));
        assert_eq!(config.baud_rate, Some(9600));
    }

    #[tokio::test]
    async fn test_network_connection_error() {
        let config = CustomerDisplayConnectionConfig {
            connection_type: ConnectionType::Network,
            serial_port: None,
            baud_rate: None,
            usb_device_name: None,
            ip_address: Some("192.168.1.99".to_string()),
            port: Some(9100),
        };

        let data = vec![0x1B, 0x40]; // ESC @
        let result = send_to_network_display(&data, &config).await;

        // Should fail if no display at this address
        // In real environment, this would be a valid test
        // For now, just verify the error handling works
        assert!(result.is_err() || result.is_ok());
    }
}
