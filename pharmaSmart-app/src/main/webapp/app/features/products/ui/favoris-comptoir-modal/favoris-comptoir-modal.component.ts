import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { forkJoin, Observable } from "rxjs";
import { DevisePipe } from "app/shared/utils/devise";
import { ButtonComponent, CardComponent } from "app/shared/ui";
import { NotificationService } from "app/shared/services/notification.service";
import { ProduitSearch } from "app/shared/model";
import { FavoriSuggere, ProduitsFavorisApiService } from "../../data-access/services/produits-favoris-api.service";

/**
 * Grille du comptoir : les produits épinglés, dans l'ordre des tuiles, et les produits à épingler d'après les ventes. Chaque
 * geste s'applique tout de suite côté serveur ; il n'y a pas d'« Enregistrer ». Se ferme sans résultat : l'écran appelant relit.
 */
@Component({
  selector: "app-favoris-comptoir-modal",
  templateUrl: "./favoris-comptoir-modal.component.html",
  styleUrl: "./favoris-comptoir-modal.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent, DevisePipe, CardComponent]
})
export class FavorisComptoirModalComponent implements OnInit {
  protected readonly epingles = signal<ProduitSearch[]>([]);
  protected readonly suggestions = signal<FavoriSuggere[]>([]);
  protected readonly chargement = signal<boolean>(true);
  protected readonly occupe = signal<boolean>(false);

  private readonly api = inject(ProduitsFavorisApiService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.charger();
  }

  protected epingler(suggestion: FavoriSuggere): void {
    this.agir(this.api.ajouter(suggestion.produit.id), () => {
      this.epingles.update(liste => [...liste, suggestion.produit]);
      this.suggestions.update(liste => liste.filter(s => s.produit.id !== suggestion.produit.id));
    });
  }

  protected retirer(produit: ProduitSearch): void {
    // Un produit retiré peut redevenir une suggestion : on relit les deux listes plutôt que de deviner.
    this.agir(this.api.retirer(produit.id), () => this.charger());
  }

  protected monter(index: number): void {
    this.deplacer(index, index - 1);
  }

  protected descendre(index: number): void {
    this.deplacer(index, index + 1);
  }

  protected fermer(): void {
    this.activeModal.close();
  }

  private deplacer(de: number, vers: number): void {
    const liste = [...this.epingles()];
    if (vers < 0 || vers >= liste.length) {
      return;
    }
    [liste[de], liste[vers]] = [liste[vers], liste[de]];
    this.agir(this.api.reordonner(liste.map(p => p.id)), () => this.epingles.set(liste));
  }

  private charger(): void {
    this.chargement.set(true);
    forkJoin({ epingles: this.api.lister(), suggestions: this.api.suggerer() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ epingles, suggestions }) => {
          this.epingles.set(epingles);
          this.suggestions.set(suggestions);
          this.chargement.set(false);
        },
        error: () => {
          this.chargement.set(false);
          this.notifications.error("Les favoris n'ont pas pu être chargés.", "Favoris du comptoir");
        },
      });
  }

  /** Un geste à la fois : tant qu'une requête est en cours, les boutons sont grisés, pour que l'ordre affiché reste celui du serveur. */
  private agir(appel: Observable<void>, apres: () => void): void {
    this.occupe.set(true);
    appel.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        apres();
        this.occupe.set(false);
      },
      error: () => {
        this.occupe.set(false);
        this.notifications.error("L'opération n'a pas abouti.", "Favoris du comptoir");
      },
    });
  }
}
