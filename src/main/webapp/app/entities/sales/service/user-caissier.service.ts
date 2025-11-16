import { Injectable, signal, WritableSignal } from '@angular/core';
import { IUser } from '../../../core/user/user.model';

@Injectable({
  providedIn: 'root',
})
export class UserCaissierService {
  caissier: WritableSignal<IUser> = signal<IUser>(null);

  setCaissier(user: IUser): void {
    this.caissier.set(user);
  }
}
