import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { IMagasin, TypeMagasin } from 'app/shared/model/magasin.model';
import { ButtonComponent, ToolbarComponent } from 'app/shared/ui';
import { ErrorService } from 'app/shared/error.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { MagasinService } from '../../data-access/services/magasin.service';
import { MagasinDetailComponent } from '../../ui/magasin-detail/magasin-detail.component';
import { UnitesGestionListComponent } from '../../ui/unites-gestion-list/unites-gestion-list.component';

/**
 * Fiche du magasin de l'utilisateur et, pour une officine, ses unités de gestion.
 *
 * Les unités ne s'affichent qu'à l'officine : c'est la seule qui en porte, et le serveur refuse
 * tout autre rattachement.
 */
@Component({
  selector: 'app-magasin-home',
  templateUrl: './magasin-home.component.html',
  styleUrl: './magasin-home.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ButtonComponent, ToolbarComponent, MagasinDetailComponent, UnitesGestionListComponent],
})
export class MagasinHomeComponent implements OnInit {
  protected readonly magasin = signal<IMagasin | undefined>(undefined);
  protected readonly unites = signal<IMagasin[]>([]);
  protected readonly chargementUnites = signal(false);
  protected readonly estOfficine = computed(() => this.magasin()?.typeMagasin === TypeMagasin.OFFICINE);

  private readonly magasinService = inject(MagasinService);
  private readonly router = inject(Router);
  private readonly notification = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  ngOnInit(): void {
    this.magasinService.getCurrenttUserMagasin().subscribe({
      next: res => {
        this.magasin.set(res.body ?? undefined);
        if (this.estOfficine()) {
          this.chargerLesUnites(res.body!.id!);
        }
      },
      error: (err: unknown) => this.notification.error(this.errorService.getErrorMessage(err, 'Chargement du magasin impossible.')),
    });
  }

  protected creerUnite(): void {
    void this.router.navigate(['/magasin', 'unite-gestion', 'new']);
  }

  protected modifierUnite(unite: IMagasin): void {
    void this.router.navigate(['/magasin', unite.id, 'edit']);
  }

  private chargerLesUnites(officineId: number): void {
    this.chargementUnites.set(true);
    this.magasinService.fetchAllDepots({ types: TypeMagasin.UNITE_GESTION }).subscribe({
      next: res => {
        this.unites.set((res.body ?? []).filter(unite => unite.parentId === officineId));
        this.chargementUnites.set(false);
      },
      error: (err: unknown) => {
        this.chargementUnites.set(false);
        this.notification.error(this.errorService.getErrorMessage(err, 'Chargement des unités de gestion impossible.'));
      },
    });
  }
}
