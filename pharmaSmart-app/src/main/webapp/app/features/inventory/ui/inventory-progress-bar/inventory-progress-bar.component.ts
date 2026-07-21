import {Component, input, ChangeDetectionStrategy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {InventoryProgressRecord} from '../../models';

@Component({
  selector: 'app-inventory-progress-bar',
  imports: [CommonModule],
  template: `
    @if (progress()) {
      <div class="inventory-progress-container">
        <div class="progress mb-1" role="progressbar" [attr.aria-valuenow]="progress()!.progressPercent" aria-valuemin="0" aria-valuemax="100">
          <div class="progress-bar" [style.width.%]="progress()!.progressPercent">{{ progress()!.progressPercent }}%</div>
        </div>
        <div class="progress-text text-muted small">
          <span>{{ progress()!.updatedLines }} / {{ progress()!.totalLines }} lignes saisies</span>
          @if (progress()!.withGap > 0) {
            <span class="ms-3 text-warning">
              <i class="pi pi-exclamation-triangle"></i>
              {{ progress()!.withGap }} écart(s)
            </span>
          }
        </div>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './inventory-progress-bar.component.scss',
})
export class InventoryProgressBarComponent {
  readonly progress = input<InventoryProgressRecord | null>(null);
}
