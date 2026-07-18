import { Component, inject, NgZone, OnDestroy, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TauriPrinterService } from 'app/shared/services/tauri-printer.service';

interface SetupDefaults {
  db_host: string;
  db_port: number;
  db_name: string;
  db_username: string;
  db_schema: string;
  server_port: number;
}

@Component({
  selector: 'app-setup-wizard',
  templateUrl: './setup-wizard.component.html',
  styleUrls: ['./setup-wizard.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [ReactiveFormsModule, NgOptimizedImage, ButtonModule, InputTextModule, PasswordModule, ProgressSpinnerModule],
})
export class SetupWizardComponent implements OnInit, OnDestroy {
  readonly visible    = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal('');

  private readonly tauriService = inject(TauriPrinterService);
  private readonly ngZone = inject(NgZone);
  private unlistenSetupRequired?: () => void;

  readonly form = new FormGroup({
    dbHost:     new FormControl('localhost',    [Validators.required]),
    dbPort:     new FormControl(5432,           [Validators.required, Validators.min(1), Validators.max(65535)]),
    dbName:     new FormControl('pharma_smart', [Validators.required]),
    dbUsername: new FormControl('pharma_smart', [Validators.required]),
    dbPassword: new FormControl(''),
    dbSchema:   new FormControl(''),
    serverPort: new FormControl(9080,           [Validators.required, Validators.min(1), Validators.max(65535)]),
  });

  async ngOnInit(): Promise<void> {
    if (!this.tauriService.isRunningInTauri()) {
      return;
    }

    try {
      const { listen } = await import('@tauri-apps/api/event');
      const { invoke } = await import('@tauri-apps/api/core');

      // Écouter les événements futurs (normal flow)
      this.unlistenSetupRequired = await listen<void>('setup-required', () => {
        this.ngZone.run((): void => { void this.openWizard(); });
      });

      // Détecter un événement manqué (Angular chargé après l'émission Rust)
      const needsSetup = await invoke<boolean>('check_needs_setup');
      if (needsSetup) {
        await this.openWizard();
      }
    } catch (e) {
      console.warn('[SetupWizard] Tauri non disponible :', e);
    }
  }

  ngOnDestroy(): void {
    this.unlistenSetupRequired?.();
  }

  private async openWizard(): Promise<void> {
    await this.loadDefaults();
    this.ngZone.run((): void => { this.visible.set(true); });
  }

  private async loadDefaults(): Promise<void> {
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      const d = await invoke<SetupDefaults>('get_setup_defaults');
      this.ngZone.run((): void => {
        this.form.patchValue({
          dbHost:     d.db_host,
          dbPort:     d.db_port,
          dbName:     d.db_name,
          dbUsername: d.db_username,
          dbSchema:   d.db_schema,
          serverPort: d.server_port,
        });
      });
    } catch (e) {
      console.warn('[SetupWizard] Impossible de charger les défauts :', e);
    }
  }

  async onSubmit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');

    try {
      const { invoke } = await import('@tauri-apps/api/core');
      const v = this.form.getRawValue();
      await invoke('complete_initial_setup', {
        dbHost:     v.dbHost ?? '',
        dbPort:     v.dbPort ?? 5432,
        dbName:     v.dbName ?? '',
        dbUsername: v.dbUsername ?? '',
        dbPassword: v.dbPassword ?? '',
        dbSchema:   v.dbSchema ?? '',
        serverPort: v.serverPort ?? 9080,
      });
      this.ngZone.run((): void => { this.visible.set(false); });
    } catch (e) {
      this.ngZone.run((): void => {
        this.errorMessage.set(String(e));
        this.submitting.set(false);
      });
    }
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!ctrl && ctrl.invalid && ctrl.touched;
  }

  onLogoError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }
}
