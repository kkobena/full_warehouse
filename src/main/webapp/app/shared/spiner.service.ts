import {inject, Injectable} from '@angular/core';
import {NgxSpinnerService} from "ngx-spinner";

@Injectable({
  providedIn: 'root',
})
export class SpinerService {
  private readonly spinner = inject(NgxSpinnerService);

  show(): void {
    this.spinner.show();
  }
  hide(): void {
    this.spinner.hide();
  }
}
