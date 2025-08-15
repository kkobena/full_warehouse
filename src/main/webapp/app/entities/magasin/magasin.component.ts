import { Component, inject, OnInit } from '@angular/core';

import { IMagasin } from 'app/shared/model/magasin.model';
import { MagasinService } from './magasin.service';
import { PanelModule } from 'primeng/panel';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'jhi-magasin',
  templateUrl: './magasin.component.html',
  imports: [CommonModule, PanelModule, ButtonModule, RouterLink]
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
