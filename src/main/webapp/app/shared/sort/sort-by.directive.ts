import { Directive, HostListener, contentChild, effect, input, inject } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faSort, faSortDown, faSortUp, IconDefinition } from '@fortawesome/free-solid-svg-icons';

import { SortDirective } from './sort.directive';

@Directive({
  standalone: true,
  selector: '[jhiSortBy]',
})
export class SortByDirective {
  private sort = inject(SortDirective, { host: true });

  readonly jhiSortBy = input.required<string>();

  iconComponent = contentChild(FaIconComponent);

  protected sortIcon = faSort;
  protected sortAscIcon = faSortUp;
  protected sortDescIcon = faSortDown;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
    effect(() => {
      if (this.iconComponent()) {
        let icon: IconDefinition = this.sortIcon;
        const { predicate, order } = this.sort.sortState()();
        if (predicate === this.jhiSortBy() && order !== undefined) {
          icon = order === 'asc' ? this.sortAscIcon : this.sortDescIcon;
        }
        this.iconComponent()!.icon = icon.iconName;
        this.iconComponent()!.render();
      }
    });
  }

  @HostListener('click')
  onClick(): void {
    if (this.iconComponent()) {
      this.sort.sort(this.jhiSortBy());
    }
  }
}
