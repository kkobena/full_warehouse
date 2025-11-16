import { Component, input } from '@angular/core';

import { User } from '../user-management.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RouterLink } from '@angular/router';
import { Toolbar } from 'primeng/toolbar';
import { TagModule } from 'primeng/tag';
import { ChipModule } from 'primeng/chip';

@Component({
  selector: 'jhi-user-mgmt-detail',
  templateUrl: './user-management-detail.component.html',
  styleUrl: './user-management-detail.component.scss',
  imports: [WarehouseCommonModule, ButtonModule, RouterLink, Toolbar, TagModule, ChipModule],
})
export default class UserManagementDetailComponent {
  user = input<User | null>(null);
}
