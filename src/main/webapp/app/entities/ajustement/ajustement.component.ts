import { Component, OnInit, ViewChild } from '@angular/core';
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
import { CalendarModule } from 'primeng/calendar';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { FormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';

@Component({
  standalone: true,
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
    CalendarModule,
    RouterModule,
    FormsModule,
    AjustementEnCoursComponent,
    InputTextModule,
    ListAjustementComponent,
  ],
})
export class AjustementComponent implements OnInit {
  protected search: string = '';
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected users: IUser[] = [];
  protected user?: IUser | null;
  protected active = 'PENDING';
  @ViewChild(AjustementEnCoursComponent)
  private ajustementEnCours: AjustementEnCoursComponent;
  @ViewChild(ListAjustementComponent)
  private ajustementList: ListAjustementComponent;

  constructor(
    protected userService: UserService,
    public translate: TranslateService,
    protected router: Router,
    protected activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.loadAllUsers();
  }

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => {
      this.users.push({ id: null, fullName: 'TOUT' });
      if (res.body) {
        this.users.push(...res.body);
      }
      this.user = { id: null, fullName: 'TOUT' };
    });
  }

  protected onSearch(): void {
    if (this.active === AjustementStatut.PENDING) {
      this.ajustementEnCours.onSearch();
    } else {
      this.ajustementList.onSearch();
    }
  }

  protected onSelectUser(): void {
    this.onSearch();
  }
}
