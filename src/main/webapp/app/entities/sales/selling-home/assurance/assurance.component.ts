import { Component } from '@angular/core';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { Footer, PrimeTemplate } from 'primeng/api';

@Component({
  selector: 'jhi-assurance',
  standalone: true,
  imports: [ButtonDirective, ConfirmDialogModule, DialogModule, Footer, PrimeTemplate],
  templateUrl: './assurance.component.html',
  styleUrl: './assurance.component.scss',
})
export class AssuranceComponent {
  commonDialog = false;

  onHideHideDialog() {
    this.commonDialog = false;
  }

  cancelCommonDialog(): void {
    this.commonDialog = false;
  }
}
