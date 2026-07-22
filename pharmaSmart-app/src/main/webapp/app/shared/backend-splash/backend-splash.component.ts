import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  inject,
  OnDestroy,
  OnInit
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {BackendStatus, BackendStatusService} from 'app/core/tauri/backend-status.service';
import {Subscription} from 'rxjs';

// États qui n'impliquent pas l'affichage du splash (états opérationnels normaux)
const HIDDEN_STATES = new Set(['ready', 'stopped', 'stopping']);

@Component({
  selector: 'app-backend-splash',
  templateUrl: './backend-splash.component.html',
  styleUrls: ['./backend-splash.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule],
})
export class BackendSplashComponent implements OnInit, OnDestroy {
  visible = false;
  status: BackendStatus = {
    status: 'initializing',
    progress: 0,
    message: 'Initialisation...',
  };
  title = 'PharmaSmart';
  private readonly backendStatusService = inject(BackendStatusService);
  private readonly cdr = inject(ChangeDetectorRef);
  private subscription?: Subscription;

  ngOnInit(): void {
    this.subscription = this.backendStatusService.getBackendStatus().subscribe(status => {
      this.status = status;
      this.visible = !HIDDEN_STATES.has(status.status);
      this.title = this.resolveTitle(status);
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  close(): void {
    this.visible = false;
  }

  getStatusColor(): string {
    switch (this.status.status) {
      case 'error':
        return 'danger';
      case 'ready':
        return 'success';
      default:
        return 'primary';
    }
  }

  getStatusIcon(): string {
    switch (this.status.status) {
      case 'error':
        return 'fa-times-circle text-danger';
      case 'ready':
        return 'fa-check-circle text-success';
      case 'checking_java':
        return 'fa-coffee';
      case 'finding_jar':
        return 'fa-search';
      case 'restarting':
        return 'fa-refresh fa-spin';
      case 'starting':
      case 'launched':
      case 'waiting':
        return 'fa-spinner fa-spin';
      default:
        return 'fa-cog fa-spin';
    }
  }

  private resolveTitle(status: BackendStatus): string {
    // Mode bundled : le message vient de backend_manager.rs (Rust)
    // Mode standard : le message contient "serveur backend" (service Angular)
    if (status.status === 'waiting' && status.message.toLowerCase().includes('serveur backend')) {
      return 'PharmaSmart Client';
    }
    if (['checking_java', 'finding_jar', 'starting', 'launched', 'restarting'].includes(status.status)) {
      return 'PharmaSmart';
    }
    return this.title; // conserve le titre actuel
  }
}
