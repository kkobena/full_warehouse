import {ChangeDetectionStrategy, Component, input} from "@angular/core";

import {IUser} from "../user-management.model";
import {RouterLink} from "@angular/router";
import {BadgeComponent, ButtonComponent, ToolbarComponent} from "app/shared/ui";
import {CommonModule} from "@angular/common";

@Component({
  selector: "app-user-mgmt-detail",
  templateUrl: "./user-management-detail.component.html",
  styleUrl: "./user-management-detail.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ButtonComponent, RouterLink, ToolbarComponent, BadgeComponent]
})
export default class UserManagementDetailComponent {
  user = input<IUser | null>(null);
}
