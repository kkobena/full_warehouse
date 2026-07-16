import { Routes } from "@angular/router";


const remiseRoute: Routes = [
  {
    path: "",
    loadComponent: () => import("./remise-nav/remise-nav.component").then(m => m.RemiseNavComponent),
    data: { defaultSort: "id,asc" }
  }
];

export default remiseRoute;
