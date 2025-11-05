import { Component, inject, OnInit } from '@angular/core';

import { IMagasin } from 'app/shared/model/magasin.model';
import { MagasinService } from './magasin.service';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-magasin',
  templateUrl: './magasin.component.html',
  styleUrl: './magasin.component.scss',
  imports: [CommonModule, CardModule, ButtonModule, TagModule, ToolbarModule, RouterLink, WarehouseCommonModule],
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
