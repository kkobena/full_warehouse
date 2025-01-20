import { inject, Injectable, signal, WritableSignal } from '@angular/core';
import { IUser, User } from '../../../core/user/user.model';
import { UserService } from '../../../core/user/user.service';
import { HttpResponse } from '@angular/common/http';
import { Authority } from '../../../shared/constants/authority.constants';

@Injectable({
  providedIn: 'root',
})
export class UserVendeurService {
  userService = inject(UserService);
  vendeur: WritableSignal<IUser> = signal<IUser>(null);
  vendeurs: WritableSignal<IUser[]> = signal<IUser[]>([]); //Authority.SALES
  constructor() {
    this.loadAllUsers();
  }

  setVendeur(user: IUser): void {
    this.vendeur.set(user);
  }

  loadAllUsers(): void {
    //  if (this.vendeurs() && this.vendeurs().length === 0) {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => {
      this.vendeurs.set(
        res.body?.filter(u => u.authorities.includes(Authority.ROLE_VENDEUR) || u.authorities.includes(Authority.ROLE_CAISSE)),
      );
    });
    //  }
  }
}
