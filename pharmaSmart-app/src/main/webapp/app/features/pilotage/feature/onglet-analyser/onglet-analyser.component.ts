import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';

import { AbilityService } from 'app/core/auth/ability.service';
import { ButtonComponent, CardComponent, FormFieldComponent, MultiSelectComponent, PillSelectorComponent, SelectComponent } from 'app/shared/ui';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import {
  AffichageAnalyse,
  AnalysePilotage,
  Axe,
  AxeAnalyse,
  ElementAnalyse,
  EtapeDescente,
  ReglageAnalyse,
  RequetePilotage,
  TriAnalyse,
  VuePilotage,
} from '../../models/pilotage.model';
import { CroiseAnalyseComponent } from '../../ui/croise-analyse/croise-analyse.component';
import { FilDescenteComponent } from '../../ui/fil-descente/fil-descente.component';
import { GraphiqueAnalyseComponent } from '../../ui/graphique-analyse/graphique-analyse.component';
import { PanneauExplicationComponent } from '../../ui/panneau-explication/panneau-explication.component';
import { PanneauVentesProduitComponent } from '../../ui/panneau-ventes-produit/panneau-ventes-produit.component';
import { PanneauVueComponent } from '../../ui/panneau-vue/panneau-vue.component';
import { TableauAnalyseComponent } from '../../ui/tableau-analyse/tableau-analyse.component';
import { elargirAuxTreizeTranches, libellerHorizon } from '../../data-access/horizon-courbe';
import { lireReglage, preparerRequete, versParametresUrl } from './reglage-analyse';

const INDICATEURS_MAX = 3;
const CLE_AIDE = 'pilotage.analyser.aide';
const GROUPES_VUES = ['Mes vues', "Vues de l'équipe", 'Vues livrées'];

/**
 * Onglet Analyser : un à trois indicateurs ventilés par un axe (puis un second), descente élément par élément jusqu'aux ventes
 * d'un produit, explication d'un écart, vues enregistrées. Tout le réglage est dans l'URL, comme la période.
 */
@Component({
  selector: 'app-onglet-analyser',
  imports: [
    FormsModule,
    NgbTooltip,
    FormFieldComponent,
    SelectComponent,
    MultiSelectComponent,
    PillSelectorComponent,
    ButtonComponent,
    CardComponent,
    FilDescenteComponent,
    GraphiqueAnalyseComponent,
    TableauAnalyseComponent,
    CroiseAnalyseComponent,
    PanneauExplicationComponent,
    PanneauVentesProduitComponent,
    PanneauVueComponent,
  ],
  templateUrl: './onglet-analyser.component.html',
  styleUrl: './onglet-analyser.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OngletAnalyserComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly telechargement = inject(BlobDownloadService);
  private readonly ability = inject(AbilityService);

  private readonly parametres = toSignal(this.route.queryParamMap);
  protected readonly reglage = computed(() => lireReglage(this.parametres()));
  protected readonly vueChoisie = computed(() => Number(this.parametres()?.get('vue')) || null);

  protected readonly peutExporter = this.ability.canSignal('export', 'pilotage.analyser');
  protected readonly peutVoirVentes = computed(() => this.ability.can('display', 'catalogue') || this.ability.can('display', 'ventes.kpi'));

  protected readonly indicateurs = rxResource({ stream: () => this.api.listerIndicateurs() });
  protected readonly axes = rxResource({ stream: () => this.api.listerAxes() });
  protected readonly vues = rxResource({ stream: () => this.api.listerVues() });

  protected readonly donnees = rxResource({
    params: () => ({ requete: this.requete(), reglage: this.reglage() }),
    stream: ({ params }) => this.api.analyser(params.requete, params.reglage),
  });

  /** La courbe suit les éléments sur 13 tranches : sur la seule période de la barre (un mois découpé par mois), elle n'aurait qu'un point. */
  protected readonly courbe = rxResource({
    params: () =>
      this.reglage().affichage === 'COURBE' ? { requete: elargirAuxTreizeTranches(this.requete()), reglage: preparerRequete(this.reglage()) } : undefined,
    stream: ({ params }) => this.api.analyser(params.requete, params.reglage),
  });
  protected readonly horizonCourbe = computed(() => libellerHorizon(this.requete().granularite));

  protected readonly erreur = computed(() => lireErreur(this.donnees.error()));

  // Panneaux latéraux.
  protected readonly elementExplique = signal<ElementAnalyse | null>(null);
  protected readonly explicationVisible = signal(false);
  protected readonly produitDetaille = signal<{ cle: string; libelle: string } | null>(null);
  protected readonly ventesVisibles = signal(false);
  protected readonly panneauVueVisible = signal(false);

  /** Aide d'utilisation, repliée par défaut ; le navigateur retient le dernier choix. */
  protected readonly aideVisible = signal(lireAideVisible());
  protected readonly infobulleEnregistrerVue =
    "Garde les indicateurs, les ventilations, le nombre d'éléments, le tri et l'affichage sous un nom, à retrouver dans « Vues enregistrées ». La période reste celle de la barre du haut.";
  protected readonly infobulleModifierVue = 'Met à jour la vue choisie avec le réglage affiché (ou la renomme, ou la partage avec l\'équipe).';

  protected readonly explication = rxResource({
    params: () => {
      const element = this.elementExplique();
      return element ? { requete: this.requete(), reglage: { ...this.reglage(), chemin: this.cheminJusqua(element) } } : undefined;
    },
    stream: ({ params }) => this.api.expliquerEcart(params.requete, params.reglage),
  });

  protected readonly affichages: { label: string; value: AffichageAnalyse; icon: string }[] = [
    { label: 'Barres', value: 'BARRES', icon: 'pi pi-chart-bar' },
    { label: 'Courbe', value: 'COURBE', icon: 'pi pi-chart-line' },
    { label: 'Tableau', value: 'TABLEAU', icon: 'pi pi-table' },
    { label: 'Croisé', value: 'CROISE', icon: 'pi pi-th-large' },
  ];

  protected readonly tops = [
    { libelle: 'Top 10', valeur: 10 },
    { libelle: 'Top 20', valeur: 20 },
    { libelle: 'Top 50', valeur: 50 },
    { libelle: 'Tout', valeur: 0 },
  ];

  protected readonly tris: { libelle: string; valeur: TriAnalyse }[] = [
    { libelle: 'Valeur', valeur: 'VALEUR' },
    { libelle: 'Plus fortes hausses', valeur: 'ECART_HAUSSE' },
    { libelle: 'Plus fortes baisses', valeur: 'ECART_BAISSE' },
  ];

  /** Mes vues d'abord, puis celles de l'équipe, puis les vues livrées ; ng-select ordonne les groupes comme ils arrivent. */
  protected readonly vuesGroupees = computed(() =>
    (this.vues.value() ?? [])
      .map(vue => ({ ...vue, rang: vue.livree ? 2 : vue.modifiable ? 0 : 1 }))
      .sort((a, b) => a.rang - b.rang)
      .map(vue => ({ ...vue, groupe: GROUPES_VUES[vue.rang] })),
  );

  protected readonly vueModifiable = computed(() => (this.vues.value() ?? []).find(vue => vue.id === this.vueChoisie() && vue.modifiable) ?? null);

  private readonly axesParCode = computed(() => new Map((this.axes.value() ?? []).map(axe => [axe.code, axe])));

  /** « Puis par » : les axes qui se croisent avec l'axe choisi et le chemin de descente (une source commune). */
  protected readonly axesCroisables = computed(() => {
    const reglage = this.reglage();
    return (this.axes.value() ?? []).filter(axe => axe.code !== reglage.axe && this.seCroisent([reglage.axe, axe.code, ...reglage.chemin.map(e => e.axe)]));
  });

  protected readonly libelleAxe = computed(() => this.axesParCode().get(this.reglage().axe)?.libelle ?? '');

  /** Libellés du réglage, pour le résumé du panneau d'enregistrement. */
  protected readonly libellesAxes = computed(() => {
    const reglage = this.reglage();
    return [reglage.axe, reglage.axe2].filter(code => !!code).map(code => this.axesParCode().get(code!)?.libelle ?? code!);
  });
  protected readonly libellesIndicateurs = computed(() => {
    const libelles = new Map((this.indicateurs.value() ?? []).map(indicateur => [indicateur.code, indicateur.libelle]));
    return this.reglage().indicateurs.map(code => libelles.get(code) ?? code);
  });

  protected basculerAide(): void {
    this.aideVisible.update(visible => !visible);
    try {
      localStorage.setItem(CLE_AIDE, String(this.aideVisible()));
    } catch {
      // Stockage indisponible (navigation privée) : le choix vaut pour la session.
    }
  }

  protected choisirVue(selection: unknown): void {
    const vue = (this.vues.value() ?? []).find(v => v.id === selection);
    if (vue) {
      const { indicateurs, axe, axe2, top, tri, affichage } = vue;
      this.naviguer({ indicateurs, axe, axe2, top, tri, affichage, chemin: [] }, vue.id);
    }
  }

  protected choisirIndicateurs(selection: unknown): void {
    const indicateurs = (selection as string[] | null) ?? [];
    if (indicateurs.length) {
      this.modifier({ indicateurs: indicateurs.slice(0, INDICATEURS_MAX) });
    }
  }

  protected choisirAxe(selection: unknown): void {
    const axe = selection as AxeAnalyse | null;
    if (axe) {
      const axe2 = this.reglage().axe2;
      const garderAxe2 = axe2 !== null && axe2 !== axe && this.seCroisent([axe, axe2, ...this.reglage().chemin.map(e => e.axe)]);
      this.modifier({ axe, axe2: garderAxe2 ? axe2 : null });
    }
  }

  protected choisirAxe2(selection: unknown): void {
    this.modifier({ axe2: (selection as AxeAnalyse | null) ?? null });
  }

  protected choisirTop(selection: unknown): void {
    this.modifier({ top: selection as number });
  }

  protected choisirTri(selection: unknown): void {
    this.modifier({ tri: selection as TriAnalyse });
  }

  protected choisirAffichage(selection: unknown): void {
    this.modifier({ affichage: selection as AffichageAnalyse });
  }

  protected descendre(element: ElementAnalyse, analyse: AnalysePilotage): void {
    const suivant = analyse.axe.suivant;
    if (suivant) {
      this.ventilerPar(this.cheminJusqua(element), suivant);
    }
  }

  protected remonter(etapes: number): void {
    const chemin = this.reglage().chemin;
    if (etapes < chemin.length) {
      this.modifier({ axe: chemin[etapes].axe, chemin: chemin.slice(0, etapes), axe2: null });
    }
  }

  protected expliquer(element: ElementAnalyse): void {
    this.elementExplique.set(element);
    this.explicationVisible.set(true);
  }

  protected ventilerElementExplique(axe: AxeAnalyse): void {
    const element = this.elementExplique();
    if (element) {
      this.explicationVisible.set(false);
      this.ventilerPar(this.cheminJusqua(element), axe);
    }
  }

  protected voirVentes(element: ElementAnalyse): void {
    this.produitDetaille.set({ cle: element.cle!, libelle: element.libelle });
    this.ventesVisibles.set(true);
  }

  protected enregistrerVue(vue: VuePilotage): void {
    this.api.enregistrerVue(vue).subscribe(enregistree => {
      this.panneauVueVisible.set(false);
      this.vues.reload();
      this.naviguer(this.reglage(), enregistree.id);
    });
  }

  protected supprimerVue(vue: VuePilotage): void {
    this.api.supprimerVue(vue.id!).subscribe(() => {
      this.vues.reload();
      this.naviguer(this.reglage(), null);
    });
  }

  /** Le tableau tel qu'affiché, généré par le serveur : valeurs complètes, référence à côté de chaque valeur. */
  protected exporter(analyse: AnalysePilotage): void {
    const requete = this.requete();
    this.telechargement.downloadFromObservable(
      this.api.exporterAnalyse(requete, this.reglage()),
      `pilotage-${analyse.axe.code.toLowerCase()}-${requete.du}-${requete.au}`,
      'csv',
    );
  }

  private ventilerPar(chemin: EtapeDescente[], axe: AxeAnalyse): void {
    const axe2 = this.reglage().axe2;
    const garderAxe2 = axe2 !== null && axe2 !== axe && this.seCroisent([axe, axe2, ...chemin.map(e => e.axe)]);
    this.modifier({ chemin, axe, axe2: garderAxe2 ? axe2 : null });
  }

  private cheminJusqua(element: ElementAnalyse): EtapeDescente[] {
    const reglage = this.reglage();
    return [...reglage.chemin, { axe: reglage.axe, cle: element.cle ?? '', libelle: element.libelle }];
  }

  private seCroisent(codes: AxeAnalyse[]): boolean {
    const axes = codes.map(code => this.axesParCode().get(code)).filter((axe): axe is Axe => !!axe);
    return (['LIGNES', 'ENTETES'] as const).some(source => axes.every(axe => axe.sources.includes(source)));
  }

  private modifier(changement: Partial<ReglageAnalyse>): void {
    this.naviguer({ ...this.reglage(), ...changement }, null);
  }

  /** Un réglage modifié à la main n'est plus la vue choisie. */
  private naviguer(reglage: ReglageAnalyse, vue: number | null): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ...versParametresUrl(reglage), vue },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}

function lireAideVisible(): boolean {
  try {
    return localStorage.getItem(CLE_AIDE) === 'true';
  } catch {
    return false;
  }
}

/** Le message métier du serveur (« Ces ventilations ne se croisent pas… ») plutôt qu'un message générique. */
function lireErreur(erreur: unknown): string | null {
  if (!erreur) {
    return null;
  }
  const detail = erreur instanceof HttpErrorResponse ? (erreur.error?.detail ?? erreur.error?.message) : null;
  return detail ?? "L'analyse n'a pas pu être calculée.";
}
