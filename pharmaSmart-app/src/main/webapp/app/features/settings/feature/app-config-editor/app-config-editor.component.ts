import { Component, inject, NgZone, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ProgressBarModule } from 'primeng/progressbar';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { AccountService } from 'app/core/auth/account.service';
import { NavigationService } from 'app/core/config/navigation.service';
import { BackendManagerService } from 'app/shared/services/backend-manager.service';
import { AppSettingsService } from 'app/core/config/app-settings.service';
import { Authority } from 'app/config/authority.constants';
import { Toolbar } from "primeng/toolbar";

export interface AppConfigDto {
  server_port: number;
  db_host: string;
  db_port: number;
  db_name: string;
  db_username: string;
  db_password: string;
  db_schema: string;
  jvm_heap_min: string;
  jvm_heap_max: string;
  jvm_metaspace_size: string;
  jvm_metaspace_max: string;
  jvm_direct_memory: string;
  jvm_gc_pause: string;
  jvm_additional_options: string[];
  mail_username: string;
  mail_email: string;
  fne_url: string;
  fne_api_key: string;
  fne_point_of_sale: string;
  port_com: string;
}

@Component({
  selector: 'app-config-editor',
  templateUrl: './app-config-editor.component.html',
  styleUrls: ['./app-config-editor.component.scss'],
  imports: [ReactiveFormsModule, ButtonModule, InputTextModule, PasswordModule, ProgressBarModule, NgbNavModule, Toolbar]
})
export class AppConfigEditorComponent implements OnInit {
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly restarting = signal(false);
  readonly errorMessage = signal('');
  readonly restartMessage = signal('');
  readonly restartProgress = signal(0);
  readonly activeTab = signal<string>('db');

  private readonly router = inject(Router);
  private readonly accountService = inject(AccountService);
  private readonly navigationService = inject(NavigationService);
  private readonly backendManager = inject(BackendManagerService);
  private readonly appSettingsService = inject(AppSettingsService);
  private readonly ngZone = inject(NgZone);

  readonly form = new FormGroup({
    serverPort:       new FormControl<number>(9080, [Validators.required, Validators.min(1), Validators.max(65535)]),
    dbHost:           new FormControl('localhost', [Validators.required]),
    dbPort:           new FormControl<number>(5432, [Validators.required, Validators.min(1), Validators.max(65535)]),
    dbName:           new FormControl('pharma_smart', [Validators.required]),
    dbUsername:       new FormControl('pharma_smart', [Validators.required]),
    dbPassword:       new FormControl(''),
    dbSchema:         new FormControl(''),
    jvmHeapMin:       new FormControl('512m', [Validators.required]),
    jvmHeapMax:       new FormControl('2g', [Validators.required]),
    jvmMetaspaceSize: new FormControl('256m'),
    jvmMetaspaceMax:  new FormControl('512m'),
    jvmDirectMemory:  new FormControl('256m'),
    jvmGcPause:       new FormControl('200'),
    jvmAdditionalOptions: new FormControl(''),
    mailUsername:     new FormControl(''),
    mailEmail:        new FormControl('', [Validators.email]),
    fneUrl:           new FormControl(''),
    fneApiKey:        new FormControl(''),
    fnePointOfSale:   new FormControl(''),
    portCom:          new FormControl(''),
  });

  async ngOnInit(): Promise<void> {
    const account = this.accountService.trackCurrentAccount()();
    const isAdmin = !!account && this.navigationService.hasAnyAuthority(Authority.ADMIN, account.authorities);
    if (!isAdmin) {
      void this.router.navigate(['/accessdenied']);
      return;
    }
    await this.loadConfig();
  }

  private async loadConfig(): Promise<void> {
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      const dto = await invoke<AppConfigDto>('get_app_config_dto');
      this.ngZone.run(() => {
        this.form.patchValue({
          serverPort:       dto.server_port,
          dbHost:           dto.db_host,
          dbPort:           dto.db_port,
          dbName:           dto.db_name,
          dbUsername:       dto.db_username,
          dbPassword:       dto.db_password,
          dbSchema:         dto.db_schema,
          jvmHeapMin:       dto.jvm_heap_min,
          jvmHeapMax:       dto.jvm_heap_max,
          jvmMetaspaceSize: dto.jvm_metaspace_size,
          jvmMetaspaceMax:  dto.jvm_metaspace_max,
          jvmDirectMemory:  dto.jvm_direct_memory,
          jvmGcPause:       dto.jvm_gc_pause,
          jvmAdditionalOptions: dto.jvm_additional_options.join('\n'),
          mailUsername:     dto.mail_username,
          mailEmail:        dto.mail_email,
          fneUrl:           dto.fne_url,
          fneApiKey:        dto.fne_api_key,
          fnePointOfSale:   dto.fne_point_of_sale,
          portCom:          dto.port_com,
        });
        this.loading.set(false);
      });
    } catch (e) {
      this.ngZone.run(() => {
        this.errorMessage.set(String(e));
        this.loading.set(false);
      });
    }
  }

  async onSave(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.errorMessage.set('');

    try {
      const { invoke } = await import('@tauri-apps/api/core');
      const v = this.form.getRawValue();
      const dto: AppConfigDto = {
        server_port:        v.serverPort ?? 9080,
        db_host:            v.dbHost ?? '',
        db_port:            v.dbPort ?? 5432,
        db_name:            v.dbName ?? '',
        db_username:        v.dbUsername ?? '',
        db_password:        v.dbPassword ?? '',
        db_schema:          v.dbSchema ?? '',
        jvm_heap_min:       v.jvmHeapMin ?? '512m',
        jvm_heap_max:       v.jvmHeapMax ?? '2g',
        jvm_metaspace_size: v.jvmMetaspaceSize ?? '256m',
        jvm_metaspace_max:  v.jvmMetaspaceMax ?? '512m',
        jvm_direct_memory:  v.jvmDirectMemory ?? '256m',
        jvm_gc_pause:       v.jvmGcPause ?? '200',
        jvm_additional_options: (v.jvmAdditionalOptions ?? '').split('\n').map(s => s.trim()).filter(Boolean),
        mail_username:      v.mailUsername ?? '',
        mail_email:         v.mailEmail ?? '',
        fne_url:            v.fneUrl ?? '',
        fne_api_key:        v.fneApiKey ?? '',
        fne_point_of_sale:  v.fnePointOfSale ?? '',
        port_com:           v.portCom ?? '',
      };
      await invoke('save_app_config_dto', { dto });
      this.appSettingsService.updateApiServerUrl(`http://localhost:${dto.server_port}`);
      this.ngZone.run(() => {
        this.saving.set(false);
        this.startRestart();
      });
    } catch (e) {
      this.ngZone.run(() => {
        this.errorMessage.set(String(e));
        this.saving.set(false);
      });
    }
  }

  private startRestart(): void {
    this.restarting.set(true);
    this.restartMessage.set('Arrêt du serveur...');
    this.restartProgress.set(10);

    this.backendManager.restartBackend().subscribe({
      next: () => {
        this.restartMessage.set('Redémarrage terminé !');
        this.restartProgress.set(100);
        setTimeout(() => window.location.reload(), 1000);
      },
      error: () => {
        this.restarting.set(false);
        this.errorMessage.set("Échec du redémarrage. Relancez l'application manuellement.");
        setTimeout(() => window.location.reload(), 2000);
      },
    });

    const steps = [
      { delay: 500,  progress: 20, msg: 'Arrêt du serveur...' },
      { delay: 1500, progress: 40, msg: 'Démarrage du nouveau processus...' },
      { delay: 3000, progress: 60, msg: 'Initialisation du serveur...' },
      { delay: 5000, progress: 80, msg: 'Vérification de la disponibilité...' },
      { delay: 7000, progress: 95, msg: 'Finalisation...' },
    ];
    steps.forEach(s => setTimeout(() => {
      if (this.restarting() && this.restartProgress() < 100) {
        this.restartProgress.set(s.progress);
        this.restartMessage.set(s.msg);
      }
    }, s.delay));
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!ctrl && ctrl.invalid && ctrl.touched;
  }

  goBack(): void {
    void this.router.navigate(['/']);
  }
}
