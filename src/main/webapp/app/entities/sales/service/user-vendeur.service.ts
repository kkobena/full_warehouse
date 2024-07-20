import { Injectable, signal, WritableSignal } from '@angular/core';
import { IUser } from '../../../core/user/user.model';

@Injectable({
  providedIn: 'root',
})
export class UserVendeurService {
  vendeur: WritableSignal<IUser> = signal<IUser>(null);

  constructor() {}

  setVendeur(user: IUser): void {
    this.vendeur.set(user);
  }
}
