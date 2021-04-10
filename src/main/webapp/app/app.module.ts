import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import './vendor';
import { WarehouseSharedModule } from 'app/shared/shared.module';
import { WarehouseCoreModule } from 'app/core/core.module';
import { WarehouseAppRoutingModule } from './app-routing.module';
import { WarehouseHomeModule } from './home/home.module';
import { WarehouseEntityModule } from './entities/entity.module';
import { MainComponent } from './layouts/main/main.component';
import { NavbarComponent } from './layouts/navbar/navbar.component';
import { FooterComponent } from './layouts/footer/footer.component';
import { PageRibbonComponent } from './layouts/profiles/page-ribbon.component';
import { ActiveMenuDirective } from './layouts/navbar/active-menu.directive';
import { ErrorComponent } from './layouts/error/error.component';
import { ChartsModule } from 'ng2-charts';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

@NgModule({
  imports: [
    BrowserModule,
    WarehouseSharedModule,
    WarehouseCoreModule,
    WarehouseHomeModule,
    WarehouseEntityModule,
    BrowserAnimationsModule,
    WarehouseAppRoutingModule,
    ChartsModule,
  ],

  declarations: [MainComponent, NavbarComponent, ErrorComponent, PageRibbonComponent, ActiveMenuDirective, FooterComponent],
  bootstrap: [MainComponent],
})
export class WarehouseAppModule {}
