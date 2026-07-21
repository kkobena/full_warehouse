import { Component, ElementRef, computed, input, output, signal, viewChild } from '@angular/core';

/**
 * Sélecteur de fichiers — remplace `p-fileupload` en mode « basic ».
 *
 * Volontairement limité au **choix** des fichiers : il n'envoie rien lui-même.
 * L'appelant récupère les `File` via `(filesSelected)` et pilote son propre `HttpClient`,
 * ce qui évite d'imposer une convention d'upload au Design System (cf. plan §3).
 *
 * @example
 * <app-file-upload
 *   accept=".csv,.xlsx"
 *   [maxSizeMb]="5"
 *   (filesSelected)="importer($event)"
 *   (rejected)="notification.error($event)"
 * />
 */
@Component({
  selector: 'app-file-upload',
  template: `
    <div class="d-flex align-items-center gap-2 flex-wrap">
      <input
        #fileInput
        type="file"
        class="d-none"
        [accept]="accept()"
        [multiple]="multiple()"
        [disabled]="disabled()"
        (change)="onFilesChosen($event)"
      />

      <button type="button" class="btn btn-outline-secondary d-inline-flex align-items-center gap-2" [disabled]="disabled()" (click)="open()">
        <i [class]="icon()" aria-hidden="true"></i>
        <span>{{ label() }}</span>
      </button>

      @if (selectedNames().length) {
        <span class="text-muted small">{{ summary() }}</span>
        <button type="button" class="btn btn-link btn-sm text-decoration-none" [disabled]="disabled()" (click)="clear()">Retirer</button>
      }
    </div>
  `,
})
export class FileUploadComponent {
  readonly label = input<string>('Choisir un fichier');

  readonly icon = input<string>('pi pi-upload');

  /** Filtre du sélecteur natif, ex. `.csv,.xlsx` ou `image/*`. */
  readonly accept = input<string>('');

  readonly multiple = input<boolean>(false);

  readonly disabled = input<boolean>(false);

  /** Taille maximale par fichier, en Mo. `0` désactive le contrôle. */
  readonly maxSizeMb = input<number>(0);

  /** Fichiers retenus, une fois le contrôle de taille passé. */
  readonly filesSelected = output<File[]>();

  /** Message expliquant pourquoi un fichier a été écarté. */
  readonly rejected = output<string>();

  private readonly fileInput = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');

  private readonly files = signal<File[]>([]);

  protected readonly selectedNames = computed(() => this.files().map(file => file.name));

  protected readonly summary = computed(() => {
    const names = this.selectedNames();
    return names.length === 1 ? names[0] : `${names.length} fichiers sélectionnés`;
  });

  protected open(): void {
    this.fileInput().nativeElement.click();
  }

  protected onFilesChosen(event: Event): void {
    const input = event.target as HTMLInputElement;
    const chosen = [...(input.files ?? [])];

    const limit = this.maxSizeMb();
    const tooLarge = limit > 0 ? chosen.filter(file => file.size > limit * 1024 * 1024) : [];
    const accepted = chosen.filter(file => !tooLarge.includes(file));

    for (const file of tooLarge) {
      this.rejected.emit(`« ${file.name} » dépasse la taille maximale de ${limit} Mo.`);
    }

    this.files.set(accepted);
    if (accepted.length) {
      this.filesSelected.emit(accepted);
    }

    // Réinitialise l'input : sans ça, resélectionner le même fichier n'émet aucun événement.
    input.value = '';
  }

  protected clear(): void {
    this.files.set([]);
    this.filesSelected.emit([]);
  }
}
