import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { NgbNav, NgbNavOutlet, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent } from '../button/button.component';

/**
 * Menu vertical d'écran — colonne de navigation à gauche, contenu de l'onglet à droite.
 *
 * Encapsule la structure recopiée sur dix-neuf écrans : la grille à deux colonnes, la carte,
 * l'en-tête coloré, et le conteneur défilant du menu. Pendant de {@link NavTabsComponent} pour
 * les onglets horizontaux.
 *
 * **Enveloppe de mise en page, pas de remplacement de `ngbNav`.** La directive reste chez
 * l'appelant : elle collecte ses onglets par `@ContentChildren(NgbNavItem)` avec
 * `descendants: false`. Des `<ng-container ngbNavItem>` *projetés* appartiennent au template
 * appelant et non au contenu de `ngbNav` — la requête ne les trouverait pas et le menu
 * s'afficherait vide. Le `[ngbNav]` doit donc rester au-dessus de ses items, dans le gabarit qui
 * les déclare.
 *
 * L'outlet, lui, est rendu ici : passer la référence de template via `[nav]`.
 *
 * @example
 * <app-nav-sidebar [collapsed]="menuReplie()" (collapsedChange)="menuReplie.set($event)"
 *                  [collapsible]="true" [nav]="nav" icon="pi pi-book" title="Comptabilité">
 *   <div #nav="ngbNav" ngbNav class="nav flex-column nav-pills" orientation="vertical"
 *        [activeId]="active()" (activeIdChange)="active.set($event)">
 *     <ng-container ngbNavItem="balance">
 *       <a class="pharma-nav-vertical-link" ngbNavLink>
 *         <i class="pi pi-calculator"></i><span>Balance caisse</span><span class="link-arrow">›</span>
 *       </a>
 *       <ng-template ngbNavContent><app-balance-mvt-caisse /></ng-template>
 *     </ng-container>
 *   </div>
 * </app-nav-sidebar>
 */
@Component({
  selector: 'app-nav-sidebar',
  imports: [NgbNavOutlet, NgbTooltip, ButtonComponent],
  template: `
    <div class="row">
      <div [class]="menuColumnClasses()">
        <div [class.is-collapsed]="collapsed()" class="pharma-nav-sidebar">
          <div class="pharma-nav-sidebar-card">
            @if (title() || icon() || collapsible()) {
              <div class="pharma-nav-sidebar-header">
                <!-- Replié, le titre s'efface mais l'icône reste : c'est elle qui identifie le menu. -->
                @if (icon()) {
                  <i [class]="icon()" aria-hidden="true"></i>
                }
                @if (!collapsed()) {
                  <span>{{ title() }}</span>
                }

                @if (collapsible()) {
                  <app-button
                    (clicked)="collapsed.set(!collapsed())"
                    [ariaLabel]="collapsed() ? 'Afficher le menu' : 'Masquer le menu'"
                    [icon]="collapsed() ? 'pi pi-angle-right' : 'pi pi-angle-left'"
                    [ngbTooltip]="collapsed() ? 'Afficher le menu' : 'Masquer le menu'"
                    [rounded]="true"
                    [text]="true"
                    class="nav-sidebar-toggle"
                    placement="right"
                    size="small"
                  />
                }
              </div>
            }

            <div class="pharma-nav-sidebar-content">
              <ng-content />
            </div>
          </div>
        </div>
      </div>

      <!--
        Conditionnel : un écran peut placer son contenu ailleurs que dans la colonne de droite,
        et n'utiliser ce composant que pour son menu.
      -->
      @if (nav()) {
        <div [class]="contentColumnClasses()">
          <ng-content select="[navContent]" />
          <div [ngbNavOutlet]="nav()!"></div>
        </div>
      }
    </div>
  `,
  styleUrl: './nav-sidebar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavSidebarComponent {
  /** Titre de l'en-tête coloré. */
  readonly title = input<string>('');

  /** Classe d'icône précédant le titre, ex. `pi pi-book`. */
  readonly icon = input<string>('');

  /**
   * Référence de template du `ngbNav` projeté, ex. `#nav="ngbNav"`. Sans elle, la colonne de
   * contenu n'est pas rendue du tout — le menu occupe alors sa largeur habituelle et l'écran
   * dispose du reste comme il l'entend.
   */
  readonly nav = input<NgbNav | undefined>(undefined);

  /**
   * Largeurs de la colonne du menu.
   *
   * <p>Paramétrables parce que les dix-neuf écrans ne s'accordaient pas : deux douzièmes ici,
   * trois ailleurs. La valeur par défaut est la plus répandue ; l'uniformiser d'autorité
   * changerait la mise en page d'écrans qui n'ont rien demandé.
   */
  readonly menuColumnClass = input<string>('col-lg-2 col-xl-2 col-xxl-2 col-md-3 col-sm-4');

  /** Largeurs de la colonne de contenu, complément de {@link menuColumnClass}. */
  readonly contentColumnClass = input<string>('col-xl-10 col-lg-10 col-md-9 col-sm-8');

  /** Affiche le bouton de repli dans l'en-tête. */
  readonly collapsible = input<boolean>(false);

  /**
   * État replié, en liaison bidirectionnelle.
   *
   * <p>`model` et non `input` : l'écran appelant doit pouvoir lire l'état — pour l'écrire dans le
   * stockage local, ou pour adapter sa propre mise en page — sans que le composant lui impose de
   * gérer le clic. Piloté de l'extérieur, il fonctionne aussi bien.
   *
   * <p><strong>Lier en deux temps, pas en `[(collapsed)]`.</strong> La syntaxe abrégée retombe sur
   * une affectation de propriété — {@code ctx.champ = $event} — dès qu'Angular ne reconnaît pas la
   * cible comme signal inscriptible, ce qui lève « Attempt to assign to const or readonly
   * variable » sur un champ `readonly`. La forme explicite n'affecte jamais rien :
   *
   * <pre>[collapsed]="menuReplie()" (collapsedChange)="menuReplie.set($event)"</pre>
   */
  readonly collapsed = model<boolean>(false);

  /**
   * Replié, la colonne du menu se dimensionne sur son contenu et le reste revient au contenu.
   *
   * <p>Garder les largeurs de la grille aurait laissé une colonne vide à la place du menu : dans
   * Bootstrap, `col-lg-2` réserve deux douzièmes que le contenu ne récupère pas.
   */
  protected readonly menuColumnClasses = computed(() => (this.collapsed() ? 'col-auto' : this.menuColumnClass()));

  protected readonly contentColumnClasses = computed(() => (this.collapsed() ? 'col' : this.contentColumnClass()));
}
