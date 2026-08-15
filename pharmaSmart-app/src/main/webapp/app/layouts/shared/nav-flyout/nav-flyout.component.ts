import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  Injector,
  input,
  OnDestroy,
  output,
  viewChildren,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterModule } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { CdkTrapFocus, FocusKeyManager } from '@angular/cdk/a11y';
import { faArrowLeft, faXmark } from '@fortawesome/free-solid-svg-icons';
import { NavItem } from '../../navbar/navbar-item.model';
import { FlyoutItemDirective } from './flyout-item.directive';

/**
 * Une ligne du panneau, après aplatissement de l'arbre.
 * Le flyout est volontairement à un seul niveau : pas de cascade, pas de
 * seconde colonne — un éventuel 3ᵉ niveau est rendu à plat sous un intertitre.
 */
export type FlyoutRow =
  | { kind: 'divider'; key: string }
  | { kind: 'group'; key: string; label: string }
  | { kind: 'link'; key: string; item: NavItem };

@Component({
  selector: 'app-nav-flyout',
  imports: [RouterModule, FaIconComponent, CdkTrapFocus, FlyoutItemDirective],
  templateUrl: './nav-flyout.component.html',
  styleUrl: './nav-flyout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavFlyoutComponent implements AfterViewInit, OnDestroy {
  /** Entrée parente dont on affiche les enfants. */
  readonly item = input.required<NavItem>();

  /**
   * Déplace le focus dans le panneau à l'ouverture, et le rend au déclencheur
   * à la fermeture. Activé seulement quand l'ouverture vient du clavier : au
   * survol ou au clic souris, voler le focus serait déroutant.
   */
  readonly autoFocus = input(false);

  /** Rendu plein écran (petits écrans) plutôt qu'en carte ancrée. */
  readonly fullscreen = input(false);

  /**
   * D'où sort le panneau : `'side'` pour un rail vertical (il glisse depuis la
   * gauche), `'below'` pour une barre horizontale (il descend). N'influe que
   * sur l'animation d'entrée — le positionnement est l'affaire de l'hôte.
   */
  readonly placement = input<'side' | 'below'>('side');

  /** Émis quand l'utilisateur active une ligne (navigation ou action). */
  readonly navigate = output<NavItem>();

  /** Émis quand le panneau demande sa fermeture. */
  readonly close = output<void>();

  /** Lignes à rendre, arbre aplati. */
  readonly rows = computed<FlyoutRow[]>(() => this.flatten(this.item().children ?? []));

  protected readonly faArrowLeft = faArrowLeft;
  protected readonly faXmark = faXmark;

  private readonly links = viewChildren(FlyoutItemDirective);
  private readonly injector = inject(Injector);
  private readonly destroyRef = inject(DestroyRef);
  private keyManager?: FocusKeyManager<FlyoutItemDirective>;

  ngAfterViewInit(): void {
    this.keyManager = new FocusKeyManager<FlyoutItemDirective>(this.links, this.injector)
      .withWrap()
      .withVerticalOrientation()
      .withHomeAndEnd()
      .withTypeAhead();

    // Maintient le roving tabindex en phase avec la ligne active.
    this.keyManager.change.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(index => {
      this.links().forEach((link, i) => link.setActive(i === index));
    });

    // `Tab` sort d'un menu : on ferme plutôt que de piéger l'utilisateur.
    this.keyManager.tabOut.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.close.emit());

    if (this.autoFocus()) {
      this.keyManager.setFirstItemActive();
    } else {
      // Sans vol de focus : la première ligne devient simplement le point
      // d'entrée `Tab` du panneau.
      this.links()[0]?.setActive(true);
    }
  }

  ngOnDestroy(): void {
    this.keyManager?.destroy();
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' || event.key === 'ArrowLeft') {
      event.preventDefault();
      event.stopPropagation();
      this.close.emit();
      return;
    }
    this.keyManager?.onKeydown(event);
  }

  protected onRowClick(item: NavItem): void {
    item.click?.();
    this.navigate.emit(item);
  }

  /**
   * Aplatit les enfants : un enfant porteur de `children` devient un intertitre
   * suivi de ses propres enfants. Rien n'est perdu si le back-office publie un
   * arbre à plus de deux niveaux.
   */
  private flatten(items: NavItem[], prefix = ''): FlyoutRow[] {
    const rows: FlyoutRow[] = [];
    for (const child of items) {
      const key = `${prefix}${child.id}`;
      if (child.divider) {
        rows.push({ kind: 'divider', key });
      } else if (child.groupLabel) {
        rows.push({ kind: 'group', key, label: child.groupLabel });
      } else if (child.children?.length) {
        rows.push({ kind: 'group', key, label: child.label });
        rows.push(...this.flatten(child.children, `${key}/`));
      } else {
        rows.push({ kind: 'link', key, item: child });
      }
    }
    return rows;
  }
}
