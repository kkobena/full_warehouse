import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, output, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { formatDateFR } from 'app/shared/utils/format-utils';
import { ButtonComponent, CardComponent, InputNumberComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { LigneObjectifs } from '../../models/pilotage.model';
import { MOIS_COURTS } from '../../ui/graphique-annees/graphique-annees.component';

const HAUSSE_PAR_DEFAUT = 5;

/**
 * Grille indicateurs × mois ; « Proposer » remplit une ligne d'après N-1 + x %, l'utilisateur ajuste puis enregistre. Les mois clos
 * sont en lecture seule (le serveur refuse aussi de les changer) : leur suivi s'est fait contre l'objectif d'alors.
 */
@Component({
  selector: 'app-saisie-objectifs',
  imports: [FormsModule, ButtonComponent, CardComponent, InputNumberComponent],
  templateUrl: './saisie-objectifs.component.html',
  styleUrls: ['../../ui/section-onglet.scss', './objectifs.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SaisieObjectifsComponent {
  readonly annee = input.required<number>();
  readonly enregistre = output<void>();

  private readonly api = inject(PilotageApiService);

  protected readonly moisCourts = MOIS_COURTS;
  protected readonly grille = rxResource({ params: () => this.annee(), stream: ({ params }) => this.api.lireObjectifs(params) });
  /** Valeurs en cours de saisie, par code d'indicateur ; vidées quand l'année change. */
  protected readonly brouillons = linkedSignal<number, Record<string, (number | null)[]>>({ source: this.annee, computation: () => ({}) });
  protected readonly hausse = signal(HAUSSE_PAR_DEFAUT);
  protected readonly enCours = signal<string | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly moisClos = computed(() => this.grille.value()?.moisClos ?? 0);
  protected readonly anneeClose = computed(() => this.moisClos() >= this.moisCourts.length);

  protected lireBrouillon(ligne: LigneObjectifs): (number | null)[] {
    return this.brouillons()[ligne.indicateur.code] ?? ligne.mois;
  }

  protected modifier(ligne: LigneObjectifs, mois: number, valeur: number | null): void {
    const valeurs = [...this.lireBrouillon(ligne)];
    valeurs[mois] = valeur;
    this.brouillons.update(brouillons => ({ ...brouillons, [ligne.indicateur.code]: valeurs }));
  }

  protected proposer(ligne: LigneObjectifs): void {
    this.erreur.set(null);
    this.api.proposerObjectifs(this.annee(), ligne.indicateur.code, this.hausse()).subscribe({
      // La proposition ne remplit que les mois ouverts : un mois clos garde son objectif.
      next: valeurs => {
        const actuelles = this.lireBrouillon(ligne);
        const ouvertes = valeurs.map((valeur, rang) => (this.estClos(rang) ? actuelles[rang] : valeur));
        this.brouillons.update(brouillons => ({ ...brouillons, [ligne.indicateur.code]: ouvertes }));
      },
      error: () => this.erreur.set(`La proposition pour « ${ligne.indicateur.libelle} » n'a pas pu être calculée.`),
    });
  }

  protected enregistrer(ligne: LigneObjectifs): void {
    this.enCours.set(ligne.indicateur.code);
    this.erreur.set(null);
    this.api.enregistrerObjectifs({ annee: this.annee(), indicateur: ligne.indicateur.code, mois: this.lireBrouillon(ligne) }).subscribe({
      next: grille => {
        this.grille.set(grille);
        this.brouillons.update(({ [ligne.indicateur.code]: _enregistre, ...autres }) => autres);
        this.enCours.set(null);
        this.enregistre.emit();
      },
      error: (erreur: unknown) => {
        this.enCours.set(null);
        const detail = erreur instanceof HttpErrorResponse ? (erreur.error?.detail ?? erreur.error?.message) : null;
        this.erreur.set(detail ?? `Les objectifs « ${ligne.indicateur.libelle} » n'ont pas pu être enregistrés.`);
      },
    });
  }

  protected estClos(rang: number): boolean {
    return rang < this.moisClos();
  }

  protected estModifiee(ligne: LigneObjectifs): boolean {
    return ligne.indicateur.code in this.brouillons();
  }

  protected libellerModification(ligne: LigneObjectifs): string {
    return ligne.modifieLe ? `Modifié${ligne.modifiePar ? ` par ${ligne.modifiePar}` : ''} le ${formatDateFR(ligne.modifieLe)}` : 'Aucun objectif';
  }

  protected compterDecimales(ligne: LigneObjectifs): number {
    return ligne.indicateur.unite === 'POURCENTAGE' ? 1 : 0;
  }

  protected lireSuffixe(ligne: LigneObjectifs): string {
    return ligne.indicateur.unite === 'POURCENTAGE' ? ' %' : '';
  }
}
