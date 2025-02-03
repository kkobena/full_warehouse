import { Component, inject, OnInit, viewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';

import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { IUser, User } from '../../core/user/user.model';
import { UserService } from '../../core/user/user.service';
import { TranslateService } from '@ngx-translate/core';
import { AjustementEnCoursComponent } from './ajustement-en-cours/ajustement-en-cours.component';
import { ListAjustementComponent } from './list-ajustement/list-ajustement.component';
import { AjustementStatut } from '../../shared/model/enumerations/ajustement-statut.model';
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
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { APPEND_TO } from '../../shared/constants/pagination.constants';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputGroup } from 'primeng/inputgroup';
import { InputGroupAddon } from 'primeng/inputgroupaddon';

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
    DatePicker,
    Select,
    IconField,
    InputIcon,
    InputGroup,
    InputGroupAddon,
  ],
})
export class AjustementComponent implements OnInit {
  protected userService = inject(UserService);
  translate = inject(TranslateService);
  protected router = inject(Router);
  protected activatedRoute = inject(ActivatedRoute);

  ajustementEnCours = viewChild(AjustementEnCoursComponent);
  ajustementList = viewChild(ListAjustementComponent);
  ajustementService = inject(AjustementService);
  protected search: string = '';
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected users: IUser[] = [];
  protected userId?: number | null = null;
  protected active = 'CLOSED';
  protected readonly appendTo = APPEND_TO;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  ngOnInit(): void {
    this.loadAllUsers();
  }

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => {
      //  this.users.push({ id: null, fullName: 'TOUT' });
      if (res.body) {
        this.users = [{ id: null, abbrName: 'TOUT' }];

        this.users = [...this.users, ...res.body];
      }
      // this.user = { id: null, fullName: 'TOUT' };
    });
  }

  protected onSearch(): void {
    this.ajustementService.updateToolbarParam({
      search: this.search,
      fromDate: this.fromDate,
      toDate: this.toDate,
      userId: this.userId,
    });
    if (this.active === AjustementStatut.PENDING) {
      this.ajustementEnCours().onSearch();
    } else {
      this.ajustementList().onSearch();
    }
  }

  protected onSelectUser(): void {
    // this.userId = userControl.value;
    this.onSearch();
  }
}
