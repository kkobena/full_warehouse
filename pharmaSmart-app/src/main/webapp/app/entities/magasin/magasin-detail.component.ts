import { Component, inject, OnInit, ChangeDetectionStrategy, signal } from "@angular/core";
import { ActivatedRoute, RouterModule } from "@angular/router";

import { IMagasin } from "app/shared/model/magasin.model";
import TranslateDirective from "../../shared/language/translate.directive";
import { FaIconComponent } from "@fortawesome/angular-fontawesome";

@Component({
  selector: "app-magasin-detail",
  templateUrl: "./magasin-detail.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterModule, TranslateDirective, FaIconComponent]
})
export class MagasinDetailComponent implements OnInit {
  protected activatedRoute = inject(ActivatedRoute);

  protected readonly magasin = signal<IMagasin | null>(null);


  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ magasin }) => (this.magasin.set(magasin)));
  }

  previousState(): void {
    window.history.back();
  }
}
