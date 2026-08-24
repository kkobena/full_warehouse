import { Component, inject, OnInit, ChangeDetectionStrategy, signal } from "@angular/core";
import { ActivatedRoute, RouterModule } from "@angular/router";

import { ICategorie } from "app/shared/model/categorie.model";
import { AlertErrorComponent } from "../../shared/alert/alert-error.component";
import TranslateDirective from "../../shared/language/translate.directive";
import { FaIconComponent } from "@fortawesome/angular-fontawesome";

@Component({
  selector: "app-categorie-detail",
  templateUrl: "./categorie-detail.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterModule, AlertErrorComponent, TranslateDirective, FaIconComponent]
})
export class CategorieDetailComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);

  protected readonly categorie = signal<ICategorie | null>(null);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ categorie }) => (this.categorie.set(categorie)));
  }

  previousState(): void {
    window.history.back();
  }
}
