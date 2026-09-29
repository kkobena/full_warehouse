import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InputComponent } from 'app/shared/ui';
import { DashboardItem, WidgetComponent } from '../../models/dashboard.model';

/** Intertitre pleine largeur pour regrouper les tuiles par thème. */
@Component({
  selector: 'app-section-title-widget',
  imports: [FormsModule, InputComponent],
  template: `
    @if (editMode()) {
      <app-input
        class="w-100"
        size="large"
        ariaLabel="Titre de la section"
        placeholder="Titre de la section"
        [ngModel]="text()"
        (ngModelChange)="paramsChange.emit({ text: $event ?? '' })"
      />
    } @else {
      <h2 class="section-title">{{ text() || 'Section' }}</h2>
    }
  `,
  styles: `
    :host {
      display: flex;
      align-items: flex-end;
      height: 100%;
      padding: 0 0.25rem;
    }
    .section-title {
      width: 100%;
      margin: 0;
      padding-bottom: 0.25rem;
      font-size: 1.15rem;
      font-weight: 600;
      border-bottom: 2px solid var(--bs-primary);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionTitleWidgetComponent implements WidgetComponent {
  readonly item = input.required<DashboardItem>();
  readonly editMode = input(false);
  readonly paramsChange = output<Record<string, string>>();

  protected readonly text = computed(() => this.item().params?.['text'] ?? '');
}
