# Tauri Standalone Installer Configuration

This document explains the custom installer configuration features for the PharmaSmart Tauri standalone application.

## Features

The standalone installer now includes:

1. **Custom Installation Directory**: Users can choose where to install the application
2. **Configurable Backend Port**: Users can specify the backend server port during installation
3. **Installation Directory Logs**: Log files are written to the installation directory (not user home)

## Installation Process

### 1. Choose Installation Directory

During installation, the NSIS installer allows you to choose a custom installation directory. The default is:
```
C:\Program Files\PharmaSmart
```

You can change this to any directory with write permissions, for example:
```
D:\PharmaSmart
C:\Applications\PharmaSmart
```

### 2. Configure Backend Port

The installer presents a configuration page where you can set the backend server port:

- **Default Port**: 8080
- **Valid Range**: 1024 - 65535
- **Recommended**: Use ports above 1024 to avoid conflicts with system services

### 3. Log File Location

Log files are automatically created in the installation directory:
```
<Installation Directory>\logs\pharmasmart.log
```

For example, if you installed to `C:\Program Files\PharmaSmart`, logs will be at:
```
C:\Program Files\PharmaSmart\logs\pharmasmart.log
```

## Configuration File

After installation, a `config.json` file is created in the installation directory:

```json
{
  "server": {
    "port": 8080
  },
  "logging": {
    "directory": "C:\\Program Files\\PharmaSmart\\logs",
    "file": "C:\\Program Files\\PharmaSmart\\logs\\pharmasmart.log"
  },
  "installation": {
    "directory": "C:\\Program Files\\PharmaSmart"
  }
}
```

### Modifying Configuration After Installation

You can change the configuration by editing `config.json` in the installation directory:

#### Change Backend Port

1. Open `config.json` with a text editor (as Administrator if installed in Program Files)
2. Modify the `port` value under `server`:
   ```json
   "server": {
     "port": 9090
   }
   ```
3. Save the file
4. Restart the PharmaSmart application

#### Change Log Directory

1. Open `config.json` with a text editor
2. Modify the `directory` and `file` values under `logging`:
   ```json
   "logging": {
     "directory": "D:\\PharmaSmart\\logs",
     "file": "D:\\PharmaSmart\\logs\\pharmasmart.log"
   }
   ```
3. Ensure the directory exists or the application will create it
4. Save the file
5. Restart the PharmaSmart application

## Finding Your Configuration

The `CONFIGURATION.txt` file in the installation directory shows your current settings:

```
==================================================================
PharmaSmart - Configuration
==================================================================

Installation Directory: C:\Program Files\PharmaSmart
Backend Port: 8080
Log Directory: C:\Program Files\PharmaSmart\logs
Log File: C:\Program Files\PharmaSmart\logs\pharmasmart.log

==================================================================
```

## Troubleshooting

### Cannot Write to Log Directory

If the application cannot write to the log directory (e.g., due to permissions), you can:

1. Edit `config.json` to use a directory with write permissions:
   ```json
   "logging": {
     "directory": "C:\\Users\\YourUsername\\PharmaSmart\\logs",
     "file": "C:\\Users\\YourUsername\\PharmaSmart\\logs\\pharmasmart.log"
   }
   ```

2. Or run the application as Administrator (not recommended)

### Port Already in Use

If the configured port is already in use:

1. Check what's using the port:
   ```cmd
   netstat -ano | findstr :8080
   ```

2. Change to a different port in `config.json`:
   ```json
   "server": {
     "port": 8081
   }
   ```

3. Restart the application

### Configuration File Not Found

If `config.json` is missing, the application will use defaults:
- **Port**: 8080
- **Log Directory**: `%USERPROFILE%\PharmaSmart\logs`

To restore the configuration file:

1. Create `config.json` in the installation directory
2. Copy the structure shown above
3. Modify values as needed

## Building the Installer

To build the standalone installer with these features:

```bash
# Build with bundled JRE and backend
npm run tauri:build:bundled-jre

# Or via Maven
./mvnw.cmd clean install -DskipTests -Pstandalone
```

The installer will be located in:
```
src-tauri/target/release/bundle/nsis/PharmaSmart_0.0.1_x64-setup.exe
```

## Technical Details

### NSIS Installer Hooks

The custom installer hooks (`src-tauri/installer-hooks/installer.nsh`) implement:

- Custom dialog page for port configuration
- Validation of port number range
- Creation of `config.json` with installation settings
- Creation of `CONFIGURATION.txt` for easy reference
- Automatic log directory creation

### Rust Configuration Module

The Rust configuration module (`src-tauri/src/config.rs`) provides:

- Loading configuration from `config.json`
- Fallback to defaults if config file is missing
- Type-safe configuration access
- Automatic log directory creation
- Configuration saving capabilities

### Backend Manager Integration

The backend manager (`src-tauri/src/backend_manager.rs`) uses the configuration to:

- Start the Spring Boot backend on the configured port
- Write logs to the configured directory
- Pass configuration to Java via command-line arguments

## Security Considerations

1. **Port Selection**: Choose ports above 1024 to avoid conflicts with system services
2. **File Permissions**: If installing to Program Files, log directory may require Administrator privileges
3. **Configuration Protection**: The `config.json` file inherits permissions from the installation directory

## Best Practices

1. **Installation Directory**: Choose a directory where the application user has write permissions
2. **Port Selection**: Use a port that doesn't conflict with other services (check with `netstat -ano`)
3. **Log Rotation**: Implement log rotation to prevent the log file from growing too large
4. **Backup Configuration**: Keep a backup of `config.json` before making changes

## Support

For issues or questions about installer configuration:

1. Check the `logs\pharmasmart.log` file for errors
2. Review `CONFIGURATION.txt` for current settings
3. Verify `config.json` is valid JSON
4. Ensure the installation directory has proper permissions
