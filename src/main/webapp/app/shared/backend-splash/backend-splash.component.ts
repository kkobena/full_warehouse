import { ChangeDetectorRef, Component, inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProgressBarModule } from 'primeng/progressbar';
import { BackendStatus, BackendStatusService } from 'app/core/tauri/backend-status.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'jhi-backend-splash',
  templateUrl: './backend-splash.component.html',
  styleUrls: ['./backend-splash.component.scss'],
  imports: [CommonModule, ProgressBarModule],
})
export class BackendSplashComponent implements OnInit, OnDestroy {
  visible = false;
  status: BackendStatus = {
    status: 'initializing',
    progress: 0,
    message: 'Initializing backend...',
  };
  title = 'PharmaSmart';
  private readonly backendStatusService = inject(BackendStatusService);
  private readonly cdr = inject(ChangeDetectorRef);
  private subscription?: Subscription;

  ngOnInit(): void {
    // Detect mode from status messages
    this.subscription = this.backendStatusService.getBackendStatus().subscribe(status => {
      console.log('[BackendSplash] Status update:', status);
      this.status = status;

      // Update title based on mode
      if (status.message.includes('Waiting for backend server')) {
        this.title = 'PharmaSmart Client';
      } else if (status.status === 'checking_java' || status.status === 'starting' || status.status === 'launched') {
        this.title = 'PharmaSmart Standalone';
      }

      // Show splash screen only when backend is not ready
      const shouldBeVisible = status.status !== 'ready';
      console.log('[BackendSplash] Status:', status.status, 'Should be visible:', shouldBeVisible);

      this.visible = shouldBeVisible;

      // Force change detection to ensure view updates
      this.cdr.detectChanges();

      console.log('[BackendSplash] Splash visible after update:', this.visible);
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
        return 'fa-times-circle';
      case 'ready':
        return 'fa-check-circle';
      case 'checking_java':
        return 'fa-coffee';
      case 'finding_jar':
        return 'fa-search';
      case 'starting':
      case 'launched':
      case 'waiting':
        return 'fa-spinner fa-spin';
      default:
        return 'fa-cog fa-spin';
    }
  }
}
