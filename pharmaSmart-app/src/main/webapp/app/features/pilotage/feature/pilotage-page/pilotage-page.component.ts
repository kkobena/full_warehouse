import { afterNextRender, ChangeDetectionStrategy, Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap, Params, Router } from '@angular/router';
import { NgbNavModule, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { map } from 'rxjs';

import { AbilityService } from 'app/core/auth/ability.service';
import { NavTabsComponent, ToolbarComponent } from 'app/shared/ui';
import { NavSectionLinkComponent } from 'app/shared/ui/nav-sidebar/nav-section-link.component';
import { Granularite, PeriodePredefinie, RequetePilotage, TypeComparaison } from '../../models/pilotage.model';
import { ONGLETS_PILOTAGE } from '../../pilotage-onglets';
import { BarrePeriodeComponent, calculerPeriode } from '../../ui/barre-periode/barre-periode.component';
import { OngletAVenirComponent } from '../../ui/onglet-a-venir/onglet-a-venir.component';
import { OngletAchatsStockComponent } from '../onglet-achats-stock/onglet-achats-stock.component';
import { OngletAnalyserComponent } from '../onglet-analyser/onglet-analyser.component';
import { OngletClientsEquipeComponent } from '../onglet-clients-equipe/onglet-clients-equipe.component';
import { OngletObjectifsComponent } from '../onglet-objectifs/onglet-objectifs.component';
import { OngletComparerAnneesComponent } from '../onglet-comparer-annees/onglet-comparer-annees.component';
import { OngletRentabiliteComponent } from '../onglet-rentabilite/onglet-rentabilite.component';
import { OngletTableauDeBordComponent } from '../onglet-tableau-de-bord/onglet-tableau-de-bord.component';
import { OngletTresorerieComponent } from '../onglet-tresorerie/onglet-tresorerie.component';

/**
 * Page unique du pilotage : un onglet par question, filtrés par droit. L'URL porte l'onglet actif et la requête (période,
 * comparaison, découpage) : un lien partagé ou un retour arrière retombe au même endroit. Une période prédéfinie se recalcule
 * à partir d'aujourd'hui ; seule une période personnalisée garde ses dates dans l'URL.
 */
@Component({
  selector: 'app-pilotage-page',
  imports: [
    NgbTooltip,
    NgbNavModule,
    NavTabsComponent,
    ToolbarComponent,
    NavSectionLinkComponent,
    OngletAVenirComponent,
    BarrePeriodeComponent,
    OngletTableauDeBordComponent,
    OngletAnalyserComponent,
    OngletComparerAnneesComponent,
    OngletRentabiliteComponent,
    OngletAchatsStockComponent,
    OngletTresorerieComponent,
    OngletClientsEquipeComponent,
    OngletObjectifsComponent,
  ],
  templateUrl: './pilotage-page.component.html',
  styleUrl: './pilotage-page.component.scss',
  host: { '(window:resize)': 'mesurerHauteur()' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PilotagePageComponent {
  private readonly ability = inject(AbilityService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly parametres = toSignal(this.route.queryParamMap);
  private readonly ongletDemande = toSignal(this.route.queryParamMap.pipe(map(params => params.get('onglet'))));

  protected readonly ongletsVisibles = computed(() => ONGLETS_PILOTAGE.filter(onglet => this.ability.can('display', onglet.code)));
  protected readonly avecObjectifs = computed(() => this.ongletsVisibles().some(onglet => onglet.id === 'objectifs'));

  /** L'onglet de l'URL s'il est autorisé, sinon le premier autorisé. */
  protected readonly ongletActif = computed(() => {
    const visibles = this.ongletsVisibles();
    const demande = this.ongletDemande();
    return visibles.find(onglet => onglet.id === demande)?.id ?? visibles[0]?.id;
  });

  protected readonly requete = computed(() => lireRequete(this.parametres()));

  private readonly page = viewChild.required<ElementRef<HTMLElement>>('page');
  /** Hauteur de la page jusqu'au bas de l'écran, réserve du bas de la zone principale (bannière de licence) déduite. */
  protected readonly hauteur = signal<string | null>(null);

  constructor() {
    afterNextRender(() => this.mesurerHauteur());
  }

  protected mesurerHauteur(): void {
    const haut = this.page().nativeElement.getBoundingClientRect().top + window.scrollY;
    const zone = document.getElementById('main-content');
    const reserveBas = zone ? parseFloat(getComputedStyle(zone).paddingBottom) || 0 : 0;
    this.hauteur.set(`${Math.max(0, window.innerHeight - haut - reserveBas - MARGE_BAS)}px`);
  }

  protected changerOnglet(id: string): void {
    this.naviguer({ onglet: id });
  }

  protected changerRequete(requete: RequetePilotage): void {
    const personnalisee = requete.predefinie === 'PERSONNALISEE';
    this.naviguer({
      periode: requete.predefinie,
      du: personnalisee ? requete.du : null,
      au: personnalisee ? requete.au : null,
      comparaison: requete.comparaison,
      annees: requete.comparaison === 'ANNEE_N_MOINS_K' ? requete.anneesEnArriere : null,
      aDate: requete.aDate,
      granularite: requete.granularite,
    });
  }

  private naviguer(queryParams: Params): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams, queryParamsHandling: 'merge', replaceUrl: true });
  }
}

/** Marge sous la page : le bas de la carte qui l'entoure. */
const MARGE_BAS = 16;

/** Valeurs par défaut du plan : mois en cours, comparé à date au même mois N-1, découpé par mois. */
function lireRequete(params: ParamMap | undefined): RequetePilotage {
  const predefinie = (params?.get('periode') as PeriodePredefinie | null) ?? 'MOIS_EN_COURS';
  const bornes =
    predefinie === 'PERSONNALISEE' ? { du: params?.get('du') ?? '', au: params?.get('au') ?? '' } : calculerPeriode(predefinie);
  return {
    predefinie,
    ...bornes,
    comparaison: (params?.get('comparaison') as TypeComparaison | null) ?? 'MEME_PERIODE_N_1',
    anneesEnArriere: Number(params?.get('annees') ?? 1),
    aDate: params?.get('aDate') !== 'false',
    granularite: (params?.get('granularite') as Granularite | null) ?? 'MOIS',
  };
}
