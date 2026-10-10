import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { map } from 'rxjs';

import { AbilityService } from 'app/core/auth/ability.service';
import { NavTabsComponent, ToolbarComponent } from 'app/shared/ui';
import { NavSectionLinkComponent } from 'app/shared/ui/nav-sidebar/nav-section-link.component';
import { ExportModele } from '../../models/exports.model';
import { OngletCatalogueComponent } from '../../ui/onglet-catalogue/onglet-catalogue.component';
import { OngletHistoriqueComponent } from '../../ui/onglet-historique/onglet-historique.component';
import { OngletModelesComponent } from '../../ui/onglet-modeles/onglet-modeles.component';

const ONGLETS = [
  { id: 'catalogue', code: 'exports.catalogue', libelle: 'Catalogue', icone: 'pi pi-th-large' },
  { id: 'historique', code: 'exports.historique', libelle: 'Historique', icone: 'pi pi-history' },
  { id: 'modeles', code: 'exports.modeles', libelle: 'Modèles', icone: 'pi pi-bookmark' },
] as const;

/**
 * Menu Exports : catalogue (données brutes, exports existants), historique des fichiers générés, modèles rejouables et
 * programmés. Un export demandé se génère en tâche de fond : on bascule sur l'historique, qui suit son avancement.
 */
@Component({
  selector: 'app-exports-page',
  imports: [NgbNavModule, NavTabsComponent, ToolbarComponent, NavSectionLinkComponent, OngletCatalogueComponent, OngletHistoriqueComponent, OngletModelesComponent],
  templateUrl: './exports-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExportsPageComponent {
  private readonly ability = inject(AbilityService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly ongletDemande = toSignal(this.route.queryParamMap.pipe(map(params => params.get('onglet'))));

  protected readonly ongletsVisibles = computed(() => ONGLETS.filter(onglet => this.ability.can('display', onglet.code)));

  protected readonly ongletActif = computed(() => {
    const visibles = this.ongletsVisibles();
    return visibles.find(onglet => onglet.id === this.ongletDemande())?.id ?? visibles[0]?.id;
  });

  /** Modèle proposé à l'onglet Modèles depuis le catalogue (« Enregistrer comme modèle »). */
  protected modeleAPreparer: ExportModele | null = null;

  protected changerOnglet(id: string): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { onglet: id }, queryParamsHandling: 'merge', replaceUrl: true });
  }

  protected preparerModele(modele: ExportModele): void {
    this.modeleAPreparer = modele;
    this.changerOnglet('modeles');
  }
}
