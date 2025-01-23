import { Component, OnInit } from '@angular/core';

import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { IMagasin } from 'app/shared/model/magasin.model';
import { MagasinService } from './magasin.service';
import { MagasinDeleteDialogComponent } from './magasin-delete-dialog.component';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { PanelModule } from 'primeng/panel';
import { RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';

@Component({
    selector: 'jhi-magasin',
    templateUrl: './magasin.component.html',
    imports: [WarehouseCommonModule, PanelModule, RouterModule, ButtonModule, RippleModule]
})
export class MagasinComponent implements OnInit {
  magasin?: IMagasin;

  constructor(
    protected magasinService: MagasinService,
    protected modalService: NgbModal,
  ) {}

  loadAll(): void {
    this.magasinService.findPromise().then(magasin => {
      this.magasin = magasin;
    });
  }

  ngOnInit(): void {
    this.loadAll();
    this.registerChangeInMagasins();
  }

  trackId(index: number, item: IMagasin): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  registerChangeInMagasins(): void {
    this.loadAll();
  }

  delete(magasin: IMagasin): void {
    const modalRef = this.modalService.open(MagasinDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
    });
    modalRef.componentInstance.magasin = magasin;
  }
}
