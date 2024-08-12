import { Injectable, signal, WritableSignal } from '@angular/core';
import { ICommande } from '../../shared/model/commande.model';

@Injectable({
  providedIn: 'root',
})
export class CommandCommonService {
  currentCommand: WritableSignal<ICommande> = signal<ICommande>(null);

  constructor() {}

  updateCommand(commande: ICommande): void {
    this.currentCommand.set(commande);
  }
}
