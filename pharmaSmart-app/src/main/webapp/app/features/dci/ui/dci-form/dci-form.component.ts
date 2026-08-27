import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  signal,
  viewChild
} from '@angular/core';
import {form, FormField, FormRoot, maxLength, required, submit} from '@angular/forms/signals';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {firstValueFrom} from 'rxjs';
import {ButtonComponent, CardComponent} from '../../../../shared/ui';
import {NotificationService} from '../../../../shared/services/notification.service';
import {ErrorService} from '../../../../shared/error.service';
import {DciApiService} from '../../data-access/services/dci-api.service';
import {DciFormModel, EMPTY_DCI_FORM, IDci} from '../../models/dci.model';
import {FormsModule} from "@angular/forms";

/**
 * Saisie d'une DCI, en modale.
 *
 * Bâti sur les *signal forms* : le modèle est un simple signal, `form()` en dérive
 * l'arbre de champs, et les règles se déclarent dans le schéma plutôt que sur
 * chaque contrôle. Le template lie les champs par `[formField]`.
 *
 * Le CODE est volontairement facultatif : laissé vide, il est dérivé du libellé
 * côté serveur. C'est la même règle que pour l'import CSV, et elle n'est donc
 * énoncée qu'à un seul endroit.
 */
@Component({
  selector: 'app-dci-form',
  templateUrl: './dci-form.component.html',
  styleUrl: './dci-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormField, FormRoot, ButtonComponent, CardComponent, FormsModule],
})
export class DciFormComponent implements AfterViewInit {
  /** Renseigné par l'appelant avant ouverture ; absent en création. */
  dci?: IDci;
  header = '';

  protected readonly isSaving = signal(false);

  protected readonly model = signal<DciFormModel>({...EMPTY_DCI_FORM});

  protected readonly dciForm = form(this.model, path => {
    required(path.libelle, {message: 'Le libellé est obligatoire.'});
    maxLength(path.libelle, 255, {message: 'Le libellé ne peut dépasser 255 caractères.'});
    // Le code n'est pas requis : vide, il sera dérivé du libellé.
    maxLength(path.code, 20, {message: 'Le code ne peut dépasser 20 caractères.'});
  });

  private readonly libelleInput = viewChild.required<ElementRef<HTMLInputElement>>('libelleInput');

  private readonly api = inject(DciApiService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notification = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  ngAfterViewInit(): void {
    setTimeout(() => this.libelleInput().nativeElement.focus(), 100);
  }

  /** Appelé par l'ouvrant après instanciation, avant affichage. */
  init(dci?: IDci): void {
    this.dci = dci;
    this.header = dci?.id ? 'Modifier la DCI' : 'Nouvelle DCI';
    this.model.set({
      id: dci?.id ?? null,
      code: dci?.code ?? '',
      libelle: dci?.libelle ?? '',
    });
  }

  protected async save(): Promise<void> {
    await submit(this.dciForm, async (): Promise<void> => {
      this.isSaving.set(true);
      const valeur = this.model();
      const payload: IDci = {
        id: valeur.id ?? undefined,
        code: valeur.code?.trim() ? valeur.code.trim() : undefined,
        libelle: valeur.libelle.trim(),
      };

      try {
        const reponse = payload.id
          ? await firstValueFrom(this.api.update(payload))
          : await firstValueFrom(this.api.create(payload));
        this.activeModal.close(reponse.body);
      } catch (err: unknown) {
        this.notification.error(this.errorService.getErrorMessage(err, 'Enregistrement impossible.'));
      } finally {
        this.isSaving.set(false);
      }
    });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
