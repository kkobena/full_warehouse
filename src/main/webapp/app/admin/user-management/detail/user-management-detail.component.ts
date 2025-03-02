import { Component, input } from '@angular/core';

import { User } from '../user-management.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { PanelModule } from 'primeng/panel';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'jhi-user-mgmt-detail',
  templateUrl: './user-management-detail.component.html',
  imports: [WarehouseCommonModule, ButtonModule, PanelModule, RouterLink],
})
export default class UserManagementDetailComponent {
  user = input<User | null>(null);
}
