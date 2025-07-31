import { Component, inject, OnInit, viewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';

import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { IUser, User } from '../../core/user/user.model';
import { UserService } from '../../core/user/user.service';
import { TranslateService } from '@ngx-translate/core';
import { ListAjustementComponent } from './list-ajustement/list-ajustement.component';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { NgxSpinnerModule } from 'ngx-spinner';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { FormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { AjustementService } from './ajustement.service';
import { Select } from 'primeng/select';
import { APPEND_TO } from '../../shared/constants/pagination.constants';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { DatePickerComponent } from '../../shared/date-picker/date-picker.component';
import { FloatLabel } from 'primeng/floatlabel';
import { Panel } from 'primeng/panel';

@Component({
  selector: 'jhi-ajustement',
  templateUrl: './ajustement.component.html',
  imports: [
    WarehouseCommonModule,
    DividerModule,
    DropdownModule,
    ButtonModule,
    TableModule,
    NgxSpinnerModule,
    CardModule,
    ToolbarModule,
    RouterModule,
    FormsModule,
    InputTextModule,
    ListAjustementComponent,
    Select,
    IconField,
    InputIcon,
    DatePickerComponent,
    FloatLabel,
    Panel,
  ],
})
export class AjustementComponent implements OnInit {
  translate = inject(TranslateService);
  ajustementList = viewChild(ListAjustementComponent);
  ajustementService = inject(AjustementService);
  protected userService = inject(UserService);
  protected router = inject(Router);
  protected activatedRoute = inject(ActivatedRoute);
  protected search = '';
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected users: IUser[] = [];
  protected user?: IUser | null = null;
  protected active = 'CLOSED';
  protected readonly appendTo = APPEND_TO;

  ngOnInit(): void {
    this.user = { id: null, abbrName: 'TOUT' };
    this.loadAllUsers();
  }

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => {
      if (res.body) {
        this.users = [{ id: null, abbrName: 'TOUT' }];
        this.users = [...this.users, ...res.body];
      }
    });
  }

  protected onSearch(): void {
    this.ajustementService.updateToolbarParam({
      search: this.search,
      fromDate: this.fromDate,
      toDate: this.toDate,
      userId: this.user?.id,
    });
    this.ajustementList().onSearch();
  }

  protected onSelectUser(): void {
    this.onSearch();
  }
}
