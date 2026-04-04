import { Component, input } from '@angular/core';
import { IDiffereSummary } from '../../data-access/models';
import { CommonModule } from "@angular/common";

@Component({
  selector: 'app-differe-kpi-banner',
  imports: [CommonModule],
  templateUrl: './differe-kpi-banner.component.html',
  styleUrl: './differe-kpi-banner.component.scss',
})
export class DiffereKpiBannerComponent {
  readonly summary = input<IDiffereSummary | null>(null);
  readonly loading = input(false);
}
