import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, TemplateRef, contentChild, inject, signal } from '@angular/core';

/**
 * Cellule éditable — remplace le couple `pEditableColumn` / `p-celleditor`.
 *
 * Un clic affiche le template `#input` ; la perte de focus (clic ou tabulation hors de la
 * cellule) ou `Entrée`/`Échap` y remet le template `#output`. Contrairement à `p-table`
 * (`editMode="cell"`/`"row"`), l'état d'édition appartient à la cellule elle-même — il n'y a
 * pas de suivi centralisé du champ en cours d'édition à câbler côté écran.
 *
 * Un crayon (`pi-pencil`) apparaît au survol du template `#output` — signal visuel qu'une
 * colonne est éditable, sans rien à ajouter côté appelant.
 *
 * Le champ du template `#input` reçoit le focus et sa valeur y est sélectionnée dès le
 * passage en édition — l'utilisateur retape directement une nouvelle valeur sans devoir
 * d'abord effacer l'ancienne.
 *
 * @example
 * <td>
 *   <app-editable-cell>
 *     <ng-template #input>
 *       <input class="form-control form-control-sm" [(ngModel)]="produit.quantite" autofocus />
 *     </ng-template>
 *     <ng-template #output>{{ produit.quantite | number }}</ng-template>
 *   </app-editable-cell>
 * </td>
 */
@Component({
  selector: 'app-editable-cell',
  imports: [NgTemplateOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'app-editable-cell',
    '[class.app-editable-cell--editing]': 'editing()',
    '(click)': 'startEdit()',
    '(focusin)': 'onFocusIn($event)',
    '(focusout)': 'onFocusOut($event)',
    '(keydown.enter)': 'stopEdit()',
    '(keydown.escape)': 'stopEdit()',
  },
  template: `
    @if (editing()) {
      <ng-container [ngTemplateOutlet]="inputTemplate() ?? null" />
    } @else {
      <span class="app-editable-cell__output">
        <ng-container [ngTemplateOutlet]="outputTemplate() ?? null" />
        <i class="pi pi-pencil app-editable-cell__hint" aria-hidden="true"></i>
      </span>
    }
  `,
  styles: `
    :host {
      display: block;
      cursor: pointer;
      border-radius: var(--p-content-border-radius);
    }

    :host(:hover) {
      outline: 1px dashed var(--p-primary-color);
      outline-offset: -2px;
    }

    :host(.app-editable-cell--editing) {
      cursor: default;
    }

    :host(.app-editable-cell--editing:hover) {
      outline: none;
    }

    // Boîte inline-flex (pas de width: 100%) : elle reste un bloc atomique dans le flux,
    // donc l'alignement (\`text-align\` de la <td> parente, ex. .text-right) continue à
    // s'appliquer normalement, comme s'il n'y avait qu'un texte simple.
    .app-editable-cell__output {
      display: inline-flex;
      align-items: center;
      gap: 5px;
    }

    // Crayon discret, révélé au survol — seul indice visuel qu'une cellule est éditable,
    // sans ajouter de bordure ou de fond permanent qui alourdirait le tableau au repos.
    .app-editable-cell__hint {
      font-size: 0.65rem;
      color: var(--p-primary-color);
      opacity: 0;
      transition: opacity 0.15s ease;
      flex-shrink: 0;
    }

    :host(:hover) .app-editable-cell__hint {
      opacity: 1;
    }
  `,
})
export class EditableCellComponent {
  private readonly elementRef: ElementRef<HTMLElement> = inject(ElementRef);

  protected readonly inputTemplate = contentChild<TemplateRef<unknown>>('input');
  protected readonly outputTemplate = contentChild<TemplateRef<unknown>>('output');

  protected readonly editing = signal(false);

  protected startEdit(): void {
    if (this.editing()) return;
    this.editing.set(true);
    // Le template #input n'est rendu qu'après ce tick (le `@if` bascule via le signal) —
    // on attend le prochain pour trouver le champ dans le DOM et le focus.
    setTimeout(() => this.focusField());
  }

  protected stopEdit(): void {
    this.editing.set(false);
  }

  /** Sort du mode édition seulement si le focus quitte réellement la cellule (pas un enfant). */
  protected onFocusOut(event: FocusEvent): void {
    const next = event.relatedTarget as Node | null;
    if (!next || !(event.currentTarget as HTMLElement).contains(next)) {
      this.editing.set(false);
    }
  }

  /** Sélectionne la valeur dès que le champ (saisi au clic ou par tabulation) reçoit le focus. */
  protected onFocusIn(event: FocusEvent): void {
    const target = event.target;
    if (target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement) {
      target.select();
    }
  }

  private focusField(): void {
    this.elementRef.nativeElement.querySelector<HTMLInputElement | HTMLTextAreaElement>('input, textarea')?.focus();
  }
}
