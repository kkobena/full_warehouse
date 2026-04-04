import { computed, inject, Injectable, signal } from '@angular/core';
import { MagasinService } from '../../entities/magasin/magasin.service';

@Injectable({ providedIn: 'root' })
export class OfficineContextService {
  private readonly magasinService = inject(MagasinService);

  readonly officineName = signal<string>('');
  readonly officineFullName = signal<string | null>(null);

  /** "Bienvenue à la GRANDE PHARMACIE..." ou fallback "Bienvenue !" */
  readonly welcomeMessage = computed(() => {
    const name = this.officineFullName() ?? this.officineName();
    return name ? `Bienvenue à la ${name}` : 'Bienvenue !';
  });

  private loaded = false;

  load(): void {
    if (this.loaded) return;
    this.loaded = true;

    this.magasinService.getCurrenttUserMagasin().subscribe({
      next: res => {
        if (res.body) {
          this.officineName.set(res.body.name ?? '');
          this.officineFullName.set(res.body.fullName ?? null);
        }
      },
      error: () => {
        this.loaded = false;
      },
    });
  }
}

