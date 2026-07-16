import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { ICategorie } from 'app/shared/model/categorie.model';
import { CategorieService } from './categorie.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from "@angular/common";
import { AlertErrorComponent } from "../../shared/alert/alert-error.component";
import TranslateDirective from "../../shared/language/translate.directive";
import { FaIconComponent } from "@fortawesome/angular-fontawesome";

@Component({
  templateUrl: './categorie-delete-dialog.component.html',
  imports: [CommonModule, FormsModule, AlertErrorComponent, TranslateDirective, FaIconComponent]
})
export class CategorieDeleteDialogComponent {
  protected categorieService = inject(CategorieService);
  activeModal = inject(NgbActiveModal);

  categorie?: ICategorie;


  constructor() {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.categorieService.delete(id).subscribe(() => {
      this.activeModal.close();
    });
  }
}
