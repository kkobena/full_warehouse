import { Component, DestroyRef, inject, OnInit, Renderer2, RendererFactory2 } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterOutlet } from '@angular/router';
import { LangChangeEvent, TranslateService } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { fromEvent } from 'rxjs';
import { filter } from 'rxjs/operators';
import dayjs from 'dayjs/esm';

import { AccountService } from 'app/core/auth/account.service';
import { AppPageTitleStrategy } from 'app/app-page-title-strategy';
import '@angular/common/locales/global/fr';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { LayoutService } from '../../core/config/layout.service';
import { BackendSplashComponent } from 'app/shared/backend-splash/backend-splash.component';
import { TitlebarComponent } from 'app/shared/titlebar/titlebar.component';
import { TauriPrinterService } from '../../shared/services/tauri-printer.service';
import { BreadcrumbComponent } from '../../shared/components/breadcrumb/breadcrumb.component';
import { PeremptionAlertService } from '../../shared/services/peremption-alert.service';

@Component({
  selector: 'jhi-main',
  templateUrl: './main.component.html',
  styleUrl: './main.component.scss',
  providers: [AppPageTitleStrategy, ConfirmationService],
  imports: [RouterOutlet, ConfirmDialogModule, CommonModule, BackendSplashComponent, TitlebarComponent, BreadcrumbComponent],
  host: { '[class.tauri-mode]': 'isTauriMode' },
})
export default class MainComponent implements OnInit {
  isTauriMode = false;
  protected readonly layoutService = inject(LayoutService);
  private readonly renderer: Renderer2;
  private readonly router = inject(Router);
  private readonly appPageTitleStrategy = inject(AppPageTitleStrategy);
  private readonly accountService = inject(AccountService);
  private readonly translateService = inject(TranslateService);
  private readonly rootRenderer = inject(RendererFactory2);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly peremptionAlertService = inject(PeremptionAlertService);
  private readonly ALERT_INTERVAL_MS = 15 * 60 * 1000; // 15 minutes

  constructor() {
    this.renderer = this.rootRenderer.createRenderer(document.querySelector('html'), null);
    this.isTauriMode = this.tauriPrinterService.isRunningInTauri();

    /** Alt+V → Nouvelle Vente  */
    fromEvent<KeyboardEvent>(window, 'keydown')
      .pipe(
        filter(e => e.altKey && e.key.toLowerCase() === 'v'),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe(e => {
        e.preventDefault();
        void this.router.navigate(['/sales-home']);
      });
  }

  ngOnInit(): void {
    this.accountService.identity().subscribe();

    this.translateService.onLangChange.subscribe((langChangeEvent: LangChangeEvent) => {
      this.appPageTitleStrategy.updateTitle(this.router.routerState.snapshot);
      dayjs.locale(langChangeEvent.lang);
      this.renderer.setAttribute(document.querySelector('html'), 'lang', langChangeEvent.lang);
    });

    // Alertes péremptions : fetch initial + polling toutes les 15 min
    this.peremptionAlertService.fetchAlerts();
    setInterval(() => this.peremptionAlertService.fetchAlerts(), this.ALERT_INTERVAL_MS);
  }
}
