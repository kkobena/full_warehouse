import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Barre contextuelle regroupant les actions appliquées à une sélection. */
@Component({
  selector: 'app-bulk-action-bar',
  template: `
    <div [attr.aria-label]="ariaLabel()" class="app-bulk-action-bar" role="region">
      <span aria-live="polite" class="app-bulk-action-bar__count">
        {{ countLabel() }}
      </span>
      <div class="app-bulk-action-bar__actions">
        <ng-content />
      </div>
    </div>
  `,
  styleUrl: './bulk-action-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BulkActionBarComponent {
  readonly count = input.required<number>();
  readonly label = input('élément sélectionné');
  readonly labelPlural = input('éléments sélectionnés');
  readonly ariaLabel = input('Actions sur la sélection');

  protected readonly countLabel = computed(() => `${this.count()} ${this.count() === 1 ? this.label() : this.labelPlural()}`);
}
