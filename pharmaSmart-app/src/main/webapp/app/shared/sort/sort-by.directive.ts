import { contentChild, Directive, effect, HostListener, inject, input } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faSort, faSortDown, faSortUp, IconDefinition } from '@fortawesome/free-solid-svg-icons';

import { SortDirective } from './sort.directive';

@Directive({
  selector: '[jhiSortBy]',
})
export class SortByDirective {
  readonly jhiSortBy = input.required<string>();

  iconComponent = contentChild(FaIconComponent);

  protected sortIcon = faSort;
  protected sortAscIcon = faSortUp;
  protected sortDescIcon = faSortDown;

  private readonly sort = inject(SortDirective, { host: true });

  constructor() {
    effect(() => {
      const icone = this.iconComponent();
      if (!icone) {
        return;
      }
      let icon: IconDefinition = this.sortIcon;
      const { predicate, order } = this.sort.sortState();
      if (predicate === this.jhiSortBy() && order !== undefined) {
        icon = order === 'asc' ? this.sortAscIcon : this.sortDescIcon;
      }
      // `icon` est un model() depuis angular-fontawesome 5 : il s'écrit, là où l'ancienne
      // affectation d'@Input ne compilait plus. La colonne triée affichait donc toujours la
      // même icône neutre.
      icone.icon.set(icon);
    });
  }

  @HostListener('click')
  onClick(): void {
    if (this.iconComponent()) {
      this.sort.sort(this.jhiSortBy());
    }
  }
}
