import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from 'app/shared/shared.module';
import { HOME_ROUTE } from './home.route';
import { HomeComponent } from './home.component';
import { AgGridModule } from 'ag-grid-angular';
import { DailyDataComponent } from './daily/daily-data/daily-data.component';
import { HalfyearlyDataComponent } from './halfyearly/halfyearly-data/halfyearly-data.component';
import { MonthlyDataComponent } from './monthly/monthly-data/monthly-data.component';
import { WeeklyDataComponent } from './weekly/weekly-data/weekly-data.component';
import { YearlyDataComponent } from './yearly/yearly-data/yearly-data.component';
import { HomeGrapheComponent } from './home-graphe/home-graphe.component';
import { RapportActivityComponent } from './rapport-activity/rapport-activity.component';
import { GrapheDailyComponent } from './home-graphe/daily/graphe-daily.component';
import { GrapheHalfyearlyComponent } from './home-graphe/halfyearly/graphe-halfyearly.component';
import { GrapheYearlyComponent } from './home-graphe/yearly/graphe-yearly.component';
import { GrapheMonthlyComponent } from './home-graphe/monthly/graphe-monthly.component';
import { GrapheWeeklyComponent } from './home-graphe/weekly/graphe-weekly.component';

@NgModule({
  imports: [SharedModule, AgGridModule, RouterModule.forChild([HOME_ROUTE])],
  declarations: [
    HomeComponent,
    DailyDataComponent,
    HalfyearlyDataComponent,
    MonthlyDataComponent,
    WeeklyDataComponent,
    YearlyDataComponent,
    HomeGrapheComponent,
    RapportActivityComponent,
    GrapheDailyComponent,
    GrapheHalfyearlyComponent,
    GrapheYearlyComponent,
    GrapheMonthlyComponent,
    GrapheWeeklyComponent,
  ],
})
export class HomeModule {}
