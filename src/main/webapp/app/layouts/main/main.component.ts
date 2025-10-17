import { Component, inject, OnInit, Renderer2, RendererFactory2 } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { LangChangeEvent, TranslateService } from '@ngx-translate/core';
import { AsyncPipe, CommonModule, NgIf } from '@angular/common';
import dayjs from 'dayjs/esm';

import { AccountService } from 'app/core/auth/account.service';
import { AppPageTitleStrategy } from 'app/app-page-title-strategy';
import '@angular/common/locales/global/fr';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { LayoutService } from '../../core/config/layout.service';
import { Observable } from 'rxjs';


import { TitleBarComponent } from '../title-bar/title-bar.component';


@Component({
  selector: 'jhi-main',
  templateUrl: './main.component.html',
  styleUrl: './main.component.scss',
  providers: [AppPageTitleStrategy, ConfirmationService],
  imports: [RouterOutlet, ConfirmDialogModule, CommonModule, AsyncPipe, TitleBarComponent],
})
export default class MainComponent implements OnInit {
  private readonly renderer: Renderer2;

  private readonly router = inject(Router);
  private readonly appPageTitleStrategy = inject(AppPageTitleStrategy);
  private readonly accountService = inject(AccountService);
  private readonly translateService = inject(TranslateService);
  private readonly rootRenderer = inject(RendererFactory2);
  protected readonly layoutService = inject(LayoutService);

  layoutMode$: Observable<string>;
  sidebarCollapsed$: Observable<boolean>;

  constructor() {
    this.renderer = this.rootRenderer.createRenderer(document.querySelector('html'), null);
    this.layoutMode$ = this.layoutService.layoutMode$;
    this.sidebarCollapsed$ = this.layoutService.sidebarCollapsed$;
  }

  public isElectron = (): boolean => {
    return !!window.ipcRenderer;
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
