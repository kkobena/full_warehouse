import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IAuthority } from '../../shared/model/authority.model';
import { IMenu, IPrivillegesWrapper } from '../../shared/model/menu.model';
import { HttpResponse } from '@angular/common/http';
import { PrivillegeService } from './privillege.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { PickListModule } from 'primeng/picklist';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { PanelModule } from 'primeng/panel';

@Component({
  selector: 'jhi-menu-detail',
  templateUrl: './menu-detail.component.html',
  standalone: true,
  imports: [WarehouseCommonModule, PickListModule, ButtonModule, RippleModule, PanelModule],
})
export class MenuDetailComponent implements OnInit {
  entity: IAuthority | null = null;
  associes?: IMenu[];
  others?: IMenu[];

  constructor(
    protected privillegeService: PrivillegeService,
    protected activatedRoute: ActivatedRoute,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ privilege }) => {
      this.entity = privilege;
      this.loadAll();
    });
  }

  onMoveToTarget(): void {
    this.updateAuthority();
  }

  onMoveToSource(): void {
    this.updateAuthority();
  }

  loadAll(): void {
    this.privillegeService.getAllPrivillegesByRole(this.entity.name).subscribe((res: HttpResponse<IPrivillegesWrapper>) => {
      const wrapper: IPrivillegesWrapper = res.body;
      this.associes = wrapper.associes;
      this.others = wrapper.others;
      this.cdr.markForCheck();
    });
  }

  previousState(): void {
    window.history.back();
  }

  onMoveAllToTarget(): void {
    console.log(this.associes);
    this.updateAuthority();
  }

  onMoveAllToSource(): void {
    this.updateAuthority();
  }

  private updateAuthority(): void {
    const authoritiesName = this.associes.map(e => e.name);
    this.entity.privilleges = authoritiesName;
    this.privillegeService.update(this.entity).subscribe({
      error: () => {
        this.loadAll();
      },
    });
  }
}
