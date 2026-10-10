import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { CardComponent, DataTableComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { ReglageAnalyse, RequetePilotage } from '../../models/pilotage.model';
import { CroiseAnalyseComponent } from '../../ui/croise-analyse/croise-analyse.component';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';
import { VariationComponent } from '../../ui/variation/variation.component';
import { REGLAGE_PAR_DEFAUT } from '../onglet-analyser/reglage-analyse';
import { CelluleAnalyse, LigneVendeur, SensFavorable, UniteIndicateur } from '../../models/pilotage.model';
import { AideComponent } from '../../ui/aide/aide.component';

const VENDEURS_PAR_HEURE: ReglageAnalyse = { ...REGLAGE_PAR_DEFAUT, indicateurs: ['CA_TTC'], axe: 'VENDEUR', axe2: 'HEURE', top: 0, affichage: 'CROISE' };

interface ColonneVendeur {
  libelle: string;
  aide?: string;
  unite: UniteIndicateur;
  sens: SensFavorable;
  lire: (ligne: LigneVendeur) => CelluleAnalyse;
}

const COLONNES: readonly ColonneVendeur[] = [
  { libelle: 'Ventes', unite: 'NOMBRE', sens: 'HAUSSE', lire: ligne => ligne.nbVentes },
  { libelle: 'CA TTC', unite: 'MONTANT', sens: 'HAUSSE', lire: ligne => ligne.caTtc },
  { libelle: 'Panier moyen', aide: 'CA TTC / nombre de ventes.', unite: 'MONTANT', sens: 'HAUSSE', lire: ligne => ligne.panierMoyen },
  { libelle: 'Articles par vente', aide: 'Quantités vendues / nombre de ventes.', unite: 'RATIO', sens: 'HAUSSE', lire: ligne => ligne.articlesParVente },
  { libelle: 'Taux de marge', aide: 'Marge / CA HT.', unite: 'POURCENTAGE', sens: 'HAUSSE', lire: ligne => ligne.tauxMarge },
  { libelle: 'Taux de remise', aide: 'Remises / CA TTC.', unite: 'POURCENTAGE', sens: 'BAISSE', lire: ligne => ligne.tauxRemise },
  { libelle: 'Annulations', aide: 'Ventes annulées par le vendeur.', unite: 'NOMBRE', sens: 'BAISSE', lire: ligne => ligne.annulations },
  { libelle: 'Avoirs', aide: 'Avoirs émis : produits demandés mais non servis.', unite: 'NOMBRE', sens: 'BAISSE', lire: ligne => ligne.avoirs },
  { libelle: 'Part ordonnance', aide: 'Part du CA du vendeur réalisée sur ordonnance.', unite: 'POURCENTAGE', sens: 'NEUTRE', lire: ligne => ligne.partOrdonnance },
];

/** Équipe : chaque vendeur face à la référence et à l'équipe entière ; CA par vendeur et par heure (qui est présent quand). */
@Component({
  selector: 'app-section-equipe',
  imports: [AideComponent, CardComponent, DataTableComponent, CroiseAnalyseComponent, VariationComponent, ValeurIndicateurPipe],
  templateUrl: './section-equipe.component.html',
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionEquipeComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);

  protected readonly colonnes = COLONNES;
  protected readonly equipe = rxResource({ params: () => this.requete(), stream: ({ params }) => this.api.lireEquipe(params) });
  protected readonly parHeure = rxResource({ params: () => this.requete(), stream: ({ params }) => this.api.analyser(params, VENDEURS_PAR_HEURE) });

  protected readonly croise = computed(() => this.parHeure.value()?.croise ?? null);
  protected readonly indicateurCa = computed(() => this.parHeure.value()?.indicateurs[0] ?? null);
}
