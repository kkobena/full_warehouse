import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { NgbNav, NgbNavChangeEvent, NgbNavItem, NgbNavLink, NgbNavContent, NgbNavOutlet } from '@ng-bootstrap/ng-bootstrap';
import { CommandCommonService } from '../../../../entities/commande/command-common.service';
import { CommandeEnCoursComponent } from '../../ui/commande-en-cours/commande-en-cours.component';
import { AppBonEnCoursComponent } from '../../ui/bon-en-cours/bon-en-cours.component';
import { AppListBonsComponent } from '../../ui/list-bons/list-bons.component';
import { AppRetourFournisseurComponent } from '../retour-fournisseur/retour-fournisseur.component';
import { AppRepartitionStockComponent } from '../repartition-stock/repartition-stock.component';
import { SuggestionHomeComponent } from '../suggestion/suggestion-home.component';
import { CommandeDashboardComponent } from '../commande-dashboard/commande-dashboard.component';
import { SemoisClasseConfigComponent } from '../semois-classe-config/semois-classe-config.component';
import { TranslateService } from '@ngx-translate/core';
import { PrimeNG } from 'primeng/config';

@Component({
  selector: 'app-commande-home',
  templateUrl: './commande-home.component.html',
  styleUrl: './commande-home.component.scss',
  imports: [
    CommonModule,
    RouterModule,
    NgbNav,
    NgbNavItem,
    NgbNavLink,
    NgbNavContent,
    NgbNavOutlet,
    CommandeEnCoursComponent,
    AppBonEnCoursComponent,
    AppListBonsComponent,
    AppRetourFournisseurComponent,
    AppRepartitionStockComponent,
    SuggestionHomeComponent,
    CommandeDashboardComponent,
    SemoisClasseConfigComponent,
  ],
})
export class CommandeHomeComponent implements OnInit {
  protected active = 'DASHBOARD';

  private readonly route = inject(ActivatedRoute);
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly translate = inject(TranslateService);
  private readonly primeNGConfig = inject(PrimeNG);

  ngOnInit(): void {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    this.route.queryParams.subscribe(params => {
      if (params['tab']) {
        this.active = params['tab'];
        this.commandCommonService.updateCommandPreviousActiveNav(this.active);
      } else {
        this.active = this.commandCommonService.commandPreviousActiveNav();
      }
    });
  }

  protected onNavChange(evt: NgbNavChangeEvent): void {
    this.active = evt.nextId;
    this.commandCommonService.updateCommandPreviousActiveNav(this.active);
  }
}
