import { Injectable, signal, WritableSignal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class CommonService {
  categorieTiersPayant: WritableSignal<string> = signal<string>(null);
  categorie: WritableSignal<string> = signal<string>(null);

  setCategorieTiersPayant(categorieTiersPayant: string): void {
    this.categorieTiersPayant.set(categorieTiersPayant);
  }

  setCategorie(categorie: string): void {
    this.categorie.set(categorie);
  }
}
