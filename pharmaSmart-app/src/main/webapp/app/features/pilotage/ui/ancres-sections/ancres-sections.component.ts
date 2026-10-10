import { DOCUMENT } from '@angular/common';
import { afterNextRender, ChangeDetectionStrategy, Component, DestroyRef, ElementRef, inject, input, signal } from '@angular/core';

export interface AncreSection {
  /** `id` du titre de la sous-section. */
  id: string;
  libelle: string;
}

/**
 * Une section devient courante quand son titre arrive sous la barre d'ancres, à cette marge près. Elle couvre la marge de défilement
 * des titres (4rem, cf. section-onglet.scss) : un titre atteint par un clic s'arrête là, et doit compter comme lu.
 */
const MARGE_ACTIVATION = 72;
/** Durée maximale d'un défilement doux déclenché par un clic, si le navigateur ne signale pas sa fin (`scrollend`). */
const DUREE_DEFILEMENT_CLIC = 1200;
const PREFIXE_CLE = 'pilotage.section.';
/** Le temps que les sections au-dessus finissent de charger : la section retrouvée est recalée tant que la page grandit. */
const DUREE_RESTAURATION = 4000;

/**
 * Liens vers les sous-sections d'un onglet (contenu projeté à droite : un réglage propre à l'onglet), collés en haut de la zone qui défile ; le lien de la section lue est mis en avant
 * (logique du scrollspy de Bootstrap). La section lue est retenue par le navigateur, par onglet : au retour, on y revient. Pas de `href="#…"` : avec la base `/`, il quitterait la page ; on défile jusqu'au titre
 * et on y place le focus (clavier, lecteur d'écran).
 */
@Component({
  selector: 'app-ancres-sections',
  template: `
    <div class="ancres-sections">
      <nav aria-label="Sous-sections" class="nav nav-pills ancres-liens">
        @for (ancre of ancres(); track ancre.id) {
          <button
            (click)="aller(ancre.id)"
            [attr.aria-current]="ancre.id === active() ? 'location' : null"
            [class.active]="ancre.id === active()"
            class="nav-link ancre-lien"
            type="button"
          >
            {{ ancre.libelle }}
          </button>
        }
      </nav>
      <ng-content />
    </div>
  `,
  styles: `
    // Collée au bord de la zone qui défile, marge interne comprise : à top: 0, le contenu repasserait au-dessus de la barre.
    :host {
      position: sticky;
      top: -0.3rem;
      z-index: 3;
      display: block;
    }

    .ancres-sections {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
      padding: 0.3rem;
      border: 1px solid var(--pharma-surface-border);
      border-radius: var(--pharma-surface-radius);
      background: var(--pharma-surface);
      box-shadow: 0 2px 6px rgb(0 0 0 / 6%);
    }

    .ancres-liens {
      flex-wrap: wrap;
      gap: 0.25rem;
    }

    .ancre-lien {
      padding: 0.3rem 0.75rem;
      border-radius: 999px;
      color: var(--pharma-text);
      font-size: 0.85rem;
      font-weight: 500;

      &:hover {
        color: var(--pharma-chrome-tab);
        background: var(--pharma-chrome-tab-tint);
      }

      &:focus-visible {
        outline: 2px solid var(--pharma-chrome-tab);
        outline-offset: 1px;
      }

      &.active {
        color: var(--pharma-chrome-tab);
        background: var(--pharma-chrome-tab-tint);
        font-weight: 600;
        box-shadow: inset 0 0 0 1px var(--pharma-chrome-tab);
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AncresSectionsComponent {
  readonly ancres = input.required<readonly AncreSection[]>();

  /** Clé de mémorisation propre à l'onglet ; par défaut, l'identifiant de la première ancre. */
  readonly cle = input<string>('');

  protected readonly active = signal<string | null>(null);

  private readonly document = inject(DOCUMENT);
  private readonly hote = inject<ElementRef<HTMLElement>>(ElementRef);
  private demande = 0;
  private restauration = false;
  /** Section visée par un clic : tant que le défilement doux court, le suivi ne la remplace pas par une section traversée. */
  private cibleClic: string | null = null;
  private finClic = 0;
  private zone: HTMLElement | null = null;

  constructor() {
    const destroyRef = inject(DestroyRef);
    afterNextRender(() => {
      const zone = this.lireZoneDefilante();
      this.zone = zone;
      const cible: HTMLElement | Window = zone ?? this.document.defaultView!;
      const surDefilement = (): void => this.planifierSuivi(zone);
      const surFinDefilement = (): void => this.terminerClic();
      cible.addEventListener('scroll', surDefilement, { passive: true });
      cible.addEventListener('scrollend', surFinDefilement, { passive: true });
      this.document.defaultView?.addEventListener('resize', surDefilement, { passive: true });
      const arreterRestauration = this.restaurer(zone);
      destroyRef.onDestroy(() => {
        cible.removeEventListener('scroll', surDefilement);
        cible.removeEventListener('scrollend', surFinDefilement);
        clearTimeout(this.finClic);
        this.document.defaultView?.removeEventListener('resize', surDefilement);
        cancelAnimationFrame(this.demande);
        arreterRestauration();
      });
      this.suivre(zone);
    });
  }

  protected aller(id: string): void {
    const titre = this.document.getElementById(id);
    this.restauration = false;
    this.cibleClic = id;
    this.retenir(id);
    clearTimeout(this.finClic);
    this.finClic = window.setTimeout(() => this.terminerClic(), DUREE_DEFILEMENT_CLIC);
    titre?.setAttribute('tabindex', '-1');
    titre?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    titre?.focus({ preventScroll: true });
  }

  private planifierSuivi(zone: HTMLElement | null): void {
    cancelAnimationFrame(this.demande);
    this.demande = requestAnimationFrame(() => this.suivre(zone));
  }

  /** La dernière section dont le titre a franchi la barre ; en bas de zone, la dernière section. */
  private suivre(zone: HTMLElement | null): void {
    const ancres = this.ancres();
    if (!ancres.length || this.cibleClic) {
      return;
    }
    const enBas = zone
      ? zone.scrollTop + zone.clientHeight >= zone.scrollHeight - 2
      : this.document.defaultView!.innerHeight + this.document.defaultView!.scrollY >= this.document.documentElement.scrollHeight - 2;
    if (enBas) {
      this.retenir(ancres[ancres.length - 1].id);
      return;
    }
    const seuil = this.hote.nativeElement.getBoundingClientRect().bottom + MARGE_ACTIVATION;
    let courante = ancres[0].id;
    for (const ancre of ancres) {
      const titre = this.document.getElementById(ancre.id);
      if (titre && titre.getBoundingClientRect().top <= seuil) {
        courante = ancre.id;
      }
    }
    this.retenir(courante);
  }

  /** Fin du défilement d'un clic : le suivi reprend la main (la section visée reste active si elle est bien atteinte). */
  private terminerClic(): void {
    if (this.cibleClic) {
      this.cibleClic = null;
      clearTimeout(this.finClic);
      this.suivre(this.zone);
    }
  }

  /** Section courante, retenue par le navigateur sauf pendant qu'on revient à la section mémorisée. */
  private retenir(id: string): void {
    this.active.set(id);
    if (!this.restauration) {
      try {
        localStorage.setItem(this.lireCle(), id);
      } catch {
        // Stockage indisponible (navigation privée) : le choix vaut pour la session.
      }
    }
  }

  /**
   * Revient à la section mémorisée ; les sections au-dessus chargent leurs données après coup et la repoussent, d'où un recalage
   * tant que la page grandit, jusqu'à ce que l'utilisateur fasse défiler lui-même. Rend la fonction qui arrête le recalage.
   */
  private restaurer(zone: HTMLElement | null): () => void {
    const id = this.lireMemorisee();
    const titre = id && id !== this.ancres()[0]?.id ? this.document.getElementById(id) : null;
    const contenu = zone?.firstElementChild;
    if (!titre || !zone || !contenu) {
      return () => undefined;
    }
    this.restauration = true;
    const recaler = (): void => {
      if (this.restauration) {
        titre.scrollIntoView?.({ block: 'start' });
        this.active.set(id);
      }
    };
    const arreter = (): void => {
      this.restauration = false;
      observateur?.disconnect();
      clearTimeout(minuterie);
      for (const evenement of ['wheel', 'touchstart', 'keydown', 'mousedown']) {
        zone.removeEventListener(evenement, arreter);
      }
    };
    const observateur = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(recaler);
    observateur?.observe(contenu);
    const minuterie = setTimeout(arreter, DUREE_RESTAURATION);
    for (const evenement of ['wheel', 'touchstart', 'keydown', 'mousedown']) {
      zone.addEventListener(evenement, arreter, { passive: true });
    }
    recaler();
    return arreter;
  }

  private lireMemorisee(): string | null {
    try {
      return localStorage.getItem(this.lireCle());
    } catch {
      return null;
    }
  }

  private lireCle(): string {
    return PREFIXE_CLE + (this.cle() || this.ancres()[0]?.id || 'onglet');
  }

  /** Le premier ancêtre qui défile (le contenu des onglets du pilotage) ; à défaut, la fenêtre. */
  private lireZoneDefilante(): HTMLElement | null {
    let element = this.hote.nativeElement.parentElement;
    while (element) {
      const { overflowY } = getComputedStyle(element);
      if (overflowY === 'auto' || overflowY === 'scroll') {
        return element;
      }
      element = element.parentElement;
    }
    return null;
  }
}
