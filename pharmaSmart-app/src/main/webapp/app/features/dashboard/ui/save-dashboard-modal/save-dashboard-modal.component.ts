import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { ButtonComponent, InputComponent, SelectComponent } from 'app/shared/ui';
import { DashboardScope } from 'app/shared/model/dashboard-layout.model';

export interface SaveDashboardResult {
  name: string;
  description: string;
  scope: DashboardScope;
}

/** « Enregistrer sous » : nom, description et visibilité d'un nouveau dashboard. */
@Component({
  selector: 'app-save-dashboard-modal',
  imports: [FormsModule, ButtonComponent, InputComponent, SelectComponent],
  template: `
    <div class="modal-header">
      <h2 class="modal-title fs-5"><i class="pi pi-save me-2" aria-hidden="true"></i>Enregistrer le tableau de bord</h2>
      <button type="button" class="btn-close" aria-label="Fermer" (click)="activeModal.dismiss()"></button>
    </div>

    <form class="modal-body d-flex flex-column gap-3" (submit)="$event.preventDefault(); save()">
      <div>
        <div class="form-label">Nom <span class="text-danger">*</span></div>
        <app-input
          ariaLabel="Nom du tableau de bord"
          placeholder="Mon tableau de bord"
          [maxlength]="255"
          [required]="true"
          [ngModel]="name()"
          (ngModelChange)="name.set($event ?? '')"
          [ngModelOptions]="{ standalone: true }"
        />
      </div>
      <div>
        <div class="form-label">Visibilité</div>
        <app-select
          ariaLabel="Visibilité"
          [items]="scopeOptions"
          bindLabel="label"
          bindValue="value"
          [searchable]="false"
          [ngModel]="scope()"
          (ngModelChange)="$event && scope.set($event)"
          [ngModelOptions]="{ standalone: true }"
        />
        <div class="form-text">Un tableau de bord public peut être ouvert et dupliqué par les autres utilisateurs.</div>
      </div>
      <div>
        <label for="dashboard-description" class="form-label">Description</label>
        <textarea
          id="dashboard-description"
          class="form-control"
          rows="2"
          [value]="description()"
          (input)="description.set($any($event.target).value)"
        ></textarea>
      </div>
    </form>

    <div class="modal-footer">
      <app-button label="Annuler" severity="secondary" [text]="true" (clicked)="activeModal.dismiss()" />
      <app-button label="Enregistrer" icon="pi pi-check" [disabled]="!name().trim()" (clicked)="save()" />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SaveDashboardModalComponent {
  readonly activeModal = inject(NgbActiveModal);

  readonly name = signal('');
  readonly description = signal('');
  readonly scope = signal<DashboardScope>(DashboardScope.PRIVATE);

  protected readonly scopeOptions = [
    { label: 'Privé — visible par moi seul', value: DashboardScope.PRIVATE },
    { label: 'Public — visible par tous les utilisateurs', value: DashboardScope.PUBLIC },
  ];

  save(): void {
    const name = this.name().trim();
    if (!name) {
      return;
    }
    this.activeModal.close({ name, description: this.description().trim(), scope: this.scope() } satisfies SaveDashboardResult);
  }
}
