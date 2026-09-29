import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { DashboardItem, WidgetComponent } from '../../models/dashboard.model';

/** Texte libre : consigne d'équipe, rappel, commentaire sur les chiffres voisins. */
@Component({
  selector: 'app-note-widget',
  template: `
    @if (editMode()) {
      <textarea
        class="form-control note-editor"
        aria-label="Texte de la note"
        placeholder="Saisissez votre note…"
        [value]="text()"
        (input)="paramsChange.emit({ text: $any($event.target).value })"
      ></textarea>
    } @else if (text()) {
      <p class="note-text">{{ text() }}</p>
    } @else {
      <p class="note-empty">Note vide — passez en mode édition pour la rédiger.</p>
    }
  `,
  styles: `
    :host {
      display: block;
      height: 100%;
    }
    .note-editor {
      height: 100%;
      resize: none;
    }
    .note-text {
      margin: 0;
      white-space: pre-wrap;
    }
    .note-empty {
      margin: 0;
      color: var(--bs-secondary-color);
      font-style: italic;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NoteWidgetComponent implements WidgetComponent {
  readonly item = input.required<DashboardItem>();
  readonly editMode = input(false);
  readonly paramsChange = output<Record<string, string>>();

  protected readonly text = computed(() => this.item().params?.['text'] ?? '');
}
