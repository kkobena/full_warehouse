import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ActivatedRoute, Router } from '@angular/router';

import { AbilityService } from 'app/core/auth/ability.service';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { ButtonComponent, CardComponent, CheckboxComponent, DataTableComponent, FormFieldComponent, PillSelectorComponent, SelectComponent } from 'app/shared/ui';
import { ISO_TO_NGB_DATE } from 'app/shared/util/warehouse-util';
import { formatDecimal } from 'app/shared/utils/format-utils';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { ComparaisonAnnees, CroiseAnalyse, LigneCroisee, ModeAnnees, ReglageAnnees, RequetePilotage } from '../../models/pilotage.model';
import { CroiseAnalyseComponent } from '../../ui/croise-analyse/croise-analyse.component';
import { VariationComponent } from '../../ui/variation/variation.component';
import { GraphiqueAnneesComponent, MOIS_COURTS } from '../../ui/graphique-annees/graphique-annees.component';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';
import { lireReglageAnnees, versParametresAnnees } from './reglage-annees';
import { AideComponent } from '../../ui/aide/aide.component';

const MOIS_LONGS = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'];

/**
 * Onglet « Comparer les années » : l'année en cours face à une à cinq années civiles, mois par mois, en cumul ou sur douze mois
 * glissants ; trimestres, saisonnalité, synthèse annuelle, familles et types de vente par année. Réglage dans l'URL.
 */
@Component({
  selector: 'app-onglet-comparer-annees',
  imports: [
    NgbTooltip,
    AideComponent,
    FormFieldComponent,
    VariationComponent,
    FormsModule,
    SelectComponent,
    PillSelectorComponent,
    CheckboxComponent,
    ButtonComponent,
    CardComponent,
    DataTableComponent,
    CroiseAnalyseComponent,
    GraphiqueAnneesComponent,
    ValeurIndicateurPipe,
  ],
  templateUrl: './onglet-comparer-annees.component.html',
  styleUrl: './onglet-comparer-annees.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OngletComparerAnneesComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly telechargement = inject(BlobDownloadService);
  private readonly valeurIndicateur = new ValeurIndicateurPipe();

  private readonly parametres = toSignal(this.route.queryParamMap);
  protected readonly reglage = computed(() => lireReglageAnnees(this.parametres()));
  protected readonly peutExporter = inject(AbilityService).canSignal('export', 'pilotage.comparer-annees');

  protected readonly indicateurs = rxResource({ stream: () => this.api.listerIndicateurs() });

  protected readonly donnees = rxResource({
    params: () => ({ aDate: this.requete().aDate, reglage: this.reglage() }),
    stream: ({ params }) => this.api.comparerAnnees(params.aDate, params.reglage),
  });

  protected readonly erreur = computed(() => {
    const erreur = this.donnees.error();
    if (!erreur) {
      return null;
    }
    return (erreur instanceof HttpErrorResponse ? (erreur.error?.detail ?? erreur.error?.message) : null) ?? "La comparaison n'a pas pu être calculée.";
  });

  protected readonly nombres = [2, 3, 4, 5, 6].map(nombre => ({ libelle: `${nombre} ans`, valeur: nombre }));
  protected readonly modes: { label: string; value: ModeAnnees }[] = [
    { label: 'Mois', value: 'MENSUEL' },
    { label: 'Cumul depuis janvier', value: 'CUMULE' },
    { label: '12 mois glissants', value: 'GLISSANT' },
  ];

  /** Mois × années ; le mois en cours porte « au 15 » : l'année en cours y est incomplète. */
  protected readonly parMois = computed<CroiseAnalyse | null>(() => {
    const comparaison = this.donnees.value();
    if (!comparaison) {
      return null;
    }
    const jusquAu = ISO_TO_NGB_DATE(comparaison.jusquAu)!;
    const lignes: LigneCroisee[] = MOIS_COURTS.map((mois, rang) => ({
      cle: String(rang + 1),
      libelle: rang === jusquAu.month - 1 ? `${mois} (au ${jusquAu.day})` : mois,
      cellules: comparaison.annees.map(annee => annee.mois[rang]),
    }));
    lignes.push({ cle: 'total', libelle: 'Année', cellules: comparaison.annees.map(annee => annee.total) });
    return { colonnes: this.colonnesAnnees(comparaison), lignes };
  });

  /** Une phrase de lecture : le mois le plus élevé, la croissance annuelle moyenne. */
  protected readonly lecture = computed(() => {
    const comparaison = this.donnees.value();
    if (!comparaison) {
      return '';
    }
    const phrases: string[] = [];
    const maximum = comparaison.moisMaximum;
    if (maximum) {
      const valeur = this.valeurIndicateur.transform(maximum.valeur, comparaison.indicateur.unite);
      phrases.push(`Mois le plus élevé : ${MOIS_LONGS[maximum.mois - 1]} ${maximum.annee} (${valeur}).`);
    }
    if (comparaison.croissanceAnnuelleMoyenne !== null) {
      const signe = comparaison.croissanceAnnuelleMoyenne > 0 ? '+' : '';
      phrases.push(
        `Croissance annuelle moyenne ${comparaison.croissanceDepuis}-${comparaison.croissanceJusqua} : ${signe}${formatDecimal(comparaison.croissanceAnnuelleMoyenne, 1)} %.`,
      );
    }
    return phrases.join(' ');
  });

  protected readonly jourOuvreSansObjet = computed(() => this.reglage().parJourOuvre && this.donnees.value()?.parJourOuvre === false);

  protected choisirIndicateur(selection: unknown): void {
    if (selection) {
      this.modifier({ indicateur: selection as string });
    }
  }

  protected choisirNombre(selection: unknown): void {
    if (selection) {
      this.modifier({ annees: selection as number });
    }
  }

  protected choisirMode(selection: unknown): void {
    this.modifier({ mode: selection as ModeAnnees });
  }

  protected choisirJourOuvre(parJourOuvre: boolean): void {
    this.modifier({ parJourOuvre });
  }

  protected filtrerFamille(ligne: LigneCroisee): void {
    this.modifier({ filtre: { axe: 'FAMILLE', cle: ligne.cle, libelle: ligne.libelle } });
  }

  protected filtrerNatureVente(ligne: LigneCroisee): void {
    this.modifier({ filtre: { axe: 'NATURE_VENTE', cle: ligne.cle, libelle: ligne.libelle } });
  }

  protected retirerFiltre(): void {
    this.modifier({ filtre: null });
  }

  /** Mois × années, généré par le serveur avec des valeurs complètes, une colonne par année. */
  protected exporter(comparaison: ComparaisonAnnees): void {
    this.telechargement.downloadFromObservable(
      this.api.exporterAnnees(this.requete().aDate, this.reglage()),
      `pilotage-annees-${comparaison.indicateur.code.toLowerCase()}`,
      'csv',
    );
  }

  private colonnesAnnees(comparaison: ComparaisonAnnees): CroiseAnalyse['colonnes'] {
    return comparaison.annees.map(annee => ({ cle: String(annee.annee), libelle: annee.complete ? String(annee.annee) : `${annee.annee} (en cours)` }));
  }

  private modifier(changement: Partial<ReglageAnnees>): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: versParametresAnnees({ ...this.reglage(), ...changement }),
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}
