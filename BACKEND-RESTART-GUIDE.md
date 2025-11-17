# Backend Restart Guide - Tauri Standalone Mode

This guide explains how to restart the Spring Boot backend when running in Tauri standalone mode.

## 🎯 Overview

When configuration changes are made (like updating the `use-simple-sale` parameter), the backend needs to be restarted to pick up the new configuration. The restart functionality allows you to do this without closing the entire application.

## 🔧 Architecture

### Rust Backend (Tauri)
- **`backend_manager.rs`**: Core functions for backend process management
  - `start_backend()`: Starts the Spring Boot JAR
  - `stop_backend()`: Stops the running backend process
  - `restart_backend()`: Stops then starts the backend

### Tauri Commands (exposed to frontend)
- `restart_backend_main`: Async command to restart backend
- `stop_backend_main`: Command to stop backend
- `get_backend_status`: Get current backend status

### Angular Service
- **`backend-manager.service.ts`**: TypeScript service to call Tauri commands
  - `restartBackend()`: Returns Observable for restart operation
  - `stopBackend()`: Returns Observable for stop operation
  - `isTauriEnvironment()`: Check if running in Tauri
  - `isBundledBackendAvailable()`: Check if bundled backend is available

## 📝 Usage Examples

### Example 1: Simple Restart Button

**Component TypeScript:**
```typescript
import { Component, inject } from '@angular/core';
import { BackendManagerService } from '../shared/services/backend-manager.service';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [ButtonModule],
  template: `
    @if (showRestartButton) {
      <p-button
        (click)="restartBackend()"
        [loading]="isRestarting"
        icon="pi pi-refresh"
        label="Redémarrer le serveur"
        severity="warning"
      />
    }
  `
})
export class SettingsComponent {
  private backendManager = inject(BackendManagerService);

  showRestartButton = false;
  isRestarting = false;

  ngOnInit() {
    // Only show restart button in Tauri bundled mode
    this.backendManager.isBundledBackendAvailable().then(available => {
      this.showRestartButton = available;
    });
  }

  restartBackend() {
    if (confirm('Voulez-vous vraiment redémarrer le serveur ? Cela prendra quelques secondes.')) {
      this.isRestarting = true;

      this.backendManager.restartBackend().subscribe({
        next: (message) => {
          console.log('Restart successful:', message);
          alert('Le serveur a été redémarré avec succès !');
          this.isRestarting = false;
        },
        error: (error) => {
          console.error('Restart failed:', error);
          alert('Échec du redémarrage du serveur: ' + error);
          this.isRestarting = false;
        }
      });
    }
  }
}
```

### Example 2: Restart After Configuration Change

**Configuration Component:**
```typescript
import { Component, inject } from '@angular/core';
import { ConfigurationService } from '../shared/configuration.service';
import { BackendManagerService } from '../shared/services/backend-manager.service';

@Component({
  selector: 'app-admin-config',
  template: `
    <div class="config-section">
      <h3>Mode de vente</h3>
      <p-checkbox
        [(ngModel)]="useSimpleSale"
        (onChange)="onConfigChange()"
        binary="true"
        label="Activer le mode vente simple (comptant uniquement)"
      />

      @if (needsRestart) {
        <div class="alert alert-warning">
          <i class="pi pi-exclamation-triangle"></i>
          Le serveur doit être redémarré pour appliquer les modifications.
          <p-button
            (click)="restartAndApply()"
            [loading]="isRestarting"
            label="Redémarrer maintenant"
            size="small"
          />
        </div>
      }
    </div>
  `
})
export class AdminConfigComponent {
  private configService = inject(ConfigurationService);
  private backendManager = inject(BackendManagerService);

  useSimpleSale = false;
  needsRestart = false;
  isRestarting = false;

  ngOnInit() {
    // Load current configuration
    this.configService.getSimpleSaleConfig().subscribe(enabled => {
      this.useSimpleSale = enabled;
    });
  }

  onConfigChange() {
    // Save the configuration change
    const config = {
      name: 'use-simple-sale',
      value: this.useSimpleSale ? 'true' : 'false',
      description: 'Enable simplified cash-only sales interface'
    };

    this.configService.update(config).subscribe(() => {
      console.log('Configuration saved');
      this.needsRestart = true;
    });
  }

  restartAndApply() {
    this.isRestarting = true;

    this.backendManager.restartBackend().subscribe({
      next: () => {
        this.isRestarting = false;
        this.needsRestart = false;

        // Wait for backend to be ready, then reload the page
        setTimeout(() => {
          window.location.reload();
        }, 5000);
      },
      error: (error) => {
        this.isRestarting = false;
        console.error('Restart failed:', error);
      }
    });
  }
}
```

### Example 3: Listen to Backend Status Events

**App Component:**
```typescript
import { Component, inject, OnInit } from '@angular/core';
import { listen } from '@tauri-apps/api/event';

interface BackendStatus {
  status: string;  // 'initializing', 'starting', 'waiting', 'ready', 'error', 'stopping', 'restarting'
  progress: number; // 0-100
  message: string;
}

@Component({
  selector: 'app-root',
  template: `
    @if (backendStatus && backendStatus.status !== 'ready') {
      <div class="backend-status-overlay">
        <div class="status-card">
          <h3>{{ getStatusTitle() }}</h3>
          <p-progressbar [value]="backendStatus.progress" />
          <p>{{ backendStatus.message }}</p>
        </div>
      </div>
    }
  `
})
export class AppComponent implements OnInit {
  backendStatus: BackendStatus | null = null;

  async ngOnInit() {
    // Listen to backend status events
    await listen<BackendStatus>('backend-status', (event) => {
      this.backendStatus = event.payload;
      console.log('Backend status:', this.backendStatus);
    });
  }

  getStatusTitle(): string {
    switch (this.backendStatus?.status) {
      case 'initializing': return 'Initialisation...';
      case 'starting': return 'Démarrage du serveur...';
      case 'waiting': return 'En attente du serveur...';
      case 'restarting': return 'Redémarrage du serveur...';
      case 'stopping': return 'Arrêt du serveur...';
      case 'error': return 'Erreur';
      default: return 'Chargement...';
    }
  }
}
```

## 🔄 Backend Status States

The backend goes through several states during startup/restart:

1. **`initializing`** (0%): Backend manager is initializing
2. **`starting`** (10-30%): Launching Java process
3. **`waiting`** (50-95%): Waiting for backend to respond to health checks
4. **`ready`** (100%): Backend is fully operational
5. **`error`**: Something went wrong
6. **`stopping`**: Backend is shutting down
7. **`restarting`**: Backend is being restarted

## ⚙️ Configuration

The restart functionality is only available when:
- Running in Tauri environment (`window.__TAURI__` is defined)
- Built with `bundled-backend` feature flag
- Backend is managed by Tauri (standalone mode)

### Check if Restart is Available:

```typescript
const backendManager = inject(BackendManagerService);

if (backendManager.isTauriEnvironment()) {
  const isAvailable = await backendManager.isBundledBackendAvailable();

  if (isAvailable) {
    // Show restart button
  } else {
    // Running in standard mode with external backend
  }
}
```

## 🚀 Rebuild Requirements

After modifying the restart functionality, rebuild Tauri:

```bash
# Debug build (faster)
npm run tauri:build:debug

# Production build
npm run tauri:build
```

## 📌 Important Notes

1. **Restart takes 5-10 seconds**: The backend needs time to shut down and start up
2. **Active connections will be lost**: Any ongoing API calls will fail during restart
3. **Use with caution**: Don't restart during critical operations (e.g., during a sale)
4. **Windows only (currently)**: The stop command uses `taskkill` on Windows. Linux/macOS support uses `SIGTERM`
5. **Configuration reload**: After restart, the backend will load the latest configuration from the database

## 🐛 Troubleshooting

### Backend won't restart
- Check if the backend process is still running (Task Manager)
- Check the Tauri console logs for errors
- Ensure Java is installed and in PATH

### Restart command not found
- Verify you're running in Tauri standalone mode
- Check that the app was built with `bundled-backend` feature
- Rebuild the Tauri application

### Backend status stuck at "waiting"
- The backend might have failed to start
- Check the log file: `[Installation Directory]/logs/pharmasmart.log`
- Verify database connectivity

## 📚 Related Documentation

- [TAURI_BACKEND_SETUP.md](TAURI_BACKEND_SETUP.md) - Complete Tauri backend integration guide
- [LOGS-QUICK-REFERENCE.md](LOGS-QUICK-REFERENCE.md) - Backend logging guide
- [HOW-TO-CONFIGURE-BACKEND.md](HOW-TO-CONFIGURE-BACKEND.md) - Backend configuration guide
