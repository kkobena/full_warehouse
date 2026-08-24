import { Component, inject, OnInit, ChangeDetectionStrategy, signal } from "@angular/core";

import { IMagasin } from "app/shared/model/magasin.model";
import { MagasinService } from "./magasin.service";
import { RouterLink } from "@angular/router";
import { CommonModule } from "@angular/common";
import { BadgeComponent, ButtonComponent } from "../../shared/ui";

@Component({
  selector: "app-magasin",
  templateUrl: "./magasin.component.html",
  styleUrls: ["./magasin.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, ButtonComponent, BadgeComponent]
})
export class MagasinComponent implements OnInit {
  protected readonly magasin = signal<IMagasin | undefined>(undefined);
  private readonly magasinService = inject(MagasinService);

  ngOnInit(): void {
    this.registerChangeInMagasins();
  }

  protected loadAll(): void {
    this.magasinService.findCurrentUserMagasin().then(magasin => {
      this.magasin.set(magasin);
    });
  }

  protected registerChangeInMagasins(): void {
    this.loadAll();
  }
}
