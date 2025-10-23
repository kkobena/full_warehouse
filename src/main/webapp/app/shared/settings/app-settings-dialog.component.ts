import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { AppSettingsService } from 'app/core/config/app-settings.service';
import { Card } from 'primeng/card';
import { Button } from 'primeng/button';

@Component({
  selector: 'jhi-app-settings-dialog',
  imports: [CommonModule, FormsModule, Card, Button],
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
            <div class="form-text">
              Exemples: http://localhost:8080, http://192.168.1.100:8080
            </div>
          </div>

          <div class="d-flex gap-2 mb-3">
            <button
              type="button"
              class="btn btn-sm btn-outline-primary"
              (click)="testConnection()"
              [disabled]="testing || !apiServerUrl"
            >
              <i class="bi" [ngClass]="testing ? 'bi-hourglass-split' : 'bi-wifi'"></i>
              {{ testing ? 'Test en cours...' : 'Tester la connexion' }}
            </button>

            <button
              type="button"
              class="btn btn-sm btn-outline-secondary"
              (click)="resetToDefaults()"
            >
              <i class="bi bi-arrow-counterclockwise"></i>
              Réinitialiser
            </button>
          </div>
          @if (connectionTestResult !== null) {
            <div class="alert" [ngClass]="{
          'alert-success': connectionTestResult === true,
          'alert-danger': connectionTestResult === false
        }">
              <i class="bi" [ngClass]="{
            'bi-check-circle': connectionTestResult === true,
            'bi-x-circle': connectionTestResult === false
          }"></i>

              {{
                connectionTestResult ? 'Connexion réussie!' : errorMsg
              }}

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

      <p-button class="mr-2" (click)="dismiss()" icon="pi pi-times" label="Annuler" raised="true" severity="secondary"
                type="button"></p-button>
      <p-button
        [disabled]="!apiServerUrl || testing"
        class="mr-1"
        icon="pi pi-check-lg"
        label="Enregistrer et Redémarrer"
        raised="true"
        severity="primary"
        (click)="save()"
        type="button"
      ></p-button>


    </div>
  `

})
export class AppSettingsDialogComponent implements OnInit {
 protected apiServerUrl: string = '';
 protected errorMsg: string = "Échec de la connexion. Vérifiez l'adresse et réessayez.";
 protected testing = false;
 protected connectionTestResult: boolean | null = null;

  constructor(
    public activeModal: NgbActiveModal,
    private appSettingsService: AppSettingsService
  ) {}

  ngOnInit(): void {
    const settings = this.appSettingsService.getSettings();
    this.apiServerUrl = settings.apiServerUrl;
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
    this.activeModal.close('saved');

    // Reload the application to apply new settings
    setTimeout(() => {
      window.location.reload();
    }, 500);
  }

  dismiss(): void {
    this.activeModal.dismiss('cancel');
  }
}
