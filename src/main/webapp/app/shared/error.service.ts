import { inject, Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ErrorService {
  translateService = inject(TranslateService);

  getErrorMessageTranslation(errorKey: string): Observable<any> {
    if (errorKey) {
      return this.translateService.get('error.' + errorKey);
    }
    return new Observable<{}>();
  }

  getErrorMessage(error: any): string {
    const status = error.status;
    if (status !== 500) {
      return error?.error?.message || 'Erreur interne du serveur.';
    }
    return 'Erreur interne du serveur.';
  }
}
