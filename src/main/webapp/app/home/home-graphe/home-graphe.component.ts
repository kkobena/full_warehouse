import { Component, OnInit } from "@angular/core";
import { WarehouseCommonModule } from "../../shared/warehouse-common/warehouse-common.module";
import { GrapheDailyComponent } from "./daily/graphe-daily.component";

@Component({
  selector: "jhi-home-graphe",
  templateUrl: "./home-graphe.component.html",
  styleUrls: ["./home-graphe.component.scss"],
  imports: [
    WarehouseCommonModule,
    GrapheDailyComponent
  ]
})
export class HomeGrapheComponent implements OnInit {
  active = "graphe-daily";

  constructor() {
  }

  ngOnInit(): void {
  }
}
