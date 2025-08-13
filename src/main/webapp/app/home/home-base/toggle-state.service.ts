import { Injectable, signal, WritableSignal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ToggleStateService {
  toggleState: WritableSignal<boolean> = signal<boolean>(false);

  update(value: boolean): void {
    this.toggleState.set(value);
  }
}
