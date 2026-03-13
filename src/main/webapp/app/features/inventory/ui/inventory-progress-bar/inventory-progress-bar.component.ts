import {Component, input} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ProgressBar} from 'primeng/progressbar';
import {InventoryProgressRecord} from '../../models';

@Component({
  selector: 'app-inventory-progress-bar',
  imports: [CommonModule, ProgressBar],
  template: `
    @if (progress()) {
      <div class="inventory-progress-container">
        <p-progressbar
          [value]="progress()!.progressPercent"
          [showValue]="true"
          class="mb-1"
        />
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
  styleUrl: './inventory-progress-bar.component.scss',
})
export class InventoryProgressBarComponent {
  readonly progress = input<InventoryProgressRecord | null>(null);
}
