import { Component, HostBinding, inject, OnInit, Renderer2, RendererFactory2 } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { LangChangeEvent, TranslateService } from '@ngx-translate/core';
import { AsyncPipe, CommonModule } from '@angular/common';
import dayjs from 'dayjs/esm';

import { AccountService } from 'app/core/auth/account.service';
import { AppPageTitleStrategy } from 'app/app-page-title-strategy';
import '@angular/common/locales/global/fr';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { LayoutService } from '../../core/config/layout.service';
import { Observable } from 'rxjs';
import { BackendSplashComponent } from 'app/shared/backend-splash/backend-splash.component';
import { TitlebarComponent } from 'app/shared/titlebar/titlebar.component';
import { TauriPrinterService } from '../../shared/services/tauri-printer.service';

@Component({
  selector: 'jhi-main',
  templateUrl: './main.component.html',
  styleUrl: './main.component.scss',
  providers: [AppPageTitleStrategy, ConfirmationService],
  imports: [RouterOutlet, ConfirmDialogModule, CommonModule, AsyncPipe, BackendSplashComponent, TitlebarComponent],
})
export default class MainComponent implements OnInit {
  layoutMode$: Observable<string>;
  sidebarCollapsed$: Observable<boolean>;
  isTauriMode = false;
  protected readonly layoutService = inject(LayoutService);
  private readonly renderer: Renderer2;
  private readonly router = inject(Router);
  private readonly appPageTitleStrategy = inject(AppPageTitleStrategy);
  private readonly accountService = inject(AccountService);
  private readonly translateService = inject(TranslateService);
  private readonly rootRenderer = inject(RendererFactory2);
  private readonly tauriPrinterService = inject(TauriPrinterService);

  constructor() {
    this.renderer = this.rootRenderer.createRenderer(document.querySelector('html'), null);
    this.layoutMode$ = this.layoutService.layoutMode$;
    this.sidebarCollapsed$ = this.layoutService.sidebarCollapsed$;

    // Detect if running in Tauri
    this.isTauriMode = this.tauriPrinterService.isRunningInTauri();
  }

  @HostBinding('class.tauri-mode') get isTauri(): boolean {
    return this.isTauriMode;
  }

  ngOnInit(): void {
    this.accountService.identity().subscribe();

    this.translateService.onLangChange.subscribe((langChangeEvent: LangChangeEvent) => {
      this.appPageTitleStrategy.updateTitle(this.router.routerState.snapshot);
      dayjs.locale(langChangeEvent.lang);
      this.renderer.setAttribute(document.querySelector('html'), 'lang', langChangeEvent.lang);
    });
  }
}
