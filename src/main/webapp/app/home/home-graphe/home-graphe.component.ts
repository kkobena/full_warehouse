import { Component, OnInit } from '@angular/core';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { GrapheYearlyComponent } from './yearly/graphe-yearly.component';
import { GrapheWeeklyComponent } from './weekly/graphe-weekly.component';
import { GrapheHalfyearlyComponent } from './halfyearly/graphe-halfyearly.component';
import { GrapheMonthlyComponent } from './monthly/graphe-monthly.component';
import { GrapheDailyComponent } from './daily/graphe-daily.component';

@Component({
    selector: 'jhi-home-graphe',
    templateUrl: './home-graphe.component.html',
    styleUrls: ['./home-graphe.component.scss'],
    imports: [
        WarehouseCommonModule,
        GrapheYearlyComponent,
        GrapheWeeklyComponent,
        GrapheHalfyearlyComponent,
        GrapheMonthlyComponent,
        GrapheDailyComponent,
    ]
})
export class HomeGrapheComponent implements OnInit {
  active = 'graphe-daily';

  constructor() {}

  ngOnInit(): void {}
}
