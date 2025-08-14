import { Component, input } from '@angular/core';

import { User } from '../user-management.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { PanelModule } from 'primeng/panel';
import { RouterLink } from '@angular/router';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Toolbar } from 'primeng/toolbar';

@Component({
  selector: 'jhi-user-mgmt-detail',
  templateUrl: './user-management-detail.component.html',
  imports: [WarehouseCommonModule, ButtonModule, PanelModule, RouterLink, IconField, InputIcon, InputText, Toolbar]
})
export default class UserManagementDetailComponent {
  user = input<User | null>(null);
}
