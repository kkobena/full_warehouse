import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { IUser } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

const userTemplate = {} as IUser;

const newUser: IUser = {
  langKey: 'fr',
  activated: true,
} as IUser;

@Component({
  selector: 'jhi-user-mgmt-update',
  templateUrl: './user-management-update.component.html',
  imports: [WarehouseCommonModule, FormsModule, ReactiveFormsModule, PanelModule, ButtonModule, InputTextModule],
})
export default class UserManagementUpdateComponent implements OnInit {
  authorities = signal<string[]>([]);
  isSaving = false;
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
    firstName: new FormControl(userTemplate.firstName, {
      nonNullable: true,
      validators: [Validators.maxLength(50), Validators.required],
    }),
    lastName: new FormControl(userTemplate.lastName, {
      nonNullable: true,
      validators: [Validators.maxLength(100), Validators.required],
    }),
    email: new FormControl(userTemplate.email, {
      validators: [Validators.minLength(5), Validators.maxLength(254), Validators.email],
    }),
    authorities: new FormControl(userTemplate.authorities, {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });
  protected isAdmin = false;
  private userService = inject(UserManagementService);
  private route = inject(ActivatedRoute);

  ngOnInit(): void {
    this.route.data.subscribe(({ user }) => {
      if (user) {
        this.isAdmin = user.id === 3;
        this.editForm.reset(user);
      } else {
        this.editForm.reset(newUser);
      }
    });
    this.userService.authorities().subscribe(authorities => this.authorities.set(authorities));
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const user = this.editForm.getRawValue();
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
    this.isSaving = false;
    this.previousState();
  }

  private onSaveError(): void {
    this.isSaving = false;
  }
}
