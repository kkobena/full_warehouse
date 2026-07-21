import {ChangeDetectionStrategy, Component, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {NgbActiveModal, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {
  BadgeComponent,
  ButtonComponent,
  CardComponent,
  CheckboxComponent,
  DataTableComponent,
  InputNumberComponent,
  SkeletonComponent,
} from 'app/shared/ui';
import {SemoisService} from 'app/entities/semois/semois.service';
import {ISemoisClasseConfig} from 'app/shared/model/semois';
import {
  CLASSE_CRITICITE_INFO,
  ClasseCriticite,
  IClasseCriticiteInfo
} from 'app/shared/model/semois/classe-criticite.model';
import {NotificationService} from "../../../../shared/services/notification.service";
import {ErrorService} from "../../../../shared/error.service";

@Component({
  selector: 'app-semois-classe-config',
  templateUrl: './semois-classe-config.component.html',
  styleUrls: ['./semois-classe-config.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, BadgeComponent, DataTableComponent, ButtonComponent, NgbTooltip, InputNumberComponent, CheckboxComponent, SkeletonComponent, CardComponent],
})
export class SemoisClasseConfigComponent implements OnInit {
  readonly configs = signal<ISemoisClasseConfig[]>([]);
  readonly loading = signal(true);
  readonly savingClasse = signal<ClasseCriticite | null>(null);
  private readonly notificationService = inject(NotificationService);
  private readonly semoisService = inject(SemoisService);
  private readonly errorService = inject(ErrorService);
  private readonly activeModal = inject(NgbActiveModal);

  ngOnInit(): void {
    this.semoisService.getClasseConfigs().subscribe({
      next: res => {
        this.configs.set(res.body ?? []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  getClasseInfo(classe: ClasseCriticite): IClasseCriticiteInfo {
    return CLASSE_CRITICITE_INFO[classe];
  }

  save(config: ISemoisClasseConfig): void {
    this.savingClasse.set(config.classeCriticite);
    this.semoisService.updateClasseConfig(config.classeCriticite, config).subscribe({
      next: res => {
        const updated = res.body;
        if (updated) {
          this.configs.update(list => list.map(c => (c.classeCriticite === updated.classeCriticite ? updated : c)));
        }
        this.savingClasse.set(null);
        this.notificationService.success(`Classe ${config.classeCriticite} mise à jour`, 'Succès');

      },
      error: (err) => {
        this.savingClasse.set(null);
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');

      },
    });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  isSaving(classe: ClasseCriticite): boolean {
    return this.savingClasse() === classe;
  }
}
