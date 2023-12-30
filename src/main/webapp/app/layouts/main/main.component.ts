import { Component, OnInit, Renderer2, RendererFactory2 } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { LangChangeEvent, TranslateService } from '@ngx-translate/core';
import dayjs from 'dayjs/esm';

import { AccountService } from 'app/core/auth/account.service';
import { AppPageTitleStrategy } from '../../app-page-title-strategy';
import '@angular/common/locales/global/fr';

@Component({
  selector: 'jhi-main',
  templateUrl: './main.component.html',
  standalone: true,
  providers: [AppPageTitleStrategy],
  imports: [RouterOutlet],
})
export default class MainComponent implements OnInit {
  private renderer: Renderer2;

  constructor(
    private accountService: AccountService,
    private appPageTitleStrategy: AppPageTitleStrategy,
    private router: Router,
    private translateService: TranslateService,
    rootRenderer: RendererFactory2,
  ) {
    this.renderer = rootRenderer.createRenderer(document.querySelector('html'), null);
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
