import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IAuthority } from '../../shared/model/authority.model';
import { IMenu, IPrivillegesWrapper } from '../../shared/model/menu.model';
import { HttpResponse } from '@angular/common/http';
import { PrivillegeService } from './privillege.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { Toolbar } from 'primeng/toolbar';
import { TagModule } from 'primeng/tag';
import { TableModule } from 'primeng/table';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'jhi-menu-detail',
  templateUrl: './menu-detail.component.html',
  styleUrls: ['./menu-detail.component.scss'],
  imports: [
    WarehouseCommonModule,
    TagModule,
    TableModule,
    ButtonModule,
    RippleModule,
    Toolbar,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    TooltipModule,
    FormsModule,
  ],
})
export class MenuDetailComponent implements OnInit {
  constructor(
    private privillegeService: PrivillegeService,
    private activatedRoute: ActivatedRoute,
    private cdr: ChangeDetectorRef,
  ) {}
  entity: IAuthority | null = null;
  associes: IMenu[] = [];
  others: IMenu[] = [];
  selectedAvailable: IMenu[] = [];
  selectedAssociated: IMenu[] = [];
  sourceFilter = '';
  targetFilter = '';
  protected scrollHeight = 'calc(100vh - 350px)';

  get filteredOthers(): IMenu[] {
    if (!this.others) return [];
    if (!this.sourceFilter) return this.others;
    return this.others.filter(item => item.libelle?.toLowerCase().includes(this.sourceFilter.toLowerCase()));
  }

  get filteredAssocies(): IMenu[] {
    if (!this.associes) return [];
    if (!this.targetFilter) return this.associes;
    return this.associes.filter(item => item.libelle?.toLowerCase().includes(this.targetFilter.toLowerCase()));
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ privilege }) => {
      this.entity = privilege;
      this.loadAll();
    });
  }

  loadAll(): void {
    this.privillegeService.getAllPrivillegesByRole(this.entity.name).subscribe((res: HttpResponse<IPrivillegesWrapper>) => {
      const wrapper: IPrivillegesWrapper = res.body;
      this.associes = wrapper.associes || [];
      this.others = wrapper.others || [];
      this.selectedAvailable = [];
      this.selectedAssociated = [];
      this.cdr.markForCheck();
    });
  }

  previousState(): void {
    window.history.back();
  }

  moveToAssociated(): void {
    if (!this.selectedAvailable || this.selectedAvailable.length === 0) return;

    // Move selected items from others to associes
    this.associes = [...this.associes, ...this.selectedAvailable];
    this.others = this.others.filter(item => !this.selectedAvailable.includes(item));
    this.selectedAvailable = [];

    this.updateAuthority();
  }

  moveAllToAssociated(): void {
    if (!this.others || this.others.length === 0) return;

    // Move all items from others to associes
    this.associes = [...this.associes, ...this.others];
    this.others = [];
    this.selectedAvailable = [];

    this.updateAuthority();
  }

  moveToAvailable(): void {
    if (!this.selectedAssociated || this.selectedAssociated.length === 0) return;

    // Move selected items from associes to others
    this.others = [...this.others, ...this.selectedAssociated];
    this.associes = this.associes.filter(item => !this.selectedAssociated.includes(item));
    this.selectedAssociated = [];

    this.updateAuthority();
  }

  moveAllToAvailable(): void {
    if (!this.associes || this.associes.length === 0) return;

    // Move all items from associes to others
    this.others = [...this.others, ...this.associes];
    this.associes = [];
    this.selectedAssociated = [];

    this.updateAuthority();
  }

  private updateAuthority(): void {
    const authoritiesName = this.associes.map(e => e.name);
    this.entity.privilleges = authoritiesName;
    this.privillegeService.update(this.entity).subscribe({
      next: () => {
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadAll();
      },
    });
  }
}
