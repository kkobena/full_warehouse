import { AfterViewInit, Directive, effect, ElementRef, inject, input, Renderer2 } from '@angular/core';

import { LicenseService } from 'app/core/license/license.service';

/** Éléments natifs qui comprennent l'attribut `disabled`. */
const DISABLABLE = new Set(['BUTTON', 'INPUT', 'SELECT', 'TEXTAREA', 'FIELDSET', 'OPTGROUP', 'OPTION']);

/**
 * Neutralise une commande d'écriture tant que la licence n'autorise pas les modifications.
 *
 * <p><strong>Confort, pas sécurité.</strong> Le blocage fait autorité côté serveur (HTTP 402) :
 * cette directive évite seulement à l'utilisateur de saisir un formulaire entier pour se voir
 * refuser l'enregistrement. La retirer depuis les outils de développement ne débloque rien.
 *
 * <p>Elle descend d'un niveau dans le DOM pour atteindre les contrôles natifs imbriqués : c'est ce
 * qui la rend utilisable sur les composants du Design System (`<app-button>` rend un `<button>`
 * interne, qu'un `disabled` posé sur l'hôte n'atteindrait pas).
 *
 * @example
 * <app-button appLicenseReadOnly label="Enregistrer" (clicked)="save()" />
 * <button appLicenseReadOnly type="submit">Valider la vente</button>
 *
 * Cf. docs/PLAN-GESTION-LICENCE.md §5.4.
 */
@Directive({
  selector: '[appLicenseReadOnly]',
})
export class LicenseReadOnlyDirective implements AfterViewInit {
  /** Infobulle affichée quand la commande est neutralisée. */
  readonly appLicenseReadOnly = input<string>('');

  private readonly licenseService = inject(LicenseService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly renderer = inject(Renderer2);

  /** `false` tant que la vue n'est pas rendue : les contrôles imbriqués n'existent pas encore. */
  private viewReady = false;

  /**
   * Infobulles d'origine, restaurées au dégel.
   *
   * Sans elles, une commande portant déjà un `title` explicatif le perdrait définitivement après le
   * premier passage en lecture seule.
   */
  private readonly previousTitles = new WeakMap<HTMLElement, string | null>();

  constructor() {
    effect(() => {
      const readOnly = this.licenseService.isReadOnly();
      if (this.viewReady) {
        this.apply(readOnly);
      }
    });
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.apply(this.licenseService.isReadOnly());
  }

  private apply(readOnly: boolean): void {
    const element = this.host.nativeElement;
    const tooltip = this.appLicenseReadOnly() || this.licenseService.license()?.message || 'Licence non valide.';

    for (const target of this.targets(element)) {
      if (readOnly) {
        this.renderer.setAttribute(target, 'disabled', 'true');
        this.renderer.setAttribute(target, 'aria-disabled', 'true');
        this.freezeTitle(target, tooltip);
      } else {
        this.renderer.removeAttribute(target, 'disabled');
        this.renderer.removeAttribute(target, 'aria-disabled');
        this.restoreTitle(target);
      }
    }

    // Un lien n'a pas d'attribut `disabled` : on coupe l'interaction par le style.
    if (element.tagName === 'A') {
      if (readOnly) {
        this.renderer.addClass(element, 'pe-none');
        this.renderer.addClass(element, 'opacity-50');
        this.renderer.setAttribute(element, 'aria-disabled', 'true');
        this.freezeTitle(element, tooltip);
      } else {
        this.renderer.removeClass(element, 'pe-none');
        this.renderer.removeClass(element, 'opacity-50');
        this.renderer.removeAttribute(element, 'aria-disabled');
        this.restoreTitle(element);
      }
    }
  }

  private freezeTitle(target: HTMLElement, tooltip: string): void {
    if (!this.previousTitles.has(target)) {
      this.previousTitles.set(target, target.getAttribute('title'));
    }
    this.renderer.setAttribute(target, 'title', tooltip);
  }

  private restoreTitle(target: HTMLElement): void {
    if (!this.previousTitles.has(target)) {
      return;
    }
    const original = this.previousTitles.get(target) ?? null;
    if (original === null) {
      this.renderer.removeAttribute(target, 'title');
    } else {
      this.renderer.setAttribute(target, 'title', original);
    }
    this.previousTitles.delete(target);
  }

  private targets(element: HTMLElement): HTMLElement[] {
    if (DISABLABLE.has(element.tagName)) {
      return [element];
    }
    return Array.from(element.querySelectorAll<HTMLElement>('button, input, select, textarea'));
  }
}
