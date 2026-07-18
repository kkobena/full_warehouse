import { Component, inject, OnInit, ChangeDetectionStrategy } from "@angular/core";
import { ActivatedRoute, RouterModule } from "@angular/router";

import { ICategorie } from "app/shared/model/categorie.model";
import { AlertErrorComponent } from "../../shared/alert/alert-error.component";
import TranslateDirective from "../../shared/language/translate.directive";
import { FaIconComponent } from "@fortawesome/angular-fontawesome";

@Component({
  selector: "app-categorie-detail",
  templateUrl: "./categorie-detail.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [RouterModule, AlertErrorComponent, TranslateDirective, FaIconComponent]
})
export class CategorieDetailComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);

  categorie: ICategorie | null = null;

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ categorie }) => (this.categorie = categorie));
  }

  previousState(): void {
    window.history.back();
  }
}
