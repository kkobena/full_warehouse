import { Component, input, ChangeDetectionStrategy } from "@angular/core";

import { IUser } from "../user-management.model";
import { ButtonModule } from "primeng/button";
import { RouterLink } from "@angular/router";
import { Toolbar } from "primeng/toolbar";
import { TagModule } from "primeng/tag";
import { ChipModule } from "primeng/chip";
import { CommonModule } from "@angular/common";

@Component({
  selector: "jhi-user-mgmt-detail",
  templateUrl: "./user-management-detail.component.html",
  styleUrl: "./user-management-detail.component.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, ButtonModule, RouterLink, Toolbar, TagModule, ChipModule]
})
export default class UserManagementDetailComponent {
  user = input<IUser | null>(null);
}
