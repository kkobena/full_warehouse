import { Component, inject, OnInit, ChangeDetectionStrategy } from "@angular/core";
import { ActivatedRoute, RouterModule } from "@angular/router";

import { IMagasin } from "app/shared/model/magasin.model";
import { PanelModule } from "primeng/panel";
import TranslateDirective from "../../shared/language/translate.directive";
import { FaIconComponent } from "@fortawesome/angular-fontawesome";

@Component({
  selector: "app-magasin-detail",
  templateUrl: "./magasin-detail.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [PanelModule, RouterModule, TranslateDirective, FaIconComponent]
})
export class MagasinDetailComponent implements OnInit {
  protected activatedRoute = inject(ActivatedRoute);

  magasin: IMagasin | null = null;


  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ magasin }) => (this.magasin = magasin));
  }

  previousState(): void {
    window.history.back();
  }
}
