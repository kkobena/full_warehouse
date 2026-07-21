import { AfterViewInit, Component, ElementRef, inject, viewChild, ChangeDetectionStrategy } from "@angular/core";
import {
  AbstractControl,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { NavApiService } from "app/core/data-access/nav-api.service";
import { NotificationService } from "app/shared/services/notification.service";
import { ButtonComponent, CardComponent, KeyFilterDirective } from "app/shared/ui";

const ROLE_PATTERN = /^[A-Z][A-Z0-9_]*$/;

@Component({
  selector: "app-role-form",
  templateUrl: "./role-form.component.html",
  styleUrl: "./role-form.component.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, ReactiveFormsModule, ButtonComponent, CardComponent, KeyFilterDirective]
})
export class RoleFormComponent implements AfterViewInit {
  /** Existing role names — set by parent for uniqueness check. */
  existingNames: string[] = [];

  protected isSaving = false;

  private readonly activeModal = inject(NgbActiveModal);
  private readonly navApi = inject(NavApiService);
  private readonly notif = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  private readonly suffixInput = viewChild<ElementRef>("suffixInput");

  protected readonly editForm = this.fb.group({
    suffix: ["", [Validators.required, Validators.pattern(ROLE_PATTERN), this.duplicateValidator.bind(this)]],
    libelle: ["",[Validators.required]],
  });

  ngAfterViewInit(): void {
    setTimeout(() => this.suffixInput()?.nativeElement.focus(), 100);
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    if (this.editForm.invalid) return;
    const suffix = this.editForm.get("suffix")!.value!.trim().toUpperCase();
    const name = `ROLE_${suffix}`;
    const libelle = this.editForm.get("libelle")!.value?.trim() || name;
    this.isSaving = true;
    this.navApi.createRole(name, libelle).subscribe({
      next: () => {
        this.isSaving = false;
        this.activeModal.close({ name, libelle });
      },
      error: () => {
        this.isSaving = false;
        this.notif.error("Erreur lors de la création du rôle.");
      }
    });
  }

  protected toUpperSuffix(event: Event): void {
    const input = event.target as HTMLInputElement;
    const upper = input.value.toUpperCase();
    if (input.value !== upper) {
      const pos = input.selectionStart ?? upper.length;
      input.value = upper;
      input.setSelectionRange(pos, pos);
    }
    this.editForm.get('suffix')!.setValue(upper, { emitEvent: true });
  }

  private duplicateValidator(control: AbstractControl): ValidationErrors | null {
    const val = (control.value ?? "").trim().toUpperCase();
    return this.existingNames.includes(`ROLE_${val}`) ? { duplicate: true } : null;
  }
}
