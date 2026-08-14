import { Injectable, inject } from '@angular/core';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { LicenseService } from 'app/core/license/license.service';

@Injectable()
export class AppPageTitleStrategy extends TitleStrategy {
  private readonly translateService = inject(TranslateService);
  private readonly licenseService = inject(LicenseService);

  override updateTitle(routerState: RouterStateSnapshot): void {
    let pageTitle = this.buildTitle(routerState);
    if (!pageTitle) {
      pageTitle = 'global.title';
    }
    this.translateService.get(pageTitle).subscribe(title => {
      // Mention « DÉMO » dans le titre de la fenêtre (§3.5). En mode Tauri, le titre de la fenêtre
      // est souvent la seule chose visible sur une capture d'écran ou une barre des tâches : c'est
      // le dernier repère qui empêche de confondre une démonstration avec une installation réelle.
      document.title = this.licenseService.isDemo() ? `[DÉMO] ${title}` : title;
    });
  }
}
