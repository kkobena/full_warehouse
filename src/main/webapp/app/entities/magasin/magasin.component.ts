import { Component, inject, OnInit } from '@angular/core';

import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { IMagasin } from 'app/shared/model/magasin.model';
import { MagasinService } from './magasin.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { PanelModule } from 'primeng/panel';
import { RouterLink, RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { Card } from 'primeng/card';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'jhi-magasin',
  templateUrl: './magasin.component.html',
  imports: [CommonModule, PanelModule, ButtonModule, Card, TranslatePipe, RouterLink]
})
export class MagasinComponent implements OnInit {
  magasin?: IMagasin;
  private readonly magasinService = inject(MagasinService);


  ngOnInit(): void {
    this.loadAll();
    this.registerChangeInMagasins();
  }

  protected loadAll(): void {
    this.magasinService.findCurrentUserMagasin().then(magasin => {
      this.magasin = magasin;
    });
  }

  protected registerChangeInMagasins(): void {
    this.loadAll();
  }
}
