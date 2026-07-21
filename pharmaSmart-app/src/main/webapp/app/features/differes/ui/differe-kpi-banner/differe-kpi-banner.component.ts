import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { IDiffereSummary } from '../../data-access/models';
import { CommonModule } from "@angular/common";
import { KpiItemComponent, KpiStripComponent } from '../../../../shared/ui';

@Component({
  selector: 'app-differe-kpi-banner',
  imports: [CommonModule, KpiItemComponent, KpiStripComponent],
  templateUrl: './differe-kpi-banner.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './differe-kpi-banner.component.scss',
})
export class DiffereKpiBannerComponent {
  readonly summary = input<IDiffereSummary | null>(null);
  readonly loading = input(false);
}
