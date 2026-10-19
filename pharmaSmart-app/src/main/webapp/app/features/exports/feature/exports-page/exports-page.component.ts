import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  Signal,
  signal,
  viewChild
} from "@angular/core";
import { toSignal } from "@angular/core/rxjs-interop";
import { ActivatedRoute, Router } from "@angular/router";
import { NgbNavModule } from "@ng-bootstrap/ng-bootstrap";
import { map } from "rxjs";

import { AbilityService } from "app/core/auth/ability.service";
import { NavTabsComponent, ToolbarComponent } from "app/shared/ui";
import { NavSectionLinkComponent } from "app/shared/ui/nav-sidebar/nav-section-link.component";
import { ExportModele } from "../../models/exports.model";
import { OngletCatalogueComponent } from "../../ui/onglet-catalogue/onglet-catalogue.component";
import { OngletHistoriqueComponent } from "../../ui/onglet-historique/onglet-historique.component";
import { OngletModelesComponent } from "../../ui/onglet-modeles/onglet-modeles.component";

const ONGLETS = [
  { id: "catalogue", code: "exports.catalogue", libelle: "Catalogue", icone: "pi pi-th-large" },
  { id: "historique", code: "exports.historique", libelle: "Historique", icone: "pi pi-history" },
  { id: "modeles", code: "exports.modeles", libelle: "Modèles", icone: "pi pi-bookmark" }
] as const;

/**
 * Menu Exports : catalogue (données brutes, exports existants), historique des fichiers générés, modèles rejouables et
 * programmés. Un export demandé se génère en tâche de fond : on bascule sur l'historique, qui suit son avancement.
 */
@Component({
  selector: "app-exports-page",
  imports: [NgbNavModule, NavTabsComponent, ToolbarComponent, NavSectionLinkComponent, OngletCatalogueComponent, OngletHistoriqueComponent, OngletModelesComponent],
  templateUrl: "./exports-page.component.html",
  styleUrl: "./exports-page.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { "(window:resize)": "mesurerHauteurs()" }
})
export class ExportsPageComponent {
  private readonly ability = inject(AbilityService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  // `read: ElementRef` est indispensable : `#barre` posé sur un composant désigne l'instance de
  // `ToolbarComponent`, pas son élément — sans cela, `nativeElement` est `undefined`.
  private readonly barre: Signal<ElementRef<HTMLElement> | undefined> = viewChild("barre", { read: ElementRef });
  private readonly barreOnglets: Signal<ElementRef<HTMLElement> | undefined> = viewChild("barreOnglets", { read: ElementRef });

  private readonly ongletDemande = toSignal(this.route.queryParamMap.pipe(map(params => params.get("onglet"))));

  protected readonly ongletsVisibles = computed(() => ONGLETS.filter(onglet => this.ability.can("display", onglet.code)));

  protected readonly ongletActif = computed(() => {
    const visibles = this.ongletsVisibles();
    return visibles.find(onglet => onglet.id === this.ongletDemande())?.id ?? visibles[0]?.id;
  });

  /** Modèle proposé à l'onglet Modèles depuis le catalogue (« Enregistrer comme modèle »). */
  protected modeleAPreparer: ExportModele | null = null;

  /** Hauteur de la barre d'outils, sous laquelle les onglets viennent se coller. */
  protected readonly hautToolbar = signal("0px");

  /** Hauteur de la barre d'onglets, sous laquelle se colle à son tour la barre d'ancres du catalogue. */
  protected readonly hautOnglets = signal("0px");

  constructor() {
    afterNextRender(() => this.mesurerHauteurs());
  }

  /**
   * Trois bandes se superposent en haut de l'écran : la barre d'outils, les onglets, puis la barre
   * d'ancres du catalogue. Chacune s'arrête sous la précédente, et aucune hauteur n'est fixe — la
   * barre d'outils grandit quand ses filtres passent à la ligne, les onglets quand leurs libellés
   * se replient. Où la première s'arrête est publié pour toute l'application par `MainComponent`
   * (`--pharma-haut-chrome`).
   */
  protected mesurerHauteurs(): void {
    // Mesures facultatives : une barre absente vaut zéro, la suivante se colle simplement plus
    // haut. Une exception emporterait la page entière — un crochet de rendu qui lève est rejoué à
    // chaque cycle, et le moindre clic suffit alors à figer l'onglet.
    this.hautToolbar.set(mesurer(this.barre()));
    this.hautOnglets.set(mesurer(this.barreOnglets()));
  }

  protected changerOnglet(id: string): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { onglet: id },
      queryParamsHandling: "merge",
      replaceUrl: true
    });
  }

  protected preparerModele(modele: ExportModele): void {
    this.modeleAPreparer = modele;
    this.changerOnglet("modeles");
  }
}

function mesurer(element: ElementRef<HTMLElement> | undefined): string {
  return `${element ? element.nativeElement.getBoundingClientRect().height : 0}px`;
}
