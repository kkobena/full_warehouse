import {ChangeDetectionStrategy, Component, computed, input, output, signal} from '@angular/core';

/**
 * Bandeau d'aide contextuel, écartable — le « hint premier usage ».
 *
 * Les styles sont portés par le composant plutôt que par un partiel partagé : le bandeau n'a qu'un
 * seul rendu, et l'encapsuler ici évite qu'un écran hôte hérite de règles dont il n'a que faire.
 *
 * **La mémorisation est prise en charge** quand `storageKey` est fourni : le bandeau ne réapparaît
 * plus une fois écarté. Sans cette clé, chaque écran redéveloppait le même trio signal /
 * `localStorage` / `dismiss`, avec autant d'occasions d'oublier la persistance — le bandeau revenait
 * alors à chaque visite, ce qui est précisément ce qu'un conseil de premier usage ne doit pas faire.
 *
 * @example
 * <app-hint storageKey="produit-list-hint">
 *   Cliquez sur une ligne pour afficher le détail du produit.
 * </app-hint>
 *
 * @example Pilotage externe, sans mémorisation
 * <app-hint [visible]="showHint()" (dismissed)="showHint.set(false)">…</app-hint>
 */
@Component({
  selector: 'app-hint',
  template: `
    @if (affiche()) {
      <div class="app-hint" role="note">
        <i [class]="icon()" aria-hidden="true"></i>
        <span><ng-content /></span>
        @if (dismissible()) {
          <button type="button" class="app-hint__close" [attr.aria-label]="dismissAriaLabel()" (click)="dismiss()">
            <i class="pi pi-times" aria-hidden="true"></i>
          </button>
        }
      </div>
    }
  `,
  styles: `
    .app-hint {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.4rem 0.75rem;
      margin-bottom: 0.25rem;
      background: #e8f4fd;
      border: 1px solid #b6d9f7;
      border-radius: 4px;
      font-size: 0.8rem;
      color: #1a6fa8;
      flex-shrink: 0;
    }

    .app-hint > i {
      font-size: 0.9rem;
      flex-shrink: 0;
    }

    .app-hint > span {
      flex: 1;
    }

    .app-hint__close {
      background: none;
      border: none;
      cursor: pointer;
      color: inherit;
      padding: 0 0.25rem;
      line-height: 1;
      opacity: 0.7;
      flex-shrink: 0;

      &:hover {
        opacity: 1;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HintComponent {
  /**
   * Clé de mémorisation dans `localStorage`. Fournie, le bandeau se souvient d'avoir été écarté ;
   * omise, il réapparaît à chaque affichage et c'est à l'hôte de gérer sa visibilité.
   */
  readonly storageKey = input<string>('');

  /**
   * Visibilité imposée par l'hôte — pour masquer le conseil quand il n'a plus lieu d'être, par
   * exemple lorsqu'un panneau de détail est déjà ouvert. Se combine avec la mémorisation : le
   * bandeau n'apparaît que si les deux conditions sont réunies.
   */
  readonly visible = input<boolean>(true);

  readonly icon = input<string>('pi pi-info-circle');

  readonly dismissible = input<boolean>(true);

  readonly dismissAriaLabel = input<string>('Fermer');

  /** Émis quand l'utilisateur écarte le bandeau, que la mémorisation soit active ou non. */
  readonly dismissed = output<void>();

  private readonly ecarte = signal(false);

  protected readonly affiche = computed(() => this.visible() && !this.ecarte() && !this.dejaEcarte());

  /**
   * Lu à chaque évaluation plutôt que mis en cache à la construction : un même écran peut afficher
   * plusieurs hints, et l'un ne doit pas rester visible parce qu'il a été instancié avant l'autre.
   */
  private dejaEcarte(): boolean {
    const cle = this.storageKey();
    return !!cle && localStorage.getItem(this.cleComplete(cle)) === '1';
  }

  protected dismiss(): void {
    const cle = this.storageKey();
    if (cle) {
      localStorage.setItem(this.cleComplete(cle), '1');
    }
    this.ecarte.set(true);
    this.dismissed.emit();
  }

  /** Préfixe commun : évite qu'une clé trop générique n'entre en collision avec un autre stockage. */
  private cleComplete(cle: string): string {
    return `app-hint:${cle}`;
  }
}
