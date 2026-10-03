import {ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit, signal} from "@angular/core";
import {FormsModule} from "@angular/forms";
import {HttpResponse} from "@angular/common/http";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {Subject, takeUntil} from "rxjs";
import {TiersPayantService} from "app/entities/tiers-payant/tierspayant.service";
import {ITiersPayant} from "app/shared/model/tierspayant.model";
import {ButtonComponent, SelectSearchComponent} from "../../../shared/ui";

/** Ce que la fenêtre renvoie : un organisme existant à ouvrir, ou `null` pour en créer un nouveau. */
export interface ResultatRechercheTiersPayant {
  existant: ITiersPayant | null;
}

/**
 * Recherche d'un organisme AVANT d'en créer un, depuis la gestion des tiers payants.
 *
 * Le contrôle de doublon du serveur est exact (nom, nom long, code, NCC) : « Mutuelle Fraternité »,
 * « MUTUELLE FRATERNITE » et « Fraternité Mutuelle » passeraient. Chercher d'abord est le réflexe
 * que le parcours « client assuré » impose déjà ; ce composant l'étend au parcours back-office.
 */
@Component({
  selector: "app-rechercher-tiers-payant",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: "./rechercher-tiers-payant.component.scss",
  imports: [FormsModule, ButtonComponent, SelectSearchComponent],
  template: `
    <div class="modal-header">
      <h4 class="modal-title">{{ title }}</h4>
      <button (click)="annuler()" class="btn-close" type="button" aria-label="Fermer">&times;</button>
    </div>
    <div class="modal-body">
      <p class="text-muted small">
        Vérifiez d'abord que l'organisme n'existe pas déjà, sous un nom voisin.
      </p>
      <label for="rechercheTiersPayant" class="form-label">Rechercher un organisme</label>
      <app-select-search
        [(ngModel)]="selection"
        (searched)="rechercher($event)"
        (selectionChange)="choisir($event)"
        [typeahead]="saisie$"
        [items]="resultats()"
        [minSearchLength]="longueurMinimale"
        appendTo="body"
        bindLabel="fullName"
        inputId="rechercheTiersPayant"
        placeholder="Taper au moins {{ longueurMinimale }} lettres"
      />
      @if (aucunResultat()) {
        <small class="form-text text-muted" data-testid="aucun-organisme-trouve">Aucun organisme trouvé : vous pouvez en créer un.</small>
      }
    </div>
    <div class="modal-footer">
      <app-button (clicked)="annuler()" [text]="true" icon="pi pi-times" label="Annuler" severity="secondary" />
      <app-button (clicked)="creer()" icon="pi pi-plus" label="Créer un nouveau" severity="primary" />
    </div>
  `
})
export class RechercherTiersPayantComponent implements OnInit, OnDestroy {
  categorie?: string | null = null;
  title?: string;
  protected selection: ITiersPayant | null = null;
  protected readonly longueurMinimale = 2;
  protected readonly saisie$ = new Subject<string>();
  protected readonly resultats = signal<ITiersPayant[]>([]);
  protected readonly aucunResultat = signal(false);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Observateur voulu vide : les termes sont traités par `(searched)`, le sujet n'existe que pour
    // donner un abonné à `[typeahead]` (même montage que le formulaire client assuré).
    this.saisie$.pipe(takeUntil(this.destroy$)).subscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.saisie$.complete();
  }

  protected rechercher(terme: string): void {
    this.tiersPayantService
      .query({page: 0, size: 10, type: this.categorie, search: terme || ""})
      .pipe(takeUntil(this.destroy$))
      .subscribe((res: HttpResponse<ITiersPayant[]>) => {
        const trouves = res.body ?? [];
        this.resultats.set(trouves);
        this.aucunResultat.set(trouves.length === 0 && (terme?.length ?? 0) >= this.longueurMinimale);
      });
  }

  /** Un organisme existant est choisi : on l'ouvre en modification plutôt que d'en créer un second. */
  protected choisir(tiersPayant: ITiersPayant | null): void {
    if (tiersPayant?.id) {
      this.activeModal.close({existant: tiersPayant} satisfies ResultatRechercheTiersPayant);
    }
  }

  protected creer(): void {
    this.activeModal.close({existant: null} satisfies ResultatRechercheTiersPayant);
  }

  protected annuler(): void {
    this.activeModal.dismiss();
  }
}
