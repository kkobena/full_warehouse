import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { AppSettingsService } from 'app/core/config/app-settings.service';
import { Card } from 'primeng/card';
import { Button } from 'primeng/button';
import { ProgressBar } from 'primeng/progressbar';
import { BackendManagerService } from '../services/backend-manager.service';

@Component({
  selector: 'jhi-app-settings-dialog',
  imports: [CommonModule, FormsModule, Card, Button, ProgressBar],
  styleUrls: ['../../entities/common-modal.component.scss'],
  template: `
    <div class="modal-header">
      <h4 class="modal-title">Configuration du serveur</h4>
      <button type="button" class="btn-close" aria-label="Close" (click)="dismiss()"></button>
    </div>
    <p-card>
      <div class="modal-body">
        <div class="alert alert-info">
          <i class="bi bi-info-circle"></i>
          Configurez l'adresse du serveur backend pour vous connecter à votre système PharmaSmart sur le réseau local.
        </div>

        <form #settingsForm="ngForm">
          <div class="mb-3">
            <label for="apiServerUrl" class="form-label">
              <strong>Adresse du Serveur Backend</strong>
            </label>
            <input
              type="url"
              class="form-control"
              id="apiServerUrl"
              name="apiServerUrl"
              [(ngModel)]="apiServerUrl"
              placeholder="http://192.168.1.100:8080"
              required
              pattern="http?://.+"
            />
            <div class="form-text">Exemples: http://localhost:8080, http://192.168.1.100:8080</div>
          </div>

          <div class="d-flex gap-2 mb-3">
            <button type="button" class="btn btn-sm btn-outline-primary" (click)="testConnection()" [disabled]="testing || !apiServerUrl">
              <i class="bi" [ngClass]="testing ? 'bi-hourglass-split' : 'bi-wifi'"></i>
              {{ testing ? 'Test en cours...' : 'Tester la connexion' }}
            </button>

            <button type="button" class="btn btn-sm btn-outline-secondary" (click)="resetToDefaults()">
              <i class="bi bi-arrow-counterclockwise"></i>
              Réinitialiser
            </button>
          </div>
          @if (connectionTestResult !== null) {
            <div
              class="alert"
              [ngClass]="{
                'alert-success': connectionTestResult === true,
                'alert-danger': connectionTestResult === false,
              }"
            >
              <i
                class="bi"
                [ngClass]="{
                  'bi-check-circle': connectionTestResult === true,
                  'bi-x-circle': connectionTestResult === false,
                }"
              ></i>

              {{ connectionTestResult ? 'Connexion réussie!' : errorMsg }}
            </div>
          }

          @if (isRestarting) {
            <div class="alert alert-info">
              <div class="d-flex align-items-center mb-2">
                <i class="bi bi-arrow-repeat me-2"></i>
                <strong>{{ restartMessage }}</strong>
              </div>
              <p-progressbar [value]="restartProgress" [showValue]="false" />
              <small class="text-muted mt-2 d-block">Veuillez patienter pendant le redémarrage du serveur...</small>
            </div>
          }
        </form>

        <div class="mt-4 pt-3 border-top">
          <h6>Informations:</h6>
          <ul class="small text-muted">
            <li>Le serveur doit être accessible sur votre réseau local</li>
            <li>Assurez-vous que le port (généralement 8080) n'est pas bloqué par le pare-feu</li>
            <li>L'application redémarrera automatiquement après la sauvegarde</li>
          </ul>
        </div>
      </div>
    </p-card>

    <div class="modal-footer">
      <p-button
        class="mr-2"
        (click)="dismiss()"
        icon="pi pi-times"
        label="Annuler"
        raised="true"
        severity="secondary"
        type="button"
      ></p-button>
      <p-button
        [disabled]="!apiServerUrl || testing || isRestarting"
        [loading]="isRestarting"
        class="mr-1"
        icon="pi pi-check-lg"
        label="Enregistrer et Redémarrer"
        raised="true"
        severity="primary"
        (click)="save()"
        type="button"
      ></p-button>
    </div>
  `,
})
export class AppSettingsDialogComponent implements OnInit {
  protected apiServerUrl: string = '';
  protected errorMsg: string = "Échec de la connexion. Vérifiez l'adresse et réessayez.";
  protected testing = false;
  protected connectionTestResult: boolean | null = null;
  protected isRestarting = false;
  protected restartMessage = 'Redémarrage en cours...';
  protected restartProgress = 0;
  protected isBundledBackend = false;

  private readonly backendManager = inject(BackendManagerService);

  constructor(
    public activeModal: NgbActiveModal,
    private appSettingsService: AppSettingsService,
  ) {}

  async ngOnInit(): Promise<void> {
    const settings = this.appSettingsService.getSettings();
    this.apiServerUrl = settings.apiServerUrl;

    // Check if running in Tauri bundled backend mode
    this.isBundledBackend = await this.backendManager.isBundledBackendAvailable();
  }

  async testConnection(): Promise<void> {
    this.testing = true;
    this.connectionTestResult = null;

    try {
      this.connectionTestResult = await this.appSettingsService.testConnection(this.apiServerUrl);
    } catch (error) {
      this.connectionTestResult = false;
    } finally {
      this.testing = false;
    }
  }

  resetToDefaults(): void {
    this.apiServerUrl = 'http://localhost:8080';
    this.connectionTestResult = null;
  }

  save(): void {
    this.appSettingsService.updateApiServerUrl(this.apiServerUrl);

    if (this.isBundledBackend) {
      // Restart backend in Tauri standalone mode
      this.restartBackendAndClose();
    } else {
      // Standard mode - just reload
      this.activeModal.close('saved');
      setTimeout(() => {
        window.location.reload();
      }, 500);
    }
  }

  private restartBackendAndClose(): void {
    this.isRestarting = true;
    this.restartMessage = 'Arrêt du serveur...';
    this.restartProgress = 10;

    this.backendManager.restartBackend().subscribe({
      next: message => {
        console.log('Backend restart successful:', message);
        this.restartMessage = 'Redémarrage terminé!';
        this.restartProgress = 100;

        // Close dialog and reload after successful restart
        setTimeout(() => {
          this.activeModal.close('saved');
          window.location.reload();
        }, 1000);
      },
      error: error => {
        console.error('Backend restart failed:', error);
        this.isRestarting = false;
        this.restartMessage = 'Échec du redémarrage';
        this.connectionTestResult = false;
        this.errorMsg = 'Échec du redémarrage du serveur: ' + error;

        // Fallback: try reloading anyway
        setTimeout(() => {
          window.location.reload();
        }, 2000);
      },
    });

    // Simulate progress during restart
    this.simulateRestartProgress();
  }

  private simulateRestartProgress(): void {
    const progressSteps = [
      { delay: 500, progress: 20, message: 'Arrêt du serveur...' },
      { delay: 1500, progress: 40, message: 'Démarrage du nouveau processus...' },
      { delay: 3000, progress: 60, message: 'Initialisation du serveur...' },
      { delay: 5000, progress: 80, message: 'Vérification de la disponibilité...' },
      { delay: 7000, progress: 95, message: 'Finalisation...' },
    ];

    progressSteps.forEach(step => {
      setTimeout(() => {
        if (this.isRestarting && this.restartProgress < 100) {
          this.restartProgress = step.progress;
          this.restartMessage = step.message;
        }
      }, step.delay);
    });
  }

  dismiss(): void {
    this.activeModal.dismiss('cancel');
  }
}
