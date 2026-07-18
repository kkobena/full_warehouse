import {Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {Button} from 'primeng/button';
import {Select} from 'primeng/select';
import {InputText} from 'primeng/inputtext';
import {DatePicker} from 'primeng/datepicker';
import {Tooltip} from 'primeng/tooltip';
import {StorageService} from '../../../../entities/storage/storage.service';
import {Storage} from '../../../../entities/storage/storage.model';
import {IUser} from '../../../../admin/user-management/user-management.model';
import {
  UserManagementService
} from '../../../../admin/user-management/service/user-management.service';
import {CRITERES, FREQUENCES, IPlanningInventaireTournant} from '../../models';
import {PlanningTournantApiService} from '../../data-access/services/planning-tournant-api.service';

@Component({
  selector: 'app-planning-tournant-modal',
  imports: [CommonModule, ReactiveFormsModule, Button, Select, InputText, DatePicker, Tooltip],
  templateUrl: './planning-tournant-modal.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './planning-tournant-modal.component.scss',
})
export class PlanningTournantModalComponent implements OnInit {
  readonly activeModal = inject(NgbActiveModal);
  /** Passer le planning à éditer pour le mode édition (set depuis le parent via componentInstance) */
  planning: IPlanningInventaireTournant | null = null;

  form!: FormGroup;
  storages = signal<Storage[]>([]);
  users = signal<IUser[]>([]);
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  readonly frequences = FREQUENCES;
  readonly criteres = CRITERES;

  private readonly fb = inject(FormBuilder);
  private readonly api = inject(PlanningTournantApiService);
  private readonly storageService = inject(StorageService);
  private readonly userService = inject(UserManagementService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.buildForm();
    this.loadStorages();
    this.loadUsers();
    if (this.planning) {
      this.patchForm(this.planning);
    } else {
      // En création : recalcule la date à chaque changement de fréquence
      this.form.get('frequence')!.valueChanges
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(freq => {
          this.form.get('prochaineExecution')!.setValue(this.computeNextDate(freq), {emitEvent: false});
        });
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);

    const value = this.form.getRawValue();
    const record: IPlanningInventaireTournant = {
      libelle: value.libelle,
      frequence: value.frequence,
      critere: value.critere,
      storageId: value.storageId ?? undefined,
      userId: value.userId ?? undefined,
      prochaineExecution: this.formatDate(value.prochaineExecution),
      actif: true,
    };

    const p = this.planning;
    const obs$ = p?.id
      ? this.api.update(p.id, {...record, id: p.id})
      : this.api.create(record);

    obs$.subscribe({
      next: result => {
        this.loading.set(false);
        this.activeModal.close(result);
      },
      error: err => {
        this.loading.set(false);
        this.errorMessage.set(err?.error?.detail ?? 'Une erreur est survenue');
      },
    });
  }

  dismiss(): void {
    this.activeModal.dismiss();
  }

  getUserLabel(user: IUser): string {
    const name = [user.firstName, user.lastName].filter(Boolean).join(' ');
    return name || user.login || '';
  }

  private buildForm(): void {
    this.form = this.fb.group({
      libelle: ['', [Validators.required, Validators.maxLength(200)]],
      frequence: ['HEBDO', Validators.required],
      critere: ['RAYON', Validators.required],
      storageId: [null],
      userId: [null],
      prochaineExecution: [this.computeNextDate('HEBDO'), Validators.required],
    });
  }

  private computeNextDate(frequence: string): Date {
    const d = new Date();
    switch (frequence) {
      case 'QUOTIDIEN':
        d.setDate(d.getDate() + 1);
        break;
      case 'HEBDO':
        d.setDate(d.getDate() + 7);
        break;
      case 'MENSUEL':
        d.setMonth(d.getMonth() + 1);
        break;
      case 'TRIMESTRIEL':
        d.setMonth(d.getMonth() + 3);
        break;
    }
    return d;
  }

  private patchForm(p: IPlanningInventaireTournant): void {
    this.form.patchValue({
      libelle: p.libelle,
      frequence: p.frequence,
      critere: p.critere,
      storageId: p.storageId,
      userId: p.userId,
      prochaineExecution: p.prochaineExecution ? new Date(p.prochaineExecution) : new Date(),
    });
  }

  private loadStorages(): void {
    this.storageService.fetchUserStorages().subscribe({
      next: res => this.storages.set(res.body ?? []),
      error: () => {
      },
    });
  }

  private loadUsers(): void {
    this.userService.query({page: 0, size: 200, sort: ['lastName,asc']}).subscribe({
      next: res => this.users.set((res.body ?? []).filter(u => u.activated)),
      error: () => {
      },
    });
  }

  private formatDate(d: Date | string | null): string {
    if (!d) {
      return '';
    }
    if (typeof d === 'string') {
      return d;
    }
    return d.toISOString().substring(0, 10);
  }
}
