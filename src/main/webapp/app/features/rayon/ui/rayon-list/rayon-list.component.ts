import { Component, input, output } from '@angular/core';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { IRayon, TYPE_ZONE_SEVERITY } from '../../models/rayon.model';

@Component({
  selector: 'app-rayon-list',
  templateUrl: './rayon-list.component.html',
  styleUrl: './rayon-list.component.scss',
  imports: [TableModule, ButtonModule, TagModule, TooltipModule],
})
export class RayonListComponent {
  readonly rayons = input<IRayon[]>([]);
  readonly totalItems = input(0);
  readonly loading = input(false);
  readonly rows = input(20);
  readonly first = input(0);
  readonly selectedRayon = input<IRayon | null>(null);

  readonly rayonSelected = output<IRayon>();
  readonly lazyLoad = output<TableLazyLoadEvent>();
  readonly editRequested = output<IRayon>();
  readonly deleteRequested = output<IRayon>();
  readonly inventaireRequested = output<IRayon>();

  protected readonly typeZoneSeverity = TYPE_ZONE_SEVERITY;

  protected onRowClick(rayon: IRayon): void {
    this.rayonSelected.emit(rayon);
  }

  protected onViewDetail(rayon: IRayon, event: Event): void {
    event.stopPropagation();
    this.rayonSelected.emit(rayon);
  }

  protected onEdit(rayon: IRayon, event: Event): void {
    event.stopPropagation();
    this.editRequested.emit(rayon);
  }

  protected onDelete(rayon: IRayon, event: Event): void {
    event.stopPropagation();
    this.deleteRequested.emit(rayon);
  }

  protected onLancerInventaire(rayon: IRayon, event: Event): void {
    event.stopPropagation();
    this.inventaireRequested.emit(rayon);
  }

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    this.lazyLoad.emit(event);
  }

  protected isSelected(rayon: IRayon): boolean {
    return this.selectedRayon()?.id === rayon.id;
  }

  protected typeZoneSev(typeZone?: string): string {
    if (!typeZone) return 'secondary';
    return this.typeZoneSeverity[typeZone as keyof typeof this.typeZoneSeverity] ?? 'secondary';
  }
}
