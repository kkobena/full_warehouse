import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { IUser } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';
import { ButtonComponent, CardComponent, SelectComponent, ToolbarComponent } from 'app/shared/ui';
import { CommonModule } from "@angular/common";
import { AlertErrorComponent } from "../../../shared/alert/alert-error.component";
import { IAuthority, NavApiService } from "../../../core/data-access/nav-api.service";

const userTemplate = {} as IUser;

const newUser: IUser = {
  langKey: 'fr',
  activated: true,
} as IUser;

@Component({
  selector: 'jhi-user-mgmt-update',
  templateUrl: './user-management-update.component.html',
  styleUrl: './user-management-update.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, CardComponent, ButtonComponent, ToolbarComponent, AlertErrorComponent, SelectComponent]
})
export default class UserManagementUpdateComponent implements OnInit {

  authorities = signal<IAuthority[]>([]);
  isSaving = signal(false);

  editForm = new FormGroup({
    id: new FormControl(userTemplate.id),
    login: new FormControl(userTemplate.login, {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(1),
        Validators.maxLength(50),
        Validators.pattern('^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$'),
      ],
    }),
    firstName: new FormControl(userTemplate.firstName, { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    lastName: new FormControl(userTemplate.lastName, { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    email: new FormControl(userTemplate.email, {
      validators: [Validators.minLength(5), Validators.maxLength(254), Validators.email],
    }),
    activated: new FormControl(userTemplate.activated, { nonNullable: true }),
    langKey: new FormControl(userTemplate.langKey, { nonNullable: true }),
    authorities: new FormControl<string | null>(null, { validators: [Validators.required] }),
  });
  protected isAdmin = false;
  private userService = inject(UserManagementService);
  private route = inject(ActivatedRoute);
  private readonly api = inject(NavApiService);

  ngOnInit(): void {
    this.route.data.subscribe(({ user }) => {
      if (user) {
        this.isAdmin = user.id === 3;
        // p-select attend une string (optionValue="name"), pas un tableau.
        // On extrait le premier rôle pour l'affichage, on le rewrappe en tableau à la sauvegarde.
        this.editForm.reset({
          ...user,
          authorities: user.authorities?.[0] ?? null,
        });
        // Le login n'est pas modifiable en édition
        this.editForm.get('login')?.disable();
      } else {
        this.editForm.reset({ ...newUser, authorities: null });
        this.editForm.get('login')?.enable();
      }
    });
    this.api.getAllRoles().subscribe(authorities => this.authorities.set(authorities));
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const raw = this.editForm.getRawValue();
    // Rewrap authorities en tableau pour le backend (IUser.authorities: string[])
    const user = {
      ...raw,
      authorities: raw.authorities ? [raw.authorities] : [],
    };
    if (user.id !== null) {
      this.userService.update(user).subscribe({
        next: () => this.onSaveSuccess(),
        error: () => this.onSaveError(),
      });
    } else {
      this.userService.create(user).subscribe({
        next: () => this.onSaveSuccess(),
        error: () => this.onSaveError(),
      });
    }
  }

  private onSaveSuccess(): void {
    this.isSaving.set(false);
    this.previousState();
  }

  private onSaveError(): void {
    this.isSaving.set(false);
  }
}
