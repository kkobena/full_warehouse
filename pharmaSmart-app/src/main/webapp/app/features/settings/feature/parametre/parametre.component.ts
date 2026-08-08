import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { ConfigurationService } from '../../../../shared/configuration.service';
import { Configuration, IConfiguration } from '../../../../shared/model/configuration.model';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FormParamettreComponent } from './form-paramettre/form-paramettre.component';
import { Router } from '@angular/router';
import {
  AppTableLazyLoadEvent,
  ButtonComponent,
  DataTableComponent,
  IconFieldComponent,
  ToolbarComponent,
} from '../../../../shared/ui';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';

/** Paramètre non modifiable depuis cet écran : il est piloté par l'assistant de configuration. */
const PARAM_GESTION_STOCK = 'APP_GESTION_STOCK';

@Component({
  selector: 'app-parametre',
  imports: [ButtonComponent, ToolbarComponent, IconFieldComponent, DataTableComponent],
  templateUrl: './parametre.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./parametre.component.scss'],
})
export class ParametreComponent implements OnInit {
  // Signaux plutôt que champs simples : la table ne se rafraîchissait qu'à la première
  // interaction sur la page, la réponse HTTP arrivant hors du cycle de détection.
  protected readonly apps = signal<IConfiguration[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly loading = signal(false);

  // Liés en deux sens à `app-data-table` : la table les met à jour avant d'émettre
  // `(onLazyLoad)`, on n'a donc qu'à relire l'offset courant pour recharger.
  protected readonly first = signal(0);
  protected readonly rows = signal(ITEMS_PER_PAGE);
  protected readonly rowsPerPageOptions = [15, 25, 50, 100];

  protected readonly search = signal('');
  protected readonly emptyMessage = computed(() =>
    this.search() ? `Aucun paramètre ne correspond à « ${this.search()} »` : 'Aucun paramètre',
  );

  private readonly configurationService = inject(ConfigurationService);
  private readonly modalService = inject(NgbModal);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.loadPage();
  }

  protected onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value.trim());
    // Un nouveau filtre repart de la première page, sinon l'offset courant peut dépasser le total.
    this.first.set(0);
    this.loadPage();
  }

  protected lazyLoading(_event: AppTableLazyLoadEvent): void {
    this.loadPage();
  }

  protected isEditable(app: IConfiguration): boolean {
    return app.name !== PARAM_GESTION_STOCK;
  }

  protected isActive(app: IConfiguration): boolean {
    return Number(app.value) !== 0;
  }

  protected setActive(app: Configuration, isActivated: boolean): void {
    this.configurationService.update({ ...app, value: isActivated ? '1' : '0' }).subscribe(() => this.loadPage());
  }

  protected onEdit(entity: IConfiguration): void {
    if (entity.name === 'APP_MODEL_REAPPRO') {
      this.router.navigate(['/semois/model-config']);
      return;
    }
    showCommonModal(
      this.modalService,
      FormParamettreComponent,
      {
        entity,
        header: 'Modification de [ ' + entity.name + ' ]',
      },
      () => this.loadPage(),
      'lg',
    );
  }

  protected loadPage(): void {
    const size = this.rows();
    const page = size > 0 ? Math.floor(this.first() / size) : 0;
    this.loading.set(true);
    this.configurationService.query({ page, size, search: this.search() }).subscribe({
      next: res => {
        this.totalItems.set(Number(res.headers.get('X-Total-Count')) || 0);
        this.apps.set(res.body ?? []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
