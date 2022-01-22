import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ErrorService {
  constructor(private translateService: TranslateService) {}

  getErrorMessageTranslation(errorKey: string): Observable<any> {
    if (errorKey) {
      return this.translateService.get('error.' + errorKey);
    }
    return new Observable<{}>();
  }
}
